package com.he.service;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.he.common.annotation.sql.*;
import com.he.common.vo.BaseForm;
import com.he.util.StringUtil;
import com.he.util.YamlUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * auth 贺宁
 * 目的：为了减少sql语句开发，以及后续可能的表结构变动带来的高昂维护成本
 * @param <T>
 */
@Component
@Slf4j
@Setter
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
    private static String excludeFieldString = "transid,datetime,limit,pageNum,start";

    private static DataSource dataSource ;

    private Map<Class<?>,StringBuilder> queryInitSqlMap = new HashMap<>();      //查询字段
    private Map<Class<?>,StringBuilder> queryInitOrderMap = new HashMap<>();    //查询排序
    private Set<String> excludeField = new HashSet<>(Arrays.asList("transid","datetime","limit","pageNum","start"));  //这些字段不参与查询字段

    /**
     * 可继承重写，增加额外加载配置
     */
    protected Map<String, Object> getApplicationMap() throws IOException {
        Map<String, Object> map = YamlUtils.yamlHandler("application.yml");
        String suffix = map.get("spring.profiles.active").toString();
        return YamlUtils.yamlHandler("application-" + suffix + ".yml");
    }

    /**
     * 可继承重写，增加额外加载配置
     */
    protected void initConfigInfo() throws IOException {
        try {
            Map<String, Object> appMap = getApplicationMap();
            driverClassName = appMap.get("spring.datasource.driver-class-name").toString();
            log.info("noSql使用mysql驱动:{}",driverClassName);
            username = appMap.get("spring.datasource.username").toString();
            log.info("noSql连接池用户名:{}",username);
            password = appMap.get("spring.datasource.password").toString();
            url = appMap.get("spring.datasource.url").toString();
            log.info("noSql连接池url:{}",url);

            //连接池配置
            if( appMap.get("noSql.pool.initialSize") != null ) initialSize = appMap.get("noSql.pool.initialSize").toString();
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

            if(!StringUtil.isEmpty(appMap.get("noSql.select.excludeField").toString()))excludeFieldString = appMap.get("noSql.select.excludeField").toString();
            log.info("不参与查询的字段设置：{}",excludeFieldString);

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

    final public <T> List<T> queryList(Class<T> clt) throws Exception {
        try {
            return queryList(clt.newInstance());
        } catch (Exception e) {
            log.error("查询结果列表失败");
            throw new Exception();
        }
    }

    final public <T> List<T> queryList(T tp) throws Exception {
        Class clz =  tp.getClass();
        Field[] fields = clz.getDeclaredFields();
        StringBuilder selectSql = new StringBuilder(queryInitSqlMap.get(clz));
        boolean whetherWhere = true;
        List<String> params = new ArrayList<>();
        for(Field field : fields){
            try {
                if(!field.isAnnotationPresent(IgnoreSql.class)) {
                    field.setAccessible(true);
                    if (field.get(tp) != null && !"".equals(field.get(tp))) {
                        if (whetherWhere) selectSql.append(" ").append("where").append(" ");
                        else selectSql.append(" ").append("and").append(" ");

                        appendSqlWhere(selectSql,field);

                        params.add( getFieldValue(field,tp) );
                        whetherWhere = false;
                    }
                }
            } catch (IllegalAccessException e) {
                log.error( "获取sql参数出错" );
                log.error(e.getMessage(),e);
                throw new IllegalAccessException();
            }
        }
        selectSql.append(" ").append(queryInitOrderMap.get(clz) == null?"":queryInitOrderMap.get(clz));
        return query(selectSql,params,clz);
    }

    final public <T> Map<String,Object> queryForm(Class<T> clz, BaseForm rv) throws Exception {
        //查询字段
        StringBuilder selectSql = new StringBuilder(queryInitSqlMap.get(clz));
        //查询数量
        StringBuilder selectCount = new StringBuilder("select count(1) count from ").append(clz.getAnnotation(TableName.class).value());
        //查询条件
        StringBuilder whereSql = new StringBuilder();

        Field[] fields = getAllDeclaredField(rv.getClass());
        boolean whetherWhere = true;
        List<String> params = new ArrayList<>();
        Integer start = null;
        Integer limit = null;
        for(Field field : fields){
            try {
                if(!field.isAnnotationPresent(IgnoreSql.class)) {
                    field.setAccessible(true);
                    if (field.get(rv) != null && !"".equals(field.get(rv))) {
                        if (!excludeField.contains(field.getName())) {
                            whereSql.append(" ");
                            if (whetherWhere) whereSql.append("where");
                            else whereSql.append("and");
                            whereSql.append(" ");
                            if (field.isAnnotationPresent(Condition.class)) {
                                //多字段查询，条件字段，非数据库实际字段，查询字段可能涉及多个数据库字段
                                String[] conditions = field.getAnnotation(Condition.class).value();
                                for (String condition : conditions) {
                                    if (field.isAnnotationPresent(SqlFieldLike.class))
                                        whereSql.append(condition).append(" like concat('%',?,'%')");
                                    else whereSql.append(condition).append(" =? ");
                                    params.add( getFieldValue(field,rv) );
                                }
                            } else { //单字段查询
                                appendSqlWhere(whereSql,field);
                                params.add( getFieldValue(field,rv) );
                            }
                            whetherWhere = false;
                        } else {
                            if ("start".equals(field.getName()) && field.get(rv) != null)
                                start = (Integer) field.get(rv);
                            if ("limit".equals(field.getName()) && field.get(rv) != null)
                                limit = (Integer) field.get(rv);
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                log.error( "获取sql参数出错" );
                log.error(e.getMessage(),e);
                throw new IllegalAccessException();
            }
        }
        selectCount.append(whereSql);
        Integer count = queryCount(selectCount,params).get(0);
        selectSql.append(whereSql).append( queryInitOrderMap.get(clz) == null?"":queryInitOrderMap.get(clz) );
        if(limit != null){
            selectSql.append(" ").append("limit").append(" ");
            if (start != null) selectSql.append(start).append(",");
            selectSql.append(limit);
        }
        List<T> list = query(selectSql,params,clz);
        Map<String,Object> map = new HashMap<>();
        map.put("list",list);
        map.put("count",count);
        return map;
    }

    private List<Integer> queryCount(StringBuilder selectSql,List<String> params) throws SQLException {
        List<Integer> list = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statment = connection.prepareStatement(selectSql.toString());){
            for (int i = 0; i < params.size(); i++) {
                statment.setString((i + 1), params.get(i));
            }
            log.info("SQL执行参数为：{}", String.join(",", params));
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
        /*
        ResultSet rs = invokeSql(selectSql,params);
        List<Integer> list = new ArrayList<>();
        try {
            if(rs == null)return null;
            while (rs.next()) {
                list.add(rs.getInt("count"));
            }
        }catch (SQLException e){
            log.error("解析查询结果出错");
            log.error("",e);
            throw new SQLException();
        }
         */

        return list;
    }

    private <E> List<E> query(StringBuilder selectSql,List<String> params,Class<E> returnPoClz) throws  Exception {
        List<E> list = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statment = connection.prepareStatement(selectSql.toString());){
            for (int i = 0; i < params.size(); i++) {
                statment.setString((i + 1), params.get(i));
            }
            log.info("SQL执行参数为：{}", String.join(",", params));
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

        /*
        ResultSet rs = invokeSql(selectSql,params);
        try {
            if(rs == null)return null;
            while (rs.next()) {
                E t = packageEntity(rs,returnPoClz);
                list.add(t);
            }
        }catch (SQLException | InstantiationException | IllegalAccessException e){
            log.error("解析查询结果出错");
            log.error("",e);
            throw e;
        }
        */
        log.info("获得查询结果数量:{}",list.size());

        return list;
    }

    private ResultSet invokeSql(StringBuilder selectSql,List<String> params) throws SQLException {
        try{
            return getPreparedStatment(selectSql,params). executeQuery();
        } catch (SQLException e) {
            log.error( "执行sql出错：{}",selectSql );
            log.error(e.getMessage(),e);
            throw new SQLException();
        }
    }

    private void appendSqlWhere(StringBuilder whereSql,Field field){
        //根据注解情况，获取实际查询字段
        StringBuilder selectField = whetherGetAnnotationValueAsSqlField(field);
        if (field.isAnnotationPresent(SqlDateFormat.class))selectField = new StringBuilder("date_format(").append(selectField).append(",'%Y%m%d%H%i%s')");
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
                } else if (type.getTypeName().equals(Date.class.getTypeName())) {
                    field.set(t, rs.getDate(field.getName()));
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

    final public <T> void insert(T t) throws IllegalAccessException, SQLException {
        Class<?> clz = t.getClass();
        Field[] fields = clz.getDeclaredFields();
        String tableName = clz.getAnnotation(TableName.class).value();
        StringBuilder insertSql = new StringBuilder("insert into " + tableName).append("(");
        List<String> sqlField = new ArrayList<>();
        List<String> params = new ArrayList<>();
        for(Field field : fields){
            try {
                if(!field.isAnnotationPresent(IgnoreSql.class)) {
                    field.setAccessible(true);
                    if (field.get(t) != null && !"".equals(field.get(t))) {
                        sqlField.add(field.getName());
                        params.add( getFieldValue(field,t) );
                    }
                }
            } catch (IllegalAccessException e) {
                log.error( "获取sql参数出错" );
                log.error(e.getMessage(),e);
                throw new IllegalAccessException();
            }
        }
        insertSql.append( String.join(",",sqlField) ).append(")VALUES(").append( String.join(",",params.stream().map(s -> "?").collect(Collectors.toList() ) )).append(")");

        log.info("SQL执行语句为：{}", insertSql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statment = connection.prepareStatement(insertSql.toString());){
            for (int i = 0; i < params.size(); i++) {
                statment.setString((i + 1), params.get(i));
            }
            log.info("SQL执行参数为：{}",  String.join(",", params));
            statment.execute();
        } catch (SQLException e) {
            log.error( "执行sql出错：{}",insertSql );
            throw e;
        }
    }

    /**
     * 更新条件为主键@PrimaryKey
     * @param t
     */
    final public <T> void update(T t) throws Exception {
        Class<?> clz = t.getClass();
        Field[] fields = clz.getDeclaredFields();
        String tableName = clz.getAnnotation(TableName.class).value();
        StringBuilder updateSql = new StringBuilder("update " + tableName + " set ");
        List<String> sqlField = new ArrayList<>(),params = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>(),primaryValues = new ArrayList<>();
        for(Field field : fields){
            try {
                if(!field.isAnnotationPresent(IgnoreSql.class)) {
                    field.setAccessible(true);
                    String fieldValue =  getFieldValue(field,t) ;
                    if (field.isAnnotationPresent(PrimaryKey.class)) {
                        primaryKeys.add(field.getName());
                        primaryValues.add( getFieldValue(field,t) );
                        continue;
                    }
                    if (field.get(t) != null && !"".equals(field.get(t))) {
                        sqlField.add(field.getName());
                        params.add( getFieldValue(field,t) );
                    }
                }
            } catch (IllegalAccessException e) {
                log.error( "获取sql参数出错");
                log.error(e.getMessage(),e);
                throw new IllegalAccessException();
            }
        }
        boolean whetherComma = false;
        for(int i = 0 ; i < sqlField.size(); i ++ ){
            if(whetherComma)updateSql.append(",");
            updateSql.append(" ").append(sqlField.get(i)).append(" = ? ");
            whetherComma = true;
        }
        boolean whether = true;
        if(sqlField.size() <= 0)throw new Exception("主键未找到");
        for(int i = 0; i < primaryKeys.size(); i ++){
            updateSql.append(" ");
            if(whether)updateSql.append("where ");
            else updateSql.append("and ");
            updateSql.append( primaryKeys.get(i) ).append(" = ?");
        }
        params.addAll(primaryValues);
        try {
            updateAndDelete(updateSql,params);
        } catch (SQLException e) {
            log.error("SQL执行错误",e);
            throw new SQLException("SQL执行错误");
        }
    }

    /**
     * 仅适删除条件为主键@PrimaryKey（可以为联合主键）
     * @param t
     */
    final public <T> void delete(T t) throws Exception {
        String tableName = t.getClass().getAnnotation(TableName.class).value();
        Field[] fields = t.getClass().getDeclaredFields();
        String id = null,value = null;
        List<String> sqlField = new ArrayList<>();
        List<String> params = new ArrayList<>();
        for (Field field : fields) {
            if(!field.isAnnotationPresent(IgnoreSql.class)) {
                if (field.isAnnotationPresent(PrimaryKey.class)) {
                    sqlField.add(field.getName());
                    try {
                        params.add( getFieldValue(field,t) );
                    } catch (IllegalAccessException e) {
                        log.error("获取sql参数出错");
                        log.error(e.getMessage(), e);
                        throw new IllegalAccessException();
                    }
                    break;
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
            updateAndDelete(sql,params);
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
    final public void delete(String tablename,String key,String value) throws SQLException {
        StringBuilder sql = new StringBuilder("delete from ").append(tablename).append(" where ").append(key).append(" = ?");
        List<String> params = Collections.singletonList(value);
        try {
            updateAndDelete(sql,params);
        } catch (SQLException e) {
            log.error( "获取sql参数出错");
            throw new SQLException(e);
        }
    }


    private void updateAndDelete(StringBuilder sql,List<String> params) throws SQLException {
        //getPreparedStatment(sql,params).executeUpdate();
        log.info("SQL执行语句为：{}", sql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statment = connection.prepareStatement(sql.toString());){
            for (int i = 0; i < params.size(); i++) {
                statment.setString((i + 1), params.get(i));
            }
            log.info("SQL执行参数为：{}",  String.join(",", params));
            statment.executeUpdate();
        }
    }

    private PreparedStatement getPreparedStatment(StringBuilder sql,List<String> params) throws SQLException {
        log.info("SQL执行语句为：{}", sql);
        PreparedStatement  statment = dataSource.getConnection().prepareStatement(sql.toString());
        for(int i = 0;i < params.size(); i++){
            statment.setString((i+1),params.get(i));
        }
        log.info("SQL执行参数为：{}",String.join(",",params) );
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
        return new StringBuilder(field.getName());
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
        if(StringUtil.isEmpty(scanPoPackage))throw new Exception();  //TODO 此处需要自定义Exception抛出异常”未扫描到数据表映射类“
        Set<Class<?>> classes = new LinkedHashSet<>();
        for(String packagePath : scanPoPackage.split(",")) {
            classes.addAll(getClasses(packagePath));
        }
        log.info("扫描的sql映射类共{}个",classes.size());
        for(Class<?> c:classes){
            if(c.isAnnotationPresent(TableName.class)) {
                String tableName = c.getAnnotation(TableName.class).value();
                Field[] fields = c.getDeclaredFields();
                StringBuilder selectSql = new StringBuilder("select ");
                TreeMap<Integer,String> selectOrderMap = new TreeMap<>();
                for (Field field : fields) {
                    if(!field.isAnnotationPresent(IgnoreSql.class) && !field.isAnnotationPresent(IgnoreSelectField.class)) {
                        selectSql.append(field.getName()).append(",");
                        if (field.isAnnotationPresent(OrderByAsc.class)) {
                            selectOrderMap.put(field.getAnnotation(OrderByAsc.class).value(), field.getName() + " asc");
                        }
                        if (field.isAnnotationPresent(OrderByDesc.class)) {
                            selectOrderMap.put(field.getAnnotation(OrderByDesc.class).value(), field.getName() + " desc");
                        }
                    }
                }
                selectSql.deleteCharAt(selectSql.length() - 1).append(" ").append("from").append(" ").append(tableName);
                if(selectOrderMap.size() > 0){
                    StringBuilder orderBuilder = new StringBuilder();
                    boolean hasOrder = false;
                    for(Integer i : selectOrderMap.keySet()){
                        if(!hasOrder)orderBuilder.append(" order by ");
                        else orderBuilder.append(",");
                        orderBuilder.append(selectOrderMap.get(i));
                    }
                    queryInitOrderMap.put(c , orderBuilder);
                }
                queryInitSqlMap.put(c, selectSql);
            }
        }
    }

    private static Field[] getAllDeclaredField(Class<?> clz){
        Field[] me = clz.getDeclaredFields();
        if((clz = clz.getSuperclass())!=null){
            Field[] sup = getAllDeclaredField(clz);
            Field[] fields = new Field[me.length + sup.length];
            System.arraycopy(me, 0, fields, 0, me.length);
            System.arraycopy(sup, 0, fields, me.length, sup.length);
            return fields;
        }else{
            return me;
        }
    }

    private static Set<String> getAllDeclaredFieldName(Class<?> clz){
        return Arrays.asList( getAllDeclaredField(clz) ).stream().map(Field::getName).collect(Collectors.toSet());
    }

    //class方式获取类
    private static Set<Class<?>> getClasses(String pack) throws Exception {
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        // 是否循环迭代
        boolean recursive = true;
        String packageName = pack;
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findClassesInPackageByFile(packageName, filePath, recursive, classes);
                } else if ("jar".equals(protocol)) {
                    System.out.println("jar类型的扫描");
                    JarFile jar;
                    try {
                        jar = ((JarURLConnection) url.openConnection()).getJarFile();
                        Enumeration<JarEntry> entries = jar.entries();
                        findClassesInPackageByJar(packageName, entries, packageDirName, recursive, classes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    //文件方式获取类
    private static void findClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes) {
        File dir = new File(packagePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] dirfiles = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        for (File file : dirfiles) {
            if (file.isDirectory()) {
                findClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    // 使用forName获取class时会触发static方法，所以使用classLoader的获取
                    classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void findClassesInPackageByJar(String packageName, Enumeration<JarEntry> entries, String packageDirName, final boolean recursive, Set<Class<?>> classes) {
        while (entries.hasMoreElements()) {
            // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.charAt(0) == '/') {
                name = name.substring(1);
            }
            // 如果前半部分和定义的包名相同
            if (name.startsWith(packageDirName)) {
                int idx = name.lastIndexOf('/');
                // 如果以"/"结尾 说明是一个包
                if (idx != -1) {
                    packageName = name.substring(0, idx).replace('/', '.');
                }
                if ((idx != -1) || recursive) {
                    if (name.endsWith(".class") && !entry.isDirectory()) {
                        // 去掉后面的".class" 截取出类名
                        String className = name.substring(packageName.length() + 1, name.length() - 6);
                        try {
                            classes.add(Class.forName(packageName + '.' + className));
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

}
