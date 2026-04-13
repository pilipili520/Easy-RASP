package com.wangziyu666.easyrasp.hooks;

import com.wangziyu666.easyrasp.SecurityException;
import com.wangziyu666.easyrasp.annotation.HookHandler;
import com.wangziyu666.easyrasp.enums.HookType;

public class sqlHook {//检测到用这个类，和方法的时候把sqlHook方法注入，这里简单实现的
    @HookHandler(hookClass = "com.hellokoding.springboot.Test", hookMethod = "test", hookType = HookType.BEFORE_RUN)
    public static String sqlHook(String ret) throws SecurityException {
        if (ret != null) {
            throw new SecurityException(String.format("sqlQuery %s not allowed.", ret));
        }
        return ret;
    }
}
