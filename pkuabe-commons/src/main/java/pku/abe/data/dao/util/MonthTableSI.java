package pku.abe.data.dao.util;

import java.util.Date;

/**
 * Created by LinkedME01 on 16/3/24.
 */
public class MonthTableSI implements Comparable<MonthTableSI>{
    public Date start_date;
    public Date end_date;
    int count;

    @Override
    public int compareTo(MonthTableSI other) {
        return this.start_date.compareTo(other.start_date);
    }
}
