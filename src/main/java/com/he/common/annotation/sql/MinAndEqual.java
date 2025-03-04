package com.he.common.annotation.sql;

import com.he.common.comment.Comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Comment("该字段的最小值，即SQL条件中'>='")
public @interface MinAndEqual {
    public String value();
}
