/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.ppl.antlr;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opensearch.sql.common.antlr.SyntaxCheckException;

public class PPLSyntaxParserTest {

  @Rule public final ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void testSearchCommandShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("search source=t a=1 b=2");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchCommandIgnoreSearchKeywordShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("source=t a=1 b=2");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchCommandWithMultipleIndicesShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("search source=t,u a=1 b=2");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchCommandCrossClusterShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("search source=c:t a=1 b=2");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchCommandCrossClusterHiddenShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("search source=c:.t a=1 b=2");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchCommandCrossClusterQualifiedShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("search source=c:t.u a=1 b=2");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchCommandCrossClusterHiddenQualifiedShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("search source=c:.t.u a=1 b=2");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchCommandMatchAllCrossClusterShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("search source=*:t a=1 b=2");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchCommandCrossClusterWithMultipleIndicesShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("search source=c:t,d:u,v a=1 b=2");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchCommandCrossClusterIgnoreSearchKeywordShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("source=c:t a=1 b=2");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchFieldsCommandShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("search source=t a=1 b=2 | fields a,b");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchFieldsCommandCrossClusterShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("search source=c:t a=1 b=2 | fields a,b");
    assertNotEquals(null, tree);
  }

  @Test
  public void testSearchCommandWithoutSourceShouldFail() {
    exceptionRule.expect(RuntimeException.class);
    exceptionRule.expectMessage("is not a valid term at this part of the query");

    new PPLSyntaxParser().parse("search a=1");
  }

  @Test
  public void testRareCommandShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("source=t a=1 | rare a");
    assertNotEquals(null, tree);
  }

  @Test
  public void testRareCommandWithGroupByShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("source=t a=1 | rare a by b");
    assertNotEquals(null, tree);
  }

  @Test
  public void testTopCommandWithoutNShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("source=t a=1 | top a");
    assertNotEquals(null, tree);
  }

  @Test
  public void testTopCommandWithNShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("source=t a=1 | top 1 a");
    assertNotEquals(null, tree);
  }

  @Test
  public void testTopCommandWithNAndGroupByShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("source=t a=1 | top 1 a by b");
    assertNotEquals(null, tree);
  }

  @Test
  public void testTopCommandWithoutNAndGroupByShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("source=t a=1 | top a by b");
    assertNotEquals(null, tree);
  }

  @Test
  public void testCanParseMultiMatchRelevanceFunction() {
    assertNotEquals(
        null, new PPLSyntaxParser().parse("SOURCE=test | WHERE multi_match(['address'], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE multi_match(['address', 'notes'], 'query')"));
    assertNotEquals(
        null, new PPLSyntaxParser().parse("SOURCE=test | WHERE multi_match([\"*\"], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser().parse("SOURCE=test | WHERE multi_match([\"address\"], 'query')"));
    assertNotEquals(
        null, new PPLSyntaxParser().parse("SOURCE=test | WHERE multi_match([`address`], 'query')"));
    assertNotEquals(
        null, new PPLSyntaxParser().parse("SOURCE=test | WHERE multi_match([address], 'query')"));

    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE multi_match(['address' ^ 1.0, 'notes' ^ 2.2], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE multi_match(['address' ^ 1.1, 'notes'], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE multi_match(['address', 'notes' ^ 1.5], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE multi_match(['address', 'notes' 3], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE multi_match(['address' ^ .3, 'notes' 3], 'query')"));

    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE multi_match([\"Tags\" ^ 1.5, Title, `Body` 4.2], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE multi_match([\"Tags\" ^ 1.5, Title, `Body` 4.2], 'query',"
                    + "analyzer=keyword, quote_field_suffix=\".exact\", fuzzy_prefix_length = 4)"));
  }

  @Test
  public void testCanParseSimpleQueryStringRelevanceFunction() {
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE simple_query_string(['address'], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE simple_query_string(['address', 'notes'], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser().parse("SOURCE=test | WHERE simple_query_string([\"*\"], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE simple_query_string([\"address\"], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE simple_query_string([`address`], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser().parse("SOURCE=test | WHERE simple_query_string([address], 'query')"));

    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE simple_query_string(['address' ^ 1.0, 'notes' ^ 2.2],"
                    + " 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE simple_query_string(['address' ^ 1.1, 'notes'], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE simple_query_string(['address', 'notes' ^ 1.5], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE simple_query_string(['address', 'notes' 3], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE simple_query_string(['address' ^ .3, 'notes' 3], 'query')"));

    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE simple_query_string([\"Tags\" ^ 1.5, Title, `Body` 4.2],"
                    + " 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE simple_query_string([\"Tags\" ^ 1.5, Title, `Body` 4.2],"
                    + " 'query',analyzer=keyword, quote_field_suffix=\".exact\","
                    + " fuzzy_prefix_length = 4)"));
  }

  @Test
  public void testCanParseQueryStringRelevanceFunction() {
    assertNotEquals(
        null,
        new PPLSyntaxParser().parse("SOURCE=test | WHERE query_string(['address'], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE query_string(['address', 'notes'], 'query')"));
    assertNotEquals(
        null, new PPLSyntaxParser().parse("SOURCE=test | WHERE query_string([\"*\"], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser().parse("SOURCE=test | WHERE query_string([\"address\"], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser().parse("SOURCE=test | WHERE query_string([`address`], 'query')"));
    assertNotEquals(
        null, new PPLSyntaxParser().parse("SOURCE=test | WHERE query_string([address], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE query_string(['address' ^ 1.0, 'notes' ^ 2.2], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE query_string(['address' ^ 1.1, 'notes'], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE query_string(['address', 'notes' ^ 1.5], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE query_string(['address', 'notes' 3], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE query_string(['address' ^ .3, 'notes' 3], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE query_string([\"Tags\" ^ 1.5, Title, `Body` 4.2], 'query')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE query_string([\"Tags\" ^ 1.5, Title, `Body` 4.2], 'query',"
                    + "analyzer=keyword, quote_field_suffix=\".exact\", fuzzy_prefix_length = 4)"));
  }

  @Test
  public void testDescribeCommandShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("describe t");
    assertNotEquals(null, tree);
  }

  @Test
  public void testDescribeCommandWithMultipleIndicesShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("describe t,u");
    assertNotEquals(null, tree);
  }

  @Test
  public void testDescribeCommandCrossClusterShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("describe c:t");
    assertNotEquals(null, tree);
  }

  @Test
  public void testDescribeCommandMatchAllCrossClusterShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("describe *:t");
    assertNotEquals(null, tree);
  }

  @Test
  public void testDescribeFieldsCommandShouldPass() {
    ParseTree tree = new PPLSyntaxParser().parse("describe t | fields a,b");
    assertNotEquals(null, tree);
  }

  @Test
  public void testInvalidOperatorCombinationShouldFail() {
    exceptionRule.expect(RuntimeException.class);
    exceptionRule.expectMessage(
        StringContainsInOrder.stringContainsInOrder(
            "[<EOF>] is not a valid term at this part of the query: '...= t | where x > y OR' <--"
                + " HERE.",
            "Expecting one of ",
            " possible tokens. Some examples: ",
            "..."));

    new PPLSyntaxParser().parse("source = t | where x > y OR");
  }

  @Test
  public void testCanParseExtractFunction() {
    String[] parts =
        List.of(
                "MICROSECOND",
                "SECOND",
                "MINUTE",
                "HOUR",
                "DAY",
                "WEEK",
                "MONTH",
                "QUARTER",
                "YEAR",
                "SECOND_MICROSECOND",
                "MINUTE_MICROSECOND",
                "MINUTE_SECOND",
                "HOUR_MICROSECOND",
                "HOUR_SECOND",
                "HOUR_MINUTE",
                "DAY_MICROSECOND",
                "DAY_SECOND",
                "DAY_MINUTE",
                "DAY_HOUR",
                "YEAR_MONTH")
            .toArray(new String[0]);

    for (String part : parts) {
      assertNotNull(
          new PPLSyntaxParser()
              .parse(
                  String.format("SOURCE=test | eval k = extract(%s FROM \"2023-02-06\")", part)));
    }
  }

  @Test
  public void testCanParseGetFormatFunction() {
    String[] types = {"DATE", "DATETIME", "TIME", "TIMESTAMP"};
    String[] formats = {"'USA'", "'JIS'", "'ISO'", "'EUR'", "'INTERNAL'"};

    for (String type : types) {
      for (String format : formats) {
        assertNotNull(
            new PPLSyntaxParser()
                .parse(String.format("SOURCE=test | eval k = get_format(%s, %s)", type, format)));
      }
    }
  }

  @Test
  public void testCannotParseGetFormatFunctionWithBadArg() {
    assertThrows(
        SyntaxCheckException.class,
        () ->
            new PPLSyntaxParser()
                .parse("SOURCE=test | eval k = GET_FORMAT(NONSENSE_ARG,'INTERNAL')"));
  }

  @Test
  public void testCanParseTimestampaddFunction() {
    assertNotNull(
        new PPLSyntaxParser()
            .parse("SOURCE=test | eval k = TIMESTAMPADD(MINUTE, 1, '2003-01-02')"));
    assertNotNull(
        new PPLSyntaxParser().parse("SOURCE=test | eval k = TIMESTAMPADD(WEEK,1,'2003-01-02')"));
  }

  @Test
  public void testCanParseTimestampdiffFunction() {
    assertNotNull(
        new PPLSyntaxParser()
            .parse("SOURCE=test | eval k = TIMESTAMPDIFF(MINUTE, '2003-01-02', '2003-01-02')"));
    assertNotNull(
        new PPLSyntaxParser()
            .parse("SOURCE=test | eval k = TIMESTAMPDIFF(WEEK,'2003-01-02','2003-01-02')"));
  }

  @Test
  public void testCanParseFillNullSameValue() {
    assertNotNull(new PPLSyntaxParser().parse("SOURCE=test | fillnull with 0 in a"));
    assertNotNull(new PPLSyntaxParser().parse("SOURCE=test | fillnull with 0 in a, b"));
  }

  @Test
  public void testCanParseFillNullVariousValues() {
    assertNotNull(new PPLSyntaxParser().parse("SOURCE=test | fillnull using a = 0"));
    assertNotNull(new PPLSyntaxParser().parse("SOURCE=test | fillnull using a = 0, b = 1"));
  }

  @Test
  public void testLineCommentShouldPass() {
    assertNotNull(new PPLSyntaxParser().parse("search source=t a=1 b=2 //this is a comment"));
    assertNotNull(new PPLSyntaxParser().parse("search source=t a=1 b=2 // this is a comment "));
    assertNotNull(
        new PPLSyntaxParser()
            .parse(
                """
                    // test is a new line comment \
                    search source=t a=1 b=2 // test is a line comment at the end of ppl command \
                    | fields a,b // this is line comment inner ppl command\
                    ////this is a new line comment
                    """));
  }

  @Test
  public void testBlockCommentShouldPass() {
    assertNotNull(new PPLSyntaxParser().parse("search source=t a=1 b=2 /*block comment*/"));
    assertNotNull(new PPLSyntaxParser().parse("search source=t a=1 b=2 /* block comment */"));
    assertNotNull(
        new PPLSyntaxParser()
            .parse(
                """
                    /*
                    This is a\
                        multiple\
                    line\
                    block\
                        comment */\
                    search /* block comment */ source=t /* block comment */ a=1 b=2
                    |/*
                        This is a\
                            multiple\
                        line\
                        block\
                            comment */ fields a,b /* block comment */ \
                    """));
  }

  @Test
  public void testWhereCommand() {
    assertNotEquals(null, new PPLSyntaxParser().parse("SOURCE=test | WHERE x"));
    assertNotEquals(null, new PPLSyntaxParser().parse("SOURCE=test | WHERE x = 1"));
    assertNotEquals(null, new PPLSyntaxParser().parse("SOURCE=test | WHERE x = y"));
    assertNotEquals(null, new PPLSyntaxParser().parse("SOURCE=test | WHERE x OR y"));
    assertNotEquals(null, new PPLSyntaxParser().parse("SOURCE=test | WHERE true"));
    assertNotEquals(null, new PPLSyntaxParser().parse("SOURCE=test | WHERE (1 >= 0)"));
    assertNotEquals(null, new PPLSyntaxParser().parse("SOURCE=test | WHERE (x >= 0)"));
    assertNotEquals(null, new PPLSyntaxParser().parse("SOURCE=test | WHERE (x < 1) = (y > 1)"));
    assertNotEquals(null, new PPLSyntaxParser().parse("SOURCE=test | WHERE x = (1 + 2) * 3"));
    assertNotEquals(null, new PPLSyntaxParser().parse("SOURCE=test | WHERE x = 1 + 2 * 3"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE (day_of_week_i < 2) OR (day_of_week_i > 5)"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse("SOURCE=test | WHERE match('message', 'test query', analyzer='keyword')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE multi_match(['field1', 'field2' ^ 3.2], 'test query',"
                    + " analyzer='keyword')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE simple_query_string(['field1', 'field2' ^ 3.2], 'test query',"
                    + " analyzer='keyword')"));
    assertNotEquals(
        null,
        new PPLSyntaxParser()
            .parse(
                "SOURCE=test | WHERE query_string(['field1', 'field2' ^ 3.2], 'test query',"
                    + " analyzer='keyword')"));
  }
}
