package com.budwk.nb.starter.xxl.job;

import com.budwk.nb.starter.xxl.job.excutor.XxlJobNutzBootExecutor;
import org.nutz.boot.annotation.PropDoc;
import org.nutz.boot.starter.ServerFace;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.log.Log;
import org.nutz.log.Logs;

@IocBean
public class XXLJobStarter implements ServerFace {

    private static final Log log = Logs.get();
    protected static final String PRE = "xxl.job.";

    @PropDoc(value = "调度中心部署跟地址 [选填]：如调度中心集群部署存在多个地址则用逗号分隔。执行器将会使用该地址进行\"执行器心跳注册\"和\"任务结果回调\"；为空则关闭自动注册", type = "String")
    public static final String ADMIN_ADDRESS = PRE + "admin.addresses";

    @PropDoc(value = "执行器AppName [选填]：执行器心跳注册分组依据；为空则关闭自动注册", type = "String")
    private static final String EXECUTOR_APP_NAME = PRE + "executor.appname";

    @PropDoc(value = "执行器IP [选填]：默认为空表示自动获取IP，多网卡时可手动设置指定IP，该IP不会绑定Host仅作为通讯实用；地址信息用于 \"执行器注册\" 和 \"调度中心请求并触发任务\"", type = "String")
    private static final String EXECUTOR_IP = PRE + "executor.ip";

    @PropDoc(value = "执行器端口号 [选填]：小于等于0则自动获取；默认端口为9999，单机部署多个执行器时，注意要配置不同执行器端口", type = "int")
    private static final String EXECUTOR_PORT = PRE + "executor.port";

    @PropDoc(value = "执行器通讯TOKEN [选填]：非空时启用", type = "String")
    private static final String ACCESS_TOKEN = PRE + "accessToken";

    @PropDoc(value = "执行器运行日志文件存储磁盘路径 [选填] ：需要对该路径拥有读写权限；为空则使用默认路径", defaultValue = "", type = "String")
    private static final String LOG_PATH = PRE + "log.path";

    @PropDoc(value = " 执行器日志文件保存天数 [选填] ： 过期日志自动清理, 限制值大于等于3时生效; 否则, 如-1, 关闭自动清理功能", type = "int")
    private static final String LOG_RETENTION_DAYS = PRE + "log.retention";

    @Inject
    private PropertiesProxy conf;
    // 获取ioc容器
    @Inject("refer:$ioc")
    private Ioc ioc;
    private boolean running = false;

    private XxlJobNutzBootExecutor xxlJobNutzBootExecutor;

    @Override
    public void start() throws Exception {
        if (xxlJobNutzBootExecutor == null) {
            initExecutor();
        }
        xxlJobNutzBootExecutor.start();
        running = true;
    }

    private void initExecutor() {
        xxlJobNutzBootExecutor = new XxlJobNutzBootExecutor(ioc);
        xxlJobNutzBootExecutor.setAdminAddresses(conf.get(ADMIN_ADDRESS));
        xxlJobNutzBootExecutor.setAppName(conf.get(EXECUTOR_APP_NAME,conf.get("nutz.application.name")));
        xxlJobNutzBootExecutor.setIp(conf.get(EXECUTOR_IP));
        xxlJobNutzBootExecutor.setPort(conf.getInt(EXECUTOR_PORT));
        xxlJobNutzBootExecutor.setAccessToken(conf.get(ACCESS_TOKEN));
        xxlJobNutzBootExecutor.setLogPath(conf.get(LOG_PATH));
        xxlJobNutzBootExecutor.setLogRetentionDays(conf.getInt(LOG_RETENTION_DAYS));
        xxlJobNutzBootExecutor.init();
    }

    @Override
    public void stop() throws Exception {
        if (xxlJobNutzBootExecutor == null) {
            return;
        }
        xxlJobNutzBootExecutor.destroy();
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
