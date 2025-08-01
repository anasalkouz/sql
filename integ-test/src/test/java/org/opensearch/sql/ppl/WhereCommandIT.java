/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.ppl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.opensearch.sql.legacy.TestsConstants.TEST_INDEX_ACCOUNT;
import static org.opensearch.sql.legacy.TestsConstants.TEST_INDEX_BANK_WITH_NULL_VALUES;
import static org.opensearch.sql.legacy.TestsConstants.TEST_INDEX_DATE_TIME;
import static org.opensearch.sql.util.MatcherUtils.rows;
import static org.opensearch.sql.util.MatcherUtils.schema;
import static org.opensearch.sql.util.MatcherUtils.verifyDataRows;
import static org.opensearch.sql.util.MatcherUtils.verifySchema;

import java.io.IOException;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.opensearch.sql.data.type.ExprCoreType;

public class WhereCommandIT extends PPLIntegTestCase {

  @Override
  public void init() throws Exception {
    super.init();
    loadIndex(Index.ACCOUNT);
    loadIndex(Index.BANK_WITH_NULL_VALUES);
    loadIndex(Index.GAME_OF_THRONES);
    loadIndex(Index.DATETIME);
  }

  @Test
  public void testWhereWithLogicalExpr() throws IOException {
    JSONObject result =
        executeQuery(
            String.format(
                "source=%s | fields firstname | where firstname='Amber' | fields firstname",
                TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows("Amber"));
  }

  @Test
  public void testWhereWithMultiLogicalExpr() throws IOException {
    JSONObject result =
        executeQuery(
            String.format(
                "source=%s "
                    + "| where firstname='Amber' and lastname='Duke' and age=32 "
                    + "| fields firstname, lastname, age",
                TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows("Amber", "Duke", 32));
  }

  @Test
  public void testMultipleWhereCommands() throws IOException {
    JSONObject result =
        executeQuery(
            String.format(
                "source=%s "
                    + "| where firstname='Amber' "
                    + "| fields lastname, age"
                    + "| where lastname='Duke' "
                    + "| fields age "
                    + "| where age=32 "
                    + "| fields age",
                TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows(32));
  }

  @Test
  public void testWhereEquivalentSortCommand() throws IOException {
    assertEquals(
        executeQueryToString(
            String.format("source=%s | where firstname='Amber'", TEST_INDEX_ACCOUNT)),
        executeQueryToString(String.format("source=%s firstname='Amber'", TEST_INDEX_ACCOUNT)));
  }

  @Test
  public void testLikeFunction() throws IOException {
    JSONObject result =
        executeQuery(
            String.format(
                "source=%s | fields firstname | where like(firstname, 'Ambe_') | fields firstname",
                TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows("Amber"));
  }

  @Test
  public void testLikeFunctionNoHit() throws IOException {
    JSONObject result =
        executeQuery(
            String.format(
                "source=%s | where like(firstname, 'Duk_') | fields lastname",
                TEST_INDEX_BANK_WITH_NULL_VALUES));
    assertEquals(0, result.getInt("total"));
  }

  @Test
  public void testIsNullFunction() throws IOException {
    JSONObject result =
        executeQuery(
            String.format(
                "source=%s | where isnull(age) | fields firstname",
                TEST_INDEX_BANK_WITH_NULL_VALUES));
    verifyDataRows(result, rows("Virginia"));
  }

  @Test
  public void testIsNotNullFunction() throws IOException {
    JSONObject result =
        executeQuery(
            String.format(
                "source=%s | where isnotnull(age) and like(firstname, 'Ambe_%%') | fields"
                    + " firstname",
                TEST_INDEX_BANK_WITH_NULL_VALUES));
    verifyDataRows(result, rows("Amber JOHnny"));
  }

  @Test
  public void testWhereWithMetadataFields() throws IOException {
    JSONObject result =
        executeQuery(
            String.format("source=%s | where _id='1' | fields firstname", TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows("Amber"));
  }

  @Test
  public void testWhereWithMetadataFields2() throws IOException {
    JSONObject result =
        executeQuery(String.format("source=%s | where _id='1'", TEST_INDEX_ACCOUNT));
    verifyDataRows(
        result,
        rows(
            1,
            "Amber",
            "880 Holmes Lane",
            39225,
            "M",
            "Brogan",
            "Pyrami",
            "IL",
            32,
            "amberduke@pyrami.com",
            "Duke"));
  }

  @Test
  public void testWhereWithIn() throws IOException {
    JSONObject result =
        executeQuery(
            String.format(
                "source=%s | where firstname in ('Amber') | fields firstname", TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows("Amber"));

    result =
        executeQuery(
            String.format(
                "source=%s | where firstname in ('Amber', 'Dale') | fields firstname",
                TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows("Amber"), rows("Dale"));

    result =
        executeQuery(
            String.format(
                "source=%s | where balance in (4180, 5686.0) | fields balance",
                TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows(4180), rows(5686));
  }

  @Test
  public void testWhereWithNotIn() throws IOException {
    JSONObject result =
        executeQuery(
            String.format(
                "source=%s | where account_number < 4 | where firstname not in ('Amber', 'Levine')"
                    + " | fields firstname",
                TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows("Roberta"), rows("Bradshaw"));

    result =
        executeQuery(
            String.format(
                "source=%s | where account_number < 4 | where not firstname in ('Amber', 'Levine')"
                    + " | fields firstname",
                TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows("Roberta"), rows("Bradshaw"));

    result =
        executeQuery(
            String.format(
                "source=%s | where not firstname not in ('Amber', 'Dale') | fields firstname",
                TEST_INDEX_ACCOUNT));
    verifyDataRows(result, rows("Amber"), rows("Dale"));
  }

  @Test
  public void testInWithIncompatibleType() {
    Exception e =
        assertThrows(
            Exception.class,
            () -> {
              executeQuery(
                  String.format(
                      "source=%s | where balance in (4180, 5686, '6077') | fields firstname",
                      TEST_INDEX_ACCOUNT));
            });
    MatcherAssert.assertThat(e.getMessage(), containsString(getIncompatibleTypeErrMsg()));
  }

  protected String getIncompatibleTypeErrMsg() {
    return String.format(
        "function expected %s, but got %s",
        ExprCoreType.coreTypes().stream()
            .map(type -> String.format("[%s,%s]", type.typeName(), type.typeName()))
            .collect(Collectors.joining(",", "{", "}")),
        "[LONG,STRING]");
  }

  @Test
  public void testFilterScriptPushDown() throws IOException {
    JSONObject actual =
        executeQuery(
            String.format(
                "source=%s | where firstname ='Amber' and age - 2.0 = 30 | fields firstname, age",
                TEST_INDEX_ACCOUNT));
    verifySchema(actual, schema("firstname", "string"), schema("age", "bigint"));
    verifyDataRows(actual, rows("Amber", 32));
  }

  @Test
  public void testFilterScriptPushDownWithCalciteStdFunction() throws IOException {
    JSONObject actual =
        executeQuery(
            String.format(
                "source=%s | where length(firstname) = 5 and abs(age) = 32 and balance = 39225 |"
                    + " fields firstname, age",
                TEST_INDEX_ACCOUNT));
    verifySchema(actual, schema("firstname", "string"), schema("age", "bigint"));
    verifyDataRows(actual, rows("Amber", 32));
  }

  @Test
  public void testFilterScriptPushDownWithPPLBuiltInFunction() throws IOException {
    JSONObject actual =
        executeQuery(
            String.format("source=%s | where month(login_time) = 1", TEST_INDEX_DATE_TIME));
    verifySchema(actual, schema("birthday", "timestamp"), schema("login_time", "timestamp"));
    verifyDataRows(
        actual,
        rows(null, "2015-01-01 00:00:00"),
        rows(null, "2015-01-01 12:10:30"),
        rows(null, "1970-01-19 08:31:22.955"));
  }

  @Test
  public void testFilterScriptPushDownWithCalciteStdLibraryFunction() throws IOException {
    JSONObject actual =
        executeQuery(
            String.format(
                "source=%s | where left(firstname, 3) = 'Ama' | fields firstname",
                TEST_INDEX_ACCOUNT));
    verifySchema(actual, schema("firstname", "string"));
    verifyDataRows(actual, rows("Amalia"), rows("Amanda"));
  }
}
