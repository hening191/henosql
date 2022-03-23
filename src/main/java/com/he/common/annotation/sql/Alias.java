package com.he.common.annotation.sql;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Alias {
    public String value();
}
