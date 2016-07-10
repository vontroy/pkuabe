package pku.abe.commons.util;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.util.Assert;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public final class HostUtils {
    /**
     * parse domain to ip list
     *
     * @param hostName
     * @return
     * @throws java.net.UnknownHostException
     */
    public static List<String> getIpsByHostName(String hostName) throws UnknownHostException {
        Assert.notNull(hostName, "the host name must not be null or empty str");
        List<String> lResult = new ArrayList<String>();
        InetAddress[] inetAddress;
        inetAddress = InetAddress.getAllByName(hostName);
        for (int i = 0; i < inetAddress.length; i++) {
            String ip = inetAddress[i].getHostAddress();
            lResult.add(ip);
        }
        return lResult;
    }

    public static void main(String[] args) throws UnknownHostException {
        List<String> ips = getIpsByHostName("s4680i.mars.grid.sina.com.cn");
        if (CollectionUtils.isNotEmpty(ips)) {
            for (String string : ips) {
                System.out.println(string);
            }
        }
    }
}
