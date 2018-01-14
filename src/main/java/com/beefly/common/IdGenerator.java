package com.beefly.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;
/**
* @ClassName: IdGenerator
* @Description: 全局ID生产配置加载
* @author eonh
* @date 2017年12月27日 上午11:00:58
*
*/
public final class IdGenerator {
    
    static final Logger logger = LoggerFactory.getLogger(IdGenerator.class);
    /**
     * JedisPool, luaSha
     */
    List<Pair<JedisPool, String>> jedisPoolList;
    int retryTimes;
    int index = 0;
    
    private IdGenerator() {
        
    }
    
    private IdGenerator(List<Pair<JedisPool, String>> jedisPoolList, int retryTimes) {
        this.jedisPoolList = jedisPoolList;
        this.retryTimes = retryTimes;
    }
    
    static public IdGeneratorBuilder builder() {
        return new IdGeneratorBuilder();
    }
    
    static class IdGeneratorBuilder {
        
        
        List<Pair<JedisPool, String>> jedisPoolList = new ArrayList<>();
        int retryTimes = 5;
        
        public IdGeneratorBuilder addHost(String host, int port, String pass, String luaSha) {
            JedisPoolConfig config = new JedisPoolConfig();
            //最大空闲连接数, 应用自己评估，不要超过ApsaraDB for Redis每个实例最大的连接数
            config.setMaxIdle(200);
            //最大连接数, 应用自己评估，不要超过ApsaraDB for Redis每个实例最大的连接数
            config.setMaxTotal(300);
            config.setTestOnBorrow(false);
            config.setTestOnReturn(false);
            config.setLifo(true);
            config.setMinIdle(30);
            jedisPoolList.add(Pair.of(new JedisPool(config, host, port, 1000, pass), luaSha));
            return this;
        }
        
        public IdGeneratorBuilder retryTimes(int retryTimes) {
            this.retryTimes = retryTimes;
            return this;
        }
        
        public IdGenerator build() {
            return new IdGenerator(jedisPoolList, retryTimes);
        }
    }
    
    public long next(String tab) {
        for (int i = 0; i < retryTimes; ++i) {
            Long id = innerNext(tab);
            if (id != null) {
                return id;
            }
        }
        throw new RuntimeException("Can not generate id!");
    }
    
    private Long innerNext(String tab) {
        Calendar cal = Calendar.getInstance();
        String year = String.valueOf(cal.get(Calendar.YEAR)).substring(2);
        String day = String.valueOf(cal.get(Calendar.DAY_OF_YEAR));
        if (index == jedisPoolList.size())
            index = 0;
        Pair<JedisPool, String> pair = jedisPoolList.get(index++ % jedisPoolList.size());
        JedisPool jedisPool = pair.getLeft();
        String luaSha = pair.getRight();
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            Long result = (Long)jedis.evalsha(luaSha, 3, tab, year, day);
            return result;
        } catch (JedisException e) {
            logger.error("generate id error!", e);
            return null;
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
    
    
    static class LoadIdGeneratorConfig {
        
        static List<RedisScriptConfig> scriptConf = new ArrayList<>();
        static LoadIdGeneratorConfig loadConfig = new LoadIdGeneratorConfig();
        static {
            Properties pro = new Properties();
            try {
                pro.load(LoadIdGeneratorConfig.class.getResourceAsStream("/cluster_redis_config.properties"));
                for (int i = 1; i <= 3; i++) {
                    String host = pro.getProperty("redis_cluster" + i + "_host");
                    String pass = pro.getProperty("redis_cluster" + i + "_pass");
                    if(StringUtils.isEmpty(host) || StringUtils.isEmpty(pass)) {
                        continue;
                    }
                    scriptConf.add(new RedisScriptConfig(host, Integer.valueOf(pro.getProperty("redis_cluster" + i + "_port", "6379")), pass));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        public IdGenerator buildIdGenerator()
            throws IOException {
            loadConfig.loadScript();
            IdGeneratorBuilder idGenerator = IdGenerator.builder();
            for (RedisScriptConfig conf : scriptConf) {
                idGenerator = idGenerator.addHost(conf.getHost(), conf.getPort(), conf.getPass(), conf.getScriptSha());
            }
            return idGenerator.build();
        }
        
        public void loadScript()
            throws IOException {
            int index = 1;
            for (RedisScriptConfig conf : scriptConf) {
                Jedis jedis = new Jedis(conf.getHost(), conf.getPort());
                jedis.auth(conf.getPass());
                InputStream is = LoadIdGeneratorConfig.class.getResourceAsStream("/script/redis-script-node" + index++ + ".lua");
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String readLine = null;
                StringBuilder sb = new StringBuilder();
                while ((readLine = br.readLine()) != null) {
                    sb.append(readLine);
                }
                br.close();
                is.close();
                conf.setScriptSha(jedis.scriptLoad(sb.toString()));
                jedis.close();
            }
        }
    }
    
    static class RedisScriptConfig {
        
        private String host;
        private Integer port;
        private String pass;
        private String scriptSha;
        
        public RedisScriptConfig(String host, Integer port, String pass) {
            super();
            this.host = host;
            this.port = port;
            this.pass = pass;
        }
        
        public void setScriptSha(String scriptSha) {
            this.scriptSha = scriptSha;
        }
        
        public String getHost() {
            return host;
        }
        
        public Integer getPort() {
            return port;
        }
        
        public String getPass() {
            return pass;
        }
        
        public String getScriptSha() {
            return scriptSha;
        }
        
    }
}