package demo.service;
import mvc.annotation.MyServer;

@MyServer
public class UserService implements IUserService{

    @Override
    public String login(String username, String password) {
        return "用户名:"+username+"----------密码:"+password;
    }

    @Override
    public String register(String username, String password) {
        return "用户名:"+username+"----------密码:"+password;
    }
}
