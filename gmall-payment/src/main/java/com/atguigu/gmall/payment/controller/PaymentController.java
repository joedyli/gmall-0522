package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @GetMapping("pay.html")
    public String toPay(@RequestParam("orderToken")String orderToken, Model model){
        OrderEntity orderEntity = this.paymentService.queryOrder(orderToken);
        if (orderEntity == null || orderEntity.getStatus() != 0) {
            throw new OrderException("该用户要支付的订单不合法！");
        }
        model.addAttribute("orderEntity", orderEntity);
        return "pay";
    }

    @GetMapping("alipay.html")
    @ResponseBody // 以其他视图的形式渲染方法的返回结果集，通常渲染成json
    public String toAlipay(@RequestParam("orderToken")String orderToken){
        try {
            PayVo payVo = new PayVo();
            String form = this.alipayTemplate.pay(payVo);
            return form;
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 支付宝异步回到接口
     * 在一天左右的时间回调多次
     * 更新订单状态
     * @return
     */
    @PostMapping("pay/success")
    public Object paySuccess(){

        return null;
    }

    /**
     * 支付宝同步回到接口
     * 通常跳转到成功页面
     * @return
     */
    @GetMapping("pay/ok")
    public Object payOk(){

        return "paysuccess";
    }
}
