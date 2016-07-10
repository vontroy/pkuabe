/**
 *
 */
package pku.abe.commons.thread;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pku.abe.commons.log.ApiLogger;

/**
 * from com.weibo.api.commons.hbase.ExecutorServiceUtil
 */
public class ExecutorServiceUtil {

    public static <T> T invoke(ThreadPoolExecutor threadPoolExecutor, Callable<T> task, long timeout, TimeUnit unit, boolean cancelFuture)
            throws InterruptedException, CancellationException, ExecutionException, TimeoutException, RejectedExecutionException {
        Future<T> future = null;
        try {
            /*
             * sumbit task
             */
            future = threadPoolExecutor.submit(task);
            return future.get(timeout, unit);
        } finally {
            if (future != null && !future.isDone()) {
                // log warn not done
                ApiLogger.warn("ExecutorServiceUtil invoke notDone, name:" + threadPoolExecutor.getThreadFactory().toString());
                if (cancelFuture) {
                    future.cancel(true);
                }
            }
        }

    }

    public static <T> List<Future<T>> invokes(ThreadPoolExecutor threadPoolExecutor, Collection<? extends Callable<T>> tasks, long timeout,
            TimeUnit unit, boolean cancelFuture) throws InterruptedException, TimeoutException, RejectedExecutionException {
        if (tasks == null || unit == null) {
            throw new NullPointerException();
        }
        long nanos = unit.toNanos(timeout);
        List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            /*
             * sumbit tasks
             */
            long lastTime = System.nanoTime();
            for (Callable<T> t : tasks) {
                futures.add(threadPoolExecutor.submit(t));

                long now = System.nanoTime();
                nanos -= now - lastTime;
                lastTime = now;
                if (nanos <= 0) {
                    return futures;
                }
            }

            /*
             * get results from futures
             */
            for (Future<T> f : futures) {
                if (!f.isDone()) {
                    if (nanos <= 0) {
                        return futures;
                    }
                    try {
                        f.get(nanos, TimeUnit.NANOSECONDS);
                    } catch (CancellationException ignore) {
                        ApiLogger.warn("ExecutorServiceUtil invokeAll CancellationException!");
                    } catch (ExecutionException ignore) {
                        ApiLogger.warn("ExecutorServiceUtil invokeAll " + ignore.getCause() + "!");
                    } catch (TimeoutException toe) {
                        ApiLogger.warn("ExecutorServiceUtil invokeAll TimeoutException!");
                        throw toe;
                    }
                    long now = System.nanoTime();
                    nanos -= now - lastTime;
                    lastTime = now;
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                // log warn not done
                ApiLogger.warn("ExecutorServiceUtil invokes notDone, name:" + threadPoolExecutor.getThreadFactory().toString());

                if (cancelFuture) {
                    for (Future<T> f : futures) {
                        f.cancel(true);
                    }
                }
            }
        }
    }
}
