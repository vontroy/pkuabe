package pku.abe.commons.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class WfcNamespaceHandler extends NamespaceHandlerSupport {

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
     */
    @Override
    public void init() {
        registerBeanDefinitionParser("mc", new WfcDefinitionParser(MCClientFactoryBean.class));
        registerBeanDefinitionParser("mclist", new WfcDefinitionParser(MCClientListFactoryBean.class));
        registerBeanDefinitionParser("mysql", new WfcDefinitionParser(MysqlClientFactoryBean.class));
    }

}
