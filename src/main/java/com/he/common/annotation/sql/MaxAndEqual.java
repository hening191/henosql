package com.he.common.annotation.sql;

import com.he.common.comment.Comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Comment("该字段的最大值，即SQL条件中'<='")
public @interface MaxAndEqual {
    public String value();
}
