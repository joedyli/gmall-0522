package com.atguigu.gmall.item.vo;

import lombok.Data;

import java.util.List;

@Data
public class GroupVo {
    private Long groupId;
    private String groupName;

    private List<?> attrs;
}
