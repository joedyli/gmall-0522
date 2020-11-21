package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "stock:lock:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken) {

        if (CollectionUtils.isEmpty(lockVos)){
            throw new OrderException("没有要购买的商品");
        }

        // 一定要一次性遍历所有送货清单，验库存并锁库存
        lockVos.forEach(lockVo -> {
            this.checkLock(lockVo);
        });

        // 只要有一个商品锁定失败，就要把所有锁定成功的商品解锁库存
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())){
            List<SkuLockVo> successLockVos = lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
            successLockVos.forEach(lockVo -> {
                this.wareSkuMapper.unlock(lockVo.getWareSkuId(), lockVo.getCount());
            });
            return lockVos;
        }

        // 为了方便将来减库存或者解锁库存，需要把该订单对应的锁定库存信息缓存到redis中
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));

        // 如果都锁定成功的情况下，返回null
        return null;
    }

    private void checkLock(SkuLockVo lockVo){
        RLock fairLock = this.redissonClient.getFairLock("stock:" + lockVo.getSkuId());
        fairLock.lock();

        // 1.查询库存信息
        List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.checkLock(lockVo.getSkuId(), lockVo.getCount());
        if (CollectionUtils.isEmpty(wareSkuEntities)){
            lockVo.setLock(false); // 如果没有仓库满足购买要求，则锁定失败
            fairLock.unlock();
            return;
        }

        // 2.锁定库存信息：假装大数据分析完成
        WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
        if (this.wareSkuMapper.lock(wareSkuEntity.getId(), lockVo.getCount()) == 1) {
            lockVo.setLock(true);
            lockVo.setWareSkuId(wareSkuEntity.getId());
        }

        fairLock.unlock();
    }
}
