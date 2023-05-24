package com.company.HttpRequests;

import com.company.DAO.FileDao;
import com.company.DAO.Value;
import com.company.util.Query;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public class UpsertRequest extends KVHttpRequests {

    public UpsertRequest(FileDao fileDao, int currentPort) {
        super(fileDao, currentPort);
    }

    @Override
    public Value getRequestFromCurrentNode(Query query) {
        return fileDao.upsert(query.getId(), query.getBody());
    }


    @Override
    public HttpRequest buildHttpRequest(int port, Query query) {
        String uri = getUri(port, query);
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .version(HttpClient.Version.HTTP_1_1)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(query.getBody()))
                .build();
    }

    @Override
    public Response.Status getSuccessStatusCode() {
        return Response.Status.CREATED;
    }
}
