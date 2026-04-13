package com.wangziyu666.easyrasp;

import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) throws URISyntaxException {

        System.out.println("alert run with -javaagent:xxx.jar");//以代理方式运行
        Transformer transformer = Transformer.getInstance();
        //System.out.println(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());


    }
}
