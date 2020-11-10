package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import javafx.collections.ModifiableObservableListBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping({"/", "/index"})
    public String toIndex(Model model){
        // 一级分类
        List<CategoryEntity> cates = this.indexService.queryLvl1Categories();

        // TODO：查询广告

        model.addAttribute("categories", cates);
        return "index";
    }

    @GetMapping("/index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLvl2CategoriesWithSubsByPid(@PathVariable("pid")Long pid){
        List<CategoryEntity> categoryEntities = this.indexService.queryLvl2CategoriesWithSubsByPid(pid);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping("index/test/lock")
    @ResponseBody
    public ResponseVo testLock(){
        this.indexService.testLock();
        return ResponseVo.ok();
    }
}
