package pku.abe.commons.profile;

import com.codahale.metrics.Histogram;

public class AccessStatisticResult {
    public int totalCount = 0;
    public int maxCount = -1;
    public int minCount = -1;

    public int slowCount = 0;

    public Histogram histogram = null;

    public double costTime = 0;

    public long slowThreshold = ProfileConstants.SLOW_COST;

    public long[] intervalCounts = new long[5];

}

