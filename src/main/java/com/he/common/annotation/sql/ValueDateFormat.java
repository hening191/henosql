package com.he.common.annotation.sql;

import com.he.common.comment.Comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Comment("值为SimpleDateFormat的格式，如果字段是Date类型，则值在代入SQL中前格式化后再传入")
public @interface ValueDateFormat {
    public String value();
}
