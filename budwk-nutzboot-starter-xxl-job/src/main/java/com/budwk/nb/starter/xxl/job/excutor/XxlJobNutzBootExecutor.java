package com.budwk.nb.starter.xxl.job.excutor;

import com.budwk.nb.starter.xxl.job.annotions.XxlMethodJob;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.impl.MethodJobHandler;
import org.nutz.boot.annotation.PropDoc;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import java.lang.reflect.Method;

@IocBean(create = "init")
public class XxlJobNutzBootExecutor extends XxlJobExecutor {

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

    public void init() {
        // 初始化配置
        initConfig();
        // 初始化注册 JobHandler
        intiRegisterJobHandlerRepository();
        // 初始化注册 JobHandlerMethod
        initJobHandlerMethodRepository();
    }

    private void intiRegisterJobHandlerRepository() {
        String[] jobHandlerName = ioc.getNamesByType(IJobHandler.class);
        if (jobHandlerName != null && jobHandlerName.length > 0) {
            for (String name : jobHandlerName) {
                registJobHandler(name, ioc.get(IJobHandler.class, name));
            }
        }

    }

    @XxlJob
    private void initJobHandlerMethodRepository() {

        // init job handler from method

        String[] beanDefinitionNames = ioc.getNamesByAnnotation(XxlMethodJob.class);
        if (beanDefinitionNames != null && beanDefinitionNames.length > 0) {
            for (String beanDefinitionName : beanDefinitionNames) {
                Object bean = ioc.get(null, beanDefinitionName);
                Method[] methods = bean.getClass().getMethods();
                for (Method method : methods) {
                    XxlJob xxlJob = method.getAnnotation(XxlJob.class);
                    if (xxlJob != null) {
                        // name
                        String name = xxlJob.value();
                        if (name.trim().length() == 0) {
                            log.warn("xxl-job method-jobhandler name invalid, for[" + bean.getClass() + "#" + method.getName() + "] .");
                            continue;
                        }
                        if (loadJobHandler(name) != null) {
                            log.warn("xxl-job jobhandler[" + name + "] naming conflicts.");
                            continue;
                        }

                        // execute method
                        if (!(method.getParameterTypes() != null && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(String.class))) {
                            log.warn("xxl-job method-jobhandler param-classtype invalid, for[" + bean.getClass() + "#" + method.getName() + "] , " +
                                    "The correct method format like \" public ReturnT<String> execute(String param) \" .");
                            continue;
                        }
                        if (!method.getReturnType().isAssignableFrom(ReturnT.class)) {
                            log.warn("xxl-job method-jobhandler return-classtype invalid, for[" + bean.getClass() + "#" + method.getName() + "] , " +
                                    "The correct method format like \" public ReturnT<String> execute(String param) \" .");
                            continue;
                        }
                        method.setAccessible(true);

                        // init and destory
                        Method initMethod = null;
                        Method destroyMethod = null;

                        if (xxlJob.init().trim().length() > 0) {
                            try {
                                initMethod = bean.getClass().getDeclaredMethod(xxlJob.init());
                                initMethod.setAccessible(true);
                            } catch (NoSuchMethodException e) {
                                log.warn("xxl-job method-jobhandler initMethod invalid, for[" + bean.getClass() + "#" + method.getName() + "] .");
                                continue;
                            }
                        }
                        if (xxlJob.destroy().trim().length() > 0) {
                            try {
                                destroyMethod = bean.getClass().getDeclaredMethod(xxlJob.destroy());
                                destroyMethod.setAccessible(true);
                            } catch (NoSuchMethodException e) {
                                log.warn("xxl-job method-jobhandler destroyMethod invalid, for[" + bean.getClass() + "#" + method.getName() + "] .");
                                continue;
                            }
                        }

                        // registry jobhandler
                        registJobHandler(name, new MethodJobHandler(bean, method, initMethod, destroyMethod));
                    }
                }
            }
        }

    }

    private void initConfig() {
        this.setAdminAddresses(conf.get(ADMIN_ADDRESS));
        this.setAppName(conf.get(EXECUTOR_APP_NAME));
        this.setIp(conf.get(EXECUTOR_IP));
        this.setPort(conf.getInt(EXECUTOR_PORT));
        this.setAccessToken(conf.get(ACCESS_TOKEN));
        this.setLogPath(conf.get(LOG_PATH));
        this.setLogRetentionDays(conf.getInt(LOG_RETENTION_DAYS));
    }


}
