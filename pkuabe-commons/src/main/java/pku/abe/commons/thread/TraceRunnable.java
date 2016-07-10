package pku.abe.commons.thread;

import pku.abe.commons.util.RequestTraceContext;

/**
 * support request id.
 *
 * @see TraceableThreadExecutor#execute(Runnable)
 */
public class TraceRunnable implements Runnable {
    private Runnable task;
    private RequestTraceContext context;

    public TraceRunnable(Runnable task, RequestTraceContext context) {
        super();
        this.task = task;
        this.context = context;
    }

    @Override
    public void run() {
        RequestTraceContext.spawn(context);
        try {
            task.run();
        } finally {
            RequestTraceContext.clear();
        }
    }
}
