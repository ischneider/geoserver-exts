package org.opengeo.data.importer.job;

import java.util.concurrent.Callable;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public abstract class ProgressCallable<T> implements Callable<T> {
    final ProgressMonitor listener;
    final Object key;
    
    public ProgressCallable() {
        this(null);
    }
    
    /**
     * Create a ProgressCallable that will use the provided key. The key
     * should be unique in the system but there is no enforcement of this.
     * The key must be tracked or known outside the job package as it cannot
     * be retrieved. If the provided key is not a Long, the ProgressFuture
     * method getKey will throw an exception.
     */
    public ProgressCallable(Object key) {
        this.key = key == null ? new Long(System.identityHashCode(this)) : key;
        if (this.key == null) {
            throw new IllegalArgumentException("key is null");
        }
        listener = new ProgressMonitor();
    }

    @Override
    public final T call() throws Exception {
        return call(listener);
    }

    protected abstract T call(ProgressMonitor listener) throws Exception;
    
}
