package com.budwk.nb.starter.xxl.job.test;

import com.budwk.nb.starter.xxl.job.annotions.XxlMethodJob;
import com.xxl.job.core.handler.annotation.XxlJob;

@XxlMethodJob
public class TestMethodJob {

    @XxlJob(value = "say")
    public void sayHello(){
        System.out.println("Hello");
    }
}
