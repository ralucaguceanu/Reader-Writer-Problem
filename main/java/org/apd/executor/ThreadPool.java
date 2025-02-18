package org.apd.executor;

import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
    private final BlockingQueue<StorageTask> taskQueue;
    private final List<ReaderWriterThread> threads;
    private final List<EntryResult> results;
    private final AtomicBoolean isShutdown;
    private final AtomicInteger runningTasks;

    public ThreadPool(int numberOfThreads, SharedDatabase sharedDatabase, LockType lockType) {
        this.taskQueue = new LinkedBlockingQueue<>();
        this.results = new CopyOnWriteArrayList<>(new ArrayList<>());
        this.isShutdown = new AtomicBoolean(false);
        this.runningTasks = new AtomicInteger(0);
        this.threads = new ArrayList<>();

        initializeThreads(numberOfThreads, sharedDatabase, lockType);
    }

    public List<EntryResult> getResults() {
        return results;
    }

    private void initializeThreads(int numberOfThreads, SharedDatabase sharedDatabase, LockType lockType) {
        for (int i = 0; i < numberOfThreads; i++) {
            ReaderWriterThread thread = new ReaderWriterThread(taskQueue, runningTasks, sharedDatabase, lockType,
                    results, isShutdown);
            thread.start();
            threads.add(thread);
        }
    }

    public void submit(StorageTask task) {
        taskQueue.add(task);
    }

    public void shutdown() {
        synchronized (runningTasks) {
            while (runningTasks.get() > 0 || !taskQueue.isEmpty()) {
                try {
                    runningTasks.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        isShutdown.set(true);
    }
}
