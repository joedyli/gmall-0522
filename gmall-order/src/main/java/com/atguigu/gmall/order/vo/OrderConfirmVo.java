package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    private List<UserAddressEntity> addresses;

    private List<OrderItemVo> orderItems;

    private Integer bounds;

    private String orderToken; // 防重

    // TODO：发票 优惠
}
