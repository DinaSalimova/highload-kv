package com.company;

import com.company.ServerStorage.KVServer;
import com.company.ServerStorage.KVServerFactory;
import com.company.ServerStorage.KindOfServer;
import com.company.util.Files;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Starts 3-node storage cluster and waits for shutdown
 *
 */
public final class Cluster {
    private static final int[] PORTS = {8080, 8081, 8082};

    private Cluster() {
        // Not instantiable
    }

    public static void main(String[] args) throws IOException {
        // Fill the topology
        final Set<String> topology = new HashSet<>(3);
        for (final int port : PORTS) {
            topology.add("http://localhost:" + port);
        }

        // Start nodes
        for (int i = 0; i < PORTS.length; i++) {
            final int port = PORTS[i];
            final File data = Files.createTempDirectory();

            System.out.println("Starting node " + i + " on port " + port + " and data at " + data);

            // Start the storage
            final KVServer storage = KVServerFactory.create(port, data, topology, KindOfServer.JERSEY);
            storage.start();
            Runtime.getRuntime().addShutdownHook(new Thread(storage::stop));
        }
    }
}
