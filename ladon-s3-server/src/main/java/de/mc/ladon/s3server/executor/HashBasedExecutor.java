package de.mc.ladon.s3server.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Executor for the S3Repository.
 * The idea behind it is to prevent concurrent access to objects as much as possible.
 *
 * @author ralfulrich on 29.02.16.
 */
public class HashBasedExecutor {

    private final int poolsize;
    private final List<ExecutorService> executorServices;

    public HashBasedExecutor(int poolsize) {
        this.poolsize = poolsize;
        executorServices = new ArrayList<>(poolsize);
        for (int i = 0; i < poolsize; i++) {
            executorServices.add(Executors.newSingleThreadExecutor());
        }

    }

    /**
     * Stops the executor and waits until stopped or timeout
     *
     * @param timeout in seconds
     * @throws InterruptedException
     */
    public void shutdown(int timeout) throws InterruptedException {
        for (ExecutorService executorService : executorServices) {
            executorService.shutdown();
        }
        for (ExecutorService executorService : executorServices) {
            executorService.awaitTermination(timeout, TimeUnit.SECONDS);
        }
    }

    /**
     * Execute a runnable on a thread depending on the given key. Same key - same thread
     *
     * @param key      key to determine the thread to use
     * @param runnable the runnable to execute
     */
    public void execute(String key, Runnable runnable) {
        int matchingExecutor = keyToIndex(key);
        executorServices.get(matchingExecutor).execute(runnable);
    }


    private int keyToIndex(String key) {
        return Math.abs(key.hashCode() % poolsize);
        // for testing: new Random().nextInt(poolsize);
    }


}
