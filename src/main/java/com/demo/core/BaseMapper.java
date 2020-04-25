package com.demo.core;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.jdbc.AbstractSQL;
import org.apache.ibatis.mapping.SqlCommandType;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author sk
 * @param <Entity>
 */
public interface BaseMapper<Entity> {

    @InsertProvider(type = InsertSqlProvider.class, method = "invoke")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Integer insert(Entity entity);

    @InsertProvider(type = BatchInsertSqlProvider.class, method = "invoke")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Integer batchInsert(List<Entity> entities);

    @UpdateProvider(type = UpdateSqlProvider.class, method = "invoke")
    Integer updateById(Entity entity);

    @UpdateProvider(type = UpdateSelectiveSqlProvider.class, method = "invoke")
    Integer update(Entity entity);

    @DeleteProvider(type = DeleteSqlProvider.class, method = "invoke")
    Integer deleteById(Serializable id);

    @DeleteProvider(type = DeleteByCriteriaSqlProvider.class, method = "invoke")
    Integer delete(Entity criteria);

    @SelectProvider(type = SelectByIdSqlProvider.class, method = "invoke")
    Entity selectById(Serializable id);

    @SelectProvider(type = SelectAllSqlProvider.class, method = "invoke")
    List<Entity> selectAll(String orderBy);

    @SelectProvider(type = SelectByCriteriaSqlProvider.class, method = "invoke")
    List<Entity> select(Entity criteria);

    @SelectProvider(type = SelectByCriteriaSqlProvider.class, method = "invoke")
    Entity selectOne(Entity criteria);

    @SelectProvider(type = SelectInSqlProvider.class, method = "invoke")
    List<Entity> selectByColumn(@Param("column") String column, @Param("ids") Serializable[] ids);

    @SelectProvider(type = CountByCriteriaSqlProvider.class, method = "invoke")
    Long count(Entity criteria);

    @SelectProvider(type = SelectBySqlProvider.class, method = "invoke")
    List<Entity> query(@Param("sqlBuild") Function<SQL, SQL> sqlBuild, @Param("entity") Object criteria);

    @Select("${sql}")
    List<HashMap<?,?>> sqlQuery(String sql);


    default List<Entity> selectByIds(@Param("ids") Serializable[] ids) {
        return selectByColumn("id", ids);
    }

    default boolean exist(Entity criteria) {
        List<Entity> list = select(criteria);
        return list != null && list.size() > 0;
    }

    default Integer upsert(Entity criteria) {
        return exist(criteria) ? updateById(criteria) : insert(criteria);
    }


