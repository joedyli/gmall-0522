package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    private String orderToken; // 防重
    private BigDecimal totalPrice; // 验总价
    private UserAddressEntity address; // 收货人地址
    private Integer bounds; // 使用积分信息
    private Integer payType; // 支付方式
    private String deliveryCompany; // 配送方式
    private List<OrderItemVo> items; // 送货清单
}
