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

import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;

/**
 * Builds a FIQL search condition.
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

    public Property is(String property) {
        return new Builder(properties).is(property);
    }

    public CompleteCondition and(CompleteCondition c1, CompleteCondition c2, 
                                 CompleteCondition... cn) {
        return new Builder(properties).and(c1, c2, cn);
    }

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
            return condition(FiqlParser.GT, toString(date));
        }

        public CompleteCondition before(Date date) {
            return condition(FiqlParser.LT, toString(date));
        }

        public CompleteCondition equalTo(String literalOrPattern) {
            return condition(FiqlParser.EQ, literalOrPattern);
        }

        public CompleteCondition equalTo(double number) {
            return condition(FiqlParser.EQ, number);
        }
        
        public CompleteCondition equalTo(long number) {
            return condition(FiqlParser.EQ, number);
        }

        public CompleteCondition equalTo(Date date) {
            return condition(FiqlParser.EQ, toString(date));
        }

        public CompleteCondition greaterOrEqualTo(double number) {
            return condition(FiqlParser.GE, number);
        }
        
        public CompleteCondition greaterOrEqualTo(long number) {
            return condition(FiqlParser.GE, number);
        }

        public CompleteCondition greaterThan(double number) {
            return condition(FiqlParser.GT, number);
        }
        
        public CompleteCondition greaterThan(long number) {
            return condition(FiqlParser.GT, number);
        }

        public CompleteCondition lessOrEqualTo(double number) {
            return condition(FiqlParser.LE, number);
        }
        
        public CompleteCondition lessOrEqualTo(long number) {
            return condition(FiqlParser.LE, number);
        }

        public CompleteCondition lessThan(double number) {
            return condition(FiqlParser.LT, number);
        }
        
        public CompleteCondition lessThan(long number) {
            return condition(FiqlParser.LT, number);
        }

        public CompleteCondition lexicalAfter(String literal) {
            return condition(FiqlParser.GT, literal);
        }

        public CompleteCondition lexicalBefore(String literal) {
            return condition(FiqlParser.LT, literal);
        }

        public CompleteCondition lexicalNotAfter(String literal) {
            return condition(FiqlParser.LE, literal);
        }

        public CompleteCondition lexicalNotBefore(String literal) {
            return condition(FiqlParser.GE, literal);
        }

        public CompleteCondition notAfter(Date date) {
            return condition(FiqlParser.LE, toString(date));
        }

        public CompleteCondition notBefore(Date date) {
            return condition(FiqlParser.GE, toString(date));
        }

        public CompleteCondition notEqualTo(String literalOrPattern) {
            return condition(FiqlParser.NEQ, literalOrPattern);
        }

        public CompleteCondition notEqualTo(double number) {
            return condition(FiqlParser.NEQ, number);
        }
        
        public CompleteCondition notEqualTo(long number) {
            return condition(FiqlParser.NEQ, number);
        }

        public CompleteCondition notEqualTo(Date date) {
            return condition(FiqlParser.NEQ, toString(date));
        }

        public CompleteCondition after(Duration distanceFromNow) {
            return condition(FiqlParser.GT, distanceFromNow);
        }

        public CompleteCondition before(Duration distanceFromNow) {
            return condition(FiqlParser.LT, distanceFromNow);
        }

        public CompleteCondition equalTo(Duration distanceFromNow) {
            return condition(FiqlParser.EQ, distanceFromNow);
        }

        public CompleteCondition notAfter(Duration distanceFromNow) {
            return condition(FiqlParser.LE, distanceFromNow);
        }

        public CompleteCondition notBefore(Duration distanceFromNow) {
            return condition(FiqlParser.GE, distanceFromNow);
        }

        public CompleteCondition notEqualTo(Duration distanceFromNow) {
            return condition(FiqlParser.NEQ, distanceFromNow);
        }

        protected CompleteCondition condition(String operator, Object value) {
            result += operator + value;
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
