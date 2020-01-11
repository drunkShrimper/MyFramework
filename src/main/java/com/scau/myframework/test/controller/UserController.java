package com.scau.myframework.test.controller;


import com.scau.myframework.mvc.annotation.*;
import com.scau.myframework.mvc.entity.ModelAndView;
import com.scau.myframework.test.entity.User;
import com.scau.myframework.test.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@MyController("UserController")
@MyRequestMapping("/user")
public class UserController {

    @MyAutowired("UserServiceImpl")
    private UserService userService;

    @MyRequestMapping("/findOne")
    public User findOne(HttpServletRequest request, HttpServletResponse response) {
        User user = new User();
        user.setAge(123);
        user.setName("sdfds");
        return user;
    }

    @MyRequestMapping("/putOne")
    public User putOne(@MyRequestParam("name") String name,@MyRequestParam("age") Integer age, HttpServletRequest request, HttpServletResponse response) {
        User user = new User();
        user.setAge(age);
        user.setName(name);
        return user;
    }

    @MyRequestMapping("/putOne2")
    public User putOne2(@MyRequestParam("name") String name,@MyRequestParam("age") int age, HttpServletRequest request, HttpServletResponse response) {
        User user = new User();
        user.setAge(age);
        user.setName(name);
        return user;
    }

    @MyRequestMapping("/showmv")
    public ModelAndView showMv(HttpServletRequest request, HttpServletResponse response) {

        ModelAndView mv = new ModelAndView();
        mv.setPath("show");
        return mv;
    }

    @MyRequestMapping("/showpage")
    public String showPage(HttpServletRequest request, HttpServletResponse response) {

        return "show";
    }

    @MyResponseBody
    @MyRequestMapping("/showjson")
    public List<Integer> showjson(HttpServletRequest request, HttpServletResponse response) {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(123);
        list.add(456);
        list.add(789);
        return list;
    }
}
