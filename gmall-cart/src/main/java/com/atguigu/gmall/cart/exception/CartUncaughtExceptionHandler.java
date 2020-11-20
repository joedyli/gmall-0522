package com.atguigu.gmall.cart.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
@Component
public class CartUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;
    private static final String EXCEPTION_KEY = "cart:exception";

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        log.error("异步任务出现异常信息：{}，方法：{}，参数：{}", throwable.getMessage(), method.getName(), Arrays.asList(objects));

        // 记录异步执行失败的用户id
        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(EXCEPTION_KEY);
        if (objects != null && objects.length != 0){
            setOps.add(objects[0].toString());
        }
    }
}
