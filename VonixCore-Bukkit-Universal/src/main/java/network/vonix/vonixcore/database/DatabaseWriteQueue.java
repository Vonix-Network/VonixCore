package network.vonix.vonixcore.database;

import network.vonix.vonixcore.VonixCore;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Consumer queue for batching and optimizing database write operations.
 * Provides non-blocking async writes with configurable batch processing.
 */
public class DatabaseWriteQueue {
    private static DatabaseWriteQueue INSTANCE;

    private final BlockingQueue<WriteOperation> writeQueue;
    private final ExecutorService writeExecutor;
    private final ScheduledExecutorService flushScheduler;
    private final AtomicBoolean running = new AtomicBoolean(true);

    // Configuration
    private static final int QUEUE_CAPACITY = 10000;
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 1000;
    private static final int WRITE_THREADS = 2;

    public DatabaseWriteQueue() {
        INSTANCE = this;
        this.writeQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        this.writeExecutor = Executors.newFixedThreadPool(WRITE_THREADS, r -> {
            Thread t = new Thread(r, "VonixCore-DBWrite");
            t.setDaemon(true);
            return t;
        });
        this.flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VonixCore-DBFlush");
            t.setDaemon(true);
            return t;
        });

        // Start background processor
        startBatchProcessor();

        // Start periodic flush
        flushScheduler.scheduleAtFixedRate(this::flushQueue,
                FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);

        VonixCore.getInstance().getLogger().info("[DB] Write queue initialized with " + WRITE_THREADS + " threads");
    }

    public static DatabaseWriteQueue getInstance() {
        return INSTANCE;
    }

    /**
     * Queues a write operation for async execution.
     * Returns immediately - operation runs in background.
     */
    public void queueWrite(Runnable operation) {
        queueWrite(operation, null);
    }

    /**
     * Queues a write operation with optional error callback.
     */
    public void queueWrite(Runnable operation, Consumer<Exception> onError) {
        if (!running.get()) {
            VonixCore.getInstance().getLogger().warning("[DB] Write queue is shut down, executing synchronously");
            try {
                operation.run();
            } catch (Exception e) {
                if (onError != null)
                    onError.accept(e);
            }
            return;
        }

        WriteOperation writeOp = new WriteOperation(operation, onError);
        if (!writeQueue.offer(writeOp)) {
            // Queue full - execute immediately
            VonixCore.getInstance().getLogger().warning("[DB] Write queue full, executing synchronously");
            writeExecutor.execute(() -> {
                try {
                    operation.run();
                } catch (Exception e) {
                    if (onError != null)
                        onError.accept(e);
                    else
                        VonixCore.getInstance().getLogger().warning("[DB] Write error: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Queues a high-priority write that bypasses batching.
     */
    public void queueImmediate(Runnable operation) {
        writeExecutor.execute(() -> {
            try {
                operation.run();
            } catch (Exception e) {
                VonixCore.getInstance().getLogger().warning("[DB] Immediate write error: " + e.getMessage());
            }
        });
    }

    private void startBatchProcessor() {
        writeExecutor.execute(() -> {
            while (running.get() || !writeQueue.isEmpty()) {
                try {
                    // Wait for items or timeout
                    WriteOperation op = writeQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (op != null) {
                        processBatch(op);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void processBatch(WriteOperation firstOp) {
        // Process first operation
        executeOperation(firstOp);

        // Process additional items up to batch size
        int processed = 1;
        WriteOperation nextOp;
        while (processed < BATCH_SIZE && (nextOp = writeQueue.poll()) != null) {
            executeOperation(nextOp);
            processed++;
        }
    }

    private void executeOperation(WriteOperation op) {
        try {
            op.operation.run();
        } catch (Exception e) {
            if (op.onError != null) {
                op.onError.accept(e);
            } else {
                VonixCore.getInstance().getLogger().warning("[DB] Write error: " + e.getMessage());
            }
        }
    }

    private void flushQueue() {
        // Drain remaining items
        WriteOperation op;
        int flushed = 0;
        while ((op = writeQueue.poll()) != null && flushed < BATCH_SIZE * 2) {
            executeOperation(op);
            flushed++;
        }
    }

    /**
     * Gets the current queue size.
     */
    public int getQueueSize() {
        return writeQueue.size();
    }

    /**
     * Checks if the queue is running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Shuts down the write queue, flushing remaining operations.
     */
    public void shutdown() {
        running.set(false);
        VonixCore.getInstance().getLogger()
                .info("[DB] Shutting down write queue with " + writeQueue.size() + " pending operations");

        // Stop accepting new scheduled flushes
        flushScheduler.shutdown();

        // Flush remaining operations
        flushQueue();
        while (!writeQueue.isEmpty()) {
            flushQueue();
        }

        // Shutdown executor
        writeExecutor.shutdown();
        try {
            if (!writeExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                writeExecutor.shutdownNow();
            }
            if (!flushScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                flushScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            writeExecutor.shutdownNow();
            flushScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        VonixCore.getInstance().getLogger().info("[DB] Write queue shutdown complete");
    }

    private static class WriteOperation {
        final Runnable operation;
        final Consumer<Exception> onError;

        WriteOperation(Runnable operation, Consumer<Exception> onError) {
            this.operation = operation;
            this.onError = onError;
        }
    }
}
