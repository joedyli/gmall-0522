package com.atguigu.gmall.index.aspect;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    /**
     * 缓存key的前缀
     * 结构：木块名+':'+实例名+':'
     * 例如：首页工程三级分类缓存
     *  index:cates:
     * @return
     */
    String prefix() default "gmall:cache:";

    /**
     * 缓存的过期时间：单位为分钟
     * @return
     */
    long timeout() default 5l;

    /**
     * 防止缓存雪崩，给缓存时间添加随机值
     * 这里可以指定随机值范围
     * @return
     */
    int random() default 5;

    /**
     * 为了防止缓存击穿，给缓存添加分布式锁
     * 这里可以指定分布式锁的前缀
     * 最终分布式锁名称：lock + 方法参数
     * @return
     */
    String lock() default "lock:";
}
