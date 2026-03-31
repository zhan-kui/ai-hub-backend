package com.aihub.annotation;

import com.aihub.common.enums.ResourceTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireResource {

    ResourceTypeEnum type();

    String paramName() default "id";
}