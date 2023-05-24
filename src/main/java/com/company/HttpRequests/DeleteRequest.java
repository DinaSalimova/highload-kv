package com.company.HttpRequests;

import com.company.DAO.FileDao;
import com.company.DAO.Value;
import com.company.util.Query;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public class DeleteRequest extends KVHttpRequests {
    public DeleteRequest(FileDao fileDao, int currentPort) {
        super(fileDao, currentPort);
    }

    @Override
    public Value getRequestFromCurrentNode(Query query) {
        return fileDao.delete(query.getId());
    }

    @Override
    public HttpRequest buildHttpRequest(int port, Query query) {
        String uri = getUri(port, query);
        try {
            return HttpRequest.newBuilder()
                    .uri(new URI(uri))
                    .DELETE()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response.Status getSuccessStatusCode() {
        return Response.Status.ACCEPTED;
    }
}
