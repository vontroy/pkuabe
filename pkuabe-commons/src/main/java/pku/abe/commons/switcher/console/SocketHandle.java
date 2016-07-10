package pku.abe.commons.switcher.console;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import pku.abe.commons.log.ApiLogger;
import pku.abe.commons.switcher.Switcher;
import pku.abe.commons.switcher.SwitcherManager;
import pku.abe.commons.switcher.SwitcherManagerFactoryLoader;
import pku.abe.commons.util.HttpManager.WarnMsgException;

public class SocketHandle extends IoHandlerAdapter {

    private static Map<String, SwitcherHandler> switcherHandlerContext = new HashMap<String, SwitcherHandler>();

    static {
        SocketHandle sh = new SocketHandle();
        switcherHandlerContext.put("RESOURCE", sh.new ResourceSwitcherHandler());
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        // 如果是统一的开关控制方式，则启用统一控制
        if (isUnifiedSwitcherControl(message)) {
            unifyControl(session, message);
            return;
        }

        // 处理接受到的控制指令
        String command = message.toString();
        if (command == null || command.length() < 1) {
            // 作为默认状态查询执行 “status”
            command = "status";
        }
        ApiLogger.info("console server request command :" + command);
        // 退出
        if (command.trim().equalsIgnoreCase("quit")) {
            session.close(true);
            ApiLogger.info("request quit");
            return;
        } else if (command.trim().startsWith("resource.") || command.trim().startsWith("api.") || command.trim().startsWith("feature.")) {
            String cmds[] = command.split("\\s");
            if (cmds.length != 2) {
                session.write("Usage: switcherName [on|off|show]");
                return;
            }
            String switcherName = cmds[0];
            String op = cmds[1];
            SwitcherManager switcherManager = SwitcherManagerFactoryLoader.getSwitcherManagerFactory().getSwitcherManager();
            Switcher switcher = switcherManager.getSwitcher(switcherName);
            if (switcher == null) {
                session.write("Can not find switcher by name:" + switcherName);
                return;
            }
            if ("on".equalsIgnoreCase(op)) {
                switcher.setValue(true);
            } else if ("off".equalsIgnoreCase(op)) {
                switcher.setValue(false);
            }
            session.write(switcherName + " " + switcher.getValue());
            return;
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        ApiLogger.error("socket console error:", cause);
    }

    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        ApiLogger.info("IDLE " + session.getIdleCount(status));
    }

    private void unifyControl(IoSession session, Object message) throws Exception {
        String command = message.toString().trim();
        if (StringUtils.equalsIgnoreCase(command, "stats") || StringUtils.startsWith(command, "stats ")) {
            String[] splits = command.split(" ");
            String regExp = null;
            if (splits.length > 1) {
                regExp = splits[1];
            }
            printStatus(session, regExp);
            return;
        }

        String[] splitedCmd = command.split(" ");
        String operation = getOperation(splitedCmd);
        String name = getName(splitedCmd);
        String type = getType(splitedCmd);

        SwitcherHandler handler = null;
        try {
            handler = getSwitcherHandlerByType(type);
            invoke(handler, operation, name);
            session.write(name + " " + handler.show(name));
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof WarnMsgException) {
                // 支持调用降级成功时给调用方返回警告信息
                StringBuilder msg = new StringBuilder(name).append(" ");
                if (handler != null) {
                    msg.append(handler.show(name));
                }
                msg.append(" ").append(e.getTargetException().getMessage());
                session.write(msg.toString());
            }
        } catch (Exception e) {
            ApiLogger.error("Faile To '" + operation + "' on " + name, e);
            session.write("Faile To '" + operation + "' on " + name);
        }
    }

    private void invoke(SwitcherHandler handler, String operation, String resource) throws Exception {
        Method m = handler.getClass().getMethod(operation, new Class[] {String.class});
        m.invoke(handler, resource);
    }

    private String getOperation(String[] splitedCmd) {
        return splitedCmd[0];
    }

    private String getType(String[] splitedCmd) {
        return StringUtils.upperCase(splitedCmd[1]);
    }

    private String getName(String[] splitedCmd) {
        if (splitedCmd.length <= 1) {
            return null;
        }
        return splitedCmd[2];
    }

    private void printStatus(IoSession session, String regExp) {
        Map<String, Boolean> switchers = stats(regExp);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> sw : switchers.entrySet()) {
            sb.append(sw.getKey() + "\t\t" + sw.getValue());
            sb.append("\r\n");
        }
        session.write(sb.toString());
    }

    /**
     * @param regExp
     * @return
     */
    @SuppressWarnings({"rawtypes", "unused"})
    public Map<String, Boolean> stats(String regExp) {
        Map<String, Boolean> switchers = new HashMap<String, Boolean>();
        for (Map.Entry entry : switcherHandlerContext.entrySet()) {
            String name = (String) entry.getKey();
            Map<String, Boolean> m = ((SwitcherHandler) entry.getValue()).statuses();
            for (Map.Entry<String, Boolean> e : m.entrySet()) {
                String resource = (String) e.getKey();
                if (StringUtils.isEmpty(regExp) || resource.toUpperCase().indexOf(regExp.toUpperCase()) >= 0) {
                    switchers.put(e.getKey(), e.getValue());
                }
            }
        }
        return switchers;
    }


    // message的格式为【 (stats|on|off|show) (PULLONOFF|RESOURCE|HTTP) [$name] 】
    // 则说明是统一的开关控制器，返回true
    // 其它，返回false
    private boolean isUnifiedSwitcherControl(Object message) {
        if (message == null || StringUtils.isBlank(message.toString())) {
            return false;
        }
        String[] elements = message.toString().trim().split(" ");
        if (elements.length == 0) {
            return false;
        }

        String cmd = elements[0];

        if ("stats".equalsIgnoreCase(cmd)) {
            return true;
        }

        if (("on".equalsIgnoreCase(cmd) || "off".equalsIgnoreCase(cmd) || "show".equalsIgnoreCase(cmd)) && elements.length == 3) {
            return true;
        }
        return false;
    }


    private SwitcherHandler getSwitcherHandlerByType(String type) {
        SwitcherHandler sh = switcherHandlerContext.get(type);
        if (sh == null) {
            throw new RuntimeException("SwitcherHandler not configured for type:" + type);
        }
        return sh;
    }

    interface SwitcherHandler {
        public void on(String resource);

        public void off(String resource);

        public boolean show(String resource);

        public Map<String, Boolean> statuses();
    }

    class ResourceSwitcherHandler implements SwitcherHandler {
        public void on(String resource) {
            set(resource, true);
        }


        public void off(String resource) {
            set(resource, false);
        }

        public boolean show(String resource) {
            return getSwitcher(resource).getValue();
        }

        public Map<String, Boolean> statuses() {
            SwitcherManager switcherManager = SwitcherManagerFactoryLoader.getSwitcherManagerFactory().getSwitcherManager();
            Map<String, Boolean> m = new HashMap<String, Boolean>();
            for (Switcher s : switcherManager.listSwitchers()) {
                m.put(s.getName(), s.getValue());
            }
            return m;
        }

        private void set(String resource, boolean val) {
            getSwitcher(resource).setValue(val);
        }

        Switcher getSwitcher(String resource) {
            SwitcherManager switcherManager = SwitcherManagerFactoryLoader.getSwitcherManagerFactory().getSwitcherManager();
            Switcher switcher = switcherManager.getSwitcher(resource);
            if (switcher == null) {
                throw new RuntimeException("Resource not configued on switcher:" + resource);
            }
            return switcher;
        }

    }
}
