package com.demo.springeventstore.core;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author sk
 */
public class Dal {
    private static final Logger log = LoggerFactory.getLogger(Dal.class);
    private SqlSession sqlSession;
    private Configuration configuration;
    private Map<Class<?>, BaseMapper.TableInfo> cachedTableInfo = new ConcurrentHashMap<>();

    private Dal() {}
    private void initSession(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
        this.configuration = sqlSession.getConfiguration();
    }
    private static class Holder {
        private static Dal instance = new Dal();
    }

    public static<T> Executor<T> with(Class<T> clazz) {
        return with(clazz, SpringUtil.getBean(SqlSession.class));
    }
    public static<T> Executor<T> with(Class<T> clazz, SqlSession sqlSession) {
        Dal instance = Holder.instance;
        instance.initSession(sqlSession);
        BaseMapper.TableInfo tableInfo = null;
        if(null != clazz) {
            tableInfo = instance.cachedTableInfo.computeIfAbsent(clazz, BaseMapper.Util::tableInfo);
        }
        return instance.new Executor(tableInfo);
    }

    public static<T> List<T> sql(String sql, Object param, Class<T> resultType) {
        return with(resultType).sqlQuery(sql, param, resultType);
    }

    public class Executor<E> implements BaseMapper<E> {
        BaseMapper.TableInfo table;
        Class<?> resultType;
        Executor(TableInfo table) {
            this.table = table;
            this.resultType = null != table ? table.getEntityClass() : null;
        }
        @Override
        public List<E> query(Function<SQL,SQL> sqlBuild, Object criteria) {
            Map<String, Object> maps = new HashMap<>(2);
            maps.put("sqlBuild", sqlBuild);
            maps.put("entity", criteria);
            String sql = new BaseMapper.SelectBySqlProvider().buildSql(maps, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }
        @Override
        public List<E> selectAll(String criteria) {
            String sql = new BaseMapper.SelectAllSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }
        @Override
        public List<E> select(E criteria) {
            String sql = new BaseMapper.SelectByCriteriaSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }
        @Override
        public E selectById(Serializable criteria) {
            String sql = new BaseMapper.SelectByIdSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }
        @Override
        public E selectOne(E criteria) {
            String sql = new BaseMapper.SelectByCriteriaSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }
        @Override
        public List<E> selectByColumn(String column, Serializable[] criteria) {
            Map<String, Object> maps = new HashMap<>(2);
            maps.put("column", column);
            maps.put("ids", criteria);
            String sql = new BaseMapper.SelectInSqlProvider().buildSql(maps, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, criteria);
        }
        @Override
        public Long count(E criteria) {
            String sql = new BaseMapper.CountByCriteriaSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), resultType, SqlCommandType.SELECT);
            return sqlSession.selectOne(msId, criteria);
        }
        @Override
        public Integer insert(E criteria) {
            String sql = new BaseMapper.InsertSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.INSERT);
            return sqlSession.insert(msId, criteria);
        }
        @Override
        public Integer updateById(E criteria) {
            String sql = new BaseMapper.UpdateSelectiveSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.UPDATE);
            return sqlSession.update(msId, criteria);
        }
        @Override
        public Integer update(E criteria) {
            String sql = new BaseMapper.UpdateSelectiveSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.UPDATE);
            return sqlSession.update(msId, criteria);
        }
        @Override
        public Integer delete(E criteria) {
            String sql = new BaseMapper.DeleteSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.DELETE);
            return sqlSession.delete(msId, criteria);
        }
        @Override
        public Integer deleteById(Serializable criteria) {
            String sql = new BaseMapper.DeleteSqlProvider().buildSql(criteria, this.table);
            String msId = execute(sql, table.getEntityClass(), int.class, SqlCommandType.DELETE);
            return sqlSession.delete(msId, criteria);
        }
        public List<E> sqlQuery(String sql, Object param, Class<?> resultType) {
            return sqlQuery(sql, param, param != null ? param.getClass() : Map.class, resultType);
        }
        public List<E> sqlQuery(String sql, Object param, Class<?> paramType, Class<?> resultType) {
            String msId = execute(sql, paramType, resultType, SqlCommandType.SELECT);
            return sqlSession.selectList(msId, param);
        }
        @Override
        public boolean exist(E criteria) {
            List<E> ret = select(criteria);
            return ret != null && !ret.isEmpty();
        }
        @Override
        public List<HashMap<?,?>> sqlQuery(String sql) {
            throw new RuntimeException("Not Support");
        }
        @Override
        public Integer batchInsert(List<E> es) {
            throw new RuntimeException("Not Support");
        }

    }

    private String execute(String sql, Class<?> parameterType, Class<?> resultType, SqlCommandType sqlCommandType) {
        String msId = sqlCommandType.toString() + "." + parameterType.getName() + "." + sql.hashCode();
        log.debug("Run sql : {}", sql);
        if (configuration.hasStatement(msId, false)) {
            return msId;
        }
        SqlSource sqlSource = configuration
                .getDefaultScriptingLanguageInstance()
                .createSqlSource(configuration, sql, parameterType);
        // cache MappedStatement
        newMappedStatement(msId, sqlSource, resultType, sqlCommandType);
        return msId;
    }

    private void newMappedStatement(String msId, SqlSource sqlSource, final Class<?> resultType, SqlCommandType sqlCommandType) {
        MappedStatement ms = new MappedStatement.Builder(configuration, msId, sqlSource, sqlCommandType)
                .resultMaps(Collections.singletonList(
                        new ResultMap.Builder(configuration, "defaultResultMap", resultType, new ArrayList<>(0)).build()
                ))
                .build();
        configuration.addMappedStatement(ms);
    }

}
