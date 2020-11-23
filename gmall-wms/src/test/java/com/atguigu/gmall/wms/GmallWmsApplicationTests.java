package com.atguigu.gmall.wms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class GmallWmsApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "stock:lock:";

    @Test
    void contextLoads() {
       this.redisTemplate.delete(KEY_PREFIX + "202011231429307351330760368066146305");
    }

}
