package pku.abe.commons.memcache;

import java.util.Set;

public interface LocalCacheMonitor {

    public Set<String> getAllKeys();

    public void statistic(String key);

    public void statistic(String... key);

    public boolean isHotKey(String key);

    public boolean isHotKeyAndStats(String key);

}
