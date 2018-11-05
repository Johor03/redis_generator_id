package com.zjunicode.common.generateid;

import java.io.IOException;

import com.zjunicode.common.generateid.IdGenerator.LoadIdGeneratorConfig;

/**
* @ClassName: BuildIdFactory
* @Description: 全局ID生产工厂,使用本类生成趋势有序的ID时候需要注意ID生成范围的容量与溢出情况
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
        return idGenerator.nextGenerator(applicationName+":"+tabName,12);
    }

    /**
     * 生成12为序列ID,返回Long型 length位长度序列
     * @param applicationName 应用服务名称,如:order_server
     * @param tabName 序列表名,如:t_order
     * @param length 序列位长度 ，仅支持8-19 位长度的序列，if（length > 19 || length < 8） 默认生成最低8位的序列
     * @return
     */
    public Long buildGeneratorIdByLength(final String applicationName,final String tabName,int length) {
        return idGenerator.nextGenerator(applicationName+":"+tabName,length);
    }
    
}