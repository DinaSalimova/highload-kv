package com.company.Service;

import jakarta.ws.rs.core.Response;
import lombok.extern.log4j.Log4j;
import com.company.DAO.FileDao;
import com.company.DAO.Value;
import com.company.HttpRequests.KVHttpRequests;
import com.company.util.BinarySearch;
import com.company.util.Query;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

@Log4j
public class KVService {
    /**
     * count of virtual nodes
     * as we have max 3 nodes for cluster we use 100 virtual nodes per real node
     */
    private final int countOfVNodes = 300;

    /**
     * degree of circle on which we have virtual nodes.
     */
    private final int circleDegree = 360;

    /**
     * list of endpoint of all nodes
     */
    private final List<String> topologies;

    /**
     * port of current storage
     */
    private final int currentPort;

    /**
     * storage - it's file system
     */
    private final FileDao fileDao;

    /**
     * keys represent coordinate on a circle
     * values represent endpoints of real nodes
     */
    Map<Double, String> virtualNodes;

    public KVService(FileDao fileDao, List<String> topologies, int currentPort) {
        this.topologies = topologies;
        this.fileDao = fileDao;
        this.currentPort = currentPort;
        initializeVirtualNodes();
    }

    /**
     *
     * @param query parameter of query (id, ack, from)
     * @param kvHttpRequests class to send GET/PUT/DELETE request
     * @return response (explanation in  readme.md)
     *
     * gets nodes and sends async requests to another storage
     */
    public Response sendRequestsToTopologies(Query query, KVHttpRequests kvHttpRequests) {
        List<CompletableFuture<HttpResponse<byte[]>>> responses = new LinkedList<>();
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(300)).build();
        Set<String> currentTopologies = getNodesConsistentHashing(query, kvHttpRequests);
        for (String topology : currentTopologies) {
            int port = Integer.parseInt(topology.substring(topology.lastIndexOf(":") + 1));
            if (port != currentPort) {
                HttpRequest request = kvHttpRequests.buildHttpRequest(port, query);
                responses.add(httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                        .exceptionally(r -> {
                            log.debug("exception in KVService.sendRequestsToTopologies: ", r.getCause());
                            return null;//if server down to avoid timeout exception on get() method of CompletableFuture
                        })); //handle exception here
            }
        }
        return kvHttpRequests.getResultResponse(query, responses);
    }

    public Value getById(Query query) {
        return fileDao.getById(query.getId());
    }

    public Value upsert(Query query) {
        return fileDao.upsert(query.getId(), query.getBody());
    }

    public Value delete(Query query) {
        return fileDao.delete(query.getId());
    }

    /**
     * @param query parameter of query (id, ack, from)
     * @param kvHttpRequests class to check if node is active
     * @return set of necessary endpoints
     * Method uses consistent hashing and virtual nodes (for better distribution).
     * It finds virtual node by hash of incoming key and returns real nodes.
     * Amount of real nodes depends on "from" parameter of query.
     *
     */
    private Set<String> getNodesConsistentHashing(Query query, KVHttpRequests kvHttpRequests) {
        int hashCode = query.getId().hashCode();
        hashCode = hashCode * (hashCode / Math.abs(hashCode));
        int valueOfKey = hashCode % circleDegree;
        Set<Double> vNodes = virtualNodes.keySet();
        Double[] vNodesArray = vNodes.toArray(new Double[vNodes.size()]);
        int sizeOfVNodes = vNodesArray.length;
        int nodeIndex = BinarySearch.search(vNodesArray, 0, sizeOfVNodes - 1, valueOfKey);
        int countOfNodes = 0;
        TreeSet<String> resultTopologies = new TreeSet<>();
        while (resultTopologies.size() < query.getFrom() - 1) { //to avoid adding current port
            String endpoint = virtualNodes.get(vNodesArray[nodeIndex % sizeOfVNodes]);//to avoid index out of bound
            int port = Integer.parseInt(endpoint.substring(endpoint.lastIndexOf(":") + 1));
            if (port != currentPort) {
                //TODO: it's quite long to check is server active,
                // because another request here is created. Rewrite this bottle neck...
                if (kvHttpRequests.isServerActive(port)) {
                    resultTopologies.add(endpoint);
                }
            }
            nodeIndex++;
            countOfNodes++;
            if (countOfNodes == topologies.size()){
                break;
            }
        }
        return resultTopologies;
    }


    /**
     * method to initialize of virtual nodes.
     * Values of virtual nodes are mapped on circle to value of real nodes (endpoints)
     * for better distribution
     */
    private void initializeVirtualNodes() {
        virtualNodes = new TreeMap<>();
        double step = (double) circleDegree / countOfVNodes;
        int j = 0;
        int sizeOfEndpoints = topologies.size();
        for (double i = 0; i <= circleDegree; i += step) {
            virtualNodes.put(i, topologies.get(j % sizeOfEndpoints));
            j++;
        }
    }
}
