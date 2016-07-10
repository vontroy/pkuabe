/**
 *
 */
package pku.abe.commons.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import pku.abe.commons.log.ApiLogger;

public class ApiDateUtil {
    private static ThreadLocal<DateFormat> formatter = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
        }
    };

    private static ThreadLocal<DateFormat> yearMonthSdf = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yy_MM", Locale.ENGLISH);
        }
    };

    private static ThreadLocal<DateFormat> yearMonthDaySdf = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    private static ThreadLocal<DateFormat> dateTimeSdf = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    private static final int DAY_IN_MILLIONSECONDS = 86400000;

    /**
     * format: "E MMM dd HH:mm:ss Z yyyy"
     *
     * @param date
     * @param defaultValue
     * @return
     */
    public static String formatDate(Date date, String defaultValue) {
        if (date == null) {
            return defaultValue;
        }
        try {
            return formatter.get().format(date);
        } catch (RuntimeException e) {
            ApiLogger.error(new StringBuilder(64).append("Error: in ApiUtil.formatDate, date=").append(date).append(", default_value=")
                    .append(defaultValue), e);
        }
        return null;
    }

    /**
     * format: "E MMM dd HH:mm:ss Z yyyy"
     *
     * @param dateStr
     * @param defaultValue
     * @return
     */
    public static Date parseDate(String dateStr, Date defaultValue) {
        if (dateStr == null) {
            return defaultValue;
        }
        try {
            return formatter.get().parse(dateStr);
        } catch (ParseException e) {
            return defaultValue;
        }
    }

    public static String getYearMonth(Date date) {
        return yearMonthSdf.get().format(date);
    }

    public static String formateYearMonthDay(Date date) {
        return yearMonthDaySdf.get().format(date);
    }

    public static Date parseYearMonthDay(String timeStr, Date defaultValue) {
        if (timeStr == null) {
            return defaultValue;
        }
        try {
            return yearMonthDaySdf.get().parse(timeStr);
        } catch (ParseException e) {
            return defaultValue;
        }
    }

    public static String formateDateTime(Date date) {
        return dateTimeSdf.get().format(date);
    }

    public static Date parseDateTime(String timeStr, Date defaultValue) {
        if (timeStr == null) {
            return defaultValue;
        }
        try {
            return dateTimeSdf.get().parse(timeStr);
        } catch (ParseException e) {
            return defaultValue;
        }
    }

    public static final int getCurrentHour() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    public static final int getLastHour() {
        int hour = getCurrentHour();
        return hour == 0 ? 23 : hour - 1;
    }

    public static final int getNextHour() {
        int hour = getCurrentHour();
        return hour == 23 ? 0 : hour + 1;
    }


    public static Date getFirstDayOfCurMonth() {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    public static Date getFirstDayInMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        if (date != null) {
            calendar.setTime(date);
        }
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    /**
     * 获得距离这个月的n个月的起始日期
     *
     * @param month：负数表示 之前的月份; 正数表示以后的月份；0表示当前月份:
     * @return
     */
    public static Date getFirstDayInMonth(int month) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, month);
        return getFirstDayInMonth(c.getTime());
    }

    public static boolean isCurrentMonth(Date date) {
        if (date != null) {
            Calendar dest = Calendar.getInstance();
            dest.setTime(date);
            Calendar now = Calendar.getInstance();
            return now.get(Calendar.YEAR) == dest.get(Calendar.YEAR) && now.get(Calendar.MONTH) == dest.get(Calendar.MONTH);
        }
        return false;
    }

    /**
     * calculate days between two time stamps.
     *
     * @param from represented by time stamp;
     * @param to represented by time stamp;
     * @return number of between days.
     */
    public static int daysBetween(long from, long to) {
        return Math.abs(Math.round((to - from) / DAY_IN_MILLIONSECONDS));
    }
}
