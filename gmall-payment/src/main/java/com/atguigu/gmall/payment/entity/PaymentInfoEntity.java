package com.atguigu.gmall.payment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("payment_info")
public class PaymentInfoEntity {

    @TableId
    private Long id;

    private String outTradeNo;

    private Integer paymentType;

    private String tradeNo;

    private BigDecimal totalAmount;

    private String subject;

    private Integer paymentStatus;

    private Date createTime;

    private Date callbackTime;

    private String callbackContent;
}
