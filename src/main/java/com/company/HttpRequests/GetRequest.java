package com.company.HttpRequests;

import com.company.DAO.FileDao;
import com.company.DAO.Value;
import com.company.util.Query;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Log4j
public class GetRequest extends KVHttpRequests {


    public GetRequest(FileDao fileDao, int currentPort,
                      KVHttpRequests kvUpsertHttpRequests, KVHttpRequests kvDeleteHttpRequests) {
        super(fileDao, currentPort);
        this.kvUpsertHttpRequests = kvUpsertHttpRequests;
        this.kvDeleteHttpRequests = kvDeleteHttpRequests;
    }

    private final KVHttpRequests kvUpsertHttpRequests;

    private final KVHttpRequests kvDeleteHttpRequests;

    @Override
    public Value getRequestFromCurrentNode(Query query) {
        return fileDao.getById(query.getId());
    }

    @Override
    public HttpRequest buildHttpRequest(int port, Query query) {
        String uri = getUri(port, query);
        try {
            return HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .version(HttpClient.Version.HTTP_1_1)//specify this parameter as jersey automatically
                    //convert it to another version of http and can't catch PUT request with body
                    //due to incompatibility
                    .GET()
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response.Status getSuccessStatusCode() {
        return Response.Status.OK;
    }


    /**
     * @param query     to get ack parameter
     * @param responses completableFuture responses from other storages
     * @return response success if it gets ack amount of queries otherwise response with error
     * <p>
     * method get responses from other nodes and check the latest update (by modifiedTime header which is sent in requests)
     * after that it checks do another nodes has the same value or the value is absent.
     * If latest value is marked as "deleted", it sends request to delete this value to other nodes (that do not have this value)
     * If latest value is exist, it sends request to other nodes (that do not have this value) to update them
     */
    @Override
    public Response getResultResponse(Query query, List<CompletableFuture<HttpResponse<byte[]>>> responses) {
        Map<Value.Status, Integer> counters = initCounters();
        Value correctValue = new Value(null, 0L, Value.Status.ABSENT);
        var valueOfCurrentNode = getRequestFromCurrentNode(query);
        if (valueOfCurrentNode.getStatus() == Value.Status.EXISTING) {
            correctValue.updateValue(valueOfCurrentNode.getBytesOfFile(), valueOfCurrentNode.getLastModifiedTime(),
                    valueOfCurrentNode.getStatus());
        }
        Map<Integer, Value> values = new HashMap<>();
        values.put(currentPort, valueOfCurrentNode);
        values.putAll(countResponsesFromExternalNodes(responses, counters, query, correctValue));
        if (correctValue.getStatus() == valueOfCurrentNode.getStatus()) {
            counters.merge(valueOfCurrentNode.getStatus(), 1, Integer::sum);
        }
        List<CompletableFuture<HttpResponse<byte[]>>> remainingResponses =
                sendRemainingQueries(values, correctValue, query, counters);
        countResponsesFromExternalNodes(remainingResponses, counters, query, new Value(null, 0L, Value.Status.ABSENT));
        if (correctValue.getBytesOfFile() != null && counters.get(Value.Status.EXISTING) >= query.getAck()) {
            return Response
                    .status(getSuccessStatusCode().getStatusCode())
                    .entity(correctValue.getBytesOfFile())
                    .build();
        }
        if (counters.get(Value.Status.ABSENT) == query.getAck() || counters.get(Value.Status.DELETED) >= 1) {
            return Response
                    .status(404, "Not Found")
                    .build();
        }
        return Response
                .status(504, "Not Enough Replicas")
                .build();

    }

    /**
     * @param responses    from other nodes
     * @param counters     counters of success/not success answers
     * @param query        to get ack parameter
     * @param correctValue the latest updated value from node
     * @return all values from other nodes
     */
    private Map<Integer, Value> countResponsesFromExternalNodes(List<CompletableFuture<HttpResponse<byte[]>>> responses,
                                                                Map<Value.Status, Integer> counters,
                                                                Query query, Value correctValue) {
        Map<Integer, Value> values = new HashMap<>();
        for (var responseFuture : responses) {
            if (counters.get(Value.Status.EXISTING) == query.getAck()) {
                break;
            }
            try {
                HttpResponse<byte[]> response = responseFuture.get();
                Long currentTimeStamp = null;
                if (response != null) {
                    Value.Status status = Value.Status.valueOf(response.headers().firstValue("status").get());
                    if (response.headers().firstValue("modifiedTime").isPresent()) {
                        currentTimeStamp = Long.parseLong(response.headers().firstValue("modifiedTime").get());
                        if (currentTimeStamp > correctValue.getLastModifiedTime()) {
                            correctValue.updateValue(response.body(), currentTimeStamp, status);
                        }
                    }
                    counters.merge(status, 1, Integer::sum);
                    int port = response.uri().getPort();
                    values.put(port, new Value(response.body(), currentTimeStamp, status));
                }
            } catch (InterruptedException | ExecutionException e) {
                //throw new RuntimeException(e);
                //add log
            }
        }
        return values;
    }


    /**
     * @param values       to check if status of value the same as status of correct value
     * @param correctValue latest updated value from node
     * @param query        to get ack parameter
     * @param counters     of success/not success answers
     * @return completable future responses from other nodes
     * sending remaining queries to other nodes that do not have the same status as a
     * correct latest updated value
     * TODO:it does not support comparing of values between nodes (in case both status are existing compare value of request)
     */
    private List<CompletableFuture<HttpResponse<byte[]>>> sendRemainingQueries(Map<Integer, Value> values,
                                                                               Value correctValue,
                                                                               Query query,
                                                                               Map<Value.Status, Integer> counters) {
        List<CompletableFuture<HttpResponse<byte[]>>> responses = new LinkedList<>();
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(300)).build();
        values.forEach((port, value) -> {
            if (correctValue.getStatus() != value.getStatus()) {
                if (port == currentPort) {
                    Value valueFromCurNode = sendRequestToCurrentNode(correctValue, query);
                    counters.merge(valueFromCurNode.getStatus(), 1, Integer::sum);
                } else {
                    HttpRequest request;
                    if (correctValue.getStatus() == Value.Status.EXISTING) {
                        Query queryForUpsert = Query.builder().ack(query.getAck())
                                .from(query.getFrom())
                                .id(query.getId())
                                .body(correctValue.getBytesOfFile())
                                .isProxy(query.isProxy())
                                .build();
                        request = kvUpsertHttpRequests.buildHttpRequest(port, queryForUpsert);
                        responses.add(httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                                .exceptionally(r -> null));
                    } else if (correctValue.getStatus() == Value.Status.DELETED) {
                        request = kvDeleteHttpRequests.buildHttpRequest(port, query);
                        responses.add(httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                                .exceptionally(r -> null));
                    }
                }

            }
        });
        return responses;
    }

    public Value sendRequestToCurrentNode(Value correctValue, Query query) {
        if (correctValue.getStatus() == Value.Status.EXISTING) {
            return fileDao.upsert(query.getId(), correctValue.getBytesOfFile());
        }
        return fileDao.delete(query.getId());
    }

    private Map<Value.Status, Integer> initCounters() {
        Map<Value.Status, Integer> counters = new HashMap<>();
        counters.put(Value.Status.EXISTING, 0);
        counters.put(Value.Status.ABSENT, 0);
        counters.put(Value.Status.DELETED, 0);
        return counters;
    }

}
