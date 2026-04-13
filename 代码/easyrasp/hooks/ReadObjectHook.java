package com.wangziyu666.easyrasp.hooks;

import com.wangziyu666.easyrasp.SecurityException;
import com.wangziyu666.easyrasp.annotation.HookHandler;
import com.wangziyu666.easyrasp.enums.HookType;

import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.HashSet;

public class ReadObjectHook {//
    private static HashSet<String> forbidList = new HashSet<String>(
            Arrays.asList(//重点关注的包
                    "org.apache.commons.collections.functors.InvokerTransformer",
                    "org.apache.commons.collections4.functors.InvokerTransformer",
                    "org.apache.commons.collections.functors.InstantiateTransformer",
                    "org.apache.commons.collections4.functors.InstantiateTransformer"
            )
    );

    @HookHandler(hookClass = "java.io.ObjectInputStream", hookMethod = "readClassDesc", hookType = HookType.AFTER_RUN)
    public static ObjectStreamClass readObjectCheck(ObjectStreamClass ret) throws Exception {
        if (ret != null) {
            if (forbidList.contains(ret.getName())) {
                throw new SecurityException(String.format("Class %s deserialize not allow", ret.getName()));
            }
        }
        return ret;
    }
}
