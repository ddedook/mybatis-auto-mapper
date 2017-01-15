package com.shzisg.mybatis.mapper.auto;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OrderBy {
  
  String value();
  
  boolean desc() default true;
  
  String orderSql() default "";
}
