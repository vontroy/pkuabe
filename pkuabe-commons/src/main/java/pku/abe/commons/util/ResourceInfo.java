package pku.abe.commons.util;

/**
 * @author ShiMenglong
 *         <p>
 *         资源描述信息接口，建议所有资源客户端实现该接口
 */
public interface ResourceInfo {

    /**
     * @return String 格式：ip/域名:端口
     */
    String getResourceInfo();

}
