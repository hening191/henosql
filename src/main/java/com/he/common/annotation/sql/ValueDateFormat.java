package com.he.common.annotation.sql;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueDateFormat {
    public String value();
}
