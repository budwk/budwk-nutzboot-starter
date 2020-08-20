package com.budwk.nb.starter.swagger.annotation;

import java.lang.annotation.*;

/**
 * @author wizzer(wizzer.cn)
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ApiFormParams {
    ApiFormParam[] apiFormParams() default {};

    Class<?> implementation() default Void.class;

    String mediaType() default "application/x-www-form-urlencoded";
}
