package com.he.service;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.he.common.annotation.sql.*;
import com.he.common.comment.Comment;
import com.he.common.function.ClassFunction;
import com.he.common.function.FieldFunction;
import com.he.common.vo.BaseForm;
import com.he.entity.Carrier;
import com.he.util.StringUtil;
import com.he.util.YamlUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * auth 贺宁
 * 目的：为了减少sql语句开发，以及后续可能的表结构变动带来的高昂维护成本
 * @param <T>
 */
@Component
@Slf4j
@Setter
@Comment("查询服务")
public class NoSqlService<T> implements InitializingBean {

    private static String driverClassName;
    private static String username;
    private static String password;
    private static String url;
    private static String initialSize = "5";
    private static String maxActive = "20";
    private static String maxWait = "10000";
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

    private static DataSource dataSource ;

    private static Map<Class<?>,StringBuilder> queryInitSqlMap = new HashMap<>();      //查询字段
    private static Map<Class<?>,StringBuilder> queryInitOrderMap = new HashMap<>();    //查询排序
    private static Map<Class<?>,String> queryTable = new HashMap<>();                  //查询表名
    private static Map<Class<?>,String> queryAlias = new HashMap<>();                  //表名别名
    private static Map<Class<?>,StringBuilder> queryInitAliasSqlMap = new HashMap<>(); //带表别名的查询字段
    private static Map<Class<?>,StringBuilder> aliasFieldInitMap = new HashMap<>();    //带表别名的列名
    private static Set<String> excludeField = new HashSet<>(Arrays.asList("transid","datetime","pageNum"));  //这些字段不参与查询字段

    private static boolean printSql = false;

    /**
     * 可继承重写，增加额外加载配置
     */
    protected Map<String, Object> getApplicationMap() throws IOException {
        Map<String, Object> map = YamlUtils.yamlHandler("application.yml");
        String suffix = map.get("spring.profiles.active").toString();
        log.info("获取yml文件{}","application-" + suffix + ".yml");
        return YamlUtils.yamlHandler("application-" + suffix + ".yml");
    }

