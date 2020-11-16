package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

@Data
public class GroupVo {
    private Long groupId;
    private String groupName;

    private List<AttrValueVo> attrs;
}
