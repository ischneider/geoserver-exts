package org.opengeo.data.importer.job;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JobQueue {
    
    private final ThreadPoolExecutor executor;
    
    public JobQueue(int maxThreads, BlockingQueue<Runnable> q) {
        executor = new ThreadPoolExecutor(0, maxThreads, 60L, TimeUnit.SECONDS, q) {
            @Override
            protected <T extends Object> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                RunnableFuture future;
                if (callable instanceof ProgressCallable) {
                    future = new ProgressFuture( (ProgressCallable) callable);
                } else {
                    future = super.newTaskFor(callable);
                }
                return future;
            }
        };
    }
    
    public ProgressFuture<?> getFuture(Object key) {
        ProgressFuture<?> found = null;
        for (Runnable r: executor.getQueue()) {
            if (r instanceof ProgressFuture) {
                ProgressFuture pf = (ProgressFuture) r;
                if (pf.getKey().equals(key)) {
                    found = pf;
                    break;
                }
            }
        }
        return found;
    }
    
    public <T> ProgressFuture<T> submit(ProgressCallable<T> callable) {
        return (ProgressFuture<T>) executor.submit(callable);
    }

    public <T> Future<T> submit(Callable<T> callable) {
        return executor.submit(callable);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
