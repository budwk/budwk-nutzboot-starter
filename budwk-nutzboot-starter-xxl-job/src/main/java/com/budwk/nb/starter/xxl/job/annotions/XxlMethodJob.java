package com.budwk.nb.starter.xxl.job.annotions;

import com.xxl.job.core.handler.annotation.XxlJob;

import java.lang.annotation.*;

/**
 * 带有该注解的类，其方法上如果有 @{@link XxlJob} 注解，那么这些方法作为一个任务执行器
 */
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface XxlMethodJob {
}
