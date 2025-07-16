package com.he.common;

import com.he.util.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ConfigParam {
    private static ConfigParam instance;

    private ConfigParam() {
    }

    public static ConfigParam getInstance() {
        if (instance == null) {
            synchronized (ConfigParam.class) {
                if (instance == null) {
                    instance = new ConfigParam();
                }
            }
        }
        return instance;
    }

    private static String driverClassName;
    private static String username;
    private static String password;
    private static String url;
    private static String initialSize = "5";
    private static String maxActive = "20";
    private static String maxWait = "60000";
    private static String timeBetweenEvictionRunsMillis = "3000";
    private static String minEvictableIdleTimeMillis = "300000";
    private static String removeAbandoned = "true";
    private static String removeAbandonedTimeout = "180";
    private static String logAbandoned = "false";
    private static String testWhileIdle = "true";
    private static String testOnBorrow = "false";
    private static String testOnReturn = "false";
    private static String poolPreparedStatements = "true";
    private static String maxPoolPreparedStatementPerConnectionSize = "20";
    private static String validationQuery = "select 1";
    private static String scanPoPackage;
    private static String excludeFieldString = "transid,datetime,pageNum";

    private static boolean printSql = false;
    private static boolean userDataSourcePool = false;
    //德鲁伊配置
    private static Map druidConfigMap = new HashMap();

    public Map getDruidConfigMap() {
        return druidConfigMap;
    }

    /**
     * 可继承重写，增加额外加载配置
     */
    private Map<String, Object> getApplicationMap() throws IOException {
        Map<String, Object> map = YamlUtils.yamlHandler("application.yml");
        String suffix = map.get("spring.profiles.active").toString();
        log.info("获取yml文件{}","application-" + suffix + ".yml");
        return YamlUtils.yamlHandler("application-" + suffix + ".yml");
    }

    /**
     * 可继承重写，增加额外加载配置
     */
    public void initConfigInfo() throws IOException {
        try {
            Map<String, Object> appMap = getApplicationMap();
            log.info("获取yml文件配置信息:{}",appMap);
            driverClassName = appMap.get("spring.datasource.driver-class-name").toString();
            log.info("noSql使用mysql驱动:{}",driverClassName);
            username = appMap.get("spring.datasource.username").toString();
            log.info("noSql连接池用户名:{}",username);
            password = appMap.get("spring.datasource.password").toString();
            url = appMap.get("spring.datasource.url").toString();
            log.info("noSql连接池url:{}",url);

            //连接池配置
            if(appMap.get("noSql.pool.initialSize") != null ) initialSize = appMap.get("noSql.pool.initialSize").toString();
            if(appMap.get("noSql.pool.maxActive") != null ) maxActive = appMap.get("noSql.pool.maxActive").toString();
            if(appMap.get("noSql.pool.maxWait") != null ) maxWait = appMap.get("noSql.pool.maxWait").toString();
            if(appMap.get("noSql.pool.timeBetweenEvictionRunsMillis") != null ) timeBetweenEvictionRunsMillis = appMap.get("noSql.pool.timeBetweenEvictionRunsMillis").toString();
            if(appMap.get("noSql.pool.minEvictableIdleTimeMillis") != null ) minEvictableIdleTimeMillis = appMap.get("noSql.pool.minEvictableIdleTimeMillis").toString();
            if(appMap.get("noSql.pool.removeAbandoned") != null ) removeAbandoned = appMap.get("noSql.pool.removeAbandoned").toString();
            if(appMap.get("noSql.pool.removeAbandonedTimeout") != null ) removeAbandonedTimeout = appMap.get("noSql.pool.removeAbandonedTimeout").toString();
            if(appMap.get("noSql.pool.logAbandoned") != null ) logAbandoned = appMap.get("noSql.pool.logAbandoned").toString();
            if(appMap.get("noSql.pool.testWhileIdle") != null ) testWhileIdle = appMap.get("noSql.pool.testWhileIdle").toString();
            if(appMap.get("noSql.pool.testOnBorrow") != null ) testOnBorrow = appMap.get("noSql.pool.testOnBorrow").toString();
            if(appMap.get("noSql.pool.testOnReturn") != null ) testOnReturn = appMap.get("noSql.pool.testOnReturn").toString();
            if(appMap.get("noSql.pool.poolPreparedStatements") != null ) poolPreparedStatements = appMap.get("noSql.pool.poolPreparedStatements").toString();
            if(appMap.get("noSql.pool.maxPoolPreparedStatementPerConnectionSize") != null ) maxPoolPreparedStatementPerConnectionSize = appMap.get("noSql.pool.maxPoolPreparedStatementPerConnectionSize").toString();
            if(appMap.get("noSql.pool.validationQuery") != null ) validationQuery = appMap.get("noSql.pool.validationQuery").toString();

            scanPoPackage = appMap.get("noSql.scan.poPackage").toString();
            log.info("noSql映射实体类包:{}",scanPoPackage);

            if(appMap.get("noSql.select.excludeField") != null)excludeFieldString = appMap.get("noSql.select.excludeField").toString();
            log.info("不参与查询的字段设置：{}",excludeFieldString);

            if(appMap.get("noSql.printSql") != null)printSql = Boolean.parseBoolean(appMap.get("noSql.printSql").toString());
            if(printSql)log.info("打印SQl日志");
            else log.info("不打印SQL日志");


            if(appMap.get("noSql.userDataSourcePool") != null)userDataSourcePool = Boolean.parseBoolean(appMap.get("noSql.userDataSourcePool").toString());
            if(userDataSourcePool)log.info("使用数据源连接池");
            else log.info("不使用数据源连接池");

        } catch (IOException e) {
            log.error("读取yaml文件错误");
            throw e;
        }
    }
    /**
     * 可重写，改变连接池配置项
     */
    public void InitDruidPoolInfo() {
        druidConfigMap.put("driverClassName",driverClassName);
        druidConfigMap.put("url",url);
        druidConfigMap.put("username",username);
        druidConfigMap.put("password",password);
        druidConfigMap.put("initialSize",initialSize);
        druidConfigMap.put("maxActive",maxActive);
        druidConfigMap.put("maxWait",maxWait);
        druidConfigMap.put("timeBetweenEvictionRunsMillis",timeBetweenEvictionRunsMillis);
        druidConfigMap.put("minEvictableIdleTimeMillis",minEvictableIdleTimeMillis);
        druidConfigMap.put("removeAbandoned",removeAbandoned);
        druidConfigMap.put("removeAbandonedTimeout",removeAbandonedTimeout);
        druidConfigMap.put("logAbandoned",logAbandoned);
        druidConfigMap.put("testWhileIdle",testWhileIdle);
        druidConfigMap.put("testOnBorrow",testOnBorrow);
        druidConfigMap.put("testOnReturn",testOnReturn);
        druidConfigMap.put("poolPreparedStatements",poolPreparedStatements);
        druidConfigMap.put("maxPoolPreparedStatementPerConnectionSize",maxPoolPreparedStatementPerConnectionSize);
        druidConfigMap.put("validationQuery",validationQuery);
    }

    public boolean isPrintSql (){
        return printSql;
    }

    public boolean isUserDataSourcePool (){
        return userDataSourcePool;
    }

    public String getUrl() {
        return url;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getScanPoPackage() {
        return scanPoPackage;
    }

    public String getExcludeFieldString() {
        return excludeFieldString;
    }
}
