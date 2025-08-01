/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.calcite.utils;

import static org.apache.calcite.sql.type.SqlTypeUtil.createArrayType;
import static org.apache.calcite.sql.type.SqlTypeUtil.createMapType;
import static org.opensearch.sql.calcite.utils.OpenSearchTypeFactory.*;
import static org.opensearch.sql.calcite.utils.OpenSearchTypeFactory.ExprUDT.*;

import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.enumerable.NotNullImplementor;
import org.apache.calcite.adapter.enumerable.NullPolicy;
import org.apache.calcite.adapter.enumerable.RexToLixTranslator;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.impl.AggregateFunctionImpl;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlUserDefinedAggFunction;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.util.Optionality;
import org.opensearch.sql.calcite.type.AbstractExprRelDataType;
import org.opensearch.sql.calcite.udf.UserDefinedAggFunction;
import org.opensearch.sql.data.model.ExprValueUtils;
import org.opensearch.sql.data.type.ExprType;
import org.opensearch.sql.executor.QueryType;
import org.opensearch.sql.expression.function.FunctionProperties;
import org.opensearch.sql.expression.function.ImplementorUDF;
import org.opensearch.sql.expression.function.UDFOperandMetadata;

public class UserDefinedFunctionUtils {
  public static final RelDataType NULLABLE_DATE_UDT = TYPE_FACTORY.createUDT(EXPR_DATE, true);
  public static final RelDataType NULLABLE_TIME_UDT = TYPE_FACTORY.createUDT(EXPR_TIME, true);
  public static final RelDataType NULLABLE_TIMESTAMP_UDT =
      TYPE_FACTORY.createUDT(ExprUDT.EXPR_TIMESTAMP, true);
  public static final RelDataType NULLABLE_STRING =
      TYPE_FACTORY.createTypeWithNullability(TYPE_FACTORY.createSqlType(SqlTypeName.VARCHAR), true);

  public static RelDataType nullablePatternAggList =
      createArrayType(
          TYPE_FACTORY,
          TYPE_FACTORY.createMapType(
              TYPE_FACTORY.createSqlType(SqlTypeName.VARCHAR),
              TYPE_FACTORY.createSqlType(SqlTypeName.ANY)),
          true);
  public static RelDataType patternStruct =
      createMapType(
          TYPE_FACTORY,
          TYPE_FACTORY.createSqlType(SqlTypeName.VARCHAR),
          TYPE_FACTORY.createSqlType(SqlTypeName.ANY),
          false);
  public static RelDataType tokensMap =
      TYPE_FACTORY.createMapType(
          TYPE_FACTORY.createSqlType(SqlTypeName.VARCHAR),
          createArrayType(TYPE_FACTORY, TYPE_FACTORY.createSqlType(SqlTypeName.VARCHAR), false));
  public static Set<String> SINGLE_FIELD_RELEVANCE_FUNCTION_SET =
      ImmutableSet.of("match", "match_phrase", "match_bool_prefix", "match_phrase_prefix");
  public static Set<String> MULTI_FIELDS_RELEVANCE_FUNCTION_SET =
      ImmutableSet.of("simple_query_string", "query_string", "multi_match");

  public static RelBuilder.AggCall TransferUserDefinedAggFunction(
      Class<? extends UserDefinedAggFunction> UDAF,
      String functionName,
      SqlReturnTypeInference returnType,
      List<RexNode> fields,
      List<RexNode> argList,
      RelBuilder relBuilder) {
    SqlUserDefinedAggFunction sqlUDAF =
        new SqlUserDefinedAggFunction(
            new SqlIdentifier(functionName, SqlParserPos.ZERO),
            SqlKind.OTHER_FUNCTION,
            returnType,
            null,
            null,
            AggregateFunctionImpl.create(UDAF),
            false,
            false,
            Optionality.FORBIDDEN);
    List<RexNode> addArgList = new ArrayList<>(fields);
    addArgList.addAll(argList);
    return relBuilder.aggregateCall(sqlUDAF, addArgList);
  }

  public static SqlReturnTypeInference getReturnTypeInferenceForArray() {
    return opBinding -> {
      RelDataTypeFactory typeFactory = opBinding.getTypeFactory();

      // Get argument types
      List<RelDataType> argTypes = opBinding.collectOperandTypes();

      if (argTypes.isEmpty()) {
        throw new IllegalArgumentException("Function requires at least one argument.");
      }
      RelDataType firstArgType = argTypes.getFirst();
      return createArrayType(typeFactory, firstArgType, true);
    };
  }

  public static SqlTypeName convertRelDataTypeToSqlTypeName(RelDataType type) {
    if (type instanceof AbstractExprRelDataType<?> exprType) {
      return switch (exprType.getUdt()) {
        case EXPR_DATE -> SqlTypeName.DATE;
        case EXPR_TIME -> SqlTypeName.TIME;
        case EXPR_TIMESTAMP -> SqlTypeName.TIMESTAMP;
          // EXPR_IP is mapped to SqlTypeName.OTHER since there is no
          // corresponding SqlTypeName in Calcite.
        case EXPR_IP -> SqlTypeName.OTHER;
        case EXPR_BINARY -> SqlTypeName.VARBINARY;
        default -> type.getSqlTypeName();
      };
    }
    return type.getSqlTypeName();
  }

