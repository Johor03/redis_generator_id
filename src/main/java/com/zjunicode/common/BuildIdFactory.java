package com.zjunicode.common;

import java.io.IOException;

import com.zjunicode.common.IdGenerator.LoadIdGeneratorConfig;

/**
* @ClassName: BuildIdFactory
* @Description: 全局ID生产工厂
* @author guowei
*
*/
public final class BuildIdFactory {
    
    /**
     * 序列
     */
    private final static String TAB_ORDER = "user";

    private static volatile IdGenerator idGenerator;
    private static volatile BuildIdFactory instance;
    
    private BuildIdFactory() {
    }
    
    public static BuildIdFactory getInstance() {
        if(idGenerator == null) {
        	synchronized (LoadIdGeneratorConfig.class) {
        		try {
        			idGenerator = LoadIdGeneratorConfig.loadConfig.buildIdGenerator();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
			}
        }
        if(instance == null ) {
        	synchronized (BuildIdFactory.class) {
        		instance = new BuildIdFactory();
			}
        }
        return instance;
    }
    
    public Long buildFactoryOrderId() {
        return idGenerator.next(TAB_ORDER);
    }
    
}
