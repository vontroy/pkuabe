package pku.abe.commons.redis;

/**
 * redis pipeline callback api
 * <p>
 * <pre>
 * 		1) just support read request
 * 		2) run as pipeline model
 * </pre>
 *
 * @author maijunsheng
 */
public interface JedisPipelineReadCallback {

    /**
     * it can implements multi read requests of redis
     *
     * @param pipeline
     * @param value
     */
    public void call(JedisReadPipeline pipeline);
}