  public static FunctionProperties restoreFunctionProperties(DataContext dataContext) {
    long currentTimeInNanos = DataContext.Variable.UTC_TIMESTAMP.get(dataContext);
    Instant instant =
        Instant.ofEpochSecond(
            currentTimeInNanos / 1_000_000_000, currentTimeInNanos % 1_000_000_000);
    ZoneId zoneId = ZoneOffset.UTC;
    return new FunctionProperties(instant, zoneId, QueryType.PPL);
  }

  /**
   * Convert java objects to ExprValue, so that the parameters fit the expr function signature. It
   * invokes ExprValueUtils.fromObjectValue to convert the java objects to ExprValue. Note that
   * date/time/timestamp strings will be converted to strings instead of ExprDateValue, etc.
   *
   * @param operands the operands to convert
   * @param rexCall the RexCall object containing the operands
   * @return the converted operands
   */
  public static List<Expression> convertToExprValues(List<Expression> operands, RexCall rexCall) {
    List<RelDataType> types = rexCall.getOperands().stream().map(RexNode::getType).toList();
    return convertToExprValues(operands, types);
  }

  /**
   * Convert java objects to ExprValue, so that the parameters fit the expr function signature. It
   * invokes ExprValueUtils.fromObjectValue to convert the java objects to ExprValue. Note that
   * date/time/timestamp strings will be converted to strings instead of ExprDateValue, etc.
   *
   * @param operands the operands to convert
   * @return the converted operands
   */
  public static List<Expression> convertToExprValues(
      List<Expression> operands, List<RelDataType> types) {
    List<ExprType> exprTypes =
        types.stream().map(OpenSearchTypeFactory::convertRelDataTypeToExprType).toList();
    List<Expression> exprValues = new ArrayList<>();
    for (int i = 0; i < operands.size(); i++) {
      Expression operand = Expressions.convert_(operands.get(i), Object.class);
      exprValues.add(
          i,
          Expressions.call(
              ExprValueUtils.class,
              "fromObjectValue",
              operand,
              Expressions.constant(exprTypes.get(i))));
    }
    return exprValues;
  }

  /**
   * Adapt a static expr method to a UserDefinedFunctionBuilder. It first converts the operands to
   * ExprValue, then calls the method, and finally converts the result to values recognizable by
   * Calcite by calling exprValue.valueForCalcite.
   *
   * @param type the class containing the static method
   * @param methodName the name of the method
   * @param returnTypeInference the return type inference of the UDF
   * @param nullPolicy the null policy of the UDF
   * @param operandMetadata type checker
   * @return an adapted ImplementorUDF with the expr method, which is a UserDefinedFunctionBuilder
   */
  public static ImplementorUDF adaptExprMethodToUDF(
      java.lang.reflect.Type type,
      String methodName,
      SqlReturnTypeInference returnTypeInference,
      NullPolicy nullPolicy,
      @Nullable UDFOperandMetadata operandMetadata) {
    NotNullImplementor implementor =
        (translator, call, translatedOperands) -> {
          List<Expression> operands =
              convertToExprValues(
                  translatedOperands, call.getOperands().stream().map(RexNode::getType).toList());
          Expression exprResult = Expressions.call(type, methodName, operands);
          return Expressions.call(exprResult, "valueForCalcite");
        };
    return new ImplementorUDF(implementor, nullPolicy) {
      @Override
      public SqlReturnTypeInference getReturnTypeInference() {
        return returnTypeInference;
      }

      @Override
      public UDFOperandMetadata getOperandMetadata() {
        return operandMetadata;
      }
    };
  }

  public static List<Expression> prependFunctionProperties(
      List<Expression> operands, RexToLixTranslator translator) {
    List<Expression> operandsWithProperties = new ArrayList<>(operands);
    Expression properties =
        Expressions.call(
            UserDefinedFunctionUtils.class, "restoreFunctionProperties", translator.getRoot());
    operandsWithProperties.addFirst(properties);
    return Collections.unmodifiableList(operandsWithProperties);
  }

  public static ImplementorUDF adaptExprMethodWithPropertiesToUDF(
      java.lang.reflect.Type type,
      String methodName,
      SqlReturnTypeInference returnTypeInference,
      NullPolicy nullPolicy,
      UDFOperandMetadata operandMetadata) {
    NotNullImplementor implementor =
        (translator, call, translatedOperands) -> {
          List<Expression> operands =
              convertToExprValues(
                  translatedOperands, call.getOperands().stream().map(RexNode::getType).toList());
          List<Expression> operandsWithProperties = prependFunctionProperties(operands, translator);
          Expression exprResult = Expressions.call(type, methodName, operandsWithProperties);
          return Expressions.call(exprResult, "valueForCalcite");
        };
    return new ImplementorUDF(implementor, nullPolicy) {
      @Override
      public SqlReturnTypeInference getReturnTypeInference() {
        return returnTypeInference;
      }

      @Override
      public UDFOperandMetadata getOperandMetadata() {
        return operandMetadata;
      }
    };
  }
}
