package pku.abe.commons.spring;

import pku.abe.commons.mcq.reader.McqProcessor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * Created by LinkedME01 on 16/3/9.
 */
public class McqProcessorInitListener extends HttpServlet {
    public void init() throws ServletException {
        // 系统启动成功后,设置队列机初始化成功
        McqProcessor.setSystemInitSuccess();
    }
}
