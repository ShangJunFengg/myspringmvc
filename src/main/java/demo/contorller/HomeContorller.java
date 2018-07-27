package demo.contorller;


import demo.service.IUserService;
import mvc.annotation.MyAutowired;
import mvc.annotation.MyContorller;
import mvc.annotation.MyRequestMapping;
import mvc.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyContorller
@MyRequestMapping("/home")
public class HomeContorller {

    @MyAutowired
    IUserService userService;

    @MyRequestMapping("/login")
    public void login(HttpServletRequest request,HttpServletResponse response,
            @MyRequestParam("username")String username,@MyRequestParam("password") String password) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.getWriter().write(userService.login(username,password));
    }

    @MyRequestMapping("/register")
    public String register(HttpServletRequest request,HttpServletResponse response,
            @MyRequestParam("username")String username,@MyRequestParam("password") String password)
    {
        return userService.login(username,password);
    }

}