    class InsertSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object criteria,ProviderContext context) {
            return new SQL()
                    .INSERT_INTO(table.getTableName())
                    .INTO_COLUMNS(table.getColumns())
                    .INTO_VALUES(Stream.of(table.getFields()).map(this::bindParameter).toArray(String[]::new))
                    .commandType(SqlCommandType.INSERT);
        }
    }
    @SuppressWarnings("all")
    class BatchInsertSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object entities, ProviderContext context) {
            int size = ((List)((Map)entities).get("list")).size();
            String value = "(" + String.join(",", Stream.of(table.getFields()).map(this::bindParameter).toArray(String[]::new)) + ")";
            String[] values = new String[size];
            Arrays.fill(values, value);

            return new SQL()
                    .INSERT_INTO(table.getTableName())
                    .INTO_COLUMNS(table.getColumns())
                    .INTO_VALUES(values)
                    .commandType(SqlCommandType.INSERT);

//            return sql.toString() + " VALUES " + String.join(",", values);
        }
    }

    @SuppressWarnings("all")
    class UpdateSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object criteria, ProviderContext context) {
            return new SQL()
                    .UPDATE(table.getTableName())
                    .SET(Stream.of(table.getFields())
                            .filter(field -> !table.getPrimaryKeyColumn().equals(columnName(field)))
                            .map(field -> columnName(field) + " = " + bindParameter(field)).toArray(String[]::new))
                    .WHERE(table.getPrimaryKeyColumn() + " = #{id}")
                    .commandType(SqlCommandType.UPDATE);
        }
    }

    @SuppressWarnings("all")
    class UpdateSelectiveSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object entity, ProviderContext context) {
            return new SQL()
                    .UPDATE(table.getTableName())
                    .SET(Stream.of(table.getFields())
                            .filter(field -> value(entity, field) != null && !table.getPrimaryKeyColumn().equals(columnName(field)))
                            .map(field -> columnName(field) + " = " + bindParameter(field)).toArray(String[]::new))
                    .WHERE(table.getPrimaryKeyColumn() + " = #{id}")
                    .commandType(SqlCommandType.UPDATE);
        }
    }

    class DeleteSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object criteria, ProviderContext context) {
            return new SQL()
                    .DELETE_FROM(table.getTableName())
                    .WHERE(table.getPrimaryKeyColumn() + " = #{id}")
                    .commandType(SqlCommandType.DELETE);
        }
    }

    class DeleteByCriteriaSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object criteria, ProviderContext context) {
            return new SQL()
                    .DELETE_FROM(table.getTableName())
                    .WHERE(Stream.of(table.getFields())
                            .filter(field -> value(criteria, field) != null)
                            .map(field -> columnName(field) + " = " + bindParameter(field))
                            .toArray(String[]::new))
                    .commandType(SqlCommandType.DELETE);
        }
    }

    class SelectByIdSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object criteria, ProviderContext context) {
            return new SQL()
                    .SELECT(table.getSelectColumns())
                    .FROM(table.getTableName())
                    .WHERE(table.getPrimaryKeyColumn() + " = #{id}")
                    .commandType(SqlCommandType.SELECT);
        }
    }

    class SelectAllSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object criteria, ProviderContext context) {
            String orderBy = (String)criteria;
            SQL sql = new SQL()
                    .SELECT(table.getSelectColumns())
                    .FROM(table.getTableName());
            if (Util.isEmpty(orderBy)) {
                orderBy = table.getPrimaryKeyColumn() + " DESC";
            }
            return sql.ORDER_BY(orderBy).commandType(SqlCommandType.SELECT);
        }
    }
    @SuppressWarnings("all")
    class SelectInSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object entities, ProviderContext context) {
            Map<String, Object> param = (Map)entities;
            String inField = (String)param.get("column");
            String idStr = "(" + String.join(",", Arrays.stream((Serializable[])param.get("ids")).map(String::valueOf).toArray(String[]::new)) + ")";
            SQL sql = new SQL()
                    .SELECT(table.getSelectColumns())
                    .FROM(table.getTableName())
                    .WHERE((inField != null ? inField : table.getPrimaryKeyColumn()) + " IN " + idStr);

            return sql.commandType(SqlCommandType.SELECT);
        }
    }

    class SelectByCriteriaSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object criteria, ProviderContext context) {
            return new SQL()
                    .SELECT(table.getSelectColumns())
                    .FROM(table.getTableName())
                    .WHERE(Stream.of(table.getFields())
                            .filter(field -> value(criteria, field) != null)
                            .map(field -> columnName(field) + " = " + bindParameter(field))
                            .toArray(String[]::new))
                    .ORDER_BY(table.getPrimaryKeyColumn() + " DESC")
                    .commandType(SqlCommandType.SELECT);
        }
    }

    class SelectBySqlProvider extends AbstractSqlProviderSupport {
        @SuppressWarnings("all")
        @Override
        public SQL sql(Object entities, ProviderContext context) {
            Map<String, Object> param = (Map)entities;
            Function<SQL,SQL> sqlBuild = (Function)param.get("sqlBuild");
            Object criteria = param.get("entity");

            return sqlBuild.apply(
                    new SQL()
                            .FROM(table.getTableName())
                            .commandType(SqlCommandType.SELECT)
            );
        }
    }

    class CountByCriteriaSqlProvider extends AbstractSqlProviderSupport {
        @Override
        public SQL sql(Object criteria, ProviderContext context) {
            return new SQL()
                    .SELECT("COUNT(*)")
                    .FROM(table.getTableName())
                    .WHERE(Stream.of(table.getFields())
                            .filter(field -> value(criteria, field) != null)
                            .map(field -> columnName(field) + " = " + bindParameter(field))
                            .toArray(String[]::new))
                    .commandType(SqlCommandType.SELECT);
        }
    }

    interface Interceptor {
        /** 类似 @PrePersist **/
        default void prePersist(){}
    }

    class SQL extends AbstractSQL<SQL> {
        SqlCommandType sqlCommandType;
        public SQL commandType(SqlCommandType sqlCommandType) {
            this.sqlCommandType = sqlCommandType;
            return this;
        }
        public SQL WHERE(boolean test, String condition) {
            return test ? super.WHERE(condition) : this;
        }
        @Override
        public SQL getSelf() {
            return this;
        }
    }

    abstract class AbstractSqlProviderSupport {
        private static Map<Class<?>, TableInfo> tableCache = new ConcurrentHashMap<>(256);

        abstract SQL sql(Object criteria, ProviderContext context);

        protected TableInfo table;

        public String invoke(Object criteria, ProviderContext context) {
            return buildSql(criteria, tableInfo(context));
        }

        public String buildSql(Object criteria , TableInfo table) {
            this.table = table;
            SQL sql = sql(criteria, null);
            beforeInterceptor(criteria, sql);
            return sql.toString();
        }

        public void beforeInterceptor(Object obj, SQL sql) {
            if(obj instanceof Interceptor && sql.sqlCommandType != SqlCommandType.SELECT) {
                ((Interceptor)obj).prePersist();
            }
        }

        /**
         * 获取并缓存表信息结构
         */
        TableInfo tableInfo(ProviderContext context) {
            return tableCache.computeIfAbsent(context.getMapperType(), t-> Util.tableInfo(entityType(context)));
        }

        /**
         * 获取BaseMapper接口中的泛型类型
         * @param context ProviderContext
         * @return clz
         */
        Class<?> entityType(ProviderContext context) {
            return Stream.of(context.getMapperType().getGenericInterfaces())
                    .filter(ParameterizedType.class::isInstance)
                    .map(ParameterizedType.class::cast)
                    .filter(type -> type.getRawType() == BaseMapper.class)
                    .findFirst()
                    .map(type -> type.getActualTypeArguments()[0])
                    .filter(Class.class::isInstance)
                    .map(Class.class::cast)
                    .orElseThrow(() -> new IllegalStateException("未找到BaseMapper的泛型类 " + context.getMapperType().getName() + "."));
        }

        String bindParameter(Field field) {
            return String.format("#{%s}", field.getName());
        }

        String columnName(Field field) {
            return Util.columnName(field);
        }

        Object value(Object bean, Field field) {
            return Util.value(bean, field);
        }
    }

    class Util {

        static TableInfo tableInfo(Class<?> entityClass) {
            //获取不含有@NoColumn注解的fields
            Field[] fields = excludeNoColumnField(Util.getFields(entityClass, null));
            TableInfo info = new TableInfo();
            info.setEntityClass(entityClass);
            info.setFields(fields);
            info.setTableName(tableName(entityClass));
            info.setPrimaryKeyColumn(primaryKeyColumn(fields, "id"));
            info.setColumns(columns(fields));
            info.setSelectColumns(selectColumns(fields));
            return info;
        }

        synchronized static Object value(Object bean, Field field) {
            if(null == bean) {
                return null;
            }
            try {
                field.setAccessible(true);
                return field.get(bean);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            } finally {
                field.setAccessible(false);
            }
        }
        /**
         * 如果fields中含有@Primary的字段，则返回该字段名为主键，否则默认'id'为主键名
         * @param fields entityClass所有fields
         * @return  主键column(驼峰转为下划线)
         */
        static String primaryKeyColumn(Field[] fields, String defaultPrimaryKey) {
            return Stream.of(fields).filter(field -> field.isAnnotationPresent(Id.class))
                    .findFirst()    //返回第一个primaryKey的field
                    .map(Util::columnName)
                    .orElse(defaultPrimaryKey);
        }

        static String tableName(Class<?> entityType) {
            // table prefix todo
            return "" + camel2Underscore(entityType.getSimpleName());
        }

        /**
         * 获取所有pojo所有属性对应的数据库字段 (不包含pojo中含有@NoColumn主键的属性)
         *
         * @param fields entityClass所有fields
         * @return str[]
         */
        static String[] columns(Field[] fields) {
            return Stream.of(fields).map(Util::columnName).toArray(String[]::new);
        }

        /**
         * 过滤含有@NoColumn注解的field
         * @param totalField  entityClass所有的字段
         * @return   不包含@NoColumn注解的fields
         */
        static Field[] excludeNoColumnField(Field[] totalField) {
            return Stream.of(totalField)
                    //过滤含有@NoColumn注解的field
                    .filter(field -> !field.isAnnotationPresent(Transient.class))
                    .toArray(Field[]::new);
        }

        /**
         * 获取查询对应的字段 (不包含pojo中含有@NoColumn主键的属性)
         *
         * @param fields p
         * @return str[]
         */
        static String[] selectColumns(Field[] fields) {
            return Stream.of(fields).map(Util::selectColumnName).toArray(String[]::new);
        }

        /**
         * 获取单个属性对应的数据库字段（带有下划线字段将其转换为"字段 AS pojo属性名"形式）
         *
         * @param field Field
         * @return str
         */
        static String selectColumnName(Field field) {
            return " `"+columnName(field) + "` AS `" + field.getName() +"` ";
        }

        /**
         * 获取单个属性对应的数据库字段
         *
         * @param field  entityClass中的field
         * @return str
         */
        static String columnName(Field field) {
            if(field.isAnnotationPresent(Column.class)) {
                return field.getAnnotation(Column.class).name();
            }
            return camel2Underscore(field.getName());
        }

        static boolean isEmpty(String str) {
            return str == null || str.trim().length() == 0;
        }

        /**
         * 驼峰模式字符串转换为下划线字符串
         *
         * @param camelStr str
         * @return str
         */
        static String camel2Underscore(String camelStr) {
            return convertCamel(camelStr, '_');
        }

        /**
         * 转换驼峰字符串为指定分隔符的字符串 <br/>
         * 如：camelStr:"UserInfo"    separator:'_' <br/>
         * return "user_info"
         *
         * @param camelStr  驼峰字符串
         * @param separator 分隔符
         * @return str
         */
        static String convertCamel(String camelStr, char separator) {
            if (isEmpty(camelStr)) {
                return camelStr;
            }
            StringBuilder out = new StringBuilder();
            char[] strChar = camelStr.toCharArray();
            for (int i = 0, len = strChar.length; i < len; i++) {
                char c = strChar[i];
                if (!Character.isLowerCase(c)) {
                    //如果是首字符，则不需要添加分隔符
                    if (i == 0) {
                        out.append(Character.toLowerCase(c));
                        continue;
                    }
                    out.append(separator).append(Character.toLowerCase(c));
                    continue;
                }
                out.append(c);
            }
            return out.toString();
        }

        /**
         * 获取指定类的所有的field,包括父类
         *
         * @param clazz       字段所属类型
         * @param fieldFilter 字段过滤器
         * @return 符合过滤器条件的字段数组
         */
        static Field[] getFields(Class<?> clazz, Predicate<Field> fieldFilter) {
            List<Field> fields = new ArrayList<>(32);
            while (Object.class != clazz && clazz != null) {
                // 获得该类所有声明的字段，即包括public、private和protected，但是不包括父类的申明字段，
                // getFields：获得某个类的所有的公共（public）的字段，包括父类中的字段
                for (Field field : clazz.getDeclaredFields()) {
                    if (fieldFilter != null && !fieldFilter.test(field)) {
                        continue;
                    }
                    fields.add(field);
                }
                clazz = clazz.getSuperclass();
            }
            return fields.toArray(new Field[0]);
        }
    }


    class TableInfo {
        /**
         * 表对应的实体类型
         */
        private Class<?> entityClass;

        /**
         * 实体类型不含@NoColunm注解的field
         */
        private Field[] fields;

        /**
         * 表名
         */
        private String tableName;

        /**
         * 主键列名
         */
        private String primaryKeyColumn;

        /**
         * 所有列名
         */
        private String[] columns;

        /**
         * 所有select sql的列名，有带下划线的将其转为aa_bb AS aaBb
         */
        private String[] selectColumns;

        void setEntityClass(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        Field[] getFields() {
            return fields;
        }

        void setFields(Field[] fields) {
            this.fields = fields;
        }

        String getTableName() {
            return tableName;
        }

        void setTableName(String tableName) {
            this.tableName = tableName;
        }

        String getPrimaryKeyColumn() {
            return primaryKeyColumn;
        }

        void setPrimaryKeyColumn(String primaryKeyColumn) {
            this.primaryKeyColumn = primaryKeyColumn;
        }

        String[] getColumns() {
            return columns;
        }

        void setColumns(String[] columns) {
            this.columns = columns;
        }

        String[] getSelectColumns() {
            return selectColumns;
        }

        void setSelectColumns(String[] selectColumns) {
            this.selectColumns = selectColumns;
        }

        public Class<?> getEntityClass() {
            return entityClass;
        }
    }
}