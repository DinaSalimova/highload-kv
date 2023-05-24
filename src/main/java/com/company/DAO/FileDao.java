package com.company.DAO;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;

import static java.nio.file.Files.readAllBytes;

/**
 * storage is file system
 */
public class FileDao {

    /**
     * to avoid getting value always to storage
     */
    private HashMap<String, Value> cache;

    /**
     * prefix is used to create tombstone file that assign to delete object
     */
    private final String prefixTombStone = "tombstone";

    /**
     * directory of storage
     */
    @NotNull
    private final File dir;

    public FileDao(@NotNull final File dir) {
        this.dir = dir;
        cache = new HashMap<>(10000);
    }

    /**
     * @param key id in query
     * @return value object
     *
     * gets data from cache or from storage if cache doesn't contain value
     */
    public Value getById(String key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        final File file = getFile(key);
        if (!file.exists()) {
            if (cache.containsKey(prefixTombStone + key)) {
                return new Value(null, cache.get(prefixTombStone + key).getLastModifiedTime(),
                        Value.Status.DELETED);
            }
            if (getFile(prefixTombStone + key).exists()) {
                File tombstone = getFile(prefixTombStone + key);
                Value value = new Value(null, tombstone.lastModified(), Value.Status.DELETED);
                cache.put(key, value);
                return value;
            }
            return new Value(null, null, Value.Status.ABSENT);
        }
        final byte[] bytesOfFile;
        try {
            bytesOfFile = readAllBytes(file.toPath());
        } catch (IOException e) {
            return new Value(null, null, Value.Status.ABSENT);
        }
        Value value = new Value(bytesOfFile, file.lastModified(), Value.Status.EXISTING);
        cache.put(key, value);

        return value;
    }

    /**
     *
     * @param key id in query
     * @param value that must be written in file
     * @return value object
     *
     * updates or creates file with given array of bytes
     */
    public Value upsert(@NotNull final String key, @NotNull final byte[] value) {
        File file = getFile(key);
        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            os.write(value);
        } catch (IOException e) {
            return new Value(value, null, Value.Status.ABSENT);
        }
        cache.remove(key);
        return new Value(value, file.lastModified(), Value.Status.EXISTING);
    }

    /**
     *
     * @param key delete file by key
     * @return value object
     *
     * if file was deleted created tombstone (to mark that file was deleted)
     */
    public Value delete(@NotNull final String key) {
        if (getFile(key).delete()) {
            cache.remove(key);
            File tombstone = getFile(prefixTombStone + key);
            try {
                if (tombstone.createNewFile()) {
                    cache.put(prefixTombStone + key, new Value(null, tombstone.lastModified(), Value.Status.DELETED));
                    return new Value(null, tombstone.lastModified(), Value.Status.DELETED);
                }
            } catch (IOException e) {
                return new Value(null, null, Value.Status.ABSENT);
            }
        }
        return new Value(null, null, Value.Status.ABSENT);
    }


    @NotNull
    private File getFile(@NotNull final String key) {
        return new File(dir, key);
    }
}
