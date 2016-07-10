package pku.abe.commons.counter.task;


import pku.abe.commons.log.StatLog;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CountTaskExecutor {
    private static final ThreadFactory threadFactory = Executors.defaultThreadFactory();
    private static final RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.DiscardOldestPolicy();

    public static ThreadPoolExecutor db = new ThreadPoolExecutor(80, 100, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(100),
            threadFactory, rejectedExecutionHandler);

    static {
        StatLog.registerExecutor("pool_count_db", db);
    }
}
