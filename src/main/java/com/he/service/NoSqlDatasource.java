package com.he.service;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.he.common.ConfigParam;
import com.he.common.annotation.comment.Comment;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Slf4j
@Setter
@Comment("查询服务")
public class NoSqlDatasource{

    private NoSqlDatasource(){}

    private static NoSqlDatasource instance = null;

    public static NoSqlDatasource getInstance() {
        if (instance == null) {
            instance = new NoSqlDatasource();
        }
        return instance;
    }


    private static DataSource dataSource ;

    public void InitDruidPoolInfo() throws Exception {
        try {
            dataSource = DruidDataSourceFactory.createDataSource(ConfigParam.getInstance().getDruidConfigMap());
            log.info("数据库连接池加载完毕");
        } catch (Exception e) {
            log.error("初始化数据库连接池错误", e);
            throw e;
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

}
