package com.shzisg.mybatis.mapper.auto;

import java.lang.reflect.Method;

public class MethodResolver {
  private final MapperAnnotationAutoBuilder annotationBuilder;
  private Method method;
  
  public MethodResolver(MapperAnnotationAutoBuilder annotationBuilder, Method method) {
    this.annotationBuilder = annotationBuilder;
    this.method = method;
  }
  
  public void resolve() {
//    annotationBuilder.parseStatement(method);
  }
  
}
