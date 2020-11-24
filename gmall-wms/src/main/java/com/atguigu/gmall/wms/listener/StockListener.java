package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class StockListener {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WareSkuMapper wareSkuMapper;

    private static final String KEY_PREFIX = "stock:lock:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS_UNLOCK_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unlock(String orderToken, Channel channel, Message message){
        try {
            if (StringUtils.isBlank(orderToken)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
            if (StringUtils.isBlank(json)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            List<SkuLockVo> skuLockVos = JSON.parseArray(json, SkuLockVo.class);
            if (CollectionUtils.isEmpty(skuLockVos)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            skuLockVos.forEach(skuLockVo -> {
                this.wareSkuMapper.unlock(skuLockVo.getWareSkuId(), skuLockVo.getCount());
            });

            this.redisTemplate.expire(KEY_PREFIX + orderToken, 1, TimeUnit.SECONDS);
            this.redisTemplate.delete(KEY_PREFIX + orderToken);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "WMS_MINUS_QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minus(String orderToken, Channel channel, Message message){
        try {
            if (StringUtils.isBlank(orderToken)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
            if (StringUtils.isBlank(json)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            List<SkuLockVo> skuLockVos = JSON.parseArray(json, SkuLockVo.class);
            if (CollectionUtils.isEmpty(skuLockVos)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            skuLockVos.forEach(skuLockVo -> {
                this.wareSkuMapper.minus(skuLockVo.getWareSkuId(), skuLockVo.getCount());
            });

            this.redisTemplate.expire(KEY_PREFIX + orderToken, 1, TimeUnit.SECONDS);
            this.redisTemplate.delete(KEY_PREFIX + orderToken);

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
