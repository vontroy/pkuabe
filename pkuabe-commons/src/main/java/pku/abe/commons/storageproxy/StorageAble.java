package pku.abe.commons.storageproxy;

import org.apache.commons.lang.ArrayUtils;

import pku.abe.commons.log.StatLog;
import pku.abe.commons.memcache.CasValue;
import pku.abe.commons.util.Constants;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Means sth. has function of storage, it can be stored by local_cache, mc, db
 *
 * @author Administrator
 */
public abstract class StorageAble<T> {
    private static Map<Type, String> suffixCache = new HashMap<Type, String>();

    // private static Map<Type, StorageLevel> storageLevels = new HashMap<Type, StorageLevel>();

    static {
        StatLog.addCacheStatKeySuffix(CacheSuffix.PAGE_VECTOR_STATUS_DATE);
        StatLog.addCacheStatKeySuffix(CacheSuffix.PAGE_VECTOR_STATUS_DATE_TITLE);
        StatLog.addCacheStatKeySuffix(CacheSuffix.PAGEGROUP_VECTOR_STATUS_DATE);
        StatLog.addCacheStatKeySuffix(CacheSuffix.META_VECTOR_STATUS_DATE);
        StatLog.addCacheStatKeySuffix(CacheSuffix.META_VECTOR_STATUS_LATEST);

        StatLog.addCacheStatKeySuffix(CacheSuffix.VECTOR_STATUS_LATEST);
        StatLog.addCacheStatKeySuffix(CacheSuffix.VECTOR_STATUS_DAYS);
        StatLog.addCacheStatKeySuffix(CacheSuffix.VECTOR_HOUR);
        StatLog.addCacheStatKeySuffix(CacheSuffix.VECTOR_COMMENT_IN);
        StatLog.addCacheStatKeySuffix(CacheSuffix.VECTOR_DM_WITH);
        StatLog.addCacheStatKeySuffix(CacheSuffix.SPEC_VECTOR_UID_STATUS_FLAG);
        StatLog.addCacheStatKeySuffix(CacheSuffix.CONTENT_CACHE_STATUS_PB);
        StatLog.addCacheStatKeySuffix(CacheSuffix.D_VECTOR_FEED_FLAG);

    }

    /**
     * get value via key
     *
     * @param key
     * @return
     */
    public abstract T get(String key);

    /**
     * get value from cache, if cache not exist, read throght to db, but not back set to cache.
     *
     * @param key
     * @return
     */
    public T getNotSetBack(String key) {
        throw new IllegalArgumentException("Unsupport getNotBackSet for key=" + key);
    }

    /**
     * cas get value via key
     *
     * @param key
     * @return
     */
    public abstract CasValue<T> getCas(String key);

    /**
     * get values via keys
     *
     * @param keys
     * @return
     */
    public abstract Map<String, T> getMulti(String[] keys);

    /**
     * save key-value
     *
     * @param key
     * @param value
     */
    public abstract boolean set(String key, T value);

    /**
     * cas save key-value
     *
     * @param key
     * @param value
     */
    @Deprecated
    public abstract boolean setCas(String key, CasValue<T> value);

    /**
     * cache value with expire date, some impl like database may ignore this parameter
     *
     * @param key
     * @param value
     * @param expdate
     * @return
     */
    public abstract boolean set(String key, T value, Date expdate);

    /**
     * vika memcache client use set commond not cas cas cache value with expire date, some impl like
     * database may ignore this parameter
     *
     * @param key
     * @param value
     * @param expdate
     * @return
     */
    @Deprecated
    public abstract boolean setCas(String key, CasValue<T> value, Date expdate);

    // ----------- fix setCas bug -----------------

    /**
     * cas save key-value
     *
     * @param key
     * @param value
     */
    public boolean cas(String key, CasValue<T> value) {
        throw new IllegalArgumentException("Unsupport cas for key=" + key);
    }

    /**
     * cas cache value with expire date, some impl like database may ignore this parameter
     *
     * @param key
     * @param value
     * @param expdate
     * @return
     */
    public boolean cas(String key, CasValue<T> value, Date expdate) {
        throw new IllegalArgumentException("Unsupport cas for key=" + key);
    }

    /**
     * get value only from master via key
     *
     * @param key
     * @return
     */
    public T getFromMaster(String key) {
        throw new IllegalArgumentException(new StringBuffer(32).append("Unsupport getFromMaster for key=").append(key).toString());
    }

    public CasValue<T> getCasFromMaster(String key) {
        throw new IllegalArgumentException(new StringBuffer(32).append("Unsupport getCasFromMaster for key=").append(key).toString());
    }

    /**
     * increase the value of the key if the key exist
     *
     * @param key
     * @return
     */
    public T incr(String key) {
        throw new IllegalArgumentException(new StringBuffer(32).append("Unsupport incr for key=").append(key).toString());
    }

    /**
     * decrease the value if the k-v exists
     *
     * @param key
     * @return
     */
    public T decr(String key) {
        throw new IllegalArgumentException(new StringBuffer(32).append("Unsupport incr for key=").append(key).toString());
    }

    /**
     * append value to cache
     * 
     * @param key
     * @param value
     * @return
     *
     *         boolean append(String key, T value);
     * 
     *         /** append value to cache with expdate
     * @param key
     * @param value
     * @param expdate
     * @return
     *
     *         boolean append(String key, T value, Date expdate);
     */

    /**
     * delete value via key
     *
     * @param key
     * @return
     */
    public abstract boolean delete(String key);

    /**
     * extract sql_key from key, key=sql_key + .mid_fix + .suffix
     *
     * @param key
     * @return
     */
    public static String getSqlKey(String key) {
        int seperatorPos = key.indexOf(Constants.KEY_SEPERATOR);
        if (seperatorPos > 0) {
            return key.substring(0, seperatorPos);
        }
        return key;
    }

    public static String getMiddleKey(String key) {
        int pos1 = key.indexOf(Constants.KEY_SEPERATOR);
        int pos2 = key.lastIndexOf(Constants.KEY_SEPERATOR);
        if (pos1 > 0 && pos2 > pos1) {
            return key.substring(pos1 + 1, pos2);
        }
        return key;
    }

    public static String getFullSqlKey(String key) {
        int seperatorPos = key.lastIndexOf(Constants.KEY_SEPERATOR);
        if (seperatorPos > 0) {
            return key.substring(0, seperatorPos);
        }
        return key;
    }

