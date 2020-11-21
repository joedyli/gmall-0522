package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private CartAsyncService asyncService;

    private static final String KEY_PREFIX = "cart:info:";
    private static final String PRICE_PREFIX = "cart:price:";

    public void addCart(Cart cart) {
        // 获取登录信息。如果userId不为空，就以userId作为key，如果userId为空，就以userKey作为key
        String userId = getUserId();

        // 通过外层的key获取内层的map结构
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);

        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();

        // 判断该用户的购物车中是否包含当前这条商品
        if (hashOps.hasKey(skuId)){
            // 包含，则更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount().add(count));

            // 写redis 写mysql
            this.asyncService.updateCart(userId, cart);
        } else {
            // 不包含，则新增一条记录：skuId count
            cart.setUserId(userId);

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return;
            }
            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setDefaultImage(skuEntity.getDefaultImage());

            // 查询库存信息
            ResponseVo<List<WareSkuEntity>> listResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = listResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            // 销售属性
            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySaleAttrsBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));

            // 营销信息
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> sales = salesResponseVo.getData();
            cart.setSales(JSON.toJSONString(sales));

            cart.setCheck(true);

            this.asyncService.insertCart(userId, cart);

            // 添加价格缓存
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId, skuEntity.getPrice().toString());
        }
        hashOps.put(skuId, JSON.toJSONString(cart));
    }

    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getUserId() == null){
            return userInfo.getUserKey();
        }
        return userInfo.getUserId().toString();
    }

    public Cart queryCart(Long skuId) {
        String userId = this.getUserId();

        // 获取内存的map
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);

        if (hashOps.hasKey(skuId.toString())){
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson, Cart.class);
        }

        throw new CartException("此用户不存在这条购物车记录！");
    }



    @Async
    public void executor1(){
        try {
            System.out.println("executor1方法开始执行。。。。。");
            TimeUnit.SECONDS.sleep(4);
            int i = 1/0;
            System.out.println("executor1方法结束执行。。。。。");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Async
    public ListenableFuture<String> executor2(){
        try {
            System.out.println("executor2方法开始执行。。。。。");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor2方法结束执行。。。。。");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return AsyncResult.forValue("hello executor2");
    }

    public List<Cart> queryCarts() {
        // 1.获取userKey
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();

        // 2.查询未登录的购物车内层map
        BoundHashOperations<String, Object, Object> unLoginHashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userKey);
        List<Object> unloginCartJsons = unLoginHashOps.values();
        List<Cart> unLoginCarts = null;
        if (!CollectionUtils.isEmpty(unloginCartJsons)){
            unLoginCarts = unloginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }

        // 3.获取userId，并判断userId是否为空，为空则直接返回未登录的购物车
        Long userId = userInfo.getUserId();
        if (userId == null) {
            return unLoginCarts;
        }

        // 4.获取登录状态的购物车内层map
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);

        // 5.把未登录的购物车合并到登录状态的购物车的内层map中
        if (!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                BigDecimal count = cart.getCount();
                if (loginHashOps.hasKey(skuId)){
                    // 登录状态的购物车中包含该记录，更新数量
                    String cartjson = loginHashOps.get(skuId).toString();
                    cart = JSON.parseObject(cartjson, Cart.class);
                    cart.setCount(cart.getCount().add(count));

                    this.asyncService.updateCart(userId.toString(), cart);
                } else {
                    // 登录状态的购物车中不包含该记录，新增一条记录
                    cart.setUserId(userId.toString());
                    this.asyncService.insertCart(userId.toString(), cart);
                }
                // 更新到redis
                loginHashOps.put(skuId, JSON.toJSONString(cart));
            });

            // 6.删除未登录的购物车
            this.redisTemplate.delete(KEY_PREFIX + userKey);
            this.asyncService.deleteCart(userKey);
        }

        // 7.查询登录状态的购物车并返回
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)){
            return loginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }

        return null;
    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            BigDecimal count = cart.getCount();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            this.asyncService.updateCart(userId, cart);
            return ;
        }
        throw new CartException("该用户的购物车不包含该条记录。");
    }

    public void deleteCart(Long skuId) {

        String userId = this.getUserId();

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (hashOps.hasKey(skuId.toString())){

            hashOps.delete(skuId.toString());
            this.asyncService.deleteCartByUserIdAndSkuId(userId, skuId);
            return ;
        }
        throw new CartException("该用户的购物车不包含该条记录。");
    }

    public List<Cart> queryCheckedCartsByUserId(Long userId) {

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
        List<Object> cartjsons = hashOps.values();
        if (CollectionUtils.isEmpty(cartjsons)){
            throw new CartException("您没有购物车记录。。。");
        }
        return cartjsons.stream().map(cartjson -> JSON.parseObject(cartjson.toString(), Cart.class)).filter(Cart::getCheck).collect(Collectors.toList());
    }
}