    /**
     * 可继承重写，增加额外加载配置
     */
    protected void initConfigInfo() throws IOException {
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

        } catch (IOException e) {
            log.error("读取yaml文件错误");
            throw e;
        }
    }
    /**
     * 可重写，改变连接池配置项
     */
    protected void InitDruidPool() throws Exception {
        try {
            //德鲁伊配置
            Map configMap = new HashMap();
            configMap.put("driverClassName",driverClassName);
            configMap.put("url",url);
            configMap.put("username",username);
            configMap.put("password",password);
            configMap.put("initialSize",initialSize);
            configMap.put("maxActive",maxActive);
            configMap.put("maxWait",maxWait);
            configMap.put("timeBetweenEvictionRunsMillis",timeBetweenEvictionRunsMillis);
            configMap.put("minEvictableIdleTimeMillis",minEvictableIdleTimeMillis);
            configMap.put("removeAbandoned",removeAbandoned);
            configMap.put("removeAbandonedTimeout",removeAbandonedTimeout);
            configMap.put("logAbandoned",logAbandoned);
            configMap.put("testWhileIdle",testWhileIdle);
            configMap.put("testOnBorrow",testOnBorrow);
            configMap.put("testOnReturn",testOnReturn);
            configMap.put("poolPreparedStatements",poolPreparedStatements);
            configMap.put("maxPoolPreparedStatementPerConnectionSize",maxPoolPreparedStatementPerConnectionSize);
            configMap.put("validationQuery",validationQuery);
            dataSource = DruidDataSourceFactory.createDataSource(configMap);
            log.info("数据库连接池加载完毕");
        }  catch (Exception e) {
            log.error("初始化数据库连接池错误",e);
            throw e;
        }
    }

    @Comment("查询单个实体，参数内置")
    final public <T> T query(T t) throws Exception {
        List<T> list = queryList(t);
        if(list == null || list.size() <= 0 )return null;
        else if(list.size() > 1){
            log.error("需要一个查询结果，但获得两个");
            throw new Exception();
        }else {
            return list.get(0);
        }
    }

    @Comment("查询单个实体，参数另传")
    final public <T> T query(Class clz,T t) throws Exception {
        List<T> list = queryList(clz,t);
        if(list == null || list.size() <= 0 )return null;
        else if(list.size() > 1){
            log.error("需要一个查询结果，但获得两个");
            throw new Exception();
        }else {
            return list.get(0);
        }
    }

    @Comment("查询列表，无参数")
    final public <T> List<T> queryList(Class<T> clt) throws Exception {
        try {
            return queryList(clt.newInstance());
        } catch (Exception e) {
            log.error("查询结果列表失败");
            throw e;
        }
    }

    @Comment("查询列表，参数内置")
    final public <T> List<T> queryList(T tp) throws Exception {
        Class clz =  tp.getClass();
        return queryList(clz,tp);
    }

    @Comment("查询列表，参数另传")
    final public <T> List<T> queryList(Class clz , T tp) throws Exception {
        Field[] fields = tp.getClass().getDeclaredFields();
        StringBuilder selectSql = new StringBuilder("select " + queryInitSqlMap.get(clz)).append("from").append(" ").append(queryTable.get(clz));
        boolean whetherWhere = true;
        List<Object> params = new ArrayList<>();
        for(Field field : fields){
            try {
                if(!field.isAnnotationPresent(IgnoreSql.class)) {
                    field.setAccessible(true);
                    if (field.get(tp) != null && !"".equals(field.get(tp))) {
                        if (whetherWhere) selectSql.append(" ").append("where").append(" ");
                        else selectSql.append(" ").append("and").append(" ");

                        appendSqlWhere(selectSql,field,"");

                        params.add( getFieldValue(field,tp) );
                        whetherWhere = false;
                    }
                }
            } catch (IllegalAccessException e) {
                log.error( "获取sql参数出错" );
                log.error(e.getMessage(),e);
                throw e;
            }
        }
        selectSql.append(" ").append(queryInitOrderMap.get(clz) == null?"": " order by " + queryInitOrderMap.get(clz));
        return query(selectSql,params,clz);
    }

    @Comment("分页查询，返回总数count和当前页list，参数另传")
    final public <T,P> Map<String,Object> queryForm(Class<T> clz, P p) throws Exception {
        //查询字段
        StringBuilder selectSql = new StringBuilder("select " + queryInitSqlMap.get(clz)).append("from").append(" ").append(queryTable.get(clz));
        //查询数量
        StringBuilder selectCount = new StringBuilder("select count(*) count ").append("from").append(" ").append(queryTable.get(clz));
        //查询条件
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        Map<String,Integer> limitMap = new HashMap<>();
        //根据 rv 的内容对 whereSql,params,limitMap 三个参数做处理
        matchingParams(p,whereSql,params,limitMap);

        selectCount.append(" ").append(whereSql);
        Integer count = queryCount(selectCount,params).get(0);

        StringBuilder orderBuilder = new StringBuilder( queryInitOrderMap.get(clz) == null?"": queryInitOrderMap.get(clz) );
        fitSelectSqlOrderByAndLimit(selectSql,whereSql,limitMap,orderBuilder,params);

        List<T> list = query(selectSql,params,clz);
        Map<String,Object> map = new HashMap<>();
        map.put("list",list);
        map.put("count",count);
        return map;
    }

    @Comment("分页查询，返回当前页，参数另传")
    final public <T,P> List<T> queryFormList(Class<T> clz, P p) throws Exception {
        //查询字段
        StringBuilder selectSql = new StringBuilder("select " + queryInitSqlMap.get(clz)).append("from").append(" ").append(queryTable.get(clz));
        //查询条件
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        Map<String,Integer> limitMap = new HashMap<>();
        //根据 rv 的内容对 whereSql,params,limitMap 三个参数做处理
        matchingParams(p,whereSql,params,limitMap);

        StringBuilder orderBuilder = new StringBuilder( queryInitOrderMap.get(clz) == null?"": queryInitOrderMap.get(clz) );
        fitSelectSqlOrderByAndLimit(selectSql,whereSql,limitMap,orderBuilder,params);
        return query(selectSql,params,clz);
    }

    @Comment("分页查询，返回总数count和当前页list，参数内置")
    final public <T> Map<String,Object> queryForm(T t) throws Exception {
        return queryForm(t.getClass(),t);
    }

    @Comment("分页查询，返回当前页，参数内置")
    final public <T> List<T> queryFormList(T t) throws Exception {
        return (List<T>) queryFormList(t.getClass(),t);
    }

    @Comment("分页总数，参数另传")
    final public <T> Integer queryFormCount(Class<T> clz, BaseForm rv) throws Exception {
        //查询数量
        StringBuilder selectCount = new StringBuilder("select count(1) count ").append("from").append(" ").append(queryTable.get(clz));
        //查询条件
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        Map<String,Integer> limitMap = new HashMap<>();
        //根据 rv 的内容对 whereSql,params,limitMap 三个参数做处理
        matchingParams(rv,whereSql,params,limitMap);

        selectCount.append(" ").append(whereSql);

        return queryCount(selectCount,params).get(0);
    }

    private List<Integer> queryCount(StringBuilder selectSql,List<Object> params) throws SQLException {
        List<Integer> list = new ArrayList<>();
        if(printSql)log.info("SQL执行语句为：{}", selectSql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statment = connection.prepareStatement(selectSql.toString());){
            for (int i = 0; i < params.size(); i++) {
                statment.setObject((i + 1), params.get(i));
            }
            if(printSql)log.info("SQL执行参数为：{}", String.join(",", params.stream().map(Object::toString).collect(Collectors.toList())));
            try(ResultSet rs = statment.executeQuery()){
                if(rs == null)return null;
                while (rs.next()) {
                    list.add(rs.getInt("count"));
                }
            }catch (SQLException e ){
                log.error("解析查询结果出错");
                throw e;
            }

        } catch (SQLException e) {
            log.error( "执行sql出错：{}",selectSql );
            throw e;
        }

        return list;
    }

    @Comment("联表查询，参数另传")
    final public <P> List<Map<String,Object>> queryList(Class<?> clz,P p, Carrier... carriers) throws Exception {
        StringBuilder selectSql = new StringBuilder("select ");
        StringBuilder selectField = new StringBuilder();
        StringBuilder fromSql = new StringBuilder();
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        StringBuilder orderSql = new StringBuilder();

        fitFieldAndFrom(selectSql,selectField,fromSql,orderSql,params,clz,p,carriers);

        Map<String,Integer> limitMap = new HashMap<>();
        //根据 p 的内容对 whereSql,params,limitMap 三个参数做处理
        matchingParams(p,whereSql,params,limitMap);

        fitSelectSqlOrderByAndLimit(selectSql,whereSql,limitMap,orderSql,params);

        return query(selectSql,params,clz,selectField);
    }

    @Comment("联表分页查询，参数另传")
    final public <P> Map<String,Object> queryForm(Class<?> clz,P p, Carrier... carriers) throws Exception {
        StringBuilder selectSql = new StringBuilder("select ");
        StringBuilder selectCount = new StringBuilder("select count(1) count ");

        StringBuilder selectField = new StringBuilder();
        StringBuilder fromSql = new StringBuilder();
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        StringBuilder orderSql = new StringBuilder();

        fitFieldAndFrom(selectSql,selectField,fromSql,whereSql,orderSql,params,clz,p,carriers);

        Map<String,Integer> limitMap = new HashMap<>();
        //根据 p 的内容对 whereSql,params,limitMap 三个参数做处理
        matchingParams(p,whereSql,params,limitMap);

        selectCount.append(" ").append(fromSql).append(" ").append(whereSql);
        Integer count = queryCount(selectCount,params).get(0);

        fitSelectSqlOrderByAndLimit(selectSql,whereSql,limitMap,orderSql,params);

        List<Map<String,Object>> list = query(selectSql,params,clz,selectField);
        Map<String,Object> map = new HashMap<>();
        map.put("list",list);
        map.put("count",count);
        return map;
    }

    private <P> void fitFieldAndFrom(StringBuilder selectSql,StringBuilder selectField,StringBuilder fromSql,StringBuilder orderSql,List<Object> params, Class<?> clz,P p, Carrier[] carriers ) throws IllegalAccessException {
        fitFieldAndFrom(selectSql,selectField,fromSql,new StringBuilder(),orderSql,params,clz,p,carriers);
    }

    private <P> void fitFieldAndFrom(StringBuilder selectSql,StringBuilder selectField,StringBuilder fromSql,StringBuilder whereSql,StringBuilder orderSql,List<Object> params, Class<?> clz,P p, Carrier[] carriers ) throws IllegalAccessException {
        //查询字段
        StringBuilder selectSqlField = new StringBuilder();
        selectSqlField.append(queryInitAliasSqlMap.get(clz));
        fromSql.append("from").append(" ").append(queryTable.get(clz));
        selectField.append(aliasFieldInitMap.get(clz));
        //StringBuilder orderBuilder = new StringBuilder(  queryInitOrderMap.get(clz) == null?"": queryInitOrderMap.get(clz) );
        orderSql = new StringBuilder(  queryInitOrderMap.get(clz) == null?"": queryInitOrderMap.get(clz) );
        for(int i = 0;i < carriers.length; i++){
            selectSqlField.append(",").append(queryInitAliasSqlMap.get( carriers[i].getRightTable() ));
            selectField.append(",").append(aliasFieldInitMap.get( carriers[i].getRightTable() ));
            fromSql.append(" left join ").append( queryTable.get(carriers[i].getRightTable()) )
                    .append(" on ").append( queryAlias.get( carriers[i].getLeftTable())).append(".").append(carriers[i].getLeftKey())
                    .append(" = ").append( queryAlias.get( carriers[i].getRightTable()) ).append(".").append(carriers[i].getRightKey());
            if( queryInitOrderMap.get(carriers[i].getRightTable())!=null ) {
                if( !StringUtil.isEmpty(orderSql.toString() ) ) orderSql.append(",");
                orderSql.append( queryInitOrderMap.get(carriers[i].getRightTable()) );
            }
        }
        selectSql.append(selectSqlField).append(" ").append(fromSql);
        //Map<String,Integer> limitMap = new HashMap<>();
        //根据 p 的内容对 whereSql,params,limitMap 三个参数做处理
        //matchingParams(p,whereSql,params,limitMap);
        //fitSelectSqlOrderByAndLimit(selectSql,whereSql,limitMap,orderBuilder,params);
    }

    private void fitSelectSqlOrderByAndLimit(StringBuilder selectSql,StringBuilder whereSql,Map<String,Integer> limitMap,StringBuilder orderBuilder,List<Object> params){
        selectSql.append(whereSql).append( StringUtil.isEmpty( orderBuilder.toString())?"":" order by " + orderBuilder );
        if(limitMap.get("limit") != null){
            selectSql.append(" ").append("limit").append(" ");
            if (limitMap.get("start") != null) {
                selectSql.append("?").append(",");
                params.add(limitMap.get("start"));
            }
            selectSql.append("?");
            params.add(limitMap.get("limit"));
        }
    }

    private void fitLimit(StringBuilder limitBuilder,List<Object> params,Map<String,Integer> limitMap){
        if(limitMap.get("limit") != null){
            limitBuilder.append(" ").append("limit").append(" ");
            if (limitMap.get("start") != null) {
                limitBuilder.append("?").append(",");
                params.add(limitMap.get("start"));
            }
            limitBuilder.append("?");
            params.add(limitMap.get("limit"));
        }
    }

    private <P> void matchingParams(P p,StringBuilder whereSql,List<Object> params,Map<String,Integer> limitMap) throws IllegalAccessException {
        matchingParams(p,whereSql,params,limitMap, field -> {
            String tableAlias = "";
            if( field.isAnnotationPresent(ClassForTable.class)) {
                Class<?> fieldClass = field.getAnnotation(ClassForTable.class).value();
                if (fieldClass.isAnnotationPresent(Alias.class)) {
                    if (!StringUtil.isEmpty(fieldClass.getAnnotation(Alias.class).value())) {
                        tableAlias = fieldClass.getAnnotation(Alias.class).value() + ".";
                    }
                }
            }
            return tableAlias;
        });
    }



    private <P> void matchingParams(P p, StringBuilder whereSql, List<Object> params, Map<String,Integer> limitMap, Function<Field,String> aliasFunction) throws IllegalAccessException {
        boolean whetherWhere = true;
        Field[] fields = FieldFunction.getAllDeclaredField(p.getClass());
        for(Field field : fields){
            try {
                if(!field.isAnnotationPresent(IgnoreSql.class)) {
                    field.setAccessible(true);
                    if (field.get(p) != null && !"".equals(field.get(p))) {
                        if (!excludeField.contains(field.getName())) {
                            if(field.isAnnotationPresent(SqlStart.class)){
                                limitMap.put("start",(Integer) field.get(p));
                            }else if(field.isAnnotationPresent(SqlLimit.class)) {
                                limitMap.put("limit",(Integer) field.get(p));
                            }else {
                                String tableAlias = aliasFunction.apply(field);

                                whereSql.append(" ");
                                if (whetherWhere) whereSql.append("where");
                                else whereSql.append("and");
                                whereSql.append(" ");
                                if (field.isAnnotationPresent(Condition.class)) {
                                    //多字段查询，条件字段，非数据库实际字段，查询字段可能涉及多个数据库字段
                                    String[] conditions = field.getAnnotation(Condition.class).value();
                                    boolean conditionOr = false;
                                    whereSql.append("(");
                                    for (String condition : conditions) {
                                        if(conditionOr)whereSql.append(" or ");
                                        if (field.isAnnotationPresent(SqlFieldLike.class))
                                            whereSql.append(tableAlias + condition).append(" like concat('%',?,'%')");
                                        else whereSql.append(tableAlias + condition).append(" =? ");
                                        params.add(getFieldValue(field, p));
                                        conditionOr = true;
                                    }
                                    whereSql.append(")");
                                } else { //单字段查询
                                    appendSqlWhere(whereSql, field,tableAlias);
                                    params.add(getFieldValue(field, p));
                                }
                                whetherWhere = false;
                            }
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                log.error( "获取sql参数出错" );
                log.error(e.getMessage(),e);
                throw e;
            }
        }
    }

    /*************************************************** 两表 left join 开始 ******************************************************/
    /**
     * 仅限两表left join的简便使用方式
     */
    @Comment("两表联表查询，快捷方法，参数内置")
    final public <T,E> List<Map<String,Object>> queryTwoLeftJoinList(T t,E e,String leftKey,String rightKey) throws Exception {
        StringBuilder selectSql = new StringBuilder("select ");
        Class clt = t.getClass() , cle = e.getClass();
        StringBuilder selectSqlField = new StringBuilder();
        StringBuilder fromSql = new StringBuilder();
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        Map<String,Integer> limitMap = new HashMap<>();
        StringBuilder orderBuilder = new StringBuilder();
        StringBuilder resultField = new StringBuilder();

        fitField(selectSqlField,resultField,clt,cle);
        fitFrom(fromSql,clt,cle,leftKey,rightKey);
        matchingParamsForTLJ(t,whereSql,params,limitMap);
        matchingParamsForTLJ(e,whereSql,params,limitMap);
        fitOrder(orderBuilder,clt,cle);
        selectSql.append(" ").append(selectSqlField).append(" ").append(fromSql).append(" ").append(whereSql).append(" ").append(orderBuilder);

        return query(selectSql,params,resultField);
    }

    @Comment("两表联表查询总数，快捷方法，参数内置")
    final public <T,E> Integer queryTwoLeftJoinCount(T t,E e,String leftKey,String rightKey) throws Exception {
        StringBuilder selectCount = new StringBuilder("select count(1) count ");
        Class clt = t.getClass() , cle = e.getClass();
        StringBuilder fromSql = new StringBuilder();
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        Map<String,Integer> limitMap = new HashMap<>();

        fitFrom(fromSql,clt,cle,leftKey,rightKey);
        matchingParamsForTLJ(t,whereSql,params,limitMap);
        matchingParamsForTLJ(e,whereSql,params,limitMap);
        selectCount.append(" ").append(fromSql).append(" ").append(whereSql);

        return queryCount(selectCount,params).get(0);
    }

    @Comment("两表联表分页查询，快捷方法，参数内置")
    final public <T,E> Map<String,Object> queryTwoLeftJoinForm(T t,E e,String leftKey,String rightKey,Integer page_num,Integer page_size) throws Exception {
        Map<String,Object> result = new HashMap<>();
        StringBuilder selectSql = new StringBuilder("select ");
        StringBuilder selectCount = new StringBuilder("select count(1) count ");
        Class clt = t.getClass() , cle = e.getClass();
        StringBuilder selectSqlField = new StringBuilder();
        StringBuilder fromSql = new StringBuilder();
        StringBuilder whereSql = new StringBuilder();
        StringBuilder limitBuilder = new StringBuilder();
        List<Object> params = new ArrayList<>();
        Map<String,Integer> limitMap = new HashMap<>();
        StringBuilder orderBuilder = new StringBuilder();
        StringBuilder resultField = new StringBuilder();

        fitField(selectSqlField,resultField,clt,cle);
        fitFrom(fromSql,clt,cle,leftKey,rightKey);
        matchingParamsForTLJ(t,whereSql,params,limitMap);
        matchingParamsForTLJ(e,whereSql,params,limitMap);
        selectCount.append(" ").append(fromSql).append(" ").append(whereSql);
        result.put("count",queryCount(selectCount,params).get(0));
        fitOrder(orderBuilder,clt,cle);
        limitMap.put("limit",page_size);
        limitMap.put("start",page_size * (page_num - 1) );
        fitLimit(limitBuilder,params,limitMap);
        selectSql.append(" ").append(selectSqlField).append(" ").append(fromSql).append(" ").append(whereSql).append(" ").append(orderBuilder).append(" ").append(limitBuilder);


        result.put("list",query(selectSql,params,resultField));
        return result;
    }

    private void fitField(StringBuilder selectField ,StringBuilder resultField , Class clt,Class cle){
        //selectField.append(queryInitSqlMap.get(clt)).append(",").append(queryInitSqlMap.get(cle));
        selectField.append(queryInitAliasSqlMap.get(clt)).append(",").append(queryInitAliasSqlMap.get(cle));
        resultField.append(aliasFieldInitMap.get(clt)).append(",").append(aliasFieldInitMap.get(cle));
    }

    private void fitFrom(StringBuilder fromSql , Class clt,Class cle,String leftKey,String rightKey){
        fromSql.append(" from ").append(queryTable.get(clt)).append(" left join ").append( queryTable.get( cle ) )
                .append(" on ").append( queryAlias.get( clt ) ).append(".").append(leftKey)
                .append(" = ").append( queryAlias.get( cle ) ).append(".").append(rightKey);
    }

    private void fitOrder(StringBuilder orderBuilder,Class clt,Class cle){
        if(queryInitOrderMap.get(clt) == null && queryInitOrderMap.get(cle) == null)return;
        orderBuilder.append(" order by ");
        orderBuilder.append( queryInitOrderMap.get(clt) == null?new StringBuilder(): queryInitOrderMap.get(clt));
        if( queryInitOrderMap.get(cle)!=null ) {
            if( !StringUtil.isEmpty(orderBuilder.toString() ) ) orderBuilder.append(",");
            orderBuilder.append( queryInitOrderMap.get(cle) );
        }
    }


    private <P> void matchingParamsForTLJ(P p,StringBuilder whereSql,List<Object> params,Map<String,Integer> limitMap) throws IllegalAccessException {
        matchingParams(p,whereSql,params,limitMap, field -> {
            Class<?> c = p.getClass();
            if(c.isAnnotationPresent(Alias.class)) {
                return c.getAnnotation(Alias.class).value() + ".";
            }
            return "";
        });
    }
    /*************************************************** 两表 left join 结束 ******************************************************/


    private <E> List<E> query(StringBuilder selectSql,List<Object> params,Class<E> returnPoClz) throws  Exception {
        List<E> list = new ArrayList<>();
        if(printSql)log.info("SQL执行语句为：{}", selectSql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statment = connection.prepareStatement(selectSql.toString());){
            for (int i = 0; i < params.size(); i++) {
                statment.setObject((i + 1), params.get(i));
            }
            if(printSql)log.info("SQL执行参数为：{}", String.join(",", params.stream().map(Object::toString).collect(Collectors.toList())));
            try(ResultSet rs = statment.executeQuery()){
                if(rs == null)return null;
                while (rs.next()) {
                    E t = packageEntity(rs,returnPoClz);
                    list.add(t);
                }
            }catch (SQLException e ){
                log.error("解析查询结果出错");
                throw e;
            }
        } catch (SQLException e) {
            log.error( "执行sql出错：{}",selectSql );
            throw e;
        }

        log.info("获得查询结果数量:{}",list.size());
        return list;
    }

    private <E> List<Map<String,Object>> query(StringBuilder selectSql,List<Object> params,Class<E> returnPoClz,StringBuilder selectField) throws  Exception {
        List<Map<String,Object>> list = new ArrayList<>();
        if(printSql)log.info("SQL执行语句为：{}", selectSql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statment = connection.prepareStatement(selectSql.toString());){
            for (int i = 0; i < params.size(); i++) {
                statment.setObject((i + 1), params.get(i));
            }
            if(printSql)log.info("SQL执行参数为：{}", String.join(",", params.stream().map(Object::toString).collect(Collectors.toList())));
            String[] selectFieldArgs = selectField.toString().replaceAll(" +","").split(",");
            try(ResultSet rs = statment.executeQuery()){
                if(rs == null)return null;
                while (rs.next()) {
                    Map<String,Object> map = new HashMap<>();
                    for(String valueField : selectFieldArgs){
                        if(!StringUtil.isEmpty(valueField))map.put(valueField,rs.getObject(valueField));
                    }
                    list.add(map);
                }
            }catch (SQLException e ){
                log.error("解析查询结果出错");
                throw e;
            }
        } catch (SQLException e) {
            log.error( "执行sql出错：{}",selectSql );
            throw e;
        }
        log.info("获得查询结果数量:{}",list.size());
        return list;
    }

    private <E> List<Map<String,Object>> query(StringBuilder selectSql,List<Object> params,StringBuilder selectField) throws  Exception {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statment = connection.prepareStatement(selectSql.toString());){
            for (int i = 0; i < params.size(); i++) {
                statment.setObject((i + 1), params.get(i));
            }
            if(printSql)log.info("SQL执行语句为:{} 执行参数为:{}", selectSql,String.join(",",params.stream().map(Object::toString).collect(Collectors.toList())));
            String[] selectFieldArgs = selectField.toString().replaceAll(" +","").replaceAll("`","").split(",");
            try(ResultSet rs = statment.executeQuery()){
                if(rs == null)return null;
                while (rs.next()) {
                    Map<String,Object> map = new HashMap<>();
                    for(String valueField : selectFieldArgs){
                        if(!StringUtil.isEmpty(valueField))map.put(valueField,rs.getObject(valueField));
                    }
                    list.add(map);
                }
            }catch (SQLException e ){
                log.error("解析查询结果出错");
                throw e;
            }
        } catch (SQLException e) {
            log.error("执行sql出错：{}",selectSql );
            throw e;
        }
        log.info("获得查询结果数量:{}",list.size());
        return list;
    }

    private ResultSet invokeSql(StringBuilder selectSql,List<Object> params) throws SQLException {
        try{
            return getPreparedStatment(selectSql,params). executeQuery();
        } catch (SQLException e) {
            log.error( "执行sql出错：{}",selectSql );
            log.error(e.getMessage(),e);
            throw new SQLException();
        }
    }

    private void appendSqlWhere(StringBuilder whereSql,Field field,String tableAlias){
        //根据注解情况，获取实际查询字段
        StringBuilder selectField = whetherGetAnnotationValueAsSqlField(field);
        selectField = new StringBuilder(tableAlias).append(selectField);
        if (field.isAnnotationPresent(SqlDateFormat.class))selectField = new StringBuilder("date_format(").append(selectField).append(",'"+ field.getAnnotation(SqlDateFormat.class).value() +"')");
        //多种查询条件
        if (field.isAnnotationPresent(SqlFieldLike.class))
            whereSql.append(selectField).append(" like concat('%',?,'%')");
        else if(field.isAnnotationPresent(Min.class))whereSql.append(selectField).append(" >? ");
        else if(field.isAnnotationPresent(MinAndEqual.class))whereSql.append(selectField).append(" >=? ");
        else if(field.isAnnotationPresent(Max.class))whereSql.append(selectField).append(" <? ");
        else if(field.isAnnotationPresent(MaxAndEqual.class))whereSql.append(selectField).append(" <=? ");
        else whereSql.append(selectField).append(" =? ");
    }

    private <T> T packageEntity(ResultSet rs,Class<T> returnPoClz) throws InstantiationException, IllegalAccessException, SQLException {
        T t = returnPoClz.newInstance();
        Field[] fields = returnPoClz.getDeclaredFields();
        for(Field field : fields){
            if(!field.isAnnotationPresent(IgnoreSql.class) && !field.isAnnotationPresent(IgnoreSelectField.class)) {
                field.setAccessible(true);
                Type type = field.getType();
                if (type.getTypeName().equals(String.class.getTypeName())) { //如果需要可以继续补充
                    field.set(t, rs.getString(field.getName()));
                } else if (type.getTypeName().equals(Integer.class.getTypeName())) {
                    field.set(t, rs.getInt(field.getName()));
                } else if (type.getTypeName().equals(Long.class.getTypeName())) {
                    field.set(t, rs.getLong(field.getName()));
                } else if (type.getTypeName().equals(Short.class.getTypeName())) {
                    field.set(t, rs.getShort(field.getName()));
                } else if (type.getTypeName().equals(Byte.class.getTypeName())) {
                    field.set(t, rs.getByte(field.getName()));
                } else if (type.getTypeName().equals(java.util.Date.class.getTypeName())) {
                    field.set(t, rs.getTimestamp(field.getName()));
                } else if (type.getTypeName().equals(Boolean.class.getTypeName())) {
                    field.set(t, rs.getBoolean(field.getName()));
                } else if (type.getTypeName().equals(BigDecimal.class.getTypeName())) {
                    field.set(t, rs.getBigDecimal(field.getName()));
                } else if (type.getTypeName().equals(Double.class.getTypeName())) {
                    field.set(t, rs.getDouble(field.getName()));
                } else if (type.getTypeName().equals(Float.class.getTypeName())) {
                    field.set(t, rs.getFloat(field.getName()));
                }
            }
        }
        return t;
    }

    @Comment("插入数据")
    final public <T> Boolean insert(T t) throws IllegalAccessException, SQLException {
        Class<?> clz = t.getClass();
        Field[] fields = clz.getDeclaredFields();
        String tableName = clz.getAnnotation(TableName.class).value();
        StringBuilder insertSql = new StringBuilder("insert into " + tableName).append("(");
        List<String> sqlField = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for(Field field : fields){
            try {
                if(!field.isAnnotationPresent(IgnoreSql.class)) {
                    field.setAccessible(true);
                    if (field.get(t) != null && !"".equals(field.get(t))) {
                        sqlField.add("`" + field.getName() + "`");
                        params.add( getFieldValue(field,t) );
                    }
                }
            } catch (IllegalAccessException e) {
                log.error( "获取sql参数出错" );
                log.error(e.getMessage(),e);
                throw e;
            }
        }
        insertSql.append( String.join(",",sqlField) ).append(")VALUES(").append( String.join(",",params.stream().map(s -> "?").collect(Collectors.toList() ) )).append(")");

        log.info("SQL执行语句为：{}", insertSql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statment = connection.prepareStatement(insertSql.toString());){
            for (int i = 0; i < params.size(); i++) {
                statment.setObject((i + 1), params.get(i));
            }
            log.info("SQL执行参数为：{}",  String.join(",", params.stream().map(Object::toString).collect(Collectors.toList())));
            Boolean result = statment.execute();
            log.info("SQL插入执行结果：{}",  result);
            return result;
        } catch (SQLException e) {
            log.error( "执行sql出错：{}",insertSql );
            throw e;
        }
    }

    /**
     * 更新条件为主键@PrimaryKey
     * @param t
     */
    @Comment("更新数据，更新条件为主键@PrimaryKey")
    final public <T> Integer update(T t) throws Exception {
        Class<?> clz = t.getClass();
        Field[] fields = clz.getDeclaredFields();
        String tableName = clz.getAnnotation(TableName.class).value();
        StringBuilder updateSql = new StringBuilder("update " + tableName + " set ");
        List<Object> sqlField = new ArrayList<>(),params = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>(),primaryValues = new ArrayList<>();
        for(Field field : fields){
            try {
                if(!field.isAnnotationPresent(IgnoreSql.class)) {
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(PrimaryKey.class)) {
                        primaryKeys.add("`" + field.getName() + "`");
                        primaryValues.add( getFieldValue(field,t) );
                        continue;
                    }
                    if (field.get(t) != null && !"".equals(field.get(t))) {
                        sqlField.add("`" + field.getName() + "`");
                        params.add( getFieldValue(field,t) );
                    }
                }
            } catch (IllegalAccessException e) {
                log.error( "获取sql参数出错");
                log.error(e.getMessage(),e);
                throw e;
            }
        }
        boolean whetherComma = false;
        for(int i = 0 ; i < sqlField.size(); i ++ ){
            if(whetherComma)updateSql.append(",");
            updateSql.append(" ").append(sqlField.get(i)).append(" = ? ");
            whetherComma = true;
        }
        boolean whetherWhere = true;
        if(sqlField.size() <= 0)throw new Exception("主键未找到");
        for(int i = 0; i < primaryKeys.size(); i ++){
            updateSql.append(" ");
            if(whetherWhere)updateSql.append("where ");
            else updateSql.append("and ");
            updateSql.append( primaryKeys.get(i) ).append(" = ?");
            whetherWhere = false;
        }
        params.addAll(primaryValues);
        try {
            return updateAndDelete(updateSql,params);
        } catch (SQLException e) {
            log.error("SQL执行错误",e);
            throw new SQLException("SQL执行错误");
        }
    }

    /**
     * 仅适删除条件为主键@PrimaryKey（可以为联合主键）
     * @param t
     */
    @Comment("删除数据，仅适删除条件为主键@PrimaryKey（可以为联合主键）")
    final public <T> Integer delete(T t) throws Exception {
        return delete(t , field -> field.isAnnotationPresent(PrimaryKey.class) );
    }

    /**
     * 删除，可大量删除，谨慎使用
     */
    @Comment("删除数据，可大量删除，条件为内置参数")
    final public <T> Integer deleteByField(T t) throws Exception {
        return delete(t , field -> {
            try {
                field.setAccessible(true);
                return field.get(t) != null && !"".equals(field.get(t));
            } catch (IllegalAccessException e) {
                log.warn("",e);
            }
            return false;
        });
    }

    private <T> Integer delete(T t, Predicate<Field> predicate) throws Exception {
        String tableName = t.getClass().getAnnotation(TableName.class).value();
        Field[] fields = t.getClass().getDeclaredFields();
        List<String> sqlField = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Field field : fields) {
            if(!field.isAnnotationPresent(IgnoreSql.class)) {
                if ( predicate.test(field) ) {
                    field.setAccessible(true);
                    sqlField.add("`" + field.getName() + "`");
                    try {
                        params.add( getFieldValue(field,t) );
                    } catch (IllegalAccessException e) {
                        log.error("获取sql参数出错");
                        log.error(e.getMessage(), e);
                        throw e;
                    }
                }
            }
        }
        StringBuilder sql = new StringBuilder("delete from ").append(tableName);
        boolean whetherWhere = true;
        if(sqlField.size() <= 0)throw new Exception("主键未找到");
        for(int i = 0 ; i < sqlField.size(); i ++){
            sql.append(" ");
            if(whetherWhere)sql.append("where ");
            else sql.append(" and ");
            sql.append(sqlField.get(i)).append("= ?");
            whetherWhere = false;
        }
        try {
            return updateAndDelete(sql,params);
        } catch (SQLException e) {
            log.error( "执行sql出错" );
            log.error(e.getMessage(),e);
            throw new SQLException(e);
        }
    }


    /**
     * 适用于单值条件删除
     * @param tablename
     * @param key
     * @param value
     */
    @Comment("删除数据，适用于单值条件删除")
    final public Integer delete(String tablename,String key,String value) throws SQLException {
        StringBuilder sql = new StringBuilder("delete from ").append(tablename).append(" where ").append(key).append(" = ?");
        List<Object> params = Collections.singletonList(value);
        try {
            return updateAndDelete(sql,params);
        } catch (SQLException e) {
            log.error( "获取sql参数出错");
            throw new SQLException(e);
        }
    }


    private Integer updateAndDelete(StringBuilder sql,List<Object> params) throws SQLException {
        //getPreparedStatment(sql,params).executeUpdate();
        if(printSql)log.info("SQL执行语句为：{}", sql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statment = connection.prepareStatement(sql.toString());){
            for (int i = 0; i < params.size(); i++) {
                statment.setObject((i + 1), params.get(i));
            }
            if(printSql)log.info("SQL执行参数为：{}",  String.join(",", params.stream().map(Object::toString).collect(Collectors.toList())));
            Integer result = statment.executeUpdate();
            log.info("SQL执行结果影响条数：{}", result);
            return result;
        }
    }

    private PreparedStatement getPreparedStatment(StringBuilder sql,List<Object> params) throws SQLException {
        if(printSql)log.info("SQL执行语句为：{}", sql);
        PreparedStatement  statment = dataSource.getConnection().prepareStatement(sql.toString());
        for(int i = 0;i < params.size(); i++){
            statment.setObject((i+1),params.get(i));
        }
        if(printSql)log.info("SQL执行参数为：{}",String.join(",", params.stream().map(Object::toString).collect(Collectors.toList())));
        return statment;
    }

    public void close(ResultSet rs, PreparedStatement statement, Connection connection) throws SQLException {
        if (rs != null){
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (statement != null){
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null){
            try {
                connection.close();  //归还连接
            } catch (SQLException e) {
                log.error("",e);
                throw e;
            }
        }
    }

    /**
     * 根据注解情况获取实际查询字段
     */
    private StringBuilder whetherGetAnnotationValueAsSqlField(Field field){
        if(field.isAnnotationPresent(Min.class))return new StringBuilder(field.getAnnotation(Min.class).value());
        else if(field.isAnnotationPresent(Max.class))return new StringBuilder(field.getAnnotation(Max.class).value());
        else if(field.isAnnotationPresent(MinAndEqual.class))return new StringBuilder(field.getAnnotation(MinAndEqual.class).value());
        else if(field.isAnnotationPresent(MaxAndEqual.class))return new StringBuilder(field.getAnnotation(MaxAndEqual.class).value());
        return new StringBuilder("`" + field.getName() + "`");
    }

    private <T> String getFieldValue(Field field,T t) throws IllegalAccessException {
        if(field.getType().getTypeName().equals(Date.class.getTypeName()) && field.isAnnotationPresent(ValueDateFormat.class)){
            return new SimpleDateFormat(field.getAnnotation(ValueDateFormat.class).value()).format(field.get(t));
        }
        return  field.get(t).toString();
    }

    @Override
    final public void afterPropertiesSet() throws Exception {
        //先初始化数据库连接的用户名密码和url
        initConfigInfo();
        //在初始化德鲁伊连接池
        InitDruidPool();
        //配置非查询字段
        if(!StringUtil.isEmpty(excludeFieldString))excludeField = new HashSet<>(Arrays.asList(excludeFieldString.split(",")));
        log.info("非查询字段有{}个，分别是:{}",excludeField.size(),excludeFieldString);
        //初始化查询sql语句
        if(StringUtil.isEmpty(scanPoPackage)){
            log.error("未扫描到数据表映射类");
            System.exit(0);
        }
        Set<Class<?>> classes = new LinkedHashSet<>();
        for(String packagePath : scanPoPackage.split(",")) {
            classes.addAll(ClassFunction.getClasses(packagePath));
        }
        log.info("扫描的sql映射类共{}个",classes.size());
        for(Class<?> c:classes){
            if(c.isAnnotationPresent(TableName.class)) {
                String tableName = c.getAnnotation(TableName.class).value();
                String tableAlias = "";
                if(c.isAnnotationPresent(Alias.class)) {
                    if( !StringUtil.isEmpty(c.getAnnotation(Alias.class).value()) ) {
                        tableAlias = c.getAnnotation(Alias.class).value();
                        HNSCache.getInstance().putAliasMap(c , tableAlias);
                        tableName = tableName+" as "+c.getAnnotation(Alias.class).value();
                        if(queryAlias.containsValue(tableAlias)){
                            log.error("映射表类中定义的别名（Alias）重复！");
                            System.exit(0);
                        }
                        queryAlias.put(c,tableAlias);
                        tableAlias += ".";
                    }
                }
                queryTable.put(c,tableName);
                Field[] fields = c.getDeclaredFields();
                StringBuilder selectSql = new StringBuilder();
                StringBuilder selectAliasSql = new StringBuilder();
                StringBuilder aliasField = new StringBuilder();
                TreeMap<Integer,String> selectOrderMap = new TreeMap<>();
                for (Field field : fields) {
                    String fieldName = tableAlias + "`" + field.getName() + "`";
                    if(!field.isAnnotationPresent(IgnoreSql.class) && !field.isAnnotationPresent(IgnoreSelectField.class)) {
                        if (field.isAnnotationPresent(OrderByAsc.class)) {
                            selectOrderMap.put(field.getAnnotation(OrderByAsc.class).value(), fieldName + " asc");
                        }
                        if (field.isAnnotationPresent(OrderByDesc.class)) {
                            selectOrderMap.put(field.getAnnotation(OrderByDesc.class).value(), fieldName + " desc");
                        }
                        if(field.isAnnotationPresent(SelectDateFormat.class)){
                            fieldName = "date_format(" + fieldName + ",'" + field.getAnnotation(SelectDateFormat.class).value() +"')";
                        }
                        selectSql.append(fieldName + " as " + field.getName()).append(",");
                        selectAliasSql.append(fieldName).append(" as `").append(tableAlias).append(field.getName()).append("`").append(",");
                        aliasField.append(tableAlias).append(field.getName()).append(",");
                    }
                }
                //selectSql.deleteCharAt(selectSql.length() - 1).append(" ");
                this.replaceLastChatToSpace(selectSql,selectAliasSql,aliasField);
                if(selectOrderMap.size() > 0){
                    StringBuilder orderBuilder = new StringBuilder();
                    boolean hasOrder = false;
                    List<Integer> orderKeyList = new ArrayList<>(selectOrderMap.keySet());
                    Collections.sort(orderKeyList);
                    for(Integer i : orderKeyList){
                        if(hasOrder)orderBuilder.append(",");                  //orderBuilder.append(" order by ");
                        orderBuilder.append(selectOrderMap.get(i));
                        hasOrder = true;
                    }
                    queryInitOrderMap.put(c , orderBuilder);
                }
                queryInitSqlMap.put(c, selectSql);
                queryInitAliasSqlMap.put(c, selectAliasSql);
                aliasFieldInitMap.put(c, aliasField);
            }
        }
    }

    private void replaceLastChatToSpace(StringBuilder... strs){
        for(int i = 0; i < strs.length; i ++) {
            strs[i].deleteCharAt(strs[i].length() - 1).append(" ");
        }
    }

}
