package tech.wetech.flexmodel.sql;

import tech.wetech.flexmodel.*;
import tech.wetech.flexmodel.sql.dialect.SqlDialect;

import java.util.*;

import static tech.wetech.flexmodel.Query.Join.JoinType.INNER_JOIN;
import static tech.wetech.flexmodel.Query.Join.JoinType.LEFT_JOIN;

/**
 * @author cjbi
 */
class SqlHelper {

  public static String toQuerySql(SqlContext sqlContext, String modelName, Query query) {
    Map.Entry<String, Map<String, Object>> entry = toQuerySql(sqlContext, modelName, query, false);
    return entry.getKey();
  }

  public static Map.Entry<String, Map<String, Object>> toQuerySqlWithPrepared(SqlContext sqlContext, String modelName, Query query) {
    return toQuerySql(sqlContext, modelName, query, true);
  }

  private static Map.Entry<String, Map<String, Object>> toQuerySql(SqlContext sqlContext, String modelName, Query query, boolean prepared) {
    String sqlString;
    Map<String, Object> params = new HashMap<>();
    SqlDialect sqlDialect = sqlContext.getSqlDialect();
    Model model = sqlContext.getMappedModels().getModel(sqlContext.getSchemaName(), modelName);
    String physicalFromTableName = toPhysicalTableNameQuoteString(sqlContext, modelName);
    StringBuilder sql = new StringBuilder("\nselect ");
    Query.Projection projection = query.projection();
    Map<String, String> aliasColumnMap = new HashMap<>();
    Map<String, AssociationField> associationFields = QueryHelper.findAssociationFields(model, query);
    StringJoiner columns = new StringJoiner(", ");
    if (projection != null) {
      for (Map.Entry<String, Query.QueryCall> entry : projection.fields().entrySet()) {
        Query.QueryCall value = entry.getValue();
        String key = entry.getKey();
        if (associationFields.containsKey(key)) {
          // 不查关联字段
          continue;
        }
        String sqlCall = toSqlCall(sqlContext, value);
        aliasColumnMap.put(key, sqlCall);
        columns.add("\n " + sqlCall + " " + sqlDialect.quoteIdentifier(key));
      }
    } else {
      for (Field field : model.fields()) {
        if (associationFields.containsKey(field.name())) {
          // 不查关联字段
          continue;
        }
        columns.add("\n " + toFullColumnQuoteString(sqlContext, modelName, field.name()) + " " + sqlDialect.quoteIdentifier(field.name()));
      }
    }
    sql.append(columns);
    sql.append("\nfrom ").append(physicalFromTableName);
    Query.Joins joins = query.joiners();
    if (joins != null) {
      for (Query.Join joiner : joins.joins()) {
        String physicalTableName = toPhysicalTableNameQuoteString(sqlContext, joiner.from());
        StringBuilder joinCondition = new StringBuilder();
        if (joiner.filter() != null) {
          if (prepared) {
            SqlClauseResult leftSqlWhere = toSqlWhereClauseWithPrepared(sqlContext, joiner.filter());
            joinCondition.append(" and ")
              .append(leftSqlWhere.sqlClause());
            params.putAll(leftSqlWhere.args());
          } else {
            joinCondition.append(" and ")
              .append(toSqlWhereClause(sqlContext, joiner.filter()));
          }
        }
        String joinCause = physicalTableName + " \n on \n"
                           + toFullColumnQuoteString(sqlContext, modelName, joiner.localField())
                           + "=" + toFullColumnQuoteString(sqlContext, joiner.from(), joiner.foreignField())
                           + joinCondition;
        if (joiner.joinType() == LEFT_JOIN) {
          sql.append("\nleft join ").append(joinCause);
        }
        if (joiner.joinType() == INNER_JOIN) {
          sql.append("\ninner join ").append(joinCause);
        }
      }
    }
    if (query.filter() != null) {
      if (prepared) {
        SqlClauseResult sqlClauseResult = toSqlWhereClauseWithPrepared(sqlContext, query.filter());
        sql.append("\nwhere (").append(sqlClauseResult.sqlClause()).append(")");
        params.putAll(sqlClauseResult.args());
      } else {
        sql.append("\nwhere (").append(toSqlWhereClause(sqlContext, query.filter())).append(")");
      }
    }
    if (query.groupBy() != null) {
      sql.append("\ngroup by ");
      StringJoiner groupByColumns = new StringJoiner(", ");
      for (Query.QueryField field : query.groupBy().fields()) {
        groupByColumns.add(sqlDialect.supportsGroupByColumnAlias()
          ? toFullColumnQuoteString(sqlContext, field.modelName(), field.fieldName())
          : aliasColumnMap.getOrDefault(field.fieldName(), toFullColumnQuoteString(sqlContext, field.modelName(), field.fieldName())));
      }
      sql.append(groupByColumns);
    }
    Query.Sort sort = query.sort();
    if (sort != null) {
      sql.append("\norder by ");
      StringJoiner sortColumns = new StringJoiner(", ");
      for (Query.Sort.Order order : sort.orders()) {
        sortColumns.add(toFullColumnQuoteString(sqlContext, order.field().modelName(), order.field().fieldName()) + " " + order.direction().name().toLowerCase());
      }
      sql.append(sortColumns);
    }
    if (query.limit() != null) {
      sqlString = sqlDialect.getLimitString(sql.toString(),
        Objects.toString(query.offset(), null),
        query.limit().toString());
    } else {
      sqlString = sql.toString();
    }
    return new AbstractMap.SimpleEntry<>(sqlString, params);
  }

