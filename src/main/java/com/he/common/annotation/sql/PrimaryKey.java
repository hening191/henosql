package com.he.common.annotation.sql;

import com.he.common.comment.Comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Comment("数据表主键")
public @interface PrimaryKey {
}
