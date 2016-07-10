/**
 *
 */
package pku.abe.commons.memcache;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.switcher.ResourceSwitcherSupport;
import pku.abe.commons.switcher.Switcher;

public class SwitcherSupportSockIO extends SockIOPool.SockIO {

    private ResourceSwitcherSupport switcherSupport;

    private static final String RESOURCE_TYPE = "memcache";


    public SwitcherSupportSockIO(SockIOPool pool, String host, int timeout, int connectTimeout, boolean noDelay)
            throws IOException, UnknownHostException {
        super(pool, host, timeout, connectTimeout, noDelay);
        this.initSwitcher(host);
    }

    @Override
    protected void doConnect(String host, int timeout, int connectTimeout, boolean noDelay) throws NumberFormatException, IOException {
        this.initSwitcher(host);
        // 如果读写开关都关闭则不创建真实连接
        if (this.getWriteSwitcher().isClose() && this.getReadSwitcher().isClose()) {
            ApiLogger.warn("Switcher " + host + " read && writer is close. has not real connect.");
        } else {
            super.doConnect(host, timeout, connectTimeout, noDelay);
        }
    }

    private void initSwitcher(String host) {
        String[] parts = host.split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);
        switcherSupport = new ResourceSwitcherSupport(RESOURCE_TYPE, ip, port);
    }

    public Switcher getReadSwitcher() {
        return this.switcherSupport.getReadSwitcher();
    }

    public Switcher getWriteSwitcher() {
        return this.switcherSupport.getWriteSwitcher();
    }

    @Override
    public SocketChannel getChannel() {
        return super.getChannel();
    }

    @Override
    public String getHost() {
        return super.getHost();
    }

    @Override
    public void trueClose() throws IOException {
        super.trueClose();
    }

    @Override
    public void trueClose(boolean addToDeadPool) throws IOException {
        super.trueClose(addToDeadPool);
    }

    @Override
    void close() {
        super.close();
    }

    @Override
    boolean isConnected() {
        return super.isConnected();
    }

    @Override
    boolean isAlive() {
        // 读写开关都关闭时，默认isAlive为true
        if (this.getReadSwitcher().isClose() && this.getWriteSwitcher().isClose()) {
            return true;
        } else {
            return super.isAlive();
        }
    }

    @Override
    public byte[] readBytes(int length) throws IOException {
        return super.readBytes(length);
    }

    @Override
    public byte[] readLineBytes() throws IOException {
        return super.readLineBytes();
    }

    @Override
    public String readLine() throws IOException {
        return super.readLine();
    }

    @Override
    void flush() throws IOException {
        super.flush();
    }

    @Override
    void write(byte[] b) throws IOException {
        super.write(b);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

}
