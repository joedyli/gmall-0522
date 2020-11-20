package com.atguigu.gmall.scheduled.jobhandler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MyJobHandler {

    /**
     * 1.方法的返回值必须是ReturnT<String> ，参数必须是一个字符串的参数
     * 2.方法必须添加@XxlJob("唯一标识")
     * 3.如果想向调度中心输出日志，通过XxlJobLogger
     *
     * @param param
     * @return
     */
    @XxlJob("myJobHandler")
    public ReturnT<String> handler(String param){
        XxlJobLogger.log("我向调度中心输出日志。。。。。。。。。。。。");
        log.info("我是一个xxl-job定时任务，接受到了调度中心的参数：{}", param);

        return ReturnT.SUCCESS;
    }
}
