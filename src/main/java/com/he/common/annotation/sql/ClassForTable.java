package com.he.common.annotation.sql;

import com.he.common.comment.Comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Comment("字段别名跟随注解的Class对应的表别名")
public @interface ClassForTable {
    public Class<?> value();
}
