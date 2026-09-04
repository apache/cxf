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

import org.junit.Assert;
import org.junit.Test;

/**
 * The FIQL parser recurses once per parenthesis nesting level; nesting depth must
 * be bounded so a crafted expression cannot drive it into a StackOverflowError.
 */
public class FiqlParserDepthTest extends Assert {

    public static class Condition {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static String nested(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append('(');
        }
        sb.append("name==a");
        for (int i = 0; i < depth; i++) {
            sb.append(')');
        }
        return sb.toString();
    }

    @Test
    public void testModerateNestingParses() throws Exception {
        FiqlParser<Condition> parser = new FiqlParser<>(Condition.class);
        assertNotNull(parser.parse(nested(10)));
    }

    @Test
    public void testDefaultDepthBoundaryParses() throws Exception {
        FiqlParser<Condition> parser = new FiqlParser<>(Condition.class);
        assertNotNull(parser.parse(nested(64)));
    }

    @Test(expected = SearchParseException.class)
    public void testDeepNestingRejectedNotStackOverflow() throws Exception {
        // deep enough to overflow a default thread stack if recursion were unbounded
        new FiqlParser<>(Condition.class).parse(nested(10000));
    }

    @Test(expected = SearchParseException.class)
    public void testConfiguredDepthLimitEnforced() throws Exception {
        FiqlParser<Condition> parser = new FiqlParser<>(Condition.class,
            Collections.singletonMap(FiqlParser.MAX_PARENTHESIS_DEPTH, "2"));
        parser.parse(nested(3));
    }

    @Test
    public void testConfiguredDepthLimitAllowsWithinBound() throws Exception {
        FiqlParser<Condition> parser = new FiqlParser<>(Condition.class,
            Collections.singletonMap(FiqlParser.MAX_PARENTHESIS_DEPTH, "8"));
        assertNotNull(parser.parse(nested(8)));
    }

    @Test
    public void testBlankDepthPropertyFallsBackToDefault() throws Exception {
        FiqlParser<Condition> parser = new FiqlParser<>(Condition.class,
            Collections.singletonMap(FiqlParser.MAX_PARENTHESIS_DEPTH, "  "));
        assertNotNull(parser.parse(nested(64)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonNumericDepthPropertyRejected() throws Exception {
        new FiqlParser<>(Condition.class,
            Collections.singletonMap(FiqlParser.MAX_PARENTHESIS_DEPTH, "lots"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonPositiveDepthPropertyRejected() throws Exception {
        new FiqlParser<>(Condition.class,
            Collections.singletonMap(FiqlParser.MAX_PARENTHESIS_DEPTH, "0"));
    }
}
