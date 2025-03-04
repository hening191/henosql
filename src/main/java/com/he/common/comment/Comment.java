package com.he.common.comment;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ ElementType.FIELD,ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.CLASS) // 注解在编译时保留
public @interface Comment {
    String value();
}
