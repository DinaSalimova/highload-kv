package com.company.controller;

import com.company.Service.KVService;
import com.company.util.Query;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import com.company.DAO.Value;
import com.company.HttpRequests.KVHttpRequests;

@Path("/v0/entity")
public class CrudStorageController {

    @Inject
    private KVService kvService;

    @Inject
    @Named("GetRequest")
    KVHttpRequests kvGetHttpRequests;
    @Inject
    @Named("UpsertRequest")
    KVHttpRequests kvUpsertHttpRequests;

    @Inject
    @Named("DeleteRequest")
    KVHttpRequests kvDeleteHttpRequests;

    @GET
    public Response getEntity(@QueryParam("id") String id,
                              @QueryParam("replicas") String replicas,
                              @QueryParam("isProxy") String isProxy) {
        Query query;
        try {
            query = new Query(id, replicas, isProxy);
        } catch (IllegalArgumentException e) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .build();
        }
        if (query.isProxy()) {
            return kvService.sendRequestsToTopologies(query, kvGetHttpRequests);
        }
        final Value value = kvService.getById(query);
        if (value.getStatus() == Value.Status.DELETED | value.getStatus() == Value.Status.ABSENT) {
            Response.ResponseBuilder responseBuilder = Response
                    .status(Response.Status.NOT_FOUND)
                    .header("status", value.getStatus());
            if (value.getLastModifiedTime() == null) {
                return responseBuilder.build();
            }
            return responseBuilder.header("modifiedTime", value.getLastModifiedTime()).build();
        }
        return Response
                .status(Response.Status.OK)
                .entity(value.getBytesOfFile())
                .header("modifiedTime", value.getLastModifiedTime())
                .header("status", value.getStatus())
                .build();
    }

    @PUT
    public Response upsertEntity(@QueryParam("id") String id,
                                 @QueryParam("replicas") String replicas,
                                 @QueryParam("isProxy") String isProxy,
                                 byte[] body) {
        Query query;
        try {
            query = new Query(id, replicas, isProxy);
            query.setBody(body);
        } catch (IllegalArgumentException e) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .build();
        }
        if (query.isProxy()) {
            return kvService.sendRequestsToTopologies(query, kvUpsertHttpRequests);
        }
        Value value = kvService.upsert(query);
        if (value.getStatus() == Value.Status.ABSENT) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
        return Response
                .status(Response.Status.CREATED)
                .header("modifiedTime", value.getLastModifiedTime())
                .header("status", value.getStatus())
                .build();
    }

    @DELETE
    public Response deleteEntity(@QueryParam("id") String id,
                                 @QueryParam("replicas") String replicas,
                                 @QueryParam("isProxy") String isProxy) {
        Query query;
        try {
            query = new Query(id, replicas, isProxy);
        } catch (IllegalArgumentException e) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .build();
        }
        if (query.isProxy()) {
            return kvService.sendRequestsToTopologies(query, kvDeleteHttpRequests);
        }
        Value value = kvService.delete(query);
        if (value.getStatus() == Value.Status.ABSENT) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .build();
        }
        return Response
                .status(Response.Status.ACCEPTED)
                .header("modifiedTime", value.getLastModifiedTime())
                .header("status", value.getStatus())
                .build();

    }

}
