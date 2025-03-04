package com.he.common.annotation.sql;

import com.he.common.comment.Comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Comment("SQL中表别名")
public @interface Alias {
    public String value();
}
