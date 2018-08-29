package com.zjunicode.common.generateid;

import java.io.IOException;

import com.zjunicode.common.generateid.IdGenerator.LoadIdGeneratorConfig;

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
    private final static String TAB_ORDER = "order";

    private final static String TAB_MERCHANT = "merchant";

    private final static String TAB_USER = "user";

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
    public Long buildFactoryMerchantId() {
        return idGenerator.next(TAB_MERCHANT);
    }
    public Long buildFactoryUserId() {
        return idGenerator.next(TAB_USER);
    }
    
}