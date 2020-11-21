package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.OrderSubmitVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("confirm")
    public String confirm(Model model){
        OrderConfirmVo confirmVo = this.orderService.confirm();
        model.addAttribute("confirmVo", confirmVo);
        return "trade";
    }

    @PostMapping("submit")
    public ResponseVo<String> submit(@RequestBody OrderSubmitVo submitVo){
        String orderToken = this.orderService.submit(submitVo);
        return ResponseVo.ok(orderToken);
    }
}
