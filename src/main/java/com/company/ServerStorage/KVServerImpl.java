package com.company.ServerStorage;

import com.company.DAO.FileDao;
import com.company.HttpRequests.DeleteRequest;
import com.company.HttpRequests.GetRequest;
import com.company.HttpRequests.KVHttpRequests;
import com.company.HttpRequests.UpsertRequest;
import com.company.Service.KVService;
import com.company.controller.CrudStorageController;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import com.company.controller.Status;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * contains server, in current implementation jersey server is used.
 * Also, embedded dependency injection mechanism is used.
 * The class defines behavior of server: initialize, start and stop it.
 */
public class KVServerImpl implements KVServer {

    private final HttpServer server;

    public KVServerImpl(File data, int port, final Set<String> topology) {
        List<String> sortedTopology = topology.stream().sorted().collect(Collectors.toList());
        ResourceConfig resourceConfig = new ResourceConfig()
                .register(Status.class)
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        FileDao fileDao = new FileDao(data);
                        KVHttpRequests kvUpsertRequest = new UpsertRequest(fileDao, port);
                        KVHttpRequests kvDeleteRequest = new DeleteRequest(fileDao, port);
                        bind(new KVService(fileDao, sortedTopology, port)).to(KVService.class);
                        bind(new GetRequest(fileDao, port, kvUpsertRequest, kvDeleteRequest))
                                .to(KVHttpRequests.class).named("GetRequest");
                        bind(kvUpsertRequest).to(KVHttpRequests.class).named("UpsertRequest");
                        bind(kvDeleteRequest).to(KVHttpRequests.class).named("DeleteRequest");
                    }
                })
                .register(CrudStorageController.class);
        server = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://localhost:" + port),
                resourceConfig);
    }

    @Override
    public void start() throws IOException {
        server.start();
    }

    @Override
    public void stop() {
        server.shutdownNow();
    }
}
