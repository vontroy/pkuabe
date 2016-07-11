package pku.abe.data.model.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by vontroy on 16-7-11.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserParams {
    public String email;
    public String pwd;
    public String name;
    public String company;
    public String phone_number;
    public int role_id;
    public String last_logout_time;
    public String old_pwd;
    public String new_pwd;
    public String current_login_time;
    public String token;

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getPwd() {
        return pwd;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCompany() {
        return company;
    }

    public void setRole_id(int role_id) {
        this.role_id = role_id;
    }

    public int getRole_id() {
        return role_id;
    }

    public void setLast_logout_time(String last_logout_time) {
        this.last_logout_time = last_logout_time;
    }

    public String getLast_logout_time() {
        return last_logout_time;
    }

    public void setOld_pwd(String old_pwd) {
        this.old_pwd = old_pwd;
    }

    public String getOld_pwd() {
        return old_pwd;
    }

    public void setNew_pwd(String new_pwd) {
        this.new_pwd = new_pwd;
    }

    public String getNew_pwd() {
        return new_pwd;
    }

    public void setCurrent_login_time(String current_login_time) {
        this.current_login_time = current_login_time;
    }

    public String getCurrent_login_time() {
        return current_login_time;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }


    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }
}
