package com.he.common.annotation.sql;

import com.he.common.annotation.comment.Comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Comment("该字段的值在SQL的where条件里对应的字段")
public @interface Condition {
    public String[] value();
}
