package com.wangziyu666.easyrasp.hooks;

import com.wangziyu666.easyrasp.annotation.HookHandler;
import com.wangziyu666.easyrasp.enums.HookType;

public class RequestHook {//这个测试不是很成功
    private static ThreadLocal<Object> currRequest = new ThreadLocal<>();

    @HookHandler(hookClass = "org.springframework.web.servlet.FrameworkServlet", hookMethod = "service")
    public static Object[] requestHook(Object self, Object[] params) {
        currRequest.set(params[0]);
        return params;
    }

    @HookHandler(hookClass = "org.springframework.web.servlet.FrameworkServlet", hookMethod = "service", hookType = HookType.AFTER_RUN_FINALLY)
    public static void releaseRequest(Object ignored) {
        currRequest.remove(); // 释放内存
    }

    public static Object getRequest() {
        return currRequest.get();
    }
}
