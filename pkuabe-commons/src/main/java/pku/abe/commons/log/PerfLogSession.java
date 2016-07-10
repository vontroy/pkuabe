/**
 *
 */
package pku.abe.commons.log;

import org.perf4j.LoggingStopWatch;

public class PerfLogSession {

    private String tag;
    private LoggingStopWatch watch;

    public PerfLogSession(String tag, LoggingStopWatch watch) {
        this.tag = tag;
        this.watch = watch;
    }

    public void start() {
        watch.start(this.tag);
    }

    public void step(String tag) {
        StringBuilder buf = new StringBuilder(tag);
        watch.lap(buf.append('_').append(tag).toString());
    }

    public void step(String tag, Throwable e) {
        StringBuilder buf = new StringBuilder(tag);
        watch.lap(buf.append('_').append(tag).toString(), e);
    }

    public void step(String tag, String msg) {
        StringBuilder buf = new StringBuilder(tag);
        watch.lap(buf.append('_').append(tag).toString(), msg);
    }

    public void stop() {
        watch.stop(this.tag);
    }

    public void stop(Throwable e) {
        watch.stop(this.tag, e);
    }

    public void stop(String msg) {
        watch.stop(this.tag, msg);
    }
}
