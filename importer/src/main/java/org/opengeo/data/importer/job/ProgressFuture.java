package org.opengeo.data.importer.job;

import java.util.concurrent.FutureTask;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public final class ProgressFuture<V> extends FutureTask<V> {
    private final ProgressCallable<V> callable;
    private Throwable exception;

    public ProgressFuture(ProgressCallable<V> callable) {
        super(callable);
        this.callable = callable;
    }

    @Override
    protected void setException(Throwable t) {
        this.exception = t;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        callable.listener.setCanceled(true);
        return super.cancel(mayInterruptIfRunning);
    }
    
    public Throwable getError() {
        return exception;
    }
    
    public ProgressMonitor getMonitor() {
        return callable.listener;
    }
    
    public Long getKey() {
        if (callable.key instanceof Long) {
            return (Long) callable.key;
        }
        throw new IllegalStateException("default key not in use");
    }
    
}
