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
package org.apache.cxf.jaxrs.ext.search.client;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.datatype.Duration;

import org.apache.cxf.jaxrs.ext.search.FiqlParser;

/**
 * Builds client-side search condition that passed to backend can be consumed by {@link FiqlParser}.
 * <p>
 * Examples:
 * 
 * <pre>
 * FiqlSearchConditionBuilder b = new FiqlSearchConditionBuilder();
 * b.query().is(&quot;price&quot;).equalTo(123.5).build();
 * // gives &quot;price==123.5&quot;
 * b.query().is(&quot;price&quot;).greaterThan(30).and().is(&quot;price&quot;).lessThan(50).build();
 * // gives &quot;price=gt=30.0;price=lt=50.0&quot;
 * </pre>
 * 
 * For very complex junctions nested "and"/"or" are allowed (breaking a bit fluency of interface) and looks
 * like the following example:
 * 
 * <pre>
 * PartialCondition c = b.query();
 * c.is("price").lessThan(100).and().or(
 *      c.is("title").equalTo("The lord*"), 
 *      c.is("author").equalTo("R.R.Tolkien").build();
 * // gives "price=lt=100.0;(title==The lord*,author==R.R.Tolkien)"
 * </pre>
 */
public class FiqlSearchConditionBuilder implements SearchConditionBuilder {

    @Override
    public String build() {
        return "";
    }

    @Override
    public PartialCondition query() {
        return new Builder();
    }

    private static class Builder implements SearchConditionBuilder.Property,
        SearchConditionBuilder.CompleteCondition, SearchConditionBuilder.PartialCondition {
        private String result = "";
        private Builder parent;
        private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        public Builder() {
            parent = null;
        }

        public Builder(Builder parent) {
            this.parent = parent;
        }

        @Override
        public String build() {
            return buildPartial(null);
            // if (parent != null) {
            // return parent.build() + result;
            // } else {
            // return result;
            // }
        }

        // builds from parent but not further then exclude builder
        private String buildPartial(Builder exclude) {
            if (parent != null && !parent.equals(exclude)) {
                return parent.buildPartial(exclude) + result;
            } else {
                return result;
            }
        }

        @Override
        public CompleteCondition after(Date date) {
            result += FiqlParser.GT + toString(date);
            return this;
        }

        @Override
        public CompleteCondition before(Date date) {
            result += FiqlParser.LT + toString(date);
            return this;
        }

        @Override
        public CompleteCondition equalTo(String literalOrPattern) {
            result += FiqlParser.EQ + literalOrPattern;
            return this;
        }

        @Override
        public CompleteCondition equalTo(double number) {
            result += FiqlParser.EQ + number;
            return this;
        }

        @Override
        public CompleteCondition equalTo(Date date) {
            result += FiqlParser.EQ + toString(date);
            return this;
        }

        @Override
        public CompleteCondition greaterOrEqualTo(double number) {
            result += FiqlParser.GE + number;
            return this;
        }

        @Override
        public CompleteCondition greaterThan(double number) {
            result += FiqlParser.GT + number;
            return this;
        }

        @Override
        public CompleteCondition lessOrEqualTo(double number) {
            result += FiqlParser.LE + number;
            return this;
        }

        @Override
        public CompleteCondition lessThan(double number) {
            result += FiqlParser.LT + number;
            return this;
        }

        @Override
        public CompleteCondition lexicalAfter(String literal) {
            result += FiqlParser.GT + literal;
            return this;
        }

        @Override
        public CompleteCondition lexicalBefore(String literal) {
            result += FiqlParser.LT + literal;
            return this;
        }

        @Override
        public CompleteCondition lexicalNotAfter(String literal) {
            result += FiqlParser.LE + literal;
            return this;
        }

        @Override
        public CompleteCondition lexicalNotBefore(String literal) {
            result += FiqlParser.GE + literal;
            return this;
        }

        @Override
        public CompleteCondition notAfter(Date date) {
            result += FiqlParser.LE + toString(date);
            return this;
        }

        @Override
        public CompleteCondition notBefore(Date date) {
            result += FiqlParser.GE + toString(date);
            return this;
        }

        @Override
        public CompleteCondition notEqualTo(String literalOrPattern) {
            result += FiqlParser.NEQ + literalOrPattern;
            return this;
        }

        @Override
        public CompleteCondition notEqualTo(double number) {
            result += FiqlParser.NEQ + number;
            return this;
        }

        @Override
        public CompleteCondition notEqualTo(Date date) {
            result += FiqlParser.NEQ + toString(date);
            return this;
        }

        @Override
        public CompleteCondition after(Duration distanceFromNow) {
            result += FiqlParser.GT + distanceFromNow;
            return this;
        }

        @Override
        public CompleteCondition before(Duration distanceFromNow) {
            result += FiqlParser.LT + distanceFromNow;
            return this;
        }

        @Override
        public CompleteCondition equalTo(Duration distanceFromNow) {
            result += FiqlParser.EQ + distanceFromNow;
            return this;
        }

        @Override
        public CompleteCondition notAfter(Duration distanceFromNow) {
            result += FiqlParser.LE + distanceFromNow;
            return this;
        }

        @Override
        public CompleteCondition notBefore(Duration distanceFromNow) {
            result += FiqlParser.GE + distanceFromNow;
            return this;
        }

        @Override
        public CompleteCondition notEqualTo(Duration distanceFromNow) {
            result += FiqlParser.NEQ + distanceFromNow;
            return this;
        }

        @Override
        public PartialCondition and() {
            result += FiqlParser.AND;
            return this;
        }

        @Override
        public PartialCondition or() {
            result += FiqlParser.OR;
            return this;
        }

        @Override
        public CompleteCondition and(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn) {
            result += "(" + ((Builder)c1).buildPartial(this) + FiqlParser.AND
                      + ((Builder)c2).buildPartial(this);
            for (CompleteCondition c : cn) {
                result += FiqlParser.AND + ((Builder)c).buildPartial(this);
            }
            result += ")";
            return this;
        }

        @Override
        public CompleteCondition or(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn) {
            result += "(" + ((Builder)c1).buildPartial(this) + FiqlParser.OR
                      + ((Builder)c2).buildPartial(this);
            for (CompleteCondition c : cn) {
                result += FiqlParser.OR + ((Builder)c).buildPartial(this);
            }
            result += ")";
            return this;
        }

        @Override
        public Property is(String property) {
            Builder b = new Builder(this);
            b.result = property;
            return b;
        }

        private String toString(Date date) {
            String s = df.format(date);
            // zone in XML is "+01:00" in Java is "+0100"; adding semicolon
            int len = s.length();
            return s.substring(0, len - 2) + ":" + s.substring(len - 2, len);
        }
    }

}
