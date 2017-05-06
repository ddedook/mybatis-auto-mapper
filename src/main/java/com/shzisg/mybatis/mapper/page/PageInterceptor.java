package com.shzisg.mybatis.mapper.page;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class PageInterceptor implements Interceptor {
    
    private final Logger logger = LoggerFactory.getLogger(PageInterceptor.class);
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        ParameterHandler parameterHandler = (ParameterHandler) metaObject.getValue("parameterHandler");
        Object parameterObject = parameterHandler.getParameterObject();
        PageRequest pageRequest = null;
        if (parameterObject instanceof MapperMethod.ParamMap) {
            MapperMethod.ParamMap paramMapObject = (MapperMethod.ParamMap) parameterObject;
            for (Object key : paramMapObject.keySet()) {
                if (paramMapObject.get(key) instanceof PageRequest) {
                    pageRequest = (PageRequest) paramMapObject.get(key);
                    break;
                }
            }
        } else if (parameterObject instanceof PageRequest) {
            pageRequest = (PageRequest) parameterObject;
        }
        
        if (pageRequest != null) {
            if (pageRequest.getPage() <= 0) {
                throw new RuntimeException("Page should start with 1");
            }
            BoundSql boundSql = (BoundSql) metaObject.getValue("parameterHandler.boundSql");
            String originalSql = boundSql.getSql();
            int fromIndex = originalSql.indexOf("from");
            if (fromIndex == -1) {
                fromIndex = originalSql.indexOf("FROM");
            }
            String countSql = "select count(1) " + originalSql.substring(fromIndex);
            Connection connection = (Connection) invocation.getArgs()[0];
            MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
            long total = getTotal(countSql, connection, mappedStatement, boundSql);
            pageRequest.context = total;
            if (total == 0) {
                return invocation.proceed();
            }
            StringBuilder pageSqlBuilder = new StringBuilder(originalSql);
            if (pageRequest.getOrderBy() != null && !pageRequest.getOrderBy().isEmpty()) {
                pageSqlBuilder
                    .append(" order by ")
                    .append(pageRequest.getOrderBy())
                    .append(pageRequest.isAsc() ? " asc" : " desc");
            }
            int offset = pageRequest.getSize() * (pageRequest.getPage() - 1);
            pageSqlBuilder.append(" limit ")
                .append(offset)
                .append(",")
                .append(pageRequest.getSize());
            metaObject.setValue("delegate.boundSql.sql", pageSqlBuilder.toString());
            return invocation.proceed();
        }
        return invocation.proceed();
    }
    
    private long getTotal(String sql, Connection connection, MappedStatement mappedStatement, BoundSql boundSql) {
        PreparedStatement preparedStmt = null;
        ResultSet rs = null;
        try {
            preparedStmt = connection.prepareStatement(sql);
            BoundSql countBS = new BoundSql(mappedStatement.getConfiguration(), sql,
                boundSql.getParameterMappings(), boundSql.getParameterObject());
            ParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement,
                boundSql.getParameterObject(), countBS);
            parameterHandler.setParameters(preparedStmt);
            rs = preparedStmt.executeQuery();
            long total = 0;
            if (rs.next()) {
                total = rs.getLong(1);
            }
            return total;
        } catch (SQLException e) {
            logger.error("Find result count error", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (preparedStmt != null) {
                    preparedStmt.close();
                }
            } catch (SQLException e) {
                logger.error("Find result count error", e);
            }
        }
        return 0;
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
    }
}
