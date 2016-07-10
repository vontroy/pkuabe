package pku.abe.commons.redis;


/**
 * <pre>
 * 	最终还是决定暴露出来Jedis的接口了，因为基于Jedis的接口去维护一套对等的接口很纠结，因为不同使用方有不同的场景。
 *
 * 	因此决定牺牲：
 * 		1）后续升级的兼容性（比如Jedis接口发生变化，但是考虑到未来一段时间内可能概率比较小
 * 		2）一定程度的安全性，就是拿到Jedis可以做的任何组合操作由业务方决定
 *
 * 	换来：
 * 		1）灵活性
 * 		2）代码的维护性 （不需要去兼容各种各样的场景）
 * </pre>
 *
 * @author maijunsheng
 */
public abstract class JedisPortUpdateCallback<V> extends JedisPortCallback<V> {

    public JedisPortUpdateCallback() {
        this("");
    }

    public JedisPortUpdateCallback(String key) {
        super("callUpdate", key, true);
    }

}