  private static String toSqlCall(SqlContext sqlContext, Query.QueryCall queryCall) {
    SqlDialect sqlDialect = sqlContext.getSqlDialect();
    if (queryCall instanceof Query.QueryField field) {
      return toFullColumnQuoteString(sqlContext, field.modelName(), field.fieldName());
    } else if (queryCall instanceof Query.QueryFunc func) {
      List<String> arguments = new ArrayList<>();
      for (Object arg : func.args()) {
        if (arg instanceof Query.QueryCall callArg) {
          arguments.add(toSqlCall(sqlContext, callArg));
        } else {
          arguments.add(arg instanceof String str ? "'" + str + "'" : arg.toString());
        }
      }
      return sqlDialect.getFunctionString(func.operator(), arguments.toArray(String[]::new));
    } else if (queryCall instanceof Query.QueryValue queryValue) {
      return "'" + queryValue.value() + "'";
    }
    return null;
  }

  private static String toSqlWhereClause(SqlContext sqlContext, String condition) {
    SqlExpressionCalculator conditionCalculator = sqlContext.getConditionCalculator();
    try {
      return conditionCalculator.calculateIncludeValue(condition);
    } catch (ExpressionCalculatorException e) {
      throw new SqlExecutionException("calculate sql where error", e);
    }
  }

  private static SqlClauseResult toSqlWhereClauseWithPrepared(SqlContext sqlContext, String condition) {
    SqlExpressionCalculator conditionCalculator = sqlContext.getConditionCalculator();
    try {
      return conditionCalculator.calculate(condition, null);
    } catch (ExpressionCalculatorException e) {
      throw new SqlExecutionException("calculate sql where error", e);
    }
  }

  private static String toFullColumnQuoteString(SqlContext sqlContext, String modelName, String fieldName) {
    SqlDialect sqlDialect = sqlContext.getSqlDialect();
    PhysicalNamingStrategy physicalNamingStrategy = sqlContext.getPhysicalNamingStrategy();
    if (modelName == null) {
      return sqlDialect.quoteIdentifier(fieldName);
    }
    return sqlDialect.quoteIdentifier(physicalNamingStrategy.toPhysicalTableName(modelName)) + "." + sqlDialect.quoteIdentifier(fieldName);
  }

  private static String toPhysicalTableNameQuoteString(SqlContext sqlContext, String name) {
    SqlDialect sqlDialect = sqlContext.getSqlDialect();
    PhysicalNamingStrategy physicalNamingStrategy = sqlContext.getPhysicalNamingStrategy();
    return sqlDialect.quoteIdentifier(physicalNamingStrategy.toPhysicalTableName(name));
  }

}
