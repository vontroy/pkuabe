package pku.abe.commons.redis;

/**
 * redis pipeline callback api
 * <p>
 * <pre>
 * 		1) just support write request
 * 		2) run as pipeline model
 * </pre>
 *
 * @author maijunsheng
 */
public interface JedisPipelineWriteCallback {

    /**
     * it can implements multi write requests of redis
     *
     * @param pipeline
     * @param values
     */
    public void call(JedisWritePipeline pipeline);
}
