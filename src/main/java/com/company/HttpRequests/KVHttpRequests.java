package com.company.HttpRequests;

import com.company.DAO.FileDao;
import com.company.DAO.Value;
import com.company.util.Query;
import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Log4j
public abstract class KVHttpRequests {
    protected final FileDao fileDao;
    protected final int currentPort;

    public KVHttpRequests(FileDao fileDao, int currentPort) {
        this.fileDao = fileDao;
        this.currentPort = currentPort;
    }

    public abstract HttpRequest buildHttpRequest(int port, Query query);

    public abstract Response.Status getSuccessStatusCode();

    /**
     * @param query to get id
     * @return response from current node
     * <p>
     * uses to get response from current node for get/delete/upsert request
     */
    public abstract Value getRequestFromCurrentNode(Query query);

    /**
     * @param port to send request
     * @return true if server active otherwise false
     */
    public boolean isServerActive(int port) {
        String uri = "http://localhost:" +
                port +
                "/v0/status";
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .GET()
                    .build();
            var response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(300))
                    .build()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == Response.Status.OK.getStatusCode();
        } catch (InterruptedException | IOException | URISyntaxException e) {
            return false;
        }
    }

    /**
     * @param query     to get ack parameter
     * @param responses completableFuture responses from other storages
     * @return success response if ack requests are got otherwise return error response
     * <p>
     * Method uses for PUT and DELETE responses
     */
    public Response getResultResponse(Query query, List<CompletableFuture<HttpResponse<byte[]>>> responses) {
        int success = 0;
        var valueFromCurrentNode = getRequestFromCurrentNode(query);
        if (valueFromCurrentNode.getStatus() != Value.Status.ABSENT) {
            success++;
        }
        for (var responseFuture : responses) {
            if (success == query.getAck()) {
                break;
            }
            try {
                HttpResponse<byte[]> response = responseFuture.get();
                if (response != null && response.statusCode() == getSuccessStatusCode().getStatusCode()) {
                    success++;
                }
            } catch (InterruptedException | ExecutionException e) {
                log.debug("exception in KVHttpRequests.getResultResponse: ", e.getCause());
            }
        }
        if (success == query.getAck()) {
            return Response
                    .status(getSuccessStatusCode().getStatusCode())
                    .build();
        }
        return Response
                .status(504, "Not Enough Replicas")
                .build();
    }

    /**
     * @param port  current port
     * @param query parameter to get id
     * @return uri string
     */
    public String getUri(int port, Query query) {
        return "http://localhost:" +
                port +
                "/v0/entity?id=" +
                query.getId() +
                "&isProxy=false";
    }
}
