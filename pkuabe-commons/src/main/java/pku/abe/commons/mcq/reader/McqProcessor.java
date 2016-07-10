package pku.abe.commons.mcq.reader;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.log.StatLog;
import pku.abe.commons.mcq.McqBaseManager;
import pku.abe.commons.mcq.forward.MsgForwarder;
import pku.abe.commons.memcache.MemCacheStorage;
import pku.abe.commons.memcache.VikaCacheClient;
import pku.abe.commons.profile.ProfileType;
import pku.abe.commons.profile.ProfileUtil;
import pku.abe.commons.util.McqUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * mcq 处理mcq，为每个mcq建立独立线程进行读取。写mcq，随机选择mcq写入，如果写失败，轮询下一个，直到尝试完所有的mcq
 */
public abstract class McqProcessor implements StartReadingAble {
    protected String readKey = "linkedme";
    protected Map<String, List<VikaCacheClient>> mcqReaders;
    protected List<Thread> readThreads = new ArrayList<Thread>();
    protected Random random = new Random();

    // 读取的线程数
    protected int readThreadCountEachMcq = 3;

    // 连续读取的数量
    protected int readCountOnce = 100;

    // 连续读取若干数量 or 没有读取到后的等待时间间隔
    protected int waitTimeOnce = 100;

    /**
     * 消息分发
     */
    protected MsgForwarder beforeMsgForwarder;
    protected MsgForwarder afterMsgForwarder;

    public void startReading() {
        // tell MemCacheStorage that we are in processor
        MemCacheStorage.isInProcessor = true;

        for (List<VikaCacheClient> mcqrs : mcqReaders.values()) {
            for (VikaCacheClient mcqr : mcqrs) {
                int i = 0;
                while (i++ < readThreadCountEachMcq) {
                    Thread t = createReadThread(mcqr);
                    t.start();
                    readThreads.add(t);
                }
            }
        }

        startExtWork();
    }

    protected void startExtWork() {}

    protected abstract void handleMsq(String msg);

    protected abstract String getStatMQReadFlag();

    protected abstract String getStatMQReadStatFlag();

    protected Thread createReadThread(final VikaCacheClient mqr) {
        Thread t = new Thread("thread_" + McqUtil.processorId.addAndGet(1) + "_mq_" + mqr.getServerPort()) {
            @Override
            public void run() {
                readFrmMQ(mqr);
            }
        };
        t.setDaemon(true);
        return t;
    }

    protected void readFrmMQ(VikaCacheClient mqReader) {
        // wait a moment for system init.
        McqUtil.waitForInit("[Mcq Process]");

        String portInfo =
                new StringBuilder(64).append("KEY:").append(getReadKey()).append("\tServer:").append(mqReader.getServerPort()).toString();
        ApiLogger.info("Start mq reader!" + portInfo);
        AtomicInteger continueReadCount = new AtomicInteger(0);
        while (true) {
            try {
                String msg = null;
                while (McqBaseManager.IS_ALL_READ.get() && (msg = (String) mqReader.get(getReadKey())) != null) {
                    StatLog.inc(getStatMQReadFlag());
                    StatLog.inc(getStatMQReadStatFlag());
                    if (ApiLogger.isTraceEnabled()) {
                        StatLog.inc(getMQReadDataKey(mqReader.getServerPort(), getReadKey()));
                    }
                    long start = System.currentTimeMillis();
                    try {
                        handleMsq(msg);
                        if (continueReadCount.addAndGet(1) % readCountOnce == 0) {
                            McqUtil.safeSleep(waitTimeOnce);
                            continueReadCount.set(0);
                        }

                    } catch (Exception e) {
                        ApiLogger.warn(new StringBuilder(128).append("Error: processing the msg frm mq error, ").append(portInfo)
                                .append(", msg=").append(msg), e);
                    } finally {
                        if (readKey != null) {
                            long end = System.currentTimeMillis();
                            long cost = end - start;
                            ProfileUtil.accessStatistic(ProfileType.MCQ.value(), readKey, end, cost);
                        }
                    }
                }

                if (!McqBaseManager.IS_ALL_READ.get()) {
                    ApiLogger.info("McqProcessor is alive but not read message.");
                }

                McqUtil.safeSleep(waitTimeOnce);
                StatLog.inc(getStatMQReadStatFlag());

                // should response thread interrupted
                if (Thread.interrupted()) {
                    ApiLogger.warn(new StringBuilder(32).append("Thread interrupted :").append(Thread.currentThread().getName()));
                    break;
                }
            } catch (Exception e) {
                ApiLogger.error(new StringBuilder("Error: when reship mq. key:").append(getReadKey()), e);
            }
        }
    }

    public void setMcqReaders(Map<String, List<VikaCacheClient>> mcqReaders) {
        this.mcqReaders = mcqReaders;
        for (List<VikaCacheClient> mcqrs : mcqReaders.values()) {
            for (VikaCacheClient mcqr : mcqrs) {
                mcqr.getClient().setPrimitiveAsString(true);
            }
        }
    }

    public void setReadThreadCountEachMcq(int readThreadCountEachMcq) {
        this.readThreadCountEachMcq = readThreadCountEachMcq;
    }

    public void setReadCountOnce(int readCountOnce) {
        this.readCountOnce = readCountOnce;
    }

    public void setWaitTimeOnce(int waitTimeOnce) {
        this.waitTimeOnce = waitTimeOnce;
    }

    public void setBeforeMsgForwarder(MsgForwarder beforeMsgForwarder) {
        this.beforeMsgForwarder = beforeMsgForwarder;
    }

    public void setAfterMsgForwarder(MsgForwarder afterMsgForwarder) {
        this.afterMsgForwarder = afterMsgForwarder;
    }

    public String getReadKey() {
        return readKey;
    }

    private String getMQReadDataKey(String serverPort, String key) {
        return "read_mq_data_" + serverPort + "_" + key;
    }

    /**
     * 设置系统初始化成功状态
     */
    public static void setSystemInitSuccess() {
        McqUtil.setSystemInitSuccess();
    }
}
