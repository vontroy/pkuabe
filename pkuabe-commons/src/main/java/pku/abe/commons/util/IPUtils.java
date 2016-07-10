package pku.abe.commons.util;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class IPUtils {


    public static String ipRegix = "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
    public static Pattern ipPattern = Pattern.compile(ipRegix);

    public static boolean isIp(String in) {
        if (in == null) {
            return false;
        }
        return ipPattern.matcher(in).matches();
    }


    /**
     * Convert IP to Int.
     *
     * @param addr
     * @param isSegment true IP segment, false full IP.
     * @return
     */
    public static int ipToInt(final String addr, final boolean isSegment) {
        final String[] addressBytes = addr.split("\\.");
        int length = addressBytes.length;
        if (length < 3) {
            return 0;
        }
        int ip = 0;
        try {
            for (int i = 0; i < 3; i++) {
                ip <<= 8;
                ip |= Integer.parseInt(addressBytes[i]);
            }
            ip <<= 8;
            if (isSegment || length == 3) {
                ip |= 0;
            } else {
                ip |= Integer.parseInt(addressBytes[3]);
            }
        } catch (Exception e) {
            System.out.println("Warn ipToInt addr is wrong: addr=" + addr);
        }

        return ip;
    }

    /**
     * 将ip转化为数字，并且保持ip的大小顺序不变 如 ipToInt("10.75.0.1") > ipToInt("10.75.0.0") 如果ip不合法则返回 0
     *
     * @param ipAddress
     * @return
     */
    public static int ipToInt(final String addr) {
        return ipToInt(addr, false);
    }

    private static long[][] intranet_ip_ranges = new long[][] {{ipToInt("10.0.0.0"), ipToInt("10.255.255.255")},
            {ipToInt("172.16.0.0"), ipToInt("172.31.255.255")}, {ipToInt("192.168.0.0"), ipToInt("192.168.255.255")}};

    /**
     * 是否为内网ip A类 10.0.0.0-10.255.255.255 B类 172.16.0.0-172.31.255.255 C类
     * 192.168.0.0-192.168.255.255 不包括回环ip
     *
     * @param ip
     * @return
     */
    public static boolean isIntranetIP(String ip) {
        if (!isIp(ip)) {
            return false;
        }
        long ipNum = ipToInt(ip);
        for (long[] range : intranet_ip_ranges) {
            if (ipNum >= range[0] && ipNum <= range[1]) {
                return true;
            }
        }
        return false;
    }


    /**
     * 获取本机所有ip 返回map key为网卡名 value为对应ip yuanming@staff
     *
     * @return
     */
    public static Map<String, String> getLocalIps() {
        try {
            Map<String, String> result = new HashMap<String, String>();
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                String name = ni.getName();
                String ip = "";
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                    InetAddress address = ips.nextElement();
                    if (address instanceof Inet4Address) {
                        ip = address.getHostAddress();
                        break;
                    }
                }
                result.put(name, ip);
            }
            return result;
        } catch (SocketException e) {
            System.out.println("getLocalIP error" + e.getMessage());
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    /**
     * 获取服务器ip 判断规则 eth0 > eth1 > ... ethN > wlan > lo
     * <p>
     * yuanming@staff
     *
     * @return
     */
    public static String getLocalIp() {

        Map<String, String> ips = getLocalIps();
        List<String> faceNames = new ArrayList<String>(ips.keySet());
        Collections.sort(faceNames);

        for (String name : faceNames) {
            if ("lo".equals(name)) {
                continue;
            }
            String ip = ips.get(name);
            if (ip != null && ip.length() != 0) {
                return ip;
            }
        }
        return "127.0.0.1";
    }

    private static String localIp = null;

    /**
     * 只获取一次ip
     *
     * @return
     */
    public static String getSingleLocalIp() {
        if (localIp == null) {
            localIp = getLocalIp();
        }
        return localIp;
    }


    private static final int MIN_USER_PORT_NUMBER = 1024;
    private static final int MAX_USER_PORT_NUMBER = 65536;

    /**
     * 随机返回可用端口
     *
     * @return
     */
    public static int ramdomAvailablePort() {
        int port = 0;
        do {
            port = (int) ((MAX_USER_PORT_NUMBER - MIN_USER_PORT_NUMBER) * Math.random()) + MIN_USER_PORT_NUMBER;
        } while (!availablePort(port));
        return port;
    }


    /**
     * 检测该端口是否可用 <br/>
     * 端口必须大于 0 小于 {@value #MAX_PORT_NUMBER}
     *
     * @param port
     * @return
     */
    public static boolean availablePort(int port) {
        if (port < 0 || port > MAX_USER_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {} finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }
}
