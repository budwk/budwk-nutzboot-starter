package com.budwk.nb.starter.xxl.job;

import com.budwk.nb.starter.xxl.job.excutor.XxlJobNutzBootExecutor;
import org.nutz.boot.starter.ServerFace;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.log.Log;
import org.nutz.log.Logs;

@IocBean
public class XXLJobStarter implements ServerFace {

    private static final Log log = Logs.get();

    private boolean running = false;
    // 获取ioc容器
    @Inject("refer:$ioc")
    private Ioc ioc;

    @Override
    public void start() throws Exception {
        ioc.get(XxlJobNutzBootExecutor.class).start();
        running = true;
    }


    @Override
    public void stop() throws Exception {
        ioc.get(XxlJobNutzBootExecutor.class).destroy();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean failsafe() {
        return false;
    }
}
