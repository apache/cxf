/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.ext.search.fiql;

import java.util.Collections;

import org.apache.cxf.jaxrs.ext.search.SearchParseException;

import org.junit.Test;

public class FiqlParserReDoSTest {
    @Test(timeout = 2000)
    public void testLongExpressionWithoutComparatorIsRejectedQuickly() {
        StringBuilder expression = new StringBuilder(32768);
        for (int i = 0; i < 32768; i++) {
            expression.append('a');
        }
        try {
            new FiqlParser<>(Bean.class).parse(expression.toString());
        } catch (SearchParseException ex) {
            return;
        }
        throw new AssertionError("An expression without a comparator must be rejected");
    }

    @Test
    public void testComparatorsRemainSupported() throws SearchParseException {
        FiqlParser<Bean> parser = new FiqlParser<>(Bean.class);
        parser.parse("name==value");
        parser = new FiqlParser<>(Bean.class,
            Collections.singletonMap(FiqlParser.SUPPORT_SINGLE_EQUALS, "true"));
        parser.parse("name=first");
    }

    @Test
    public void testConfiguredExpressionLengthLimit() throws SearchParseException {
        FiqlParser<Bean> parser = new FiqlParser<>(Bean.class,
            Collections.singletonMap(FiqlParser.MAX_EXPRESSION_LENGTH, "10"));
        parser.parse("name==1234");
        try {
            parser.parse("name==12345");
        } catch (SearchParseException ex) {
            return;
        }
        throw new AssertionError("An expression over the configured length must be rejected");
    }

    @Test
    public void testDefaultExpressionLengthLimitIsEightKiB() throws SearchParseException {
        FiqlParser<Bean> parser = new FiqlParser<>(Bean.class);
        parser.parse(expressionOfLength(8192));
        try {
            parser.parse(expressionOfLength(8193));
        } catch (SearchParseException ex) {
            return;
        }
        throw new AssertionError("The default expression length limit must be 8 KiB");
    }

    private static String expressionOfLength(int length) {
        StringBuilder expression = new StringBuilder(length);
        expression.append("name==");
        while (expression.length() < length) {
            expression.append('a');
        }
        return expression.substring(0, length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidExpressionLengthLimitRejected() {
        new FiqlParser<>(Bean.class,
            Collections.singletonMap(FiqlParser.MAX_EXPRESSION_LENGTH, "0"));
    }

    public static class Bean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}