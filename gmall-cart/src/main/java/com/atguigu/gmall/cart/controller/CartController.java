package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class CartController {

    @GetMapping("test")
    public String test(HttpServletRequest request){
        System.out.println("这是一个Handler方法。。。。。。。。。。。。。" + LoginInterceptor.getUserInfo());
        return "hello interceptors";
    }
}
