package pku.abe.commons.uuid;

import pku.abe.commons.exception.LMException;
import pku.abe.commons.exception.LMExceptionFactor;
import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.memcache.VikaCacheClient;
import pku.abe.commons.util.ApiUtil;
import pku.abe.commons.util.Util;
import pku.abe.commons.util.UuidHelper;

import java.util.Random;

/**
 * Created by LinkedME01 on 16/3/5.
 */
public class UuidCreator {
    public final static int RETRY_TIMES = 5;
    public final static Random randomGenerator = new Random();

    private VikaCacheClient uuidGenerator;

    public long nextId(int bizId) {

        for (int i = 0; i < RETRY_TIMES; i++) {
            long nextId = 0;
            try {
                String uuidKey = bizId + "_" + randomGenerator.nextInt();
                String uuid = (String) uuidGenerator.get(uuidKey);
                nextId = Util.convertLong(uuid);
            } catch (Exception e) {
                ApiLogger.error("Error: in uuidCreator get");
            }
            if (UuidHelper.isValidId(nextId)) {
                return nextId;
            }
            ApiUtil.safeSleep(2 * i);
            ApiLogger
                    .warn("Warn - retry create id, uuidCreator:" + uuidGenerator.getServerPort() + ", tryTime:" + i + ", nextId=" + nextId);
        }

        // FIXME 重连
        throw new LMException(LMExceptionFactor.LM_UUID_ERROR);
    }

    public VikaCacheClient getUuidGenerator() {
        return uuidGenerator;
    }

    public void setUuidGenerator(VikaCacheClient uuidGenerator) {
        this.uuidGenerator = uuidGenerator;
    }

}
