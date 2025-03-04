package com.he.common.annotation.sql;

import com.he.common.comment.Comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Comment("SQL语句中查询字段做日期格式化，值为格式")
public @interface SelectDateFormat {
    public String value();
}
