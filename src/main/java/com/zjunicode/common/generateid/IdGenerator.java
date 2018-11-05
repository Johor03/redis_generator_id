package com.zjunicode.common.generateid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;
/**
* @ClassName: IdGenerator
* @Description: 全局ID生产配置加载
* @author guowei
*
*/
public final class IdGenerator {
    
    static final Logger logger = LoggerFactory.getLogger(IdGenerator.class);
    /**
     * JedisPool, luaSha
     */
    List<Pair<JedisPool, String>> jedisPoolList;
    List<Pair<JedisPool,Integer>> jedisPoolIndex;
    int retryTimes;
    volatile int index = 0;
    private IdGenerator() {
        
    }
    
    private IdGenerator(List<Pair<JedisPool, String>> jedisPoolList,List<Pair<JedisPool,Integer>> jedisPoolIndex, int retryTimes) {
        this.jedisPoolList = jedisPoolList;
        this.retryTimes = retryTimes;
        this.jedisPoolIndex = jedisPoolIndex;
    }
    
    static public IdGeneratorBuilder builder() {
        return new IdGeneratorBuilder();
    }
    
    static class IdGeneratorBuilder {
        
        
        List<Pair<JedisPool, String>> jedisPoolList = new ArrayList<>();
        List<Pair<JedisPool, Integer>> jedisPoolIndex = new ArrayList<>();
        int retryTimes = 5;
        
        public IdGeneratorBuilder addHost(String host, int port, String pass, String luaSha,Integer redisIncr) {
            JedisPoolConfig config = new JedisPoolConfig();
            //最大空闲连接数, 应用自己评估，不要超过ApsaraDB for Redis每个实例最大的连接数
            config.setMaxIdle(50);
            //最大连接数, 应用自己评估，不要超过ApsaraDB for Redis每个实例最大的连接数
            config.setMaxTotal(200);
            config.setTestOnBorrow(false);
            config.setTestOnReturn(false);
            config.setLifo(true);
            config.setMinIdle(30);
            jedisPoolList.add(Pair.of(StringUtils.isEmpty(pass) ? new JedisPool(config, host, port, 1000) : new JedisPool(config, host, port, 1000, pass), luaSha));
            jedisPoolIndex.add(Pair.of(StringUtils.isEmpty(pass) ? new JedisPool(config, host, port, 1000) : new JedisPool(config, host, port, 1000, pass), redisIncr));
            return this;
        }
        
        public IdGeneratorBuilder retryTimes(int retryTimes) {
            this.retryTimes = retryTimes;
            return this;
        }
        
        public IdGenerator build() {
            return new IdGenerator(jedisPoolList,jedisPoolIndex, retryTimes);
        }
    }

    public long nextGenerator(String tab,int length) {
        long initStart = 10000000L;
        if (length > 8 && length <= 19) {
            initStart = Double.valueOf(Math.pow(10,length - 1)).longValue();
        }
        for (int i = 0; i < retryTimes; ++i) {
            Long id = innerNextIncr(tab);
            if (id != null) {
                return initStart + id;
            }
        }
        throw new RuntimeException("Can not generate id!");
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
            Long result = Long.valueOf(jedis.evalsha(luaSha, 3, tab, year, day).toString());
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

    private Long innerNextIncr(String tab) {
        if (index == jedisPoolIndex.size())
            index = 0;
        Pair<JedisPool, Integer> pair = jedisPoolIndex.get(index++ % jedisPoolList.size());
        JedisPool jedisPool = pair.getLeft();
        Integer generatorIndex = pair.getRight();
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            if (jedis.get("_generator_id_length_12:"+tab) == null) {
                jedis.set("_generator_id_length_12:"+tab,generatorIndex.toString());
                return Long.valueOf(generatorIndex.toString());
            }
            return jedis.incrBy("_generator_id_length_12:"+tab, 3);
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
            Properties pro_default = new Properties();
            try {
                pro_default.load(LoadIdGeneratorConfig.class.getResourceAsStream("/cluster_redis_config.default.properties"));
                try {
                    pro.load(LoadIdGeneratorConfig.class.getResourceAsStream("/cluster_redis_config.properties"));
                    pro_default.clear();
                }catch(NullPointerException e) {
                    logger.warn("cluster_redis_config.properties not exist....");
                }
                for (int i = 1; i <= 3; i++) {
                    String hostKey = "redis_cluster" + i + "_host";
                    String passKey = "redis_cluster" + i + "_pass";
                    String host = StringUtils.isEmpty(pro.getProperty(hostKey)) ? pro_default.getProperty(hostKey) : pro.getProperty(hostKey);
                    String pass = StringUtils.isEmpty(pro.getProperty(passKey)) ? pro_default.getProperty(passKey) : pro.getProperty(passKey);
                    if(StringUtils.isEmpty(host)) {
                        continue;
                    }
                    String portKey = "redis_cluster" + i + "_port";
                    String redisInitIncrKey = "redis_cluster" + i + "_incr";
                    String port = StringUtils.isEmpty(pro.getProperty(portKey)) ? pro_default.getProperty(portKey, "6379") : pro.getProperty(portKey, "6379");
                    String redisInitIncr = StringUtils.isEmpty(pro.getProperty(redisInitIncrKey)) ? pro_default.getProperty(redisInitIncrKey) : pro.getProperty(redisInitIncrKey);
                    scriptConf.add(new RedisScriptConfig(host, Integer.valueOf(port), pass,Integer.valueOf(redisInitIncr)));
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
                idGenerator = idGenerator.addHost(conf.getHost(), conf.getPort(), conf.getPass(), conf.getScriptSha(),conf.redisIncr);
            }
            return idGenerator.build();
        }
        
        public void loadScript()
            throws IOException {
            int index = 1;
            for (RedisScriptConfig conf : scriptConf) {
                Jedis jedis = new Jedis(conf.getHost(), conf.getPort());
                if(!StringUtils.isEmpty(conf.getPass())) {
                	jedis.auth(conf.getPass());
                }
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
        private Integer redisIncr;
        
        public RedisScriptConfig(String host, Integer port, String pass,Integer redisIncr) {
            super();
            this.host = host;
            this.port = port;
            this.pass = pass;
            this.redisIncr = redisIncr;
        }

        public void setRedisIncr(Integer redisIncr) {
            this.redisIncr = redisIncr;
        }

        public Integer getRedisIncr() {
            return redisIncr;
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