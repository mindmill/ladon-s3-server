/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.repository.vfs;

import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.exceptions.OperationAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Cross-process file locks for objects ({@code locks/<bucket>/<keyhash>}),
 * implemented with {@link FileChannel#tryLock(long, long, boolean)} (i.e.
 * advisory {@code flock(2)} locks). The OS releases the lock when the holding
 * process terminates, so there are no stale lock files: a concurrent request
 * simply waits (bounded by the timeout) and is then allowed to proceed.
 *
 * <p>Mutating operations take an exclusive lock, while shared (read) locks may
 * be held concurrently. Since all content is immutable once written (content
 * addressing), readers can also read without taking a lock at all; the shared
 * lock exists for API symmetry and future extensions.</p>
 */
public final class VFSLockManager {

    private static final Logger logger = LoggerFactory.getLogger(VFSLockManager.class);

    /**
     * A held lock; releasing the handle releases the lock and closes the channel.
     */
    public interface LockHandle extends AutoCloseable {

        /**
         * Releases the lock (and the underlying channel). Failures are logged,
         * never propagated: releasing a lock must not mask the result of the
         * operation itself.
         */
        @Override
        void close();
    }

    private static final long RETRY_INTERVAL_MS = 10L;
    private static final long DEFAULT_TIMEOUT_MS = 30_000L;

    private final VFSLayout layout;
    private final long timeoutMs;

    /**
     * JVM-level per-file monitors. In-JVM concurrent lock requests on the same
     * file must be serialized in addition to the OS-level ({@code flock}) lock:
     * the JDK reports a second, overlapping {@code tryLock} in the same JVM as
     * an {@link java.nio.channels.OverlappingFileLockException} instead of a
     * null result, so two threads racing on the same file can not simply both
     * "wait for the OS lock".
     */
    private final java.util.concurrent.ConcurrentHashMap<Path, Object> monitors =
            new java.util.concurrent.ConcurrentHashMap<>();

    public VFSLockManager(VFSLayout layout) {
        this(layout, DEFAULT_TIMEOUT_MS);
    }

    public VFSLockManager(VFSLayout layout, long timeoutMs) {
        this.layout = layout;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Acquires an exclusive lock for an object.
     *
     * @param callContext the current call context (for the exception type)
     * @param bucket      the bucket
     * @param keyHash     the key hash ({@link VFSLayout#keyHash(String, String)})
     * @return the handle to release in a finally block
     * @throws OperationAbortedException if the lock could not be acquired within the timeout
     */
    public LockHandle acquireExclusive(S3CallContext callContext, String bucket, String keyHash) {
        return acquire(callContext, bucket, keyHash, false);
    }

    /**
     * Acquires a shared lock for an object. See {@link #acquireExclusive(S3CallContext, String, String)}.
     *
     * @param callContext the current call context
     * @param bucket      the bucket
     * @param keyHash     the key hash
     * @return the handle to release in a finally block
     * @throws OperationAbortedException if the lock could not be acquired within the timeout
     */
    public LockHandle acquireShared(S3CallContext callContext, String bucket, String keyHash) {
        return acquire(callContext, bucket, keyHash, true);
    }

    private LockHandle acquire(S3CallContext callContext, String bucket, String keyHash, boolean shared) {
        Path lockFile = layout.lockFile(bucket, keyHash);
        Object monitor = monitors.computeIfAbsent(lockFile, p -> new Object());
        synchronized (monitor) {
            return tryAcquire(callContext, bucket, keyHash, lockFile, shared);
        }
    }

    private LockHandle tryAcquire(S3CallContext callContext, String bucket, String keyHash,
                                  Path lockFile, boolean shared) {
        FileChannel channel = null;
        try {
            Files.createDirectories(lockFile.getParent());
            channel = FileChannel.open(lockFile,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.READ,
                    StandardOpenOption.CREATE);
            long deadline = System.currentTimeMillis() + timeoutMs;
            for (;;) {
                try {
                    FileLock lock = channel.tryLock(0L, Long.MAX_VALUE, shared);
                    if (lock != null) {
                        return new FileLockHandle(channel, lock);
                    }
                } catch (java.nio.channels.OverlappingFileLockException e) {
                    // the JVM already reports an overlapping lock (in-JVM contention) — keep waiting
                } catch (IOException e) {
                    logger.error("could not acquire lock on {}", lockFile, e);
                    closeQuietly(channel);
                    throw new de.mc.ladon.s3server.exceptions.InternalErrorException(keyHash, callContext.getRequestId());
                }
                if (System.currentTimeMillis() >= deadline) {
                    throw new OperationAbortedException(keyHash, callContext.getRequestId());
                }
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new de.mc.ladon.s3server.exceptions.InternalErrorException(keyHash, callContext.getRequestId());
                }
            }
        } catch (IOException e) {
            logger.error("could not open lock file {}", lockFile, e);
            closeQuietly(channel);
            throw new de.mc.ladon.s3server.exceptions.InternalErrorException(keyHash, callContext.getRequestId());
        } catch (OperationAbortedException e) {
            closeQuietly(channel);
            throw e;
        }
    }

    private static void closeQuietly(FileChannel channel) {
        if (channel == null) {
            return;
        }
        try {
            channel.close();
        } catch (IOException e) {
            logger.warn("could not close lock channel", e);
        }
    }

    private static final class FileLockHandle implements LockHandle {
        private final FileChannel channel;
        private final FileLock lock;
        private boolean released;

        FileLockHandle(FileChannel channel, FileLock lock) {
            this.channel = channel;
            this.lock = lock;
        }

        @Override
        public synchronized void close() {
            if (!released) {
                released = true;
                try {
                    if (lock != null && lock.isValid()) {
                        lock.release();
                    }
                } catch (IOException e) {
                    logger.warn("could not release lock", e);
                }
                closeQuietly(channel);
            }
        }
    }
}
