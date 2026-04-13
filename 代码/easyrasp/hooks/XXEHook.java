package com.wangziyu666.easyrasp.hooks;

import com.wangziyu666.easyrasp.SecurityException;
import com.wangziyu666.easyrasp.annotation.HookHandler;
import com.wangziyu666.easyrasp.enums.HookType;

public class XXEHook {//检测到用这个类，和方法的时候把xxeHook方法注入.
    @HookHandler(hookClass = "com.sun.org.apache.xerces.internal.impl.XMLEntityManager", hookMethod = "expandSystemId", hookType = HookType.AFTER_RUN)
    public static String xxeHook(String ret) throws SecurityException {
        if (ret != null) {
            throw new SecurityException(String.format("External object %s not allowed.", ret));
        }
        return ret;
    }
}
