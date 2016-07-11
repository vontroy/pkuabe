package pku.abe.service.webapi;

import pku.abe.data.model.UserInfo;
import pku.abe.data.model.params.UserParams;

/**
 * Created by vontroy on 16-7-11.
 */
public interface UserService {
    UserInfo userLogin(UserParams userParams);

    boolean userRegister(UserParams userParams);

    boolean validateEmail(UserParams userParams);

    boolean userLogout(UserParams userParams);

    boolean resetUserPwd(UserParams userParams);

    boolean forgotPwd(UserParams userParams);

    boolean resetForgottenPwd(UserParams userParams);
}
