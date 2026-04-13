package com.wangziyu666.easyrasp.annotation;

import com.wangziyu666.easyrasp.enums.HookType;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Repeatable(HookHandlers.class)//由于这个所以才有hookhandlers，为了让这个注解可以被多次引用。
public @interface HookHandler {
    String hookClass();
    String hookMethod();
    String methodDesc() default "" ;
    HookType hookType() default HookType.BEFORE_RUN;
}
