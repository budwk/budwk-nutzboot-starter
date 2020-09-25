package com.budwk.nb.starter.xxl.job.excutor;

import com.budwk.nb.starter.xxl.job.core.NutzBeanJobHandler;
import com.budwk.nb.starter.xxl.job.core.XxlMethodJob;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.nutz.ioc.Ioc;
import org.nutz.ioc.ObjectLoadException;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import java.lang.reflect.Method;

public class XxlJobNutzBootExecutor extends XxlJobExecutor {

    private static final Log log = Logs.get();


    private Ioc ioc;

    public XxlJobNutzBootExecutor(Ioc ioc) {
        this.ioc = ioc;
    }

    public void init() {
        // 初始化注册 JobHandler
        intiRegisterJobHandlerRepository();
        // 初始化注册 JobHandlerMethod
        initJobHandlerMethodRepository();
    }

    private void intiRegisterJobHandlerRepository() {
        String[] jobHandlerName = ioc.getNamesByType(IJobHandler.class);
        if (jobHandlerName != null && jobHandlerName.length > 0) {
            for (String name : jobHandlerName) {
                log.debugf("xxl-job register bean job %s",name);
                registJobHandler(name, NutzBeanJobHandler.beanJob(ioc, name, null));
            }
        }

    }

    private void initJobHandlerMethodRepository() {

        // init job handler from method
        String[] beanDefinitionNames = ioc.getNamesByAnnotation(XxlMethodJob.class);
        if (beanDefinitionNames != null && beanDefinitionNames.length > 0) {
            for (String beanDefinitionName : beanDefinitionNames) {

                Class<?> beanClazz = getBeanClass(beanDefinitionName);
                if (null == beanClazz) {
                    log.warnf("xxl-job method-jobhandler name %s invalid.", beanDefinitionName);
                    continue;
                }
                Method[] methods = beanClazz.getMethods();
                for (Method method : methods) {
                    XxlJob xxlJob = method.getAnnotation(XxlJob.class);
                    if (xxlJob != null) {
                        // name
                        String name = xxlJob.value();
                        if (name.trim().length() == 0) {
                            log.warn("xxl-job method-jobhandler name invalid, for[" + beanClazz + "#" + method.getName() + "] .");
                            continue;
                        }
                        if (loadJobHandler(name) != null) {
                            log.warn("xxl-job jobhandler[" + name + "] naming conflicts.");
                            continue;
                        }

                        // execute method
                        if (!(method.getParameterTypes() != null && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(String.class))) {
                            log.warn("xxl-job method-jobhandler param-classtype invalid, for[" + beanClazz + "#" + method.getName() + "] , " +
                                    "The correct method format like \" public ReturnT<String> execute(String param) \" .");
                            continue;
                        }
                        if (!method.getReturnType().isAssignableFrom(ReturnT.class)) {
                            log.warn("xxl-job method-jobhandler return-classtype invalid, for[" + beanClazz + "#" + method.getName() + "] , " +
                                    "The correct method format like \" public ReturnT<String> execute(String param) \" .");
                            continue;
                        }
                        method.setAccessible(true);

                        // init and destory
                        Method initMethod = null;
                        Method destroyMethod = null;

                        if (xxlJob.init().trim().length() > 0) {
                            try {
                                initMethod = beanClazz.getDeclaredMethod(xxlJob.init());
                                initMethod.setAccessible(true);
                            } catch (NoSuchMethodException e) {
                                log.warn("xxl-job method-jobhandler initMethod invalid, for[" + beanClazz + "#" + method.getName() + "] .");
                                continue;
                            }
                        }
                        if (xxlJob.destroy().trim().length() > 0) {
                            try {
                                destroyMethod = beanClazz.getDeclaredMethod(xxlJob.destroy());
                                destroyMethod.setAccessible(true);
                            } catch (NoSuchMethodException e) {
                                log.warn("xxl-job method-jobhandler destroyMethod invalid, for[" + beanClazz + "#" + method.getName() + "] .");
                                continue;
                            }
                        }

                        // registry jobhandler
                        log.debugf("xxl-job register method job %s:%s", name, method.getName());
                        registJobHandler(name, NutzBeanJobHandler.methodJob(ioc, method, beanClazz));
                    }
                }
            }
        }

    }

    /**
     * 单纯的处理下异常
     *
     * @param beanName
     * @return
     */
    private Class<?> getBeanClass(String beanName) {
        try {
            return ioc.getType(beanName);
        } catch (ObjectLoadException e) {
            log.warnf("find bean type error", e);
        }
        return null;
    }


}
