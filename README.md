# henosql
为了减少sql语句开发，以及后续可能的表结构变动带来的高昂维护成本

# version1.7
支持级联查询，新增类Carrier描述表连接关系，支持查询表名定义别名，支持自定义分页字段。部分使用示例见issues。

## version1.7.2
时间类型获取由getDate改为getTimeStamp，现在Date类型装配的值精确到毫秒

## version1.7.1
在未配置数据表映射类的包或者存证重复的表别名定义时，直接阻止启动

# version1.6
当前版本支持增、删（主键模式或字符串条件模式）、改（主键模式，支持联合主键）、查实体、查列表、分页查询。支持=、like、<、<=、>、>=，支持对字段信息进行日期格式化，支持入参日期格式化。后续版本将支持级联查询、聚合函数、缓存等等。
支持在application.yml文件中进行配置：
1、可读取spring配置的数据库信息，当前版本数据库连接使用durid连接池，后续版本将支持自定义连接池；
2、读取查询使用的映射实体类包，启动时扫描所有配置包下的实体类；
3、可配置映射实体类中的非查询字段；
4、支持自定义连接池配置（13项，不配置的使用默认值。可配置属性：initialSize、maxActive、maxWait、timeBetweenEvictionRunsMillis、minEvictableIdleTimeMillis、removeAbandoned、removeAbandonedTimeout、logAbandoned、testWhileIdle、testOnBorrow、testOnReturn、poolPreparedStatements、maxPoolPreparedStatementPerConnectionSize、validationQuery）
除数据库信息外，其他配置示意图见issues

## version1.6.0.1
增加了查询sql语句日志
