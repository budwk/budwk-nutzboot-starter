package com.budwk.nb.starter.xxl.job.core;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import org.nutz.ioc.Ioc;
import org.nutz.lang.Strings;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import java.lang.reflect.Method;

public class NutzBeanJobHandler extends IJobHandler {

    private static final Log log = Logs.get();
    /**
     * ioc 容器
     */
    private Ioc ioc;
    /**
     * bean 类型
     */
    private Class<?> beanClazz;
    /**
     * bean 名称
     */
    private String beanName;

    private Method executeMethod;

    private int errorCount;

    private Object instance = null;

    private Type type;

    public static NutzBeanJobHandler beanJob(Ioc ioc, String beanName, Class<?> beanClazz) {
        return new NutzBeanJobHandler(ioc, beanName, beanClazz);
    }

    public static NutzBeanJobHandler methodJob(Ioc ioc, Method method, Class<?> beanClazz) {
        return new NutzBeanJobHandler(ioc, method, beanClazz);
    }

    private NutzBeanJobHandler(Ioc ioc, String beanName, Class<?> beanClazz) {
        if (ioc == null || Strings.isBlank(beanName)) {
            throw new IllegalArgumentException("ioc and beanName must not be null");
        }
        this.type = Type.Bean;
        this.ioc = ioc;
        this.beanName = beanName;
        this.beanClazz = beanClazz;
    }

    private NutzBeanJobHandler(Ioc ioc, Method method, Class<?> beanClazz) {
        if (ioc == null || method == null || beanClazz == null) {
            throw new IllegalArgumentException("ioc and method and beanClazz must not be null");
        }
        this.type = Type.Method;
        this.ioc = ioc;
        this.executeMethod = method;
        this.beanClazz = beanClazz;
    }

    @Override
    public ReturnT<String> execute(String jobParams) throws Exception {
        Object instance = getInstance();
        if (null == instance) {
            return new ReturnT<>(500, "加载执行处理器失败，失败计数：" + errorCount);
        }
        switch (type) {
            case Bean:
                if (instance instanceof IJobHandler) {
                    return ((IJobHandler) instance).execute(jobParams);
                } else {
                    return ReturnT.FAIL;
                }
            case Method:
                Object invoke = this.executeMethod.invoke(instance, jobParams);
                return new ReturnT<>(Strings.safeToString(invoke, null));
        }
        return ReturnT.FAIL;
    }

    private Object getInstance() {
        try {
            if (null == instance) {
                switch (type) {
                    case Method:
                        instance = ioc.get(beanClazz);
                        break;
                    case Bean:
                        instance = ioc.get(IJobHandler.class, beanName);
                        break;
                }
            }
            return instance;
        } catch (Exception e) {
            errorCount++;
            log.warnf("ioc get bean %s (type %s) failed", beanName, beanClazz);
        }
        return null;
    }

    enum Type {
        Bean,
        Method
    }
}
