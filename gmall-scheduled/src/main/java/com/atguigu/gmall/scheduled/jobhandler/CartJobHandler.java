package com.atguigu.gmall.scheduled.jobhandler;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.atguigu.gmall.scheduled.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartJobHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String EXCEPTION_KEY = "cart:exception";
    private static final String KEY_PREFIX = "cart:info:";

    @Autowired
    private CartMapper cartMapper;

    @XxlJob("cartJobHandler")
    public ReturnT<String> handler(String param){

        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(EXCEPTION_KEY);
        String userId = setOps.pop();
        // 只要可以取出一个非空的userId，无限循环
        while (StringUtils.isNotBlank(userId)){
            // 先清空该用户所有mysql中的购物车记录
            this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId));

            // 再读取出redis中该用户的购物车记录
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            List<Object> cartjsons = hashOps.values();
            if (!CollectionUtils.isEmpty(cartjsons)){
                // 最后添加到mysql中
                cartjsons.forEach(cartJson -> {
                    Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                    this.cartMapper.insert(cart);
                });
            }

            // 取下一个
            userId = setOps.pop();
        }

        return ReturnT.SUCCESS;
    }
}
