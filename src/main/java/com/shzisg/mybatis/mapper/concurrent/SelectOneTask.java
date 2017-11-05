package com.shzisg.mybatis.mapper.concurrent;

import org.apache.ibatis.session.SqlSession;

import java.util.concurrent.Callable;

/**
 * @author harris
 */
public class SelectOneTask implements Callable {
    
    private SqlSession sqlSession;
    private String commandName;
    private Object param;
    
    public SelectOneTask(SqlSession sqlSession, String commandName, Object param) {
        this.sqlSession = sqlSession;
        this.commandName = commandName;
        this.param = param;
    }
    
    @Override
    public Object call() throws Exception {
        return sqlSession.selectOne(commandName, param);
    }
}
