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
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.xml.datatype.Duration;

import org.apache.cxf.jaxrs.ext.search.FiqlParser;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;

/**
 * Builds client-side search condition that passed to backend can be consumed by {@link FiqlParser}.
 * <p>
 * Examples:
 * 
 * <pre>
 * SearchConditionBuilder b = SearchConditionBuilder.instance("fiql");
 * b.is(&quot;price&quot;).equalTo(123.5).query();
 * // gives &quot;price==123.5&quot;
 * b.is(&quot;price&quot;).greaterThan(30).and().is(&quot;price&quot;).lessThan(50).query();
 * // gives &quot;price=gt=30.0;price=lt=50.0&quot;
 * </pre>
 * 
 * For very complex junctions nested "and"/"or" are allowed (breaking a bit fluency of interface) and looks
 * like the following example:
 * 
 * <pre>
 * SearchConditionBuilder b = SearchConditionBuilder.instance("fiql");
 * b.is(&quot;price&quot;).lessThan(100).and().or(
 *     b.is(&quot;title&quot;).equalTo(&quot;The lord*&quot;),
 *     b.is(&quot;author&quot;).equalTo(&quot;R.R.Tolkien&quot;)).query();
 * // gives &quot;price=lt=100.0;(title==The lord*,author==R.R.Tolkien)&quot;
 * </pre>
 */
public class FiqlSearchConditionBuilder extends SearchConditionBuilder {

    private Map<String, String> properties;

    public FiqlSearchConditionBuilder() {
        this(Collections.<String, String> emptyMap());
    }

    public FiqlSearchConditionBuilder(Map<String, String> properties) {
        this.properties = properties;
    }

    public String query() {
        return "";
    }

    @Override
    public Property is(String property) {
        return new Builder(properties).is(property);
    }

    @Override
    public CompleteCondition and(CompleteCondition c1, CompleteCondition c2, 
                                 CompleteCondition... cn) {
        return new Builder(properties).and(c1, c2, cn);
    }

    @Override
    public CompleteCondition or(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn) {
        return new Builder(properties).or(c1, c2, cn);
    }

    private static class Builder implements Property, CompleteCondition, PartialCondition {
        private String result = "";
        private Builder parent;
        private DateFormat df;
        private boolean timeZoneSupported;

        public Builder(Map<String, String> properties) {
            parent = null;
            df = SearchUtils.getDateFormat(properties, FiqlParser.DEFAULT_DATE_FORMAT);
            timeZoneSupported = SearchUtils.isTimeZoneSupported(properties, Boolean.TRUE);
        }

        public Builder(Builder parent) {
            this.parent = parent;
            df = parent.getDateFormat();
            timeZoneSupported = parent.isTimeZoneSupported();
        }

        public String query() {
            return buildPartial(null);
        }

        private DateFormat getDateFormat() {
            return df;
        }

        private boolean isTimeZoneSupported() {
            return timeZoneSupported;
        }

        // builds from parent but not further then exclude builder
        private String buildPartial(Builder exclude) {
            if (parent != null && !parent.equals(exclude)) {
                return parent.buildPartial(exclude) + result;
            } else {
                return result;
            }
        }

        public CompleteCondition after(Date date) {
            result += FiqlParser.GT + toString(date);
            return this;
        }

        public CompleteCondition before(Date date) {
            result += FiqlParser.LT + toString(date);
            return this;
        }

        public CompleteCondition equalTo(String literalOrPattern) {
            result += FiqlParser.EQ + literalOrPattern;
            return this;
        }

        public CompleteCondition equalTo(double number) {
            result += FiqlParser.EQ + number;
            return this;
        }

        public CompleteCondition equalTo(Date date) {
            result += FiqlParser.EQ + toString(date);
            return this;
        }

        public CompleteCondition greaterOrEqualTo(double number) {
            result += FiqlParser.GE + number;
            return this;
        }

        public CompleteCondition greaterThan(double number) {
            result += FiqlParser.GT + number;
            return this;
        }

        public CompleteCondition lessOrEqualTo(double number) {
            result += FiqlParser.LE + number;
            return this;
        }

        public CompleteCondition lessThan(double number) {
            result += FiqlParser.LT + number;
            return this;
        }

        public CompleteCondition lexicalAfter(String literal) {
            result += FiqlParser.GT + literal;
            return this;
        }

        public CompleteCondition lexicalBefore(String literal) {
            result += FiqlParser.LT + literal;
            return this;
        }

        public CompleteCondition lexicalNotAfter(String literal) {
            result += FiqlParser.LE + literal;
            return this;
        }

        public CompleteCondition lexicalNotBefore(String literal) {
            result += FiqlParser.GE + literal;
            return this;
        }

        public CompleteCondition notAfter(Date date) {
            result += FiqlParser.LE + toString(date);
            return this;
        }

        public CompleteCondition notBefore(Date date) {
            result += FiqlParser.GE + toString(date);
            return this;
        }

        public CompleteCondition notEqualTo(String literalOrPattern) {
            result += FiqlParser.NEQ + literalOrPattern;
            return this;
        }

        public CompleteCondition notEqualTo(double number) {
            result += FiqlParser.NEQ + number;
            return this;
        }

        public CompleteCondition notEqualTo(Date date) {
            result += FiqlParser.NEQ + toString(date);
            return this;
        }

        public CompleteCondition after(Duration distanceFromNow) {
            result += FiqlParser.GT + distanceFromNow;
            return this;
        }

        public CompleteCondition before(Duration distanceFromNow) {
            result += FiqlParser.LT + distanceFromNow;
            return this;
        }

        public CompleteCondition equalTo(Duration distanceFromNow) {
            result += FiqlParser.EQ + distanceFromNow;
            return this;
        }

        public CompleteCondition notAfter(Duration distanceFromNow) {
            result += FiqlParser.LE + distanceFromNow;
            return this;
        }

        public CompleteCondition notBefore(Duration distanceFromNow) {
            result += FiqlParser.GE + distanceFromNow;
            return this;
        }

        public CompleteCondition notEqualTo(Duration distanceFromNow) {
            result += FiqlParser.NEQ + distanceFromNow;
            return this;
        }

        public PartialCondition and() {
            result += FiqlParser.AND;
            return this;
        }

        public PartialCondition or() {
            result += FiqlParser.OR;
            return this;
        }

        public CompleteCondition and(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn) {
            result += "(" + ((Builder)c1).buildPartial(this) + FiqlParser.AND
                      + ((Builder)c2).buildPartial(this);
            for (CompleteCondition c : cn) {
                result += FiqlParser.AND + ((Builder)c).buildPartial(this);
            }
            result += ")";
            return this;
        }

        public CompleteCondition or(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn) {
            result += "(" + ((Builder)c1).buildPartial(this) + FiqlParser.OR
                      + ((Builder)c2).buildPartial(this);
            for (CompleteCondition c : cn) {
                result += FiqlParser.OR + ((Builder)c).buildPartial(this);
            }
            result += ")";
            return this;
        }

        public Property is(String property) {
            Builder b = new Builder(this);
            b.result = property;
            return b;
        }

        private String toString(Date date) {
            String s = df.format(date);
            if (timeZoneSupported) {
                // zone in XML is "+01:00" in Java is "+0100"; adding semicolon
                int len = s.length();
                return s.substring(0, len - 2) + ":" + s.substring(len - 2, len);
            } else {
                return s;
            }
        }
    }

}
