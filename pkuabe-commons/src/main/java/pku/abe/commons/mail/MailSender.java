package pku.abe.commons.mail;

/**
 * Created by LinkedME01 on 16/3/16.
 */
import pku.abe.commons.log.ApiLogger;

import java.util.Date;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class MailSender {

    public static Message getMailMsg(String toAddress, String mailSubject, String mailContent) {
        MailSenderInfo mailInfo = new MailSenderInfo();
        mailInfo.setToAddress(toAddress);
        mailInfo.setSubject(mailSubject);
        mailInfo.setContent(mailContent);
        Properties pro = mailInfo.getProperties();
        MailAuthenticator authenticator = new MailAuthenticator(mailInfo.getUserName(), mailInfo.getPassword());
        // 根据邮件会话属性和密码验证器构造一个发送邮件的session
        Session sendMailSession = Session.getDefaultInstance(pro, authenticator);
        try {
            // 根据session创建一个邮件消息
            Message mailMessage = new MimeMessage(sendMailSession);

            // 创建邮件发送者地址
            Address from = new InternetAddress(mailInfo.getFromAddress());

            // 设置邮件消息的发送者
            mailMessage.setFrom(from);

            // 创建邮件的接收者地址，并设置到邮件消息中
            Address to = new InternetAddress(mailInfo.getToAddress());
            mailMessage.setRecipient(Message.RecipientType.TO, to);

            // 设置邮件消息的主题
            mailMessage.setSubject(mailInfo.getSubject());

            // 设置邮件消息的主要内容
            mailMessage.setText(mailInfo.getContent());

            // 设置邮件消息发送的时间
            mailMessage.setSentDate(new Date());

            return mailMessage;
        } catch (MessagingException e) {
            ApiLogger.error("Send mail failed!", e);
        }
        return null;
    }

    /**
     * 以文本格式发送邮件
     *
     * @param toAddress 收件人的邮箱
     * @param mailSubject 邮件主题
     * @param mailContent 邮件内容
     */
    public static boolean sendTextMail(String toAddress, String mailSubject, String mailContent) {
        try {
            Transport.send(getMailMsg(toAddress, mailSubject, mailContent));
            return true;
        } catch (MessagingException e) {
            ApiLogger.error("Send mail failed!", e);
        }
        return false;
    }

    /**
     * 以HTML格式发送邮件
     *
     * @param toAddress 收件人的邮箱
     * @param mailSubject 邮件主题
     * @param mailContent 邮件内容
     */
    public static boolean sendHtmlMail(String toAddress, String mailSubject, String mailContent) {
        Message mailMessage = getMailMsg(toAddress, mailSubject, mailContent);
        Multipart mainPart = new MimeMultipart();
        // 创建一个包含HTML内容的MimeBodyPart
        BodyPart html = new MimeBodyPart();
        try {
            // 设置HTML内容
            html.setContent(mailContent, "text/html; charset=utf-8");
            mainPart.addBodyPart(html);

            // 将MiniMultipart对象设置为邮件内容
            mailMessage.setContent(mainPart);

            // 发送邮件
            Transport.send(mailMessage);
            return true;
        } catch (MessagingException e) {
            ApiLogger.error("Send mail failed!", e);
        }
        return false;
    }

    public static void main(String[] args) {
//        MailSender.sendTextMail("276386627@qq.com", "hello, wrshine", "this is a test mail from java program");
//        MailSender.sendHtmlMail("276386627@qq.com", "hello, wrshine", "this is a test mail </br> from java program");
//        MailSender.sendHtmlMail("wrshine@163.com", "hello, wrshine", "this is a test mail </br> from java program");
//        MailSender.sendHtmlMail("wrshine@gmail.com", "hello, wrshine", "this is a test mail </br> from java program");
        String url = "http://www.weibo.com";
        String resetPwdUrl = "https://www.linkedme.cc/dashboard/index.html#/access/resetpwd/123456";
        MailSender.sendHtmlMail("renyang@linkedme.cc", "注册成功", String.format("<center><div style='width:500px;text-align:left'><div><a href='https://www.linkedme.cc/'><img src='https://www.linkedme.cc/images/linkedme_logo.png' style='margin-bottom:10px' width='150'/></a></div><div style='border:solid 1px #eeeeee;border-radius:5px;padding:15px;font-size:13px;line-height:20px;'><p>Hi，%s:</p><p>您的申请已经收到，非常高兴您关注LinkedME！我是LinkedME的创始人——齐坡，我们产品在6月1日，正式公布上线！到时我们市场部的工作人员会联系您，谢谢您的信任和支持！</p><p>LinkedME应用深度链接技术为移动互联网企业提供全新的解决方案，致力于帮助移动互联网企业的App提供下载、激活、活跃、留存，变现等众多的问题。LinkedME竭诚为您提供最佳的服务，在今后您使用LinkedME的相关产品时，碰到任何不清楚和不满意的问题时，您可以直接联系我（邮箱qipo@linkedme.cc），收到您的反馈将是特别高兴的事情！我代表LinkedME团队表示重谢，您并会收到LinkedME为您准备的惊喜礼物。</p><p>深度链接，链接你我！</p></div><div id='figure'><a href='http://weibo.com/poqi1987'><img src='https://www.linkedme.cc/images/qipo_logo.png' width='50' style='vertical-align:middle;padding-top:15px'/></a> 齐坡，CEO</div></div></center>", "王仁阳"));
        boolean result = MailSender.sendHtmlMail("xxx@xx.com", "hello, wrshine", String.format("亲爱的用户:<br /><br />LinkedME重置密码的链接为:  <a href=%s>点击链接</a>. <br /> 有任何问题可以咨询我们,Email:support@linkedme.cc.<br /><br />谢谢!<br /><br />LinkedME团队", resetPwdUrl));
        System.out.println(result);
    }
}
