package com.beefly.common;

import java.util.HashSet;
import java.util.Set;

public class Test {
    public static void main(String[] args) {
        long current = System.currentTimeMillis();
        Set<Long> ids = new HashSet<Long>();
        for (int i = 0; i < 1000; i++) {
            Long id = BuildIdFactory.getInstance().buildFactoryOrderId();
            ids.add(id);
            System.out.println(id);
        }
        System.out.println( "ids >>>>>>" + ids.size());
        System.err.println(System.currentTimeMillis() - current);
    }
}
