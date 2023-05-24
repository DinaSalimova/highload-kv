package com.company.ServerStorage;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Constructs {@link KVServer} instances.
 *
 */
public final class KVServerFactory {
    private static final long MAX_HEAP = 1024 * 1024 * 1024;

    private KVServerFactory() {
        // Not supposed to be instantiated
    }

    /**
     * Construct a storage instance.
     *
     * @param port     port to bind HTTP server to
     * @param data     local disk folder to persist the data to
     * @param topology a list of all cluster endpoints {@code http://<host>:<port>} (including this one)
     * @return a storage instance
     */
    @NotNull
    public static KVServer create(
            final int port,
            @NotNull final File data,
            @NotNull final Set<String> topology,
            KindOfServer kindOfServer) throws IOException {
        if (Runtime.getRuntime().maxMemory() > MAX_HEAP) {
            throw new IllegalStateException("The heap is too big. Consider setting Xmx.");
        }

        if (port <= 0 || 65536 <= port) {
            throw new IllegalArgumentException("Port out of range");
        }

        if (!data.exists()) {
            throw new IllegalArgumentException("Path doesn't exist: " + data);
        }

        if (!data.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory: " + data);
        }

        if (kindOfServer == KindOfServer.JERSEY) {
            return new KVServerImpl(data, port, topology);
        }

        throw new UnsupportedOperationException("Implement me!");
    }
}