    public static String[] getSqlKeys(String[] keys) {
        if (keys[0].indexOf(Constants.KEY_SEPERATOR) < 1) {
            return keys;
        }
        String[] sqlKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            sqlKeys[i] = getSqlKey(keys[i]);
        }
        return sqlKeys;
    }

    public static String[] getFullSqlKeys(String[] keys) {
        if (keys[0].indexOf(Constants.KEY_SEPERATOR) < 1) {
            return keys;
        }
        String[] sqlKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            sqlKeys[i] = getFullSqlKey(keys[i]);
        }
        return sqlKeys;
    }

    public static String getKeySuffix(String key) {
        int suffixIndex = key.indexOf(Constants.KEY_SEPERATOR);
        if (suffixIndex > 0) {
            return key.substring(suffixIndex);
        } else {
            return "";
        }
    }

    public static String getBareKeySuffix(String key) {
        int suffixIndex = key.lastIndexOf(Constants.KEY_SEPERATOR);
        if (suffixIndex > 0) {
            return key.substring(suffixIndex);
        } else {
            return "";
        }
    }

    public static String getKeySuffixWithoutSep(String key) {
        int suffixIndex = key.indexOf(Constants.KEY_SEPERATOR);
        if (suffixIndex > 0) {
            return key.substring(suffixIndex + Constants.KEY_SEPERATOR.length());
        } else {
            return "";
        }
    }

    public static String getBareKeySuffixWithoutSep(String key) {
        int suffixIndex = key.lastIndexOf(Constants.KEY_SEPERATOR);
        if (suffixIndex > 0) {
            return key.substring(suffixIndex + Constants.KEY_SEPERATOR.length());
        } else {
            return "";
        }
    }

    static {
        initCacheSuffix();
    }

    private static void initCacheSuffix() {

        suffixCache.put(Type.like_recommend_vector, CacheSuffix.LIKE_RECOMMEND_VECTOR);

        suffixCache.put(Type.page_vector_status_date, CacheSuffix.PAGE_VECTOR_STATUS_DATE);
        suffixCache.put(Type.page_vector_status_date_title, CacheSuffix.PAGE_VECTOR_STATUS_DATE_TITLE);
        suffixCache.put(Type.pagegroup_vector_status_date, CacheSuffix.PAGEGROUP_VECTOR_STATUS_DATE);
        suffixCache.put(Type.meta_vector_status_date, CacheSuffix.META_VECTOR_STATUS_DATE);
        suffixCache.put(Type.meta_vector_status_latest, CacheSuffix.META_VECTOR_STATUS_LATEST);

        suffixCache.put(Type.id_created_at_comment, CacheSuffix.ID_CREATED_COMMENT);
        suffixCache.put(Type.id_created_at_dm, CacheSuffix.ID_CREATED_DM);
        suffixCache.put(Type.id_created_at_status, CacheSuffix.ID_CREATED_STATUS);

        suffixCache.put(Type.state_of_status, CacheSuffix.MAPPING_ID_STATE);
        suffixCache.put(Type.state_of_user, CacheSuffix.MAPPING_UID_STATE);

        suffixCache.put(Type.vector_uid_latest_status, CacheSuffix.VECTOR_STATUS_LATEST);
        suffixCache.put(Type.vector_uid_days_status, CacheSuffix.VECTOR_STATUS_DAYS);

        suffixCache.put(Type.vector_uid_in_comment, CacheSuffix.VECTOR_COMMENT_IN);
        suffixCache.put(Type.vector_uid_out_comment, CacheSuffix.VECTOR_COMMENT_OUT);
        suffixCache.put(Type.vector_uid_out_comment_self, CacheSuffix.VECTOR_COMMENT_OUT_SELF);
        suffixCache.put(Type.vector_uid_in_common_involve_cmt, CacheSuffix.VECTOR_COMMON_INVOLVE_CMT);
        suffixCache.put(Type.vector_uid_in_common_involve_attitude, CacheSuffix.VECTOR_COMMON_INVOLVE_ATTITUDE);
        suffixCache.put(Type.vector_uid_in_comment_attitude, CacheSuffix.VECTOR_COMMENT_ATTITUDE);
        suffixCache.put(Type.vector_uid_in_object_attitude, CacheSuffix.VECTOR_OBJECT_ATTITUDE);


        suffixCache.put(Type.vector_uid_common_cmt, CacheSuffix.VECTOR_COMMON_CMT);
        suffixCache.put(Type.vector_uid_trash_cmt, CacheSuffix.VECTOR_TRASH_CMT);
        suffixCache.put(Type.vector_uid_common_cmt_dl, CacheSuffix.VECTOR_COMMON_CMT_DL);

        suffixCache.put(Type.vector_uid_in_attitude_cf, CacheSuffix.VECTOR_ATTITUDE_IN_CF);
        suffixCache.put(Type.vector_uid_in_attitude, CacheSuffix.VECTOR_ATTITUDE_IN);
        suffixCache.put(Type.vector_uid_in_attitude_orderbystatus, CacheSuffix.SPEC_VECTOR_ATTITUDE_IN_ORDERBYSTATUS);
        suffixCache.put(Type.vector_status_attitudes, CacheSuffix.VECTOR_STATUS_ATTITUDES);
        suffixCache.put(Type.vector_status_attitudes_uid, CacheSuffix.VECTOR_STATUS_ATTITUDES_UID);
        suffixCache.put(Type.unread_status_attitude, CacheSuffix.UNREAD_STATUS_ATTITUDES);
        suffixCache.put(Type.vector_uid_attitude_status, CacheSuffix.VECTOR_UID_ATTITUDE_STATUS);

        suffixCache.put(Type.vector_object_like_list, CacheSuffix.VECTOR_OBJECT_LIKE_LIST);
        suffixCache.put(Type.vector_uid_like_by_me, CacheSuffix.VECTOR_UID_LIKE_BY_ME);
        suffixCache.put(Type.vector_uid_like_by_me_count, CacheSuffix.VECTOR_UID_LIKE_BY_ME_COUNT);
        // favorite likes to_me
        suffixCache.put(Type.favorite_likes_to_me_count, CacheSuffix.FAVORITE_LIKES_TO_ME_COUNT);
        suffixCache.put(Type.vector_uid_attitude_by_me, CacheSuffix.VECTOR_UID_ATTITUDE_BY_ME);
        suffixCache.put(Type.mapping_uid_like_exist, CacheSuffix.MAPPING_UID_LIKE_EXIST);

        suffixCache.put(Type.vector_uid_in_dm, CacheSuffix.VECTOR_DM_IN);
        suffixCache.put(Type.vector_uid_out_dm, CacheSuffix.VECTOR_DM_OUT);
        suffixCache.put(Type.vector_uid_with_dm, CacheSuffix.VECTOR_DM_WITH);
        // suffixCache.put(Type.vector_uid_state_show_self_status,
        // CacheSuffix.VECTOR_STATUS_STATE_SHOW_SELF);
        suffixCache.put(Type.vector_uid_state_no_spread, CacheSuffix.VECTOR_STATUS_STATE_NO_SPREAD);
        suffixCache.put(Type.vector_uid_hour_status, CacheSuffix.VECTOR_HOUR);
        suffixCache.put(Type.vector_uid_inbox, CacheSuffix.VECTOR_INBOX);
        suffixCache.put(Type.vector_uid_mentioned_status, CacheSuffix.VECTOR_MENTION);
        suffixCache.put(Type.vector_uid_repost_status, CacheSuffix.VECTOR_USER_REPOST);
        suffixCache.put(Type.vector_repost_status, CacheSuffix.VECTOR_REPOST);
        suffixCache.put(Type.vector_status_comments, CacheSuffix.VECTOR_STATUS_COMMENTS);
        suffixCache.put(Type.vector_status_member_comments, CacheSuffix.STATUS_MEMBER_COMMENTS_VECTOR);
        suffixCache.put(Type.vector_status_comments_asc, CacheSuffix.VECTOR_STATUS_COMMENTS_ASC);
        suffixCache.put(Type.vector_uid_cmt_mention, CacheSuffix.VECTOR_CMT_MENTION);

        // activity
        suffixCache.put(Type.new_vector_in_activiy, CacheSuffix.NEW_VECTOR_ACTIVITY_IN);

        // new activity
        suffixCache.put(Type.activity_user_vector, CacheSuffix.ACTIVITY_USER_VECTOR);
        suffixCache.put(Type.activity_user_new_vector, CacheSuffix.ACTIVITY_USER_NEW_VECTOR);
        suffixCache.put(Type.activity_user_index_vector, CacheSuffix.ACTIVITY_USER_INDEX_VECTOR);
        suffixCache.put(Type.activity_content, CacheSuffix.ACTIVITY_CONTENT);
        suffixCache.put(Type.activity_template, CacheSuffix.ACTIVITY_TEMPLATE);
        // user group status vector
        suffixCache.put(Type.user_group_status_vector, CacheSuffix.USER_GROUP_STATUS_VECTOR);
        suffixCache.put(Type.user_metagroup_status_vector, CacheSuffix.USER_METAGROUP_STATUS_VECTOR);
        // secret group dlvector
        suffixCache.put(Type.sgroup_dlvector, CacheSuffix.SGROUP_DLVECTOR);

        // suffixCache.put(Type.content_feature_value, CacheSuffix.CONTENT_FEATURE_VALUE);

        suffixCache.put(Type.vector_uid_cmt_friend, CacheSuffix.VECTOR_CMT_BY_FRIEND);
        suffixCache.put(Type.vector_uid_cmt_closefriend, CacheSuffix.VECTOR_CMT_BY_CLOSEFRIEND);
        suffixCache.put(Type.vector_uid_cmt_stranger, CacheSuffix.VECTOR_CMT_BY_STRANGER);
        suffixCache.put(Type.vector_uid_mention_friend, CacheSuffix.VECTOR_MENTION_BY_FRIEND);
        suffixCache.put(Type.vector_uid_mention_original, CacheSuffix.VECTOR_MENTION_BY_ORIGINAL);
        suffixCache.put(Type.vector_uid_mention_friend_original, CacheSuffix.VECTOR_MENTION_BY_FRIEND_ORIGINAL);
        suffixCache.put(Type.vector_uid_mention_closefriend, CacheSuffix.VECTOR_MENTION_BY_CLOSEFRIEND);
        suffixCache.put(Type.vector_uid_mention_closefriend_original, CacheSuffix.VECTOR_MENTION_BY_CLOSEFRIEND_ORIGINAL);

        suffixCache.put(Type.spec_vector_uid_status_flag, CacheSuffix.SPEC_VECTOR_UID_STATUS_FLAG);
        suffixCache.put(Type.d_vector_feed_flag, CacheSuffix.D_VECTOR_FEED_FLAG);

        // suffixCache.put(Type.spec_vector_uid_status_flag_self,
        // CacheSuffix.SPEC_VECTOR_UID_STATUS_FLAG_SELF);

        suffixCache.put(Type.content_longtext_status, CacheSuffix.CONTENT_CACHE_LONGTEXT_STATUS);
        suffixCache.put(Type.content_slice_pb, CacheSuffix.CONTENT_CACHE__SLICE_PB);

        suffixCache.put(Type.content_id_status_pb, CacheSuffix.CONTENT_CACHE_STATUS_PB);
        suffixCache.put(Type.content_id_comment_pb, CacheSuffix.CONTENT_CACHE_COMMENT_PB);
        suffixCache.put(Type.content_id_dm_pb, CacheSuffix.CONTENT_CACHE_DM_PB);
        suffixCache.put(Type.status_exposure_policy, CacheSuffix.STATUS_EXPOSURE_POLICY);
        suffixCache.put(Type.content_id_attitude_pb, CacheSuffix.CONTENT_CACHE_ATTITUDE_PB);

        // suffixCache.put(Type.mapping_nick_uid, CacheSuffix.MAPPING_NICK_UID_FLAG);
        suffixCache.put(Type.mapping_id_mid_comment, CacheSuffix.MAPPING_ID_MID_COMMENT);
        suffixCache.put(Type.mapping_mid_id_comment, CacheSuffix.MAPPING_MID_ID_COMMENT);
        suffixCache.put(Type.mapping_id_mid_dm, CacheSuffix.MAPPING_ID_MID_DM);
        suffixCache.put(Type.mapping_mid_id_dm_in, CacheSuffix.MAPPING_MID_ID_DM_IN);
        suffixCache.put(Type.mapping_mid_id_dm_out, CacheSuffix.MAPPING_MID_ID_DM_OUT);
        suffixCache.put(Type.mapping_id_sourceid_status, CacheSuffix.MAPPING_ID_SOURCEID_STATUS);
        suffixCache.put(Type.mapping_id_mid_status, CacheSuffix.MAPPING_ID_MID_STATUS);
        suffixCache.put(Type.mapping_mid_id_status, CacheSuffix.MAPPING_MID_ID_STATUS);
        suffixCache.put(Type.mapping_id_uid_status, CacheSuffix.MAPPING_ID_UID_STATUS);
        suffixCache.put(Type.mapping_id_uid_comment, CacheSuffix.MAPPING_ID_UID_COMMENT);
        suffixCache.put(Type.mapping_uid_watermark, CacheSuffix.MAPPING_UID_WATERMARK);
        suffixCache.put(Type.mapping_id_flag_value_status, CacheSuffix.MAPPING_ID_FLAG_VALUE_STATUS);
        suffixCache.put(Type.mapping_id_feature_value_comment, CacheSuffix.MAPPING_ID_FEATURE_VALUE_COMMENT);
        suffixCache.put(Type.mapping_cid_uid_comment, CacheSuffix.MAPPING_CID_UID_COMMENT);

        suffixCache.put(Type.mapping_apiid_uuid_status, CacheSuffix.MAPPING_APIID_UUID_STAUTS);
        suffixCache.put(Type.mapping_uuid_apiid_status, CacheSuffix.MAPPING_UUID_APIID_STATUS);
        suffixCache.put(Type.mapping_apiid_uuid_comment, CacheSuffix.MAPPING_APIID_UUID_COMMENT);
        suffixCache.put(Type.mapping_uuid_apiid_comment, CacheSuffix.MAPPING_UUID_APIID_COMMENT);

        suffixCache.put(Type.attention_uid_friends, CacheSuffix.ATTENTION_FRIENDS);
        suffixCache.put(Type.attention_uid_followers, CacheSuffix.ATTENTION_FOLLOWERS);
        suffixCache.put(Type.attention_uid_bothfriends, CacheSuffix.ATTENTION_BOTHFRIENDS);
        suffixCache.put(Type.attention_uid_friends_source, CacheSuffix.ATTENTION_FRIENDS_SOURCE);
        suffixCache.put(Type.attention_uid_friends_page_source, CacheSuffix.ATTENTION_FRIENDS_PAGE_SOURCE);
        suffixCache.put(Type.attention_uid_followers_source, CacheSuffix.ATTENTION_FOLLOWERS_SOURCE);
        suffixCache.put(Type.attention_uid_secret, CacheSuffix.ATTENTION_SECRET);
        suffixCache.put(Type.oset_uid_dm_withUser, CacheSuffix.OSET_DM_WITH_USER);
        suffixCache.put(Type.omap_uid_dm_withUser, CacheSuffix.OMAP_DM_WITH_USER);

        suffixCache.put(Type.sinauser_uid_pbuser, CacheSuffix.SINA_PB_USER);
        suffixCache.put(Type.sinauser_uid_persence, CacheSuffix.SINA_USER_PERSENCE);
        // suffixCache.put(Type.sinauser_nick_uid, CacheSuffix.SINA_USER_NICK_UID);
        suffixCache.put(Type.sinauser_domain_uid, CacheSuffix.SINA_USER_DOMAIN_UID);
        suffixCache.put(Type.sinauser_weinum_uid, CacheSuffix.SINA_USER_WEINUM_UID);
        suffixCache.put(Type.sinauser_weinum, CacheSuffix.SINA_USER_WEINUM);
        suffixCache.put(Type.sinauser_filter, CacheSuffix.SINA_USER_FILTER);
        suffixCache.put(Type.sinauser_type, CacheSuffix.SINA_USER_TYPE);
        suffixCache.put(Type.sinauser_level, CacheSuffix.SINA_USER_LEVEL);
        suffixCache.put(Type.sinauser_privacys, CacheSuffix.SINA_USER_PRIVACY);
        suffixCache.put(Type.sinauser_settings, CacheSuffix.SINA_USER_EXT_SETTINGS);
        suffixCache.put(Type.sinauser_login_name_uid, CacheSuffix.SINA_USER_LOGIN_NAME_UID);
        suffixCache.put(Type.user_last_status_id, CacheSuffix.USER_LAST_STATUS_ID);


        suffixCache.put(Type.page_uid_flag_friend, CacheSuffix.PAGE_CACHE_UID_FLAG_FRIEND);
        suffixCache.put(Type.page_list_flag, CacheSuffix.PAGE_CACHE_LIST_FLAG);
        suffixCache.put(Type.page_repost_flag, CacheSuffix.PAGE_CACHE_REPOST_FLAG);
        suffixCache.put(Type.page_user_repost_flag, CacheSuffix.PAGE_CACHE_USER_REPOST_FLAG);
        suffixCache.put(Type.page_uid_flag_user, CacheSuffix.PAGE_CACHE_UID_FLAG_USER);
        suffixCache.put(Type.page_uid_friend_timeline, CacheSuffix.PAGE_CACHE_UID_FRIEND_TIMELINE);
        suffixCache.put(Type.page_list_timeline, CacheSuffix.PAGE_CACHE_LIST_TIMELINE);
        suffixCache.put(Type.page_repost_timeline, CacheSuffix.PAGE_CACHE_REPOST_TIMELINE);
        suffixCache.put(Type.page_user_repost_timeline, CacheSuffix.PAGE_CACHE_USER_REPOST_TIMELINE);
        suffixCache.put(Type.page_uid_user_timeline, CacheSuffix.PAGE_CACHE_UID_USER_TIMELINE);

        suffixCache.put(Type.group_uid_info_json, CacheSuffix.GROUP_UID_INFO_JSON);
        suffixCache.put(Type.group_uid_info_xml, CacheSuffix.GROUP_UID_INFO_XML);
        suffixCache.put(Type.group_gidUid_member_json, CacheSuffix.GROUP_GIDUID_MEMBER_JSON);
        suffixCache.put(Type.group_gidUid_member_xml, CacheSuffix.GROUP_GIDUID_MEMBER_XML);
        suffixCache.put(Type.group_listed_json, CacheSuffix.GROUP_LISTED_JSON);
        suffixCache.put(Type.group_listed_xml, CacheSuffix.GROUP_LISTED_XML);
        suffixCache.put(Type.group_uids, CacheSuffix.GROUP_UIDS);
        suffixCache.put(Type.group_gid_info, CacheSuffix.GROUP_GID_INFO);
        suffixCache.put(Type.group_gid_members, CacheSuffix.GROUP_GID_MEMBERS);
        suffixCache.put(Type.group_listed, CacheSuffix.GROUP_LISTED);
        suffixCache.put(Type.group_ungroup, CacheSuffix.GROUP_UNGROUP);
        suffixCache.put(Type.group_like, CacheSuffix.GROUP_LIKE);

        suffixCache.put(Type.group_member_reverse, CacheSuffix.GROUP_MEMBER_REVERSE);
        suffixCache.put(Type.group_member_reverse_set_caching, CacheSuffix.GROUP_MEMBER_REVERSE_SET_CACHING);

        suffixCache.put(Type.user_ext_remark, CacheSuffix.USER_EXT_REMARK);
        suffixCache.put(Type.user_rank, CacheSuffix.USER_RANK);

        suffixCache.put(Type.counter_repeat_detect_status_inc, CacheSuffix.COUNTER_REPEAT_DETECTOR_STATUS_INC);
        suffixCache.put(Type.counter_repeat_detect_status_dec, CacheSuffix.COUNTER_REPEAT_DETECTOR_STATUS_DEC);
        suffixCache.put(Type.counter_repeat_detect_comment_inc, CacheSuffix.COUNTER_REPEAT_DETECTOR_COMMENT_INC);
        suffixCache.put(Type.counter_repeat_detect_comment_dec, CacheSuffix.COUNTER_REPEAT_DETECTOR_COMMENT_DEC);
        suffixCache.put(Type.counter_repeat_detect_attitude_inc, CacheSuffix.COUNTER_REPEAT_DETECTOR_ATTITUDE_INC);

        suffixCache.put(Type.counter_user_apistatus, CacheSuffix.COUNTER_USER_APISTATUS);
        suffixCache.put(Type.counter_user_apistatus_without_spec, CacheSuffix.COUNTER_USER_APISTATUS_WITHOUT_SPEC);
        suffixCache.put(Type.counter_user_fav, CacheSuffix.COUNTER_USER_FAV);
        suffixCache.put(Type.counter_user_status, CacheSuffix.COUNTER_USER_STATUS);
        suffixCache.put(Type.counter_user_follower, CacheSuffix.COUNTER_USER_FOLLOWER);
        suffixCache.put(Type.counter_user_following, CacheSuffix.COUNTER_USER_FOLLOWING);
        suffixCache.put(Type.counter_user_api_follower, CacheSuffix.COUNTER_USER_API_FOLLOWER);
        suffixCache.put(Type.counter_user_api_following, CacheSuffix.COUNTER_USER_API_FOLLOWING);
        suffixCache.put(Type.counter_user_bifollower, CacheSuffix.COUNTER_USER_BIFOLLOWER);
        suffixCache.put(Type.counter_user_comment_by_me, CacheSuffix.COUNTER_USER_COMMENT_BY_ME);
        suffixCache.put(Type.counter_user_comment_to_me_by_friends, CacheSuffix.COUNTER_USER_COMMENT_TO_ME_BY_FRIENDS);
        suffixCache.put(Type.counter_user_comment_to_me, CacheSuffix.COUNTER_USER_COMMENT_TO_ME);
        suffixCache.put(Type.counter_user_metions, CacheSuffix.COUNTER_USER_METIONS);
        suffixCache.put(Type.counter_user_comment_metions, CacheSuffix.COUNTER_USER_COMMENT_METIONS);
        suffixCache.put(Type.counter_user_repost_by_me, CacheSuffix.COUNTER_USER_REPOST_BY_ME);
        suffixCache.put(Type.counter_status_rt, CacheSuffix.COUNTER_STATUS_RT);
        suffixCache.put(Type.counter_status_comment, CacheSuffix.COUNTER_STATUS_COMMENT);
        suffixCache.put(Type.counter_user_attention_secret, CacheSuffix.COUNTER_USER_ATTENTION_SECRET);
        suffixCache.put(Type.counter_user_attitude_to_me, CacheSuffix.COUNTER_USER_ATTITUDE_TIMELINE_TO_ME);
        suffixCache.put(Type.counter_user_attitude_to_me_order_by_status, CacheSuffix.COUNTER_USER_ATTITUDE_TIMELINE_TO_ME_ORDER_BY_STATUS);
        suffixCache.put(Type.counter_user_direct_group_status, CacheSuffix.COUNTER_USER_DIRECT_GROUP_STATUS);
        suffixCache.put(Type.counter_status_attitude, CacheSuffix.COUNTER_STATUS_ATTITUDE);
        suffixCache.put(Type.counter_user_commoncmt_to_me, CacheSuffix.COUNTER_USER_COMMONCMT_TIMELINE_TO_ME);
        suffixCache.put(Type.counter_user_group_status, CacheSuffix.COUNTER_USER_GROUP_STATUS);
        suffixCache.put(Type.counter_node_status, CacheSuffix.COUNTER_NODE_STATUS);
        suffixCache.put(Type.counter_node_following, CacheSuffix.COUNTER_NODE_FOLLOWING);
        suffixCache.put(Type.counter_node_follower, CacheSuffix.COUNTER_NODE_FOLLOWER);
        suffixCache.put(Type.counter_user_comment_attitude_to_me, CacheSuffix.COUNTER_USER_COMMENT_ATTITUDE_TO_ME);
        suffixCache.put(Type.counter_user_object_attitude_to_me, CacheSuffix.COUNTER_USER_OBJECT_ATTITUDE_TO_ME);

        suffixCache.put(Type.counter_group_like, CacheSuffix.COUNTER_GROUP_LIKE);
        suffixCache.put(Type.counter_group_member, CacheSuffix.COUNTER_GROUP_MEMBER);
        suffixCache.put(Type.counter_group_member_reverse, CacheSuffix.COUNTER_GROUP_MEMBER_REVERSE);

        suffixCache.put(Type.counter_pick, CacheSuffix.COUNTER_PICK);

        suffixCache.put(Type.remind_360_user_pulgin, CacheSuffix.REMIND_360_PLUGIN);

        suffixCache.put(Type.dm_uids_summary, CacheSuffix.DM_UIDS_SUMMARY);

        suffixCache.put(Type.friendlist_attentions, CacheSuffix.FRIENDLIST_ATTENTIONS);
        suffixCache.put(Type.friendlist_fans_all, CacheSuffix.FRIENDLIST_FANS_ALL);
        suffixCache.put(Type.friendlist_fans_new, CacheSuffix.FRIENDLIST_FANS_NEW);
        suffixCache.put(Type.friendlist_fans_foremost, CacheSuffix.FRIENDLIST_FANS_FOREMOST);
        suffixCache.put(Type.friendlist_bothfans, CacheSuffix.FRIENDLIST_BOTH_FANS);
        suffixCache.put(Type.friendlist_remark, CacheSuffix.FRIENDLIST_REMARK);
        suffixCache.put(Type.fanslist_remark, CacheSuffix.FANSLIST_REMARK);
        suffixCache.put(Type.nonelist_remark, CacheSuffix.NONELIST_REMARK);
        suffixCache.put(Type.friendlist_filtered, CacheSuffix.FRIENDLIST_FILTERED);
        suffixCache.put(Type.friendlist_attentions_residual, CacheSuffix.FRIENDLIST_ATTENTIONS_RESIDUAL);
        suffixCache.put(Type.friendlist_bothfans_residual, CacheSuffix.FRIENDLIST_BOTHFANS_RESIDUAL);
        suffixCache.put(Type.friendlist_blacklist, CacheSuffix.FRIENDLIST_BLACKLIST);
        suffixCache.put(Type.friendlist_recommend, CacheSuffix.FRIENDLIST_RECOMMEND);
        suffixCache.put(Type.friendlist_interaction, CacheSuffix.FRIENDLIST_INTERACTION);
        suffixCache.put(Type.graph_followers_list, CacheSuffix.GRAPH_FOLLOWERS_LIST);
        suffixCache.put(Type.tourist_followings_list, CacheSuffix.TOURIST_FOLLOWINGS_LIST);

        suffixCache.put(Type.both_friendlist_filtered, CacheSuffix.BOTH_FRIENDLIST_FILTERED);

        suffixCache.put(Type.both_friendlist_reverse_filtered, CacheSuffix.BOTH_FRIENDLIST_REVERSE_FILTERED);

        suffixCache.put(Type.both_friendlist_be_reverse_filtered, CacheSuffix.BOTH_FRIENDLIST_BE_REVERSE_FILTERED);
        suffixCache.put(Type.both_friendlist_relations, CacheSuffix.BOTH_FRIENDLIST_RELATIONS);
        suffixCache.put(Type.ties_close_friends_initiate, CacheSuffix.TIES_CLOSE_FRIENDS_INITIATE);
        suffixCache.put(Type.ties_close_friends, CacheSuffix.TIES_CLOSE_FRIENDS);

        suffixCache.put(Type.vector_uid_cmt_mention, CacheSuffix.VECTOR_CMT_MENTION);

        suffixCache.put(Type.lists_menbers_show_count, CacheSuffix.LISTS_MENBERS_SHOW_COUNT);
        suffixCache.put(Type.lists_sub_show_count, CacheSuffix.LISTS_SUB_SHOW_COUNT);
        suffixCache.put(Type.lists_user_listed_count, CacheSuffix.LISTS_USER_LISTED_COUNT);
        suffixCache.put(Type.lists_user_sub_count, CacheSuffix.LISTS_USER_SUB_COUNT);
        suffixCache.put(Type.exclued_mention_uids, CacheSuffix.EXCLUDED_MENTION_UIDS);


        suffixCache.put(Type.blacklist_uid, CacheSuffix.BLACKLIST_UID);
        suffixCache.put(Type.uninterested, CacheSuffix.UNINTERESTED_UID);
        suffixCache.put(Type.tail_spec_vector_uid_status, CacheSuffix.TAIL_SPEC_VECTOR_UID_STATUS);
        suffixCache.put(Type.mapping_cid_uid_comment, CacheSuffix.MAPPING_CID_UID_COMMENT);
        suffixCache.put(Type.feedfilter_vector, CacheSuffix.FEEDFILTER_VECTOR);
        suffixCache.put(Type.feedfilter_item, CacheSuffix.FEEDFILTER_ITEM);

        suffixCache.put(Type.cf_cm_to_me_count, CacheSuffix.CLOSEFRIEND_COMMENTTOME_COUNT);
        suffixCache.put(Type.cf_direct_count, CacheSuffix.CLOSEFRIEND_DIRECT_COUNT);
        suffixCache.put(Type.cf_mt_to_me_count, CacheSuffix.CLOSEFRIEND_MENTIONTOME_COUNT);
        suffixCache.put(Type.cf_omt_to_me_count, CacheSuffix.CLOSEFRIEND_ORIGINMENTIONTOME_COUNT);

        suffixCache.put(Type.common_cmt_remind, CacheSuffix.COMMON_CMT_REMIND);
        // 锟斤拷尾type锟斤拷锟斤拷锟斤拷2锟杰观察，之锟斤拷去锟斤拷 fishermen 2011.7.19
        // suffixCache.put(Type.tail_vector_status, CacheSuffix.TAIL_VECTOR_STATUS);
        // suffixCache.put(Type.tail_vector_status_filter,CacheSuffix.TAIL_VECTOR_STATUS_FILTER);
        // suffixCache.put(Type.tail_vector_status_filter_self,CacheSuffix.TAIL_VECTOR_STATUS_FILTER_SELF);

        // suffixCache.put(Type.tail_status_mention, CacheSuffix.TAIL_STATUS_MENTION);
        // suffixCache.put(Type.tail_comment_status, CacheSuffix.TAIL_CONTENT_STATUS);
        // suffixCache.put(Type.tail_comment_by_me, CacheSuffix.TAIL_COMMENTS_BY_ME);
        // suffixCache.put(Type.tail_comment_timeline, CacheSuffix.TAIL_COMMENTS_TIMELINE);
        // suffixCache.put(Type.tail_comment_to_me, CacheSuffix.TAIL_COMMENTS_TO_ME);
        // suffixCache.put(Type.tail_dm_by_me, CacheSuffix.TAIL_DM_BY_ME);
        // suffixCache.put(Type.tail_dm_to_me, CacheSuffix.TAIL_DM_TO_ME);
        // suffixCache.put(Type.tail_dm_with, CacheSuffix.TAIL_DM_WITH);

        // suffixCache.put(Type.secondary_index_cmt_by_me, CacheSuffix.SECONDARY_INDEX_CMT_BYME);
        // suffixCache.put(Type.secondary_index_cmt_status, CacheSuffix.SECONDARY_INDEX_CMT_STATUS);
        // suffixCache.put(Type.secondary_index_cmt_to_me, CacheSuffix.SECONDARY_INDEX_CMT_TOME);
        // suffixCache.put(Type.secondary_index_dm_by_me, CacheSuffix.SECONDARY_INDEX_DM_BY_ME);
        // suffixCache.put(Type.secondary_index_dm_to_me, CacheSuffix.SECONDARY_INDEX_DM_TO_ME);
        // suffixCache.put(Type.secondary_index_dm_with, CacheSuffix.SECONDARY_INDEX_DM_WITH);
        // suffixCache.put(Type.secondary_index_mention, CacheSuffix.SECONDARY_INDEX_MENTION);
        // suffixCache.put(Type.secondary_index_status_timeline,
        // CacheSuffix.SECONDARY_INDEX_STATUS_TIMELINE);
        // suffixCache.put(Type.secondary_index_cmt_timeline,
        // CacheSuffix.SECONDARY_INDEX_CMT_TIMELINE);

        suffixCache.put(Type.counter_user_attitude_to_me_by_friends, CacheSuffix.COUNTER_USER_ATTITUDE_TO_ME_BY_FRIENDS);
        suffixCache.put(Type.counter_user_attitude_to_me_by_cf, CacheSuffix.COUNTER_USER_ATTITUDE_TO_ME_BY_CF);
        suffixCache.put(Type.counter_user_attitude_to_me_by_strangers, CacheSuffix.COUNTER_USER_ATTITUDE_TO_ME_BY_STRAGERS);

        suffixCache.put(Type.counter_user_attitude_to_me_order_by_status_by_friends,
                CacheSuffix.COUNTER_USER_ATTITUDE_TO_ME_ORDER_BY_STATUS_BY_FRIENDS);
        suffixCache.put(Type.counter_user_attitude_to_me_order_by_status_by_cf,
                CacheSuffix.COUNTER_USER_ATTITUDE_TO_ME_ORDER_BY_STATUS_BY_CF);
        suffixCache.put(Type.counter_user_attitude_to_me_order_by_status_by_strangers,
                CacheSuffix.COUNTER_USER_ATTITUDE_TO_ME_ORDER_BY_STATUS_BY_STRAGERS);

        suffixCache.put(Type.counter_status_attitude_by_friends, CacheSuffix.COUNTER_STATUS_ATTITUDE_BY_FRIENDS);
        suffixCache.put(Type.counter_status_attitude_by_cf, CacheSuffix.COUNTER_STATUS_ATTITUDE_BY_CF);
        suffixCache.put(Type.counter_status_attitude_by_strangers, CacheSuffix.COUNTER_STATUS_ATTITUDE_BY_STRAGERS);

        suffixCache.put(Type.counter_user_diff_status, CacheSuffix.COUNTER_USER_DIFF_STATUS);
        suffixCache.put(Type.counter_user_diff_follower, CacheSuffix.COUNTER_USER_DIFF_FOLLOWER);
        suffixCache.put(Type.counter_user_diff_following, CacheSuffix.COUNTER_USER_DIFF_FOLLOWING);

        suffixCache.put(Type.counter_object_comment, CacheSuffix.COUNTER_OBJECT_COMMENTS);
        suffixCache.put(Type.counter_object_status, CacheSuffix.COUNTER_OBJECT_STATUS);

        suffixCache.put(Type.counter_microcontacts, CacheSuffix.COUNTER_MICROCONTACTS);

        suffixCache.put(Type.counter_page_att, CacheSuffix.COUNTER_PAGE_ATTENTION);
        suffixCache.put(Type.counter_page_fans, CacheSuffix.COUNTER_PAGE_FANS);

        suffixCache.put(Type.counter_page_status, CacheSuffix.COUNTER_PAGE_STATUS);

        suffixCache.put(Type.counter_frozen_followers, CacheSuffix.COUNTER_FROZEN_FOLLOWERS);
    }

    /**
     * return if the value with the type is saved in MC with cas, now only used for vectorItem
     *
     * @param key
     * @return
     */
    public static boolean isCasKey(String key) {
        int pos = key.lastIndexOf(Constants.KEY_SEPERATOR);
        String suffix = key.substring(pos + 1);
        return suffix.startsWith("v") || suffix.startsWith("sv") || suffix.startsWith("dlv") || suffix.startsWith("tdv")
                || suffix.startsWith("dvl");
    }

    public static String getCacheKey(String rawKey, Type type) {
        return rawKey + getCacheSuffix(type);
    }

    public static String getCacheKey(long rawKey, Type type) {
        return rawKey + getCacheSuffix(type);
    }

    public static String getCacheKey(long rawKey, String keySuffix) {
        return rawKey + keySuffix;
    }

    public static String[] getCacheKeys(long[] rawKeys, Type type) {
        String cacheSuffix = getCacheSuffix(type);
        return getCacheKeys(rawKeys, cacheSuffix);
    }

    public static String[] getCacheKeys(long[] rawKeys, String cacheSuffix) {
        if (ArrayUtils.isEmpty(rawKeys)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        String[] keys = new String[rawKeys.length];
        for (int i = 0; i < rawKeys.length; i++) {
            keys[i] = rawKeys[i] + cacheSuffix;
        }
        return keys;
    }

    /**
     * params: rawKey-ids, Type, midfix example of midfix: .21 1242322322.21.vsh
     */
    public static String[] getCacheKeys(long[] rawKeys, Type type, String midfix) {
        if (ArrayUtils.isEmpty(rawKeys)) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        String cacheSuffix = getCacheSuffix(type);
        String[] keys = new String[rawKeys.length];
        for (int i = 0; i < rawKeys.length; i++) {
            keys[i] = rawKeys[i] + midfix + cacheSuffix;
        }
        return keys;
    }

    public static String getCacheKey(String rawKey, Type type, String midfix) {
        return rawKey + midfix + getCacheSuffix(type);
    }

    public static String getCacheKey(long rawKey, Type type, String midfix) {
        return rawKey + midfix + getCacheSuffix(type);
    }

    public static String getHashCacheKey(String rawKey, Type type, String field) {
        return rawKey + getCacheSuffix(type) + Constants.KEY_SEPERATOR + field;
    }

    public static String getHashCacheKey(long rawKey, Type type, long field) {
        return rawKey + getCacheSuffix(type) + Constants.KEY_SEPERATOR + field;
    }

    public static String getHashKey(String rawKey, String field) {
        return rawKey + Constants.KEY_SEPERATOR + field;
    }

    public static String getHashKey(long rawKey, long field) {
        return rawKey + Constants.KEY_SEPERATOR + field;
    }

    public static String getCacheSuffix(Type type) {
        String suffix = suffixCache.get(type);
        if (suffix != null) {
            return suffix;
        } else {
            throw new IllegalArgumentException("Error: illegal StorageAble.type, type:" + type);
        }
    }

    public static enum Type {

        like_recommend_vector, page_vector_status_date, page_vector_status_date_title, pagegroup_vector_status_date,

        meta_vector_status_date, meta_vector_status_latest,

        id_created_at_comment, id_created_at_dm, id_created_at_status,

        // priority vector
        vector_uid_latest_status, vector_uid_days_status,

        // common vector
        vector_uid_in_comment, vector_uid_out_comment, vector_uid_out_comment_self,

        vector_uid_in_common_involve_cmt, // 2014新版共同参与评论收件箱缓存

        // common comment vector
        vector_uid_common_cmt, vector_uid_trash_cmt, vector_uid_common_cmt_dl,

        // attitude vector
        vector_uid_in_attitude_cf, vector_uid_in_attitude, vector_uid_in_attitude_orderbystatus, vector_status_attitudes, vector_status_attitudes_uid, unread_status_attitude, vector_uid_attitude_status, vector_uid_in_common_involve_attitude, // 共同赞
        vector_uid_in_comment_attitude, // 赞评论
        vector_uid_in_object_attitude, // 赞对象

        vector_object_like_list, vector_uid_like_by_me, vector_uid_like_by_me_count, vector_uid_attitude_by_me, mapping_uid_like_exist,

        vector_uid_in_dm, vector_uid_out_dm, vector_uid_with_dm,

        // 雷达to_me赞计数
        favorite_likes_to_me_count,

        // vector_uid_state_show_self_status,
        vector_uid_state_no_spread, vector_uid_hour_status, vector_uid_inbox, vector_uid_mentioned_status, vector_uid_cmt_mention, vector_uid_repost_status, vector_repost_status, vector_status_comments, vector_status_comments_asc,

        vector_status_member_comments,

        // vector, ususally in Level 2 cache
        vector_uid_cmt_friend, vector_uid_cmt_stranger, vector_uid_cmt_closefriend, vector_uid_mention_friend, vector_uid_mention_original, vector_uid_mention_friend_original, vector_uid_mention_closefriend, vector_uid_mention_closefriend_original,

        // activity
        new_vector_in_activiy,

        // new activity
        activity_user_vector, activity_user_new_vector, activity_user_index_vector, activity_content, activity_template,

        user_group_status_vector, user_metagroup_status_vector, sgroup_dlvector,

        mapping_id_flag_value_status, mapping_id_feature_value_comment,

        spec_vector_uid_status_flag, d_vector_feed_flag,

        user_last_status_id,
        // spec_vector_uid_status_flag_self,

        // content_feature_value,

        // content_id_status_xml,
        // content_id_status_json,
        content_id_status_pb,

        // content_id_comment_xml,
        // content_id_comment_json,
        content_id_comment_pb, content_id_attitude_pb,
        // content_id_dm_xml,
        // content_id_dm_json,
        content_id_dm_pb,

        status_exposure_policy,

        mapping_nick_uid, mapping_id_mid_comment, mapping_mid_id_comment, mapping_id_mid_dm, mapping_mid_id_dm_in, mapping_mid_id_dm_out, mapping_id_sourceid_status, mapping_id_mid_status, mapping_mid_id_status,
        // mapping_id_apistate_status,
        // mapping_id_apistate_comment,
        mapping_id_uid_status, mapping_id_uid_comment, mapping_cid_uid_comment, mapping_uid_watermark,

        mapping_apiid_uuid_status, mapping_uuid_apiid_status, mapping_apiid_uuid_comment, mapping_uuid_apiid_comment,

        attention_uid_friends, attention_uid_followers, attention_uid_bothfriends, attention_uid_friends_source, attention_uid_friends_page_source, attention_uid_followers_source, attention_uid_secret,


        sinauser_uid_pbuser, sinauser_uid_persence, // online status
        sinauser_domain_uid, sinauser_nick_uid, sinauser_weinum_uid, sinauser_weinum, sinauser_filter, sinauser_privacys, sinauser_settings, sinauser_type, sinauser_level, sinauser_login_name_uid,

        page_uid_flag_friend, page_repost_flag, page_user_repost_flag, page_list_flag, page_uid_flag_user, page_uid_friend_timeline, page_repost_timeline, page_user_repost_timeline, page_list_timeline, page_uid_user_timeline,

        group_uid_info_json, group_uid_info_xml, group_gidUid_member_json, group_gidUid_member_xml, group_listed_json, group_listed_xml, group_uids, group_gid_info, group_listed, group_gid_members, group_ungroup, group_like,

        group_member_reverse, group_member_reverse_set_caching,

        user_ext_remark, user_rank,

        oset_uid_dm_withUser, omap_uid_dm_withUser,

        counter_user_apistatus, counter_user_fav, counter_user_apistatus_without_spec, counter_user_status, counter_user_following, counter_user_follower, counter_user_api_following, counter_user_api_follower, counter_user_bifollower, counter_user_comment_by_me, counter_user_comment_to_me_by_friends, counter_user_comment_to_me, counter_user_metions, counter_user_comment_metions, counter_user_repost_by_me, counter_user_attention_secret, counter_user_attitude_to_me, counter_user_direct_group_status, counter_user_attitude_to_me_order_by_status, counter_status_attitude, counter_user_commoncmt_to_me, counter_user_group_status, counter_user_comment_attitude_to_me, counter_user_object_attitude_to_me,

        counter_node_status, counter_node_following, counter_node_follower,

        counter_group_like, counter_group_member, counter_group_member_reverse,

        counter_pick,

        counter_repeat_detect_status_inc, counter_repeat_detect_status_dec, counter_repeat_detect_comment_inc, counter_repeat_detect_comment_dec,

        counter_repeat_detect_attitude_inc,

        counter_status_rt, counter_status_comment,

        remind_360_user_pulgin,

        friendlist_fans_all, friendlist_fans_new, friendlist_fans_foremost, friendlist_attentions, friendlist_bothfans, friendlist_remark, fanslist_remark, nonelist_remark, friendlist_filtered, friendlist_attentions_residual, friendlist_bothfans_residual, friendlist_blacklist, friendlist_recommend, friendlist_interaction,

        // 双向关注屏蔽列表
        both_friendlist_filtered,
        // 双向关注反向屏蔽列表
        both_friendlist_reverse_filtered,
        // 双向关注哪些人屏蔽了我的列表
        both_friendlist_be_reverse_filtered,
        // 双向关注更进一步的关系
        both_friendlist_relations,
        // 关系粉丝列表缓存的 Key。
        graph_followers_list,
        /**
         * 游客关注列表缓存的 Key。
         */
        tourist_followings_list,

        ties_close_friends_initiate, ties_close_friends,

        state_of_status, state_of_user,

        dm_uids_summary,

        lists_user_listed_count, lists_menbers_show_count, lists_user_sub_count, lists_sub_show_count,

        // secondary_index_cmt_status,
        // secondary_index_cmt_to_me,
        // secondary_index_cmt_by_me,
        // secondary_index_cmt_timeline,
        // secondary_index_status_timeline,
        // secondary_index_mention,
        // secondary_index_dm_to_me,
        // secondary_index_dm_by_me,
        // secondary_index_dm_with,

        // tail_vector_status,
        // tail_vector_status_filter,
        // tail_vector_status_filter_self,
        // tail_status_mention,
        // tail_comment_status1,
        // tail_comment_to_me,
        // tail_comment_by_me,
        // tail_comment_timeline,
        // tail_dm_by_me,
        // tail_dm_to_me,
        // tail_dm_with,

        tail_spec_vector_uid_status,

        exclued_mention_uids, blacklist_uid, uninterested,

        feedfilter_vector, feedfilter_item,

        cf_direct_count, cf_cm_to_me_count, cf_mt_to_me_count, cf_omt_to_me_count,

        common_cmt_remind,

        counter_user_attitude_to_me_by_friends, counter_user_attitude_to_me_by_cf, counter_user_attitude_to_me_by_strangers,

        counter_user_attitude_to_me_order_by_status_by_friends, counter_user_attitude_to_me_order_by_status_by_cf, counter_user_attitude_to_me_order_by_status_by_strangers,

        counter_status_attitude_by_friends, counter_status_attitude_by_cf, counter_status_attitude_by_strangers,

        counter_user_diff_status, counter_user_diff_follower, counter_user_diff_following,

        counter_object_comment, counter_object_status,

        counter_microcontacts,

        counter_page_att, counter_page_fans,

        counter_page_status,

        counter_frozen_followers,

        content_longtext_status,

        content_slice_pb
    }

    /**
     * cachesuffix锟斤拷 . + CacheFlag + BusinessFlag
     *
     * @author Administrator
     */
    public static class CacheSuffix {


        /**
         * 最近几天赞推荐信息
         */
        public static final String LIKE_RECOMMEND_VECTOR = ".lrv";

        /**
         * 微博相机最近的赞推荐Feed
         */
        public static final String CAMERA_LIKE_RECOMMEND_VECTOR = ".clrv";

        /**
         * 最近几天page微博 vector
         */
        public static final String PAGE_VECTOR_STATUS_DATE = ".pvd";

        /**
         * 最近几天page微博 推荐title
         */
        public static final String PAGE_VECTOR_STATUS_DATE_TITLE = ".pvdt";

        /**
         * 定向发布->群微博
         */
        public static final String PAGEGROUP_VECTOR_STATUS_DATE = ".gvd";

        /**
         * 最近一段时间微博vector
         */
        public static final String META_VECTOR_STATUS_DATE = ".mvd";

        /**
         * 最近N条微博vector
         */
        public static final String META_VECTOR_STATUS_LATEST = ".mvl";

        // vector priority
        public static final String VECTOR_STATUS_LATEST = ".vsl";
        public static final String VECTOR_STATUS_DAYS = ".usd";
        // public static final String VECTOR_STATUS_STATE_SHOW_SELF = ".vss";
        public static final String VECTOR_STATUS_STATE_NO_SPREAD = ".vsn";// add no spread state by
                                                                          // suoyuan
        public static final String VECTOR_HOUR = ".vsh";
        public static final String VECTOR_MENTION = ".vsm";
        public static final String VECTOR_CMT_MENTION = ".vcm";
        public static final String VECTOR_DM_IN = ".vdi";
        public static final String VECTOR_DM_OUT = ".vdo";
        public static final String VECTOR_DM_WITH = ".vdw";
        public static final String VECTOR_COMMENT_IN = ".vci";
        public static final String VECTOR_COMMENT_OUT = ".vco";
        public static final String VECTOR_COMMENT_OUT_SELF = ".vcs";
        public static final String VECTOR_COMMON_INVOLVE_CMT = ".vcic";
        public static final String VECTOR_COMMON_INVOLVE_ATTITUDE = ".vcia";
        public static final String VECTOR_COMMENT_ATTITUDE = ".vca";
        public static final String VECTOR_OBJECT_ATTITUDE = ".voa";
        // common comment
        public static final String VECTOR_COMMON_CMT = ".vcc";
        public static final String VECTOR_COMMON_CMT_DL = ".vccd";

        // trash comment
        public static final String VECTOR_TRASH_CMT = ".cttm";
        // attitude to_me box only contains close_friend status's attitudes.
        public static final String VECTOR_ATTITUDE_IN_CF = ".vai";
        // public status attitude to me box.
        public static final String VECTOR_ATTITUDE_IN = ".vaix";
        public static final String SPEC_VECTOR_ATTITUDE_IN_ORDERBYSTATUS = ".vais";
        public static final String VECTOR_STATUS_ATTITUDES = ".vsa";
        public static final String VECTOR_STATUS_ATTITUDES_UID = ".vsau";
        // status attitude unread
        public static final String UNREAD_STATUS_ATTITUDES = ".usa";
        public static final String VECTOR_UID_ATTITUDE_STATUS = ".uasv";

        // like object keys
        public static final String VECTOR_OBJECT_LIKE_LIST = ".oll";
        public static final String VECTOR_UID_LIKE_LIST = ".ull";
        public static final String VECTOR_UID_LIKE_BY_ME = ".ulm";
        public static final String VECTOR_UID_LIKE_BY_ME_COUNT = ".ulc";
        // favorite likes to_me
        public static final String FAVORITE_LIKES_TO_ME_COUNT = ".fltc";
        public static final String VECTOR_UID_ATTITUDE_BY_ME = ".uabm";
        public static final String MAPPING_UID_LIKE_EXIST = ".ule";


        public static final String VECTOR_REPOST = ".vrt";
        public static final String VECTOR_USER_REPOST = ".vur";
        public static final String VECTOR_STATUS_COMMENTS = ".vsc";
        public static final String VECTOR_STATUS_COMMENTS_ASC = ".vsca";
        public static final String STATUS_MEMBER_COMMENTS_VECTOR = ".smcv";
        public static final String VECTOR_INBOX = ".ibv";
        public static final String VECTOR_META_INBOX = ".mib";
        public static final String VECTOR_META_INBOX_PAGE = ".mip";

        // activity
        public static final String NEW_VECTOR_ACTIVITY_IN = ".nvai";

        // new activity
        public static final String ACTIVITY_USER_VECTOR = ".auv";
        public static final String ACTIVITY_USER_NEW_VECTOR = ".aunv";
        public static final String ACTIVITY_USER_INDEX_VECTOR = ".auiv";
        public static final String ACTIVITY_CONTENT = ".ac";
        public static final String ACTIVITY_TEMPLATE = ".at";

        // user group status inbox
        public static final String USER_GROUP_STATUS_VECTOR = ".gss";
        public static final String USER_METAGROUP_STATUS_VECTOR = ".mgs";
        public static final String SGROUP_DLVECTOR = ".sgdl";

        public static final String VECTOR_CMT_BY_FRIEND = ".svcf";
        public static final String VECTOR_CMT_BY_CLOSEFRIEND = ".svccf";
        public static final String VECTOR_CMT_BY_STRANGER = ".svcs";
        public static final String VECTOR_MENTION_BY_FRIEND = ".svmf";
        public static final String VECTOR_MENTION_BY_ORIGINAL = ".svmo";
        public static final String VECTOR_MENTION_BY_FRIEND_ORIGINAL = ".svmb";
        public static final String VECTOR_MENTION_BY_CLOSEFRIEND = ".svmcf";
        public static final String VECTOR_MENTION_BY_CLOSEFRIEND_ORIGINAL = ".svmcfo";

        public static final String SPEC_VECTOR_UID_STATUS_FLAG = ".dlvl";

        public static final String D_VECTOR_FEED_FLAG = ".dvl";

        public static final String USER_LAST_STATUS_ID = ".ulsi";

        // TODO: 这个暂时兼容保留，不再更新，也不再从db加载,预计9月底之后不再需要，届时去掉 fishermen 2011.9.1
        @Deprecated
        public static final String SPEC_VECTOR_UID_STATUS_FLAG_SELF = ".dlvp";

        // public static final String CONTENT_FEATURE_VALUE = ".cfv"; // uid -> aid&featureValue

        // public static final String CONTENT_CACHE_STATUS_XML = ".csx";
        // public static final String CONTENT_CACHE_STATUS_JSON = ".csj";
        public static final String CONTENT_CACHE_STATUS_PB = ".csp";
        public static final String CONTENT_CACHE_LONGTEXT_STATUS = ".ccls";
        public static final String CONTENT_CACHE__SLICE_PB = ".cclp";

        // public static final String CONTENT_CACHE_COMMENT_XML = ".ccx";
        // public static final String CONTENT_CACHE_COMMENT_JSON = ".ccj";
        public static final String CONTENT_CACHE_COMMENT_PB = ".ccp";
        // public static final String CONTENT_CACHE_DM_XML = ".cdx";
        // public static final String CONTENT_CACHE_DM_JSON = ".cdj";
        public static final String CONTENT_CACHE_DM_PB = ".cdp";
        public static final String CONTENT_CACHE_ATTITUDE_PB = ".cap";

        public static final String STATUS_EXPOSURE_POLICY = ".sep";

        // public static final String MAPPING_NICK_UID_FLAG = ".mn2u";
        public static final String MAPPING_ID_SOURCEID_STATUS = ".mi2si";
        public static final String MAPPING_ID_MID_STATUS = ".mi2ms";
        public static final String MAPPING_MID_ID_STATUS = ".mm2is";
        public static final String MAPPING_ID_MID_DM = ".mi2md";
        public static final String MAPPING_MID_ID_DM_IN = ".mm2idi";
        public static final String MAPPING_MID_ID_DM_OUT = ".mm2ido";
        public static final String MAPPING_ID_MID_COMMENT = ".mi2mc";
        public static final String MAPPING_MID_ID_COMMENT = ".mm2ic";
        public static final String MAPPING_ID_UID_COMMENT = ".mi2uc";
        // 微博评论-uid映射2000条
        public static final String MAPPING_CID_UID_COMMENT = ".mc2uc";
        public static final String MAPPING_ID_UID_STATUS = ".mi2us";
        public static final String MAPPING_ID_STATE = ".mi2ste";
        public static final String MAPPING_UID_STATE = ".mui2ste";
        public static final String MAPPING_ID_FLAG_VALUE_STATUS = ".mi2fv"; // status_id ->
                                                                            // aid&featureValue
        public static final String MAPPING_ID_FEATURE_VALUE_COMMENT = ".mi2fvc"; // comment_id ->
                                                                                 // aid&featureValue
        public static final String MAPPING_UID_WATERMARK = ".mui2wm";

        public static final String MAPPING_APIID_UUID_STAUTS = ".ma2us";
        public static final String MAPPING_UUID_APIID_STATUS = ".mu2as";
        public static final String MAPPING_APIID_UUID_COMMENT = ".ma2uc";
        public static final String MAPPING_UUID_APIID_COMMENT = ".mu2ac";

        public static final String ID_CREATED_STATUS = ".ids";
        public static final String ID_CREATED_COMMENT = ".idc";
        public static final String ID_CREATED_DM = ".idd";

        public static final String ATTENTION_FRIENDS = ".afri";
        public static final String ATTENTION_FOLLOWERS = ".afol";
        public static final String ATTENTION_BOTHFRIENDS = ".abtf";
        public static final String ATTENTION_FRIENDS_SOURCE = ".afris";
        public static final String ATTENTION_FRIENDS_PAGE_SOURCE = ".afrips";
        public static final String ATTENTION_FOLLOWERS_SOURCE = ".afols";
        public static final String ATTENTION_SECRET = ".asec";

        // public static final String SINA_USER = ".u";
        // DON'T CHANGE THE .u2 suffix, some hard code "+ 2" in StorageProxy.java
        public static final String SINA_PB_USER = ".u2";
        public static final String SINA_USER_PERSENCE = ".p";
        // public static final String SINA_USER_NICK_UID = MAPPING_NICK_UID_FLAG;
        public static final String SINA_USER_DOMAIN_UID = ".sd2u";
        public static final String SINA_USER_WEINUM_UID = ".sw2u";
        public static final String SINA_USER_WEINUM = ".suw";
        public static final String SINA_USER_LEVEL = ".sul";
        public static final String SINA_USER_TYPE = ".sut";
        public static final String SINA_USER_FILTER = ".suf";
        public static final String SINA_USER_PRIVACY = ".sup";
        public static final String SINA_USER_EXT_SETTINGS = ".sues";
        public static final String SINA_USER_LOGIN_NAME_UID = ".sul2u";

        // page cache: only for request, not get frm db
        public static final String PAGE_CACHE_UID_FLAG_FRIEND = ".pff";
        public static final String PAGE_CACHE_LIST_FLAG = ".plf";
        public static final String PAGE_CACHE_REPOST_FLAG = ".prf";
        public static final String PAGE_CACHE_USER_REPOST_FLAG = ".purf";
        public static final String PAGE_CACHE_UID_FLAG_USER = ".pfu";
        public static final String PAGE_CACHE_UID_FRIEND_TIMELINE = ".puft";
        public static final String PAGE_CACHE_LIST_TIMELINE = ".plt";
        public static final String PAGE_CACHE_REPOST_TIMELINE = ".prt";
        public static final String PAGE_CACHE_USER_REPOST_TIMELINE = ".purt";
        public static final String PAGE_CACHE_UID_USER_TIMELINE = ".puut";

        public static final String GROUP_UID_INFO_JSON = ".gij";
        public static final String GROUP_UID_INFO_XML = ".gix";
        public static final String GROUP_GIDUID_MEMBER_JSON = ".gmj";
        public static final String GROUP_GIDUID_MEMBER_XML = ".gmx";
        public static final String GROUP_LISTED_JSON = ".glj";
        public static final String GROUP_LISTED_XML = ".glx";
        public static final String GROUP_UIDS = ".gpus";
        public static final String GROUP_GID_INFO = ".ggif";
        public static final String GROUP_LISTED = ".grlt";
        public static final String GROUP_GID_MEMBERS = ".ggms";
        public static final String GROUP_UNGROUP = ".gung";
        public static final String GROUP_LIKE = ".gplk";

        public static final String GROUP_MEMBER_REVERSE = ".gmr";
        public static final String GROUP_MEMBER_REVERSE_SET_CACHING = ".gmrs";

        public static final String USER_EXT_REMARK = ".uer";

        public static final String USER_RANK = ".urnk";
        // ordered set cache
        public static final String OSET_DM_WITH_USER = ".owu";

        public static final String DM_UIDS_SUMMARY = ".dus";
        public static final String OMAP_DM_WITH_USER = ".omdwu";

        // counter in redis

        // api status
        public static final String COUNTER_USER_APISTATUS = ".cntas";
        // api status all (includes spec)
        public static final String COUNTER_USER_APISTATUS_WITHOUT_SPEC = ".cntac";
        // user fav
        public static final String COUNTER_USER_FAV = ".cntuf";
        // status
        public static final String COUNTER_USER_STATUS = ".cntus";
        // attention
        public static final String COUNTER_USER_FOLLOWING = ".cntui";
        // fans
        public static final String COUNTER_USER_FOLLOWER = ".cntue";
        // api attention
        public static final String COUNTER_USER_API_FOLLOWING = ".cntai";
        // api fans
        public static final String COUNTER_USER_API_FOLLOWER = ".cntae";
        // attention intersect fans
        public static final String COUNTER_USER_BIFOLLOWER = ".cntbf";
        // comment_by_me
        public static final String COUNTER_USER_COMMENT_BY_ME = ".cntbm";
        // comment_to_me_all
        public static final String COUNTER_USER_COMMENT_TO_ME = ".cntma";
        // comment_to_me_by_friends
        public static final String COUNTER_USER_COMMENT_TO_ME_BY_FRIENDS = ".cntmf";
        // metions = repost_to_me
        public static final String COUNTER_USER_METIONS = ".cntmt";
        // comment metions = repost_to_me in comment
        public static final String COUNTER_USER_COMMENT_METIONS = ".cntcm";
        // repost_time_line = repost_by_me
        public static final String COUNTER_USER_REPOST_BY_ME = ".cntrm";
        // user attention secret
        public static final String COUNTER_USER_ATTENTION_SECRET = ".cntse";

        // user attitude timeline to_me
        public static final String COUNTER_USER_ATTITUDE_TIMELINE_TO_ME = ".cntaa";
        public static final String COUNTER_USER_COMMENT_ATTITUDE_TO_ME = ".cntcaa";
        public static final String COUNTER_USER_OBJECT_ATTITUDE_TO_ME = ".cntoaa";
        // user attitude timeline to_me order by status
        public static final String COUNTER_USER_ATTITUDE_TIMELINE_TO_ME_ORDER_BY_STATUS = ".cntat";

        // user common cmt timeline to_me
        public static final String COUNTER_USER_COMMONCMT_TIMELINE_TO_ME = ".cntca";

        public static final String COUNTER_USER_GROUP_STATUS = ".cntugs";

        // status attitude
        public static final String COUNTER_STATUS_ATTITUDE = ".cntsa";

        public static final String COUNTER_OBJECT_COMMENTS = ".cntoc";

        public static final String COUNTER_OBJECT_STATUS = ".cntos";

        // user direct group status
        public static final String COUNTER_USER_DIRECT_GROUP_STATUS = ".cntgs";

        // group like
        public static final String COUNTER_GROUP_LIKE = ".";

        // group member
        public static final String COUNTER_GROUP_MEMBER = "";

        public static final String COUNTER_GROUP_MEMBER_REVERSE = ".cgmr";

        public static final String COUNTER_STATUS_RT = ".cntsr";
        public static final String COUNTER_STATUS_COMMENT = ".cntsc";

        public static final String COUNTER_REPEAT_DETECTOR_STATUS_INC = ".crdsi";
        public static final String COUNTER_REPEAT_DETECTOR_STATUS_DEC = ".crdsd";
        public static final String COUNTER_REPEAT_DETECTOR_COMMENT_INC = ".crdci";
        public static final String COUNTER_REPEAT_DETECTOR_COMMENT_DEC = ".crdcd";
        public static final String COUNTER_REPEAT_DETECTOR_ATTITUDE_INC = ".crdai";

        // handpick status counter by zhiguo4
        public static final String COUNTER_PICK = ".cpick";

        public static final String COUNTER_MICROCONTACTS = ".cmicroct";

        public static final String COUNTER_PAGE_ATTENTION = ".cpageatt";
        public static final String COUNTER_PAGE_FANS = ".cpagefans";

        public static final String COUNTER_PAGE_STATUS = ".cpagesta";

        public static final String COUNTER_FROZEN_FOLLOWERS = ".frfo";

        public static final String REMIND_360_PLUGIN = ".r3p";

        // fans/attention/both attention list in redis
        public static final String FRIENDLIST_FANS_NEW = ".ffn";
        public static final String FRIENDLIST_FANS_ALL = ".ffa";
        public static final String FRIENDLIST_FANS_FOREMOST = ".fff";
        public static final String FRIENDLIST_ATTENTIONS = ".fat";
        public static final String FRIENDLIST_BOTH_FANS = ".fbf";
        public static final String FRIENDLIST_REMARK = ".frmk";
        public static final String FANSLIST_REMARK = ".fark";
        public static final String NONELIST_REMARK = ".nrmk";
        public static final String FRIENDLIST_FILTERED = ".fftd";
        public static final String FRIENDLIST_ATTENTIONS_RESIDUAL = ".fatrs";
        public static final String FRIENDLIST_BOTHFANS_RESIDUAL = ".fbfrs";
        public static final String FRIENDLIST_BLACKLIST = ".fblk";
        public static final String FRIENDLIST_RECOMMEND = ".reco";
        public static final String FRIENDLIST_INTERACTION = ".action";

        public static final String BOTH_FRIENDLIST_FILTERED = ".bfftd";

        public static final String BOTH_FRIENDLIST_REVERSE_FILTERED = ".bfrftd";

        public static final String BOTH_FRIENDLIST_BE_REVERSE_FILTERED = ".bfbrftd";

        public static final String BOTH_FRIENDLIST_RELATIONS = ".bfres";
        // 关系业务用户粉丝列表缓存的后缀。
        public static final String GRAPH_FOLLOWERS_LIST = ".gfl";
        /**
         * 游客关注列表缓存的 Key 后缀。
         */
        public static final String TOURIST_FOLLOWINGS_LIST = ".tfal";

        public static final String TIES_CLOSE_FRIENDS_INITIATE = ".tcfi";
        public static final String TIES_CLOSE_FRIENDS = ".tcf";

        // 用户最新2000条spec vector
        public static final String TAIL_SPEC_VECTOR_UID_STATUS = ".tdvsl";

        // FEED屏蔽向量
        public static final String FEEDFILTER_VECTOR = ".ffvec";
        public static final String FEEDFILTER_ITEM = ".ffitem";

        // public static final String TAIL_VECTOR_STATUS = ".svtvs";
        // public static final String TAIL_CONTENT_STATUS = ".svtcs";
        // public static final String TAIL_COMMENTS_TO_ME = ".svtctm";
        // public static final String TAIL_COMMENTS_BY_ME =".svtcbm";
        // public static final String TAIL_COMMENTS_TIMELINE =".svtct";
        // public static final String TAIL_STATUS_MENTION =".svtsm";
        // public static final String TAIL_DM_BY_ME = ".svtdmbm";
        // public static final String TAIL_DM_TO_ME = ".svtdmtm";
        // public static final String TAIL_DM_WITH = ".svtdms";
        // public static final String TAIL_VECTOR_STATUS_FILTER = ".svtvsf";
        // public static final String TAIL_VECTOR_STATUS_FILTER_SELF = ".svtvss";

        /**
         * si cache
         */
        // public static final String SECONDARY_INDEX_CMT_STATUS = ".sics";
        // public static final String SECONDARY_INDEX_CMT_TOME = ".sictm";
        // public static final String SECONDARY_INDEX_CMT_BYME = ".sicbm";
        // public static final String SECONDARY_INDEX_CMT_TIMELINE = ".sict";
        // public static final String SECONDARY_INDEX_STATUS_TIMELINE = ".sist";
        // public static final String SECONDARY_INDEX_MENTION = ".sim";
        // public static final String SECONDARY_INDEX_DM_TO_ME = ".sidmtm";
        // public static final String SECONDARY_INDEX_DM_BY_ME = ".sidmbm";
        // public static final String SECONDARY_INDEX_DM_WITH = ".sidmw";

        public static final String LISTS_USER_LISTED_COUNT = ".lulc";
        public static final String LISTS_USER_SUB_COUNT = ".lusc";
        public static final String LISTS_MENBERS_SHOW_COUNT = ".lmsc";
        public static final String LISTS_SUB_SHOW_COUNT = ".lssc";
        public static final String EXCLUDED_MENTION_UIDS = ".emu";
        public static final String BLACKLIST_UID = ".blk";
        public static final String UNINTERESTED_UID = ".ulk";

        public static final String CLOSEFRIEND_DIRECT_COUNT = ".cdc";
        public static final String CLOSEFRIEND_COMMENTTOME_COUNT = ".ccc";
        public static final String CLOSEFRIEND_MENTIONTOME_COUNT = ".cmc";
        public static final String CLOSEFRIEND_ORIGINMENTIONTOME_COUNT = ".coc";

        public static final String COMMON_CMT_REMIND = ".ccr";

        // user attitude timeline to_me
        public static final String COUNTER_USER_ATTITUDE_TO_ME_BY_FRIENDS = ".catf";
        public static final String COUNTER_USER_ATTITUDE_TO_ME_BY_CF = ".catc";
        public static final String COUNTER_USER_ATTITUDE_TO_ME_BY_STRAGERS = ".cats";

        // user attitude timeline to_me order by status
        public static final String COUNTER_USER_ATTITUDE_TO_ME_ORDER_BY_STATUS_BY_FRIENDS = ".catsf";
        public static final String COUNTER_USER_ATTITUDE_TO_ME_ORDER_BY_STATUS_BY_CF = ".catsc";
        public static final String COUNTER_USER_ATTITUDE_TO_ME_ORDER_BY_STATUS_BY_STRAGERS = ".catss";


        // status attitude
        public static final String COUNTER_STATUS_ATTITUDE_BY_FRIENDS = ".csaf";
        public static final String COUNTER_STATUS_ATTITUDE_BY_CF = ".csac";
        public static final String COUNTER_STATUS_ATTITUDE_BY_STRAGERS = ".csas";

        public static final String COUNTER_USER_DIFF_STATUS = ".cntds";
        public static final String COUNTER_USER_DIFF_FOLLOWER = ".cntdf";
        public static final String COUNTER_USER_DIFF_FOLLOWING = ".cntdi";

        // node status
        public static final String COUNTER_NODE_STATUS = ".cnntus";
        // node attention
        public static final String COUNTER_NODE_FOLLOWING = ".cnntui";
        // node fans
        public static final String COUNTER_NODE_FOLLOWER = ".cnntue";
    }

    public static void main(String[] args) {
        initCacheSuffix();
        for (Type type : Type.values()) {
            if (getCacheSuffix(type) == null) {
                System.out.println("Not insert into cacheSuffix type:" + type);
            }
        }
        System.out.println("cacheSuffix.size():" + suffixCache.size() + ", Type.size:" + Type.values().length);

    }
}
