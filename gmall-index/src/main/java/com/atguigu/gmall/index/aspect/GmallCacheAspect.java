package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter<String> bloomFilter;

    /**
     * 获取目标方法参数：joinPoint.getArgs()
     * 获取目标方法签名：MethodSignature signature = (MethodSignature)joinPoint.getSignature()
     * 获取目标类：joinPoint.getTarget().getClass();
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        //System.out.println("环绕前通知。。。。。。。。。。。。。。。。。。。。。。");

        // 获取目标方法GmallCache注解对象
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        // 获取目标方法对象
        Method method = signature.getMethod();
        // 获取目标方法上的注解对象
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        // 获取gmallCache中的前缀属性
        String prefix = gmallCache.prefix();
        // 获取方法的返回值类型
        Class<?> returnType = method.getReturnType();

        // 获取目标方法的参数列表
        List<Object> args = Arrays.asList(joinPoint.getArgs());
        // 组装成缓存key
        String key = prefix + args;

        boolean flag = this.bloomFilter.contains(key);
        if (!flag){
            return null;
        }

        // 先查询缓存，缓存中有直接反序列化后，直接返回
        String json = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseObject(json, returnType);
        }

        // 为了防止缓存击穿，可以添加分布式锁
        String lock = gmallCache.lock();
        RLock fairLock = this.redissonClient.getFairLock(lock + args);
        fairLock.lock();

        try {
            // 再查缓存，缓存中有直接反序列化后，直接返回
            String json2 = this.redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(json2)) {
                return JSON.parseObject(json2, returnType);
            }

            // 执行目标方法，获取数据库中的数据
            Object result = joinPoint.proceed(joinPoint.getArgs());

            // 放入缓存。如果result为null，为了防止缓存穿透，依然放入缓存，但缓存时间极短
            if (result == null){
                //this.redisTemplate.opsForValue().set(key, null, 1, TimeUnit.MINUTES);
            } else {
                // 为了防止缓存雪崩，要给缓存时间添加随机值
                long timeout = gmallCache.timeout() + new Random().nextInt(gmallCache.random());
                this.redisTemplate.opsForValue().set(key, JSON.toJSONString(result), timeout, TimeUnit.MINUTES);
            }
            //System.out.println("环绕后通知。。。。。。。。。。。。。。。。。。。。。。。。");
            return result;
        } finally {
            fairLock.unlock();
        }
    }

    @Before("execution(* com.atguigu.gmall.index.service.*.*(..))")
    public void before(JoinPoint joinPoint){
        joinPoint.getArgs();
    }

    @AfterReturning(value = "execution(* com.atguigu.gmall.index.service.*.*(..))", returning = "result")
    public void AfterReturning(JoinPoint joinPoint, Object result){
        joinPoint.getArgs();
    }

    @AfterThrowing(value = "execution(* com.atguigu.gmall.index.service.*.*(..))", throwing = "ex")
    public void AfterThrowing(JoinPoint joinPoint, Throwable ex){

        joinPoint.getArgs();
    }

    @After(value = "execution(* com.atguigu.gmall.index.service.*.*(..))")
    public void After(JoinPoint joinPoint){

        joinPoint.getArgs();
    }
}
