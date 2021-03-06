package pku.abe.commons.counter.component.impl;

import pku.abe.commons.counter.CountConst;
import pku.abe.commons.counter.component.CountComponent;
import pku.abe.commons.counter.dao.CountDao;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CountComponentSimple implements CountComponent {
    private boolean daoEnable = true;
    private CountDao countDao;

    @Override
    public int get(long id, String field) {
        if (daoEnable == false) {
            return CountConst.ValueFalse.intFalse;
        }
        return countDao.get(id, field);
    }

    @Override
    public Map<Long, Integer> gets(long[] ids, String field) {
        if (daoEnable == false) {
            return Collections.emptyMap();
        }
        return countDao.gets(ids, field);
    }

    @Override
    public Map<String, Integer> getAll(long id) {
        if (daoEnable == false) {
            return CountConst.ValueFalse.mapFalse;
        }
        return countDao.getAll(id);
    }

    @Override
    public Map<Long, List<Integer>> gets(long[] ids, String[] fields) {
        if (daoEnable == false) {
            return Collections.emptyMap();
        }
        return countDao.gets(ids, fields);
    }

    @Override
    public Map<Long, Map<String, Integer>> getsAll(long[] ids) {
        if (daoEnable == false) {
            return Collections.emptyMap();
        }
        return countDao.getsAll(ids);
    }

    @Override
    public int incr(long id, String field, int value) {
        if (daoEnable == false) {
            return CountConst.ValueFalse.intFalse;
        }
        return countDao.incr(id, field, value);
    }

    @Override
    public boolean set(long id, String field, int value) {
        if (daoEnable == false) {
            return CountConst.ValueFalse.booleanFalse;
        }
        return countDao.set(id, field, value);
    }

    @Override
    public boolean delete(long id) {
        if (daoEnable == false) {
            return CountConst.ValueFalse.booleanFalse;
        }
        return countDao.delete(id);
    }

    @Override
    public boolean delete(long id, String field) {
        if (daoEnable == false) {
            return CountConst.ValueFalse.booleanFalse;
        }
        return countDao.delete(id, field);
    }

    @Override
    public boolean isDaoEnable() {
        return daoEnable;
    }

    @Override
    public void setDaoEnable(boolean daoEnable) {
        this.daoEnable = daoEnable;
    }

    public CountDao getCountDao() {
        return countDao;
    }

    public void setCountDao(CountDao countDao) {
        this.countDao = countDao;
    }
}
