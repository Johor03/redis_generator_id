package com.beefly.common;

import java.io.IOException;

import com.beefly.common.IdGenerator.LoadIdGeneratorConfig;

/**
* @ClassName: BuildIdFactory
* @Description: 全局ID生产工厂
* @author eonh
* @date 2017年12月26日 下午5:51:10
*
*/
public final class BuildIdFactory {
    
    /**
     * 订单表序列
     */
    private final static String TAB_ORDER = "order";

    private static IdGenerator idGenerator;
    private static BuildIdFactory instance;
    
    private BuildIdFactory() {
    }
    
    public static BuildIdFactory getInstance() {
        if(idGenerator == null) {
            try {
                idGenerator = LoadIdGeneratorConfig.loadConfig.buildIdGenerator();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return instance == null ? new BuildIdFactory() : instance;
    }
    
    public Long buildFactoryOrderId() {
        return idGenerator.next(TAB_ORDER);
    }
    
}
