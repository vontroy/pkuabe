package pku.abe.commons.client.balancer.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ClientBalancerLog {

    public static long REDIS_FIRE_TIME = 300; // Redis操作超时

    public static Logger log = LoggerFactory.getLogger("com.weibo.api.client.balancer");
    public static Logger fireLog = LoggerFactory.getLogger("fire");
    // public static Logger statLog = LoggerFactory.getLogger("statlog");

    public static void fire(String msg) {
        if (fireLog.isInfoEnabled()) {
            fireLog.info(msg);
        }
    }
}

