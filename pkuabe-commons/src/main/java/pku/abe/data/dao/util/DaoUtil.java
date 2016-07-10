package pku.abe.data.dao.util;

import org.springframework.dao.DataAccessException;

public class DaoUtil {
    /**
     * 此类异常属于jt抛出来的重复键异常： PreparedStatementCallback; SQL [insert into excluded_mention (status_id,
     * uid) values(?,?)]; Duplicate entry '3344092931636290-1784746013' for key 'PRIMARY'; nested
     * exception is com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViol ationException:
     * Duplicate entry '3344092931636290-1784746013' for key 'PRIMARY'
     *
     * @return
     */
    public static boolean isDuplicateInsert(DataAccessException jtException) {
        if (jtException == null) {
            return false;
        }
        // 因为jt对重复键异常进行了包装，所以只能根据emsg进行判断
        String emsg = jtException.getMessage();
        return emsg.startsWith("PreparedStatementCallback") && emsg.indexOf(
                "nested exception is com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException: Duplicate entry") > 0;
    }
}
