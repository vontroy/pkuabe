package pku.abe.commons.redis.clients.jedis;

public abstract class Builder<T> {
    public abstract T build(Object data);
}
