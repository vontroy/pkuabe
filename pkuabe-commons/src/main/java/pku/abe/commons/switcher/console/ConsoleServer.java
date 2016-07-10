package pku.abe.commons.switcher.console;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import pku.abe.commons.log.ApiLogger;

public class ConsoleServer {

    private int port;
    private int readBuffSize;
    private IoHandlerAdapter handle;
    private IoAcceptor acceptor;

    /**
     *
     * @param port 监听端口
     * @param readBuffSize 读缓冲大小
     *
     */
    public ConsoleServer(int port, int readBuffSize) {
        this.port = port;
        this.readBuffSize = readBuffSize;
        this.handle = new SocketHandle();
    }

    /**
     * 启动server
     */
    public void startup() {
        acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
        acceptor.setHandler(handle);
        acceptor.getSessionConfig().setReadBufferSize(readBuffSize);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
        try {
            acceptor.bind(new InetSocketAddress(port));
            ApiLogger.info("socket console server start up ...");
        } catch (IOException e) {
            ApiLogger.error("[ConsoleServer] socket console server bind port error:" + port, e);
            System.out.println("[ConsoleServer] socket console server bind port error:" + port);
            // 如果端口绑定失败，则停止服务
            System.exit(0);
        }
    }

    /**
     * 关闭服务器
     */
    public void shutdown() {
        if (acceptor != null) {
            acceptor.unbind();
            acceptor.dispose(true);
            ApiLogger.info("socket console server shutdown ok");
        }
    }

    public static void main(String[] args) throws IOException {
        new ConsoleServer(9013, 1024).startup();
    }
}
