package pku.abe.commons.redis;

import pku.abe.commons.client.balancer.util.ClientBalancerLog;

public class RedisLog {

    /**
     * if consumeTime > REDIS_FIRE_TIME may be log msg
     *
     * @param msg
     * @param dbIndex
     * @param consumeTime
     * @param slowTime
     */
    public static void slowLog(String msg, long consumeTime) {
        slowLog(msg, consumeTime, ClientBalancerLog.REDIS_FIRE_TIME);
    }

    /**
     * if consumeTime > slowTime may be log msg
     *
     * @param consumeTime
     * @param slowTime
     */
    public static void slowLog(String msg, long consumeTime, long slowTime) {
        if (consumeTime >= slowTime) {
            fire(msg + " Slow: " + consumeTime);
        }
    }

    /**
     * if consumeTime > slowTime may be log msg
     *
     * @param consumeTime
     * @param slowTime
     */
    public static void errorLog(String msg, Exception e) {
        fire(msg + " ERROR: " + e);
    }

    /**
     * if consumeTime > slowTime may be log msg
     *
     * @param consumeTime
     * @param slowTime
     */
    public static void fire(String msg) {
        ClientBalancerLog.fireLog.info(msg);
    }

}
