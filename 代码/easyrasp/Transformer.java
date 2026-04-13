package com.wangziyu666.easyrasp;

import com.wangziyu666.easyrasp.annotation.HookHandler;
import com.wangziyu666.easyrasp.annotation.HookHandlers;
import javassist.*;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.*;

public class Transformer implements ClassFileTransformer {//最重要的类
    private static Logger logger = LoggerFactory.getLogger(Transformer.class);
//用于在控制台生产日志
    private static Transformer transformer = new Transformer();
    private static HashMap<String, List<HookDesc>> handlers = getAllHandler();

    public static Transformer getInstance() {
        return transformer;
    }

    public static HashMap<String, List<HookDesc>> getAllHandler() {//利用现成的Reflections框架Reflections通过扫描classpath，索引元数据，并且允许在运行时查询这些元数据
        HashMap<String, List<HookDesc>> handlers = new HashMap<>();//根据名字来获取注解标签
        Reflections reflections = new Reflections("com.wangziyu666.easyrasp", new MethodAnnotationsScanner());
        Set<Method> methodsWithAnnotation = reflections.getMethodsAnnotatedWith(HookHandler.class);
        methodsWithAnnotation.addAll( reflections.getMethodsAnnotatedWith(HookHandlers.class));

        for (Method method : methodsWithAnnotation) {
            if (Modifier.isStatic(method.getModifiers())) {//识别是静态方法
                HookHandler[] hookHandlers;
                HookHandler tmp = method.getAnnotation(HookHandler.class);

                if (tmp != null) {
                    hookHandlers = new HookHandler[]{tmp};

                } else {
                    hookHandlers = method.getAnnotation(HookHandlers.class).value();
                    System.out.println("hookHandlers++" + hookHandlers.toString());

                }

                for (HookHandler hookHandler : hookHandlers) {
                    HookDesc hookDesc = HookDesc.fromHookHandler(hookHandler, method);
                    logger.info("Arm hook with " + hookDesc.handlerMethod.getName() + " in " + hookDesc.hookClassName + "." + hookDesc.hookMethodName);

                    List<HookDesc> hookDescs = handlers.get(hookDesc.hookClassName);
                    if (hookDescs == null) {
                        hookDescs = new ArrayList<>();
                        handlers.put(hookDesc.hookClassName, hookDescs);
                        //com.sun.org.apache.xerces.internal.impl.XMLEntityManager之类要拦截的类
                        System.out.println(hookDesc.hookClassName);
                    }
                    hookDescs.add(hookDesc);
                    //
                }

            } else {
                logger.info("Can't use " + method + " as it's not a static method");
            }
        }
       // System.out.println(handlers.get("com.sun.org.apache.xerces.internal.impl.XMLEntityManager").size());
        return handlers;
    }

    public static CtBehavior[] findMatchBehavior(CtBehavior[] ctBehaviors, HookDesc hookDesc) {
        if ("".equals(hookDesc.hookMethodDesc)) { // 空字符串表示匹配所有同名方法
            ArrayList<CtBehavior> result = new ArrayList<>();
            for (CtBehavior ctBehavior : ctBehaviors) {
                if (ctBehavior.getMethodInfo().getName().equals(hookDesc.hookMethodName)) {
                    result.add(ctBehavior);
                }
            }
            return result.toArray(new CtBehavior[0]);
        } else {
            for (CtBehavior ctBehavior : ctBehaviors) {
                if (ctBehavior.getMethodInfo().getName().equals(hookDesc.hookMethodName) &&
                        ctBehavior.getMethodInfo().getDescriptor().equals(hookDesc.hookMethodDesc)) {
                    return new CtBehavior[]{ctBehavior};
                }
            }
            logger.info(hookDesc.hookClassName + " with " + hookDesc.hookMethodName  + "." + hookDesc.hookMethodDesc + " don't find match function");
            return new CtBehavior[]{};
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        className = className.replace('/', '.');
       // System.out.println("test"+className);
        List<HookDesc> hookDescs = handlers.get(className);
        //

        if (hookDescs != null) {
            try {
                logger.info("Matched " + className);//运行的时候发现了有敏感类
                ClassPool classPool = ClassPool.getDefault();//开始操纵字节码
                classPool.appendClassPath(new LoaderClassPath(loader));
                //System.out.println("debugLoader" + loader.toString());
                CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));


                for (HookDesc hookDesc : hookDescs) {
                    logger.info("Install hook to " + hookDesc.hookMethodName + "." + hookDesc.hookMethodDesc);
                    CtBehavior[] ctBehaviors;
                    if ("<init>".equals(hookDesc.hookMethodName)) {
                        ctBehaviors = findMatchBehavior(ctClass.getDeclaredConstructors(), hookDesc);
                    } else {
                        ctBehaviors = findMatchBehavior(ctClass.getDeclaredMethods(), hookDesc);
                    }

                    for (CtBehavior ctBehavior : ctBehaviors) {//这里动态改变可能是恶意代码的地方，进行动态拦截
                        String funcBody = hookDesc.handlerMethod.getDeclaringClass().getName() + "." + hookDesc.handlerMethod.getName();

                        switch (hookDesc.hookType) {
                            case BEFORE_RUN:
                                if (!javassist.Modifier.isStatic(ctBehavior.getModifiers())) {
                                    funcBody = "{$args=" + funcBody + "($0,$args);}";//$0是this，$argsAn array of parameters. The type of $args is Object[].
                                    //logger.info("debug2");
                                } else {
                                    funcBody = "{$args=" + funcBody + "($args);}";

                                }

                                ctBehavior.insertBefore(funcBody); // 替换参数
                                break;
                            case AFTER_RUN:
                                funcBody = "$_=" + funcBody + "($_);"; // 替换返回值
                                //org.xml.sax.SAXException: External object file:///c:/windows/win.ini not allowed. SecurityException: External object file:///c:/windows/win.ini not allowed.
                                logger.info(funcBody);
                                ctBehavior.insertAfter(funcBody);
                                break;
                            case AFTER_RUN_FINALLY:
                                funcBody = funcBody + "($_);";
                                ctBehavior.insertAfter(funcBody, true);//是否在抛出异常的时候同样执行该源代码
                                break;
                        }
                        logger.info("Install hook success.");
                    }
                }
                return ctClass.toBytecode();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }
}
