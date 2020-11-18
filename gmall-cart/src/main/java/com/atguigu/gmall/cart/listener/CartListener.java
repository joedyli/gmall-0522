package com.atguigu.gmall.cart.listener;

import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.rabbitmq.client.Channel;
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
import java.lang.reflect.Member;
import java.util.List;

@Component
public class CartListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String PRICE_PREFIX = "cart:price:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "CART_PRICE_QUEUE", durable = "true"),
            exchange = @Exchange(value = "PMS_SPU_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId, Channel channel, Message message){
        try {
            if (spuId == null){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }

            // 查询该spuId下所有的sku
            ResponseVo<List<SkuEntity>> listResponseVo = this.pmsClient.querySkusBySpuId(spuId);
            List<SkuEntity> skuEntities = listResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuEntities)){
                skuEntities.forEach(skuEntity -> {
                    this.redisTemplate.opsForValue().setIfPresent(PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
                });
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
