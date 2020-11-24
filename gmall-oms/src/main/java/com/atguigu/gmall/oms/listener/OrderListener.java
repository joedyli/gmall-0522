package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.security.auth.login.LoginContext;
import java.io.IOException;


@Component
public class OrderListener {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "OMS_INVALID_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.invalid"}
    ))
    public void invalid(String orderToken, Channel channel, Message message){
        try {
            if (StringUtils.isBlank(orderToken)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 1.更新订单状态，更新为无效订单
            if (this.orderMapper.updateStatus(orderToken, 0, 5) == 1) {
                // 2.发送消息给wms，解锁库存信息
                this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.unlock", orderToken);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(queues = "ORDER_DEAD_QUEUE")
    public void close(String orderToken, Channel channel, Message message){
        try {
            if (StringUtils.isBlank(orderToken)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }

            // 1.更新订单状态，更新为已关闭订单
            if (this.orderMapper.updateStatus(orderToken, 0, 4) == 1) {
                // 2.发送消息给wms，解锁库存信息
                this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.unlock", orderToken);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "OMS_SUCCESS_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.success"}
    ))
    public void success(String orderToken, Channel channel, Message message){
        try {
            if (StringUtils.isBlank(orderToken)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 1.更新订单状态，更新为待发货订单
            if (this.orderMapper.updateStatus(orderToken, 0, 1) == 1) {
                // 2.发送消息给wms，减库存信息
                this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.minus", orderToken);
            }

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
