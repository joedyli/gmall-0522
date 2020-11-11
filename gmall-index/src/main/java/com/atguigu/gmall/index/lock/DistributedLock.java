package com.atguigu.gmall.index.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Thread thread;

    public Boolean tryLock(String lockName, String uuid, Integer expireTime){
        String script = "if(redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1) then redis.call('hincrby', KEYS[1], ARGV[1], 1); redis.call('expire', KEYS[1], ARGV[2]); return 1; else return 0; end;";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expireTime.toString());
        if (!flag){
            try {
                Thread.sleep(50);
                tryLock(lockName, uuid, expireTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.renewTime(lockName, uuid, expireTime);
        return true;
    }

    public void unlock(String lockName, String uuid){
        String script = "if(redis.call('hexists', KEYS[1], ARGV[1]) == 0) then return nil; elseif(redis.call('hincrby', KEYS[1], ARGV[1], -1) == 0) then redis.call('del', KEYS[1]); return 1; else return 0; end;";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid);
        if (flag == null){
            throw new RuntimeException("您要释放的锁不存在，获取在尝试释放别人的锁！");
        }
        thread.interrupt();
    }

    private void renewTime(String lockName, String uuid, Integer expireTime){
        String script = "if(redis.call('hexists', KEYS[1], ARGV[1]) == 1) then redis.call('expire', KEYS[1], ARGV[2]); return 1; else return 0; end";
        thread = new Thread(() -> {
            while(true){
                try {
                    Thread.sleep(expireTime * 2000 / 3);
                    this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expireTime.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "");
        thread.start();
    }
}
