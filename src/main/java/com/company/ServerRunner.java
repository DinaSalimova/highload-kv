package com.company;

import com.company.ServerStorage.KVServer;
import com.company.ServerStorage.KVServerFactory;
import com.company.ServerStorage.KindOfServer;
import com.company.util.Files;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * Starts storage and waits for shutdown
 *
 */
public final class ServerRunner {
    private static final int PORT = 8080;

    private ServerRunner() {
        // Not instantiable
    }

    public static void main(String[] args) throws IOException {
        // Temporary storage in the file system
        final File data = Files.createTempDirectory();

        // Start the storage
        final KVServer storage =
                KVServerFactory.create(
                        PORT,
                        data,
                        Collections.singleton("http://localhost:" + PORT), KindOfServer.JERSEY);
        storage.start();
        Runtime.getRuntime().addShutdownHook(new Thread(storage::stop));
    }
}
