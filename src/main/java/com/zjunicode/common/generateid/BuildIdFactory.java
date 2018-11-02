package com.zjunicode.common.generateid;

import java.io.IOException;

import com.zjunicode.common.generateid.IdGenerator.LoadIdGeneratorConfig;

/**
* @ClassName: BuildIdFactory
* @Description: 全局ID生产工厂
* @author stone
*
*/
public final class BuildIdFactory {

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

    /**
     * 生成17位序列ID,返回Long型 12位长度序列
     * @param applicationName 应用服务名称,如:order_server
     * @param tabName 序列表名,如:t_order
     * @return
     */
    public Long buildFactoryGeneratorId17(final String applicationName,final String tabName) {
        return idGenerator.next(applicationName+":"+tabName);
    }

    /**
     * 生成12为序列ID,返回Long型 17位长度序列
     * @param applicationName 应用服务名称,如:order_server
     * @param tabName 序列表名,如:t_order
     * @return
     */
    public Long buildGeneratorId12(final String applicationName,final String tabName) {
        return idGenerator.nextGenerator12(applicationName+":"+tabName);
    }
    
}