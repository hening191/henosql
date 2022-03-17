package com.he.common.annotation.sql;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TableName {
    public String value();
}
