package com.shzisg.mybatis.mapper.concurrent;

import com.shzisg.mybatis.mapper.auto.AutoMapperMethod;
import com.shzisg.mybatis.mapper.page.Page;
import com.shzisg.mybatis.mapper.page.PageRequest;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author harris
 */
public class SelectManyTask implements Callable {
    
    private SqlSession sqlSession;
    private AutoMapperMethod.SqlCommand command;
    private AutoMapperMethod.MethodSignature method;
    private Object[] args;
    
    public SelectManyTask(SqlSession sqlSession,
                          AutoMapperMethod.SqlCommand command,
                          AutoMapperMethod.MethodSignature method,
                          Object[] args) {
        this.sqlSession = sqlSession;
        this.command = command;
        this.method = method;
        this.args = args;
    }
    
    @Override
    public Object call() throws Exception {
        List<?> result;
        Object param = method.convertArgsToSqlCommandParam(args);
        if (method.hasRowBounds()) {
            RowBounds rowBounds = method.extractRowBounds(args);
            result = sqlSession.selectList(command.getName(), param, rowBounds);
        } else {
            result = sqlSession.selectList(command.getName(), param);
        }
        // issue #510 Collections & arrays support
        if (!method.getReturnRealType().isAssignableFrom(result.getClass())) {
            if (method.getReturnRealType().isArray()) {
                return AutoMapperMethod.convertToArray(result, method);
            } else if (Page.class.isAssignableFrom(method.getReturnRealType())) {
                PageRequest request = null;
                for (Object arg : args) {
                    if (arg instanceof PageRequest) {
                        request = (PageRequest) arg;
                    }
                }
                if (request == null) {
                    throw new BindingException("method " + command.getName() + " need Request parameter.");
                }
                return Page.from(result, request);
            } else {
                return AutoMapperMethod.convertToDeclaredCollection(sqlSession.getConfiguration(), result, method);
            }
        }
        return result;
    }
}
