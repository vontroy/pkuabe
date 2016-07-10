package pku.abe.commons.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import pku.abe.commons.log.ApiLogger;

/**
 * 缓存 bean 实例对象，避免重复创建资源链接，主要目的是为了解决测试环境下的资源链接数问题。<br/>
 * 判断重复的条件是通过 {@link #getKey()} 方法的返回值
 */
@SuppressWarnings("rawtypes")
public abstract class CacheableObjectFactoryBean<T> extends AbstractFactoryBean {

    private static Map<String, Object> beanCache = new HashMap<String, Object>();

    protected boolean cacheInstance = true;

    protected List<BeanProperty<T>> properties = new ArrayList<BeanProperty<T>>();

    @Override
    protected Object createInstance() throws Exception {
        String key = this.getKey().toString();
        Object bean = this.isCacheInstance() ? beanCache.get(key) : null;
        if (bean == null) {
            long startTime = System.currentTimeMillis();
            bean = this.doCreateInstance();
            beanCache.put(key, bean);
            long cost = System.currentTimeMillis() - startTime;
            ApiLogger.info("[createInstance]" + this.getClass().getName() + " object key:" + key + " init cost time:" + cost);
        } else {
            ApiLogger.info("[createInstance]" + this.getClass().getName() + " reuse bean instance by key:" + key);
        }
        return bean;
    }


    public boolean isCacheInstance() {
        return cacheInstance;
    }


    public void setCacheInstance(boolean cacheInstance) {
        this.cacheInstance = cacheInstance;
    }


    protected StringBuilder getKey() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.getKeyPrefix());
        Collections.sort(this.properties);
        for (BeanProperty property : properties) {
            buf.append(property.getName()).append(":").append(property.getValue());
        }
        return buf;
    }

    protected abstract String getKeyPrefix();

    protected abstract Object doCreateInstance() throws Exception;

    public static Map<String, Object> getBeanCache() {
        return Collections.unmodifiableMap(beanCache);
    }

    public abstract static class BeanProperty<T> implements Comparable<BeanProperty> {
        private String name;
        private Object value;

        public BeanProperty(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        public abstract void apply(T target);

        @Override
        public int compareTo(BeanProperty o) {
            return this.name.compareTo(o.name);
        }

    }
}
