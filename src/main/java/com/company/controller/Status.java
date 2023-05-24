package com.company.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v0/status")
public class Status {
    final String response = "ONLINE";

    /**
     * @return online status
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus() {
        return Response
                .status(Response.Status.OK)
                .entity(response)
                .build();
    }
}
