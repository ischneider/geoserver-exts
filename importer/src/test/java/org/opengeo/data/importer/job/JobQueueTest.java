package org.opengeo.data.importer.job;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import junit.framework.TestCase;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class JobQueueTest extends TestCase {
    
    public void testFutureTracking() throws Exception {
        final JobQueue q = new JobQueue(1, new LinkedBlockingQueue<Runnable>());
        
        // make some dummy jobs that will sleep
        final List<ProgressFuture> futures = new ArrayList<ProgressFuture>();
        for (int i = 0; i < 50; i++) {
            futures.add(q.submit(new ProgressCallable<Object>() {
                @Override
                protected Object call(ProgressMonitor listener) throws Exception {
                    try { Thread.sleep(100); } catch (InterruptedException ie) {}
                    return null;
                }
            }));
        }
        
        // all futures should be retrievable at this point
        // and thread-safe, too - make 4 threads lookup the futures
        List<Callable<Object>> gets = new ArrayList();
        for (int i = 0; i < futures.size(); i++) {
            final Integer k = Integer.valueOf(i);
            gets.add(Executors.callable(new Runnable() {
                public void run() {
                    ProgressFuture pf = q.getFuture(futures.get(k).getKey());
                    assertNotNull(pf);
                    assertEquals(futures.get(k).getKey(), pf.getKey());
                }
            }));
        }
        Executors.newFixedThreadPool(4).invokeAll(gets);
        
        // now cancel and sleep to let them fall out of the internal queue
        for (int i = 0; i < futures.size(); i++) {
            futures.get(i).cancel(true);
        }
        Thread.sleep(100);
        
        // no futures should be retrievable at this point
        for (int i = 0; i < futures.size(); i++) {
            assertNull(q.getFuture(futures.get(i).getKey()));
        }
    }
    
}
