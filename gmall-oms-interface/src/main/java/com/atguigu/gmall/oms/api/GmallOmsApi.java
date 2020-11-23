package com.atguigu.gmall.oms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.web.bind.annotation.*;

public interface GmallOmsApi {

    @PostMapping("oms/order/submit/{userId}")
    public ResponseVo<OrderEntity> saveOrder(@RequestBody OrderSubmitVo submitVo, @PathVariable("userId") Long userId);

    @GetMapping("oms/order/query/{orderToken}")
    public ResponseVo<OrderEntity> queryOrder(@PathVariable("orderToken")String orderToken, @RequestParam("userId")Long userId);
}
