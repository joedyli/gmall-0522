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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping("/xxx")
    @ResponseBody
    public String test(@RequestHeader("userId")String userId){
        return "获取到的用户登录信息：" + userId;
    }

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

    @GetMapping("index/test/write")
    @ResponseBody
    public ResponseVo testWrite(){
        this.indexService.testWrite();
        return ResponseVo.ok("写入成功！");
    }

    @GetMapping("index/test/read")
    @ResponseBody
    public ResponseVo testRead(){
        this.indexService.testRead();
        return ResponseVo.ok("读取成功！");
    }

    @GetMapping("index/test/latch")
    @ResponseBody
    public ResponseVo testLatch() throws InterruptedException {
        this.indexService.latch();
        return ResponseVo.ok("班长成功锁门。。。。。");
    }

    @GetMapping("index/test/countdown")
    @ResponseBody
    public ResponseVo testCountdown(){
        this.indexService.countdown();
        return ResponseVo.ok("出来了一位同学。。。。。");
    }
}
