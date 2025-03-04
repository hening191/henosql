package com.he.common.annotation.sql;

import com.he.common.comment.Comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Comment("数据库表名")
public @interface TableName {
    public String value();
}
