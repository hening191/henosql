package com.he.common.annotation.sql;

import com.he.common.comment.Comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Comment("SQL语句中where该字段条件为‘like’，而非‘=’")
public @interface SqlFieldLike {
}
