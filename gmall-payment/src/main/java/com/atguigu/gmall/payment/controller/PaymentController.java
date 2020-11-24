package com.atguigu.gmall.payment.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.config.AlipayTemplate;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.interceptor.LoginInterceptor;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import com.atguigu.gmall.payment.vo.PayVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

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
            OrderEntity orderEntity = this.paymentService.queryOrder(orderToken);
            if (orderEntity == null || orderEntity.getStatus() != 0) {
                throw new OrderException("该用户要支付的订单不合法！");
            }

            // 保存对账记录信息
            Long payId = this.paymentService.savePaymentInfo(orderEntity);

            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(orderToken);
            payVo.setTotal_amount("0.01");// 这里一定不要取订单中的金额，建议取0.01。保留两位小数
            payVo.setSubject("谷粒商城支付平台");
            payVo.setPassback_params(payId.toString());

            String form = this.alipayTemplate.pay(payVo);
            return form;
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 支付宝异步回调接口
     * 在一天左右的时间回调多次
     * 更新订单状态
     * @return
     */
    @PostMapping("pay/success")
    @ResponseBody
    public String paySuccess(PayAsyncVo payAsyncVo){
        // 1.验签：确保支付宝发送过来的
        Boolean flag = this.alipayTemplate.checkSignature(payAsyncVo);
        if (!flag){
            return "failure";
        }

        // 2.校验商家及订单的业务参数: app_id、out_trade_no、total_amount
        // 支付宝响应的业务参数
        String app_id = payAsyncVo.getApp_id();
        String out_trade_no = payAsyncVo.getOut_trade_no();
        String total_amount = payAsyncVo.getTotal_amount();
        String payId = payAsyncVo.getPassback_params();
        PaymentInfoEntity paymentInfo = this.paymentService.query(payId); // 数据库中业务参数
        if (!StringUtils.equals(app_id, this.alipayTemplate.getApp_id()) ||
                !StringUtils.equals(out_trade_no, paymentInfo.getOutTradeNo()) ||
                paymentInfo.getTotalAmount().compareTo(new BigDecimal(total_amount)) != 0
        ){
            return "failure";
        }

        // 3.校验支付状态：TRADE_SUCCESS
        String trade_status = payAsyncVo.getTrade_status();
        if (!StringUtils.equals("TRADE_SUCCESS", trade_status)){
            return "failure";
        }

        // 4.更新对账信息
        paymentInfo.setPaymentStatus(1);
        paymentInfo.setTradeNo(payAsyncVo.getTrade_no());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSON.toJSONString(payAsyncVo));
        this.paymentService.update(paymentInfo);

        // 5.发送消息给oms更新订单状态，并发送消息给wms减库存
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.success", out_trade_no);

        // 6.响应成功内容给支付宝
        return "success";
    }

    /**
     * 支付宝同步回调接口
     * 通常跳转到成功页面
     * @return
     */
    @GetMapping("pay/ok")
    public Object payOk(){

        return "paysuccess";
    }

    @GetMapping("seckill/{skuId}")
    public String seckill(@PathVariable("skuId")Long skuId, Model model) throws InterruptedException {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // 为了防止发生超卖的情况：lua脚本 或者使用分布式锁
        RLock fairLock = this.redissonClient.getFairLock("seckill:lock:" + skuId);
        fairLock.lock();

        String stockString = this.redisTemplate.opsForValue().get("seckill:stock:" + skuId);
        if (StringUtils.isBlank(stockString)){
            throw new OrderException("手慢了，请下次再来");
        }

        int stock = Integer.parseInt(stockString);
        if (stock <= 0){
            throw new OrderException("手慢了，请下次再来");
        }

        this.redisTemplate.opsForValue().decrement("seckill:stock:" + skuId);

        RSemaphore semaphore = this.redissonClient.getSemaphore("seckill:semaphore:" + skuId);
        semaphore.trySetPermits(500);
        semaphore.acquire(1);
        String orderToken = IdWorker.getTimeId();
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("skuId", skuId);
        map.put("count", 1);
        map.put("orderToken", orderToken);
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "seckill.success", map);
        semaphore.release();

        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("seckill:token:" + orderToken);
        countDownLatch.trySetCount(1l);

        fairLock.unlock();

        model.addAttribute("orderToken", orderToken);
        return "seckillsuccess";
    }

    @GetMapping("seckill/order/{orderToken}")
    public String queryOrder(@PathVariable("orderToken")String orderToken) throws InterruptedException {
        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("seckill:token:" + orderToken);
        countDownLatch.await();

        // TODO:根据订单编号查询订单

        return "xxx";
    }
}
