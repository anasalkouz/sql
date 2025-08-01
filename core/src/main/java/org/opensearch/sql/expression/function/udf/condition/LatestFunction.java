/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.expression.function.udf.condition;

import static org.opensearch.sql.calcite.utils.UserDefinedFunctionUtils.prependFunctionProperties;
import static org.opensearch.sql.utils.DateTimeUtils.getRelativeZonedDateTime;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import org.apache.calcite.adapter.enumerable.NotNullImplementor;
import org.apache.calcite.adapter.enumerable.NullPolicy;
import org.apache.calcite.adapter.enumerable.RexToLixTranslator;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.opensearch.sql.calcite.utils.PPLOperandTypes;
import org.opensearch.sql.calcite.utils.UserDefinedFunctionUtils;
import org.opensearch.sql.data.model.ExprValue;
import org.opensearch.sql.expression.function.FunctionProperties;
import org.opensearch.sql.expression.function.ImplementorUDF;
import org.opensearch.sql.expression.function.UDFOperandMetadata;

public class LatestFunction extends ImplementorUDF {
  public LatestFunction() {
    super(new LatestFunction.LatestImplementor(), NullPolicy.ANY);
  }

  @Override
  public SqlReturnTypeInference getReturnTypeInference() {
    return ReturnTypes.BOOLEAN;
  }

  @Override
  public UDFOperandMetadata getOperandMetadata() {
    return PPLOperandTypes.STRING_TIMESTAMP;
  }

  public static class LatestImplementor implements NotNullImplementor {
    @Override
    public Expression implement(
        RexToLixTranslator rexToLixTranslator, RexCall rexCall, List<Expression> list) {
      List<RelDataType> types = rexCall.getOperands().stream().map(RexNode::getType).toList();
      return Expressions.call(
          LatestFunction.class,
          "latest",
          prependFunctionProperties(
              UserDefinedFunctionUtils.convertToExprValues(list, types), rexToLixTranslator));
    }
  }

  public static Boolean latest(Object... inputs) {
    String expression = ((ExprValue) inputs[1]).stringValue();
    Instant candidate = ((ExprValue) inputs[2]).timestampValue();
    FunctionProperties functionProperties = (FunctionProperties) inputs[0];
    Clock clock = functionProperties.getQueryStartClock();
    ZonedDateTime candidateDatetime = ZonedDateTime.ofInstant(candidate, clock.getZone());
    ZonedDateTime latest =
        getRelativeZonedDateTime(
            expression, ZonedDateTime.ofInstant(clock.instant(), clock.getZone()));
    return latest.isAfter(candidateDatetime);
  }
}
