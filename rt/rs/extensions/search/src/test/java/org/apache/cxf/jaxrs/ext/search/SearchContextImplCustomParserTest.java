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
package org.apache.cxf.jaxrs.ext.search;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SearchContextImplCustomParserTest {

    @Test
    public void testQuery() {
        Message m = new MessageImpl();
        m.put(SearchContextImpl.CUSTOM_SEARCH_QUERY_PARAM_NAME, "$customfilter");
        m.put(SearchContextImpl.CUSTOM_SEARCH_PARSER_PROPERTY, new CustomParser());
        m.put(Message.QUERY_STRING, "$customfilter=color is red");
        SearchCondition<Color> sc = new SearchContextImpl(m).getCondition(Color.class);

        assertTrue(sc.isMet(new Color("red")));
        assertFalse(sc.isMet(new Color("blue")));
    }

    private static final class CustomParser implements SearchConditionParser<Color> {

        @Override
        public SearchCondition<Color> parse(String searchExpression) throws SearchParseException {
            if (!searchExpression.startsWith("color is ")) {
                throw new SearchParseException();
            }
            String value = searchExpression.substring(9);
            return new PrimitiveSearchCondition<Color>("color",
                                                        value,
                                                        ConditionType.EQUALS,
                                                        new Color(value));

        }

    }

    private static class Color {
        private String color;
        Color(String color) {
            this.color = color;
        }

        @SuppressWarnings("unused")
        public String getColor() {
            return color;
        }
    }
}