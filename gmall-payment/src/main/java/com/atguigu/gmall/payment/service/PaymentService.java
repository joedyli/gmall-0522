package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class PaymentService {

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    public OrderEntity queryOrder(String orderToken){
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.queryOrder(orderToken, userId);
        return orderEntityResponseVo.getData();
    }

    public Long savePaymentInfo(OrderEntity orderEntity) {
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setOutTradeNo(orderEntity.getOrderSn());
        paymentInfoEntity.setPaymentStatus(0);
        paymentInfoEntity.setPaymentType(1);
        paymentInfoEntity.setSubject("谷粒商城支付平台");
        paymentInfoEntity.setTotalAmount(new BigDecimal("0.01"));
        paymentInfoEntity.setCreateTime(new Date());
        this.paymentInfoMapper.insert(paymentInfoEntity);
        return paymentInfoEntity.getId();
    }

    public PaymentInfoEntity query(String payId){
        return this.paymentInfoMapper.selectById(payId);
    }

    public void update(PaymentInfoEntity paymentInfo) {
        this.paymentInfoMapper.updateById(paymentInfo);
    }
}
