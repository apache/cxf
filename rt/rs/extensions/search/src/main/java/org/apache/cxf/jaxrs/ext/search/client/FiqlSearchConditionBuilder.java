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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.Duration;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
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

    protected Map<String, String> properties;

    public FiqlSearchConditionBuilder() {
        this(Collections.<String, String> emptyMap());
    }

    public FiqlSearchConditionBuilder(Map<String, String> properties) {
        this.properties = properties;
    }

    protected Builder newBuilderInstance() {
        return new Builder(properties);
    }

    public String query() {
        return "";
    }

    public Property is(String property) {
        return newBuilderInstance().is(property);
    }

    public CompleteCondition and(CompleteCondition c1, CompleteCondition c2,
                                 CompleteCondition... cn) {
        return newBuilderInstance().and(c1, c2, cn);
    }

    public CompleteCondition and(List<CompleteCondition> conditions) {
        return newBuilderInstance().and(conditions);
    }

    public CompleteCondition or(List<CompleteCondition> conditions) {
        return newBuilderInstance().or(conditions);
    }

    public CompleteCondition or(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn) {
        return newBuilderInstance().or(c1, c2, cn);
    }

    protected static class Builder implements Property, CompleteCondition, PartialCondition {
        protected String result = "";
        protected Builder parent;
        protected DateFormat df;
        protected boolean timeZoneSupported;
        protected String currentCompositeOp;

        public Builder(Map<String, String> properties) {
            parent = null;
            df = SearchUtils.getDateFormat(properties);
            timeZoneSupported = SearchUtils.isTimeZoneSupported(properties, Boolean.FALSE);
        }

        public Builder(Builder parent) {
            this.parent = parent;
            df = parent.getDateFormat();
            timeZoneSupported = parent.isTimeZoneSupported();
        }

        public String query() {
            return buildPartial(null);
        }

        protected DateFormat getDateFormat() {
            return df;
        }

        protected boolean isTimeZoneSupported() {
            return timeZoneSupported;
        }

        // builds from parent but not further then exclude builder
        protected String buildPartial(Builder exclude) {
            if (parent != null && !parent.equals(exclude)) {
                return parent.buildPartial(exclude) + result;
            }
            return result;
        }

        public CompleteCondition after(Date date) {
            return condition(FiqlParser.GT, toString(date));
        }

        public CompleteCondition before(Date date) {
            return condition(FiqlParser.LT, toString(date));
        }

        public CompleteCondition comparesTo(ConditionType type, String value) {

            return condition(toFiqlPrimitiveCondition(type), value);
        }

        public CompleteCondition comparesTo(ConditionType type, Double value) {

            return condition(toFiqlPrimitiveCondition(type), value);
        }

        public CompleteCondition comparesTo(ConditionType type, Integer value) {

            return condition(toFiqlPrimitiveCondition(type), value);
        }

        public CompleteCondition comparesTo(ConditionType type, Long value) {

            return condition(toFiqlPrimitiveCondition(type), value);
        }

        public CompleteCondition comparesTo(ConditionType type, Date value) {

            return condition(toFiqlPrimitiveCondition(type), value);
        }

        public CompleteCondition comparesTo(ConditionType type, Duration value) {

            return condition(toFiqlPrimitiveCondition(type), value);
        }

        public CompleteCondition equalTo(String value, String...moreValues) {
            return condition(FiqlParser.EQ, value, (Object[])moreValues);
        }

        public CompleteCondition equalTo(Double number, Double... moreValues) {
            return condition(FiqlParser.EQ, number, (Object[])moreValues);
        }

        public CompleteCondition equalTo(Long number, Long... moreValues) {
            return condition(FiqlParser.EQ, number, (Object[])moreValues);
        }

        public CompleteCondition equalTo(Integer number, Integer... moreValues) {
            return condition(FiqlParser.EQ, number, (Object[])moreValues);
        }

        public CompleteCondition equalTo(Date date, Date... moreValues) {
            return condition(FiqlParser.EQ, date, (Object[])moreValues);
        }

        public CompleteCondition equalTo(Duration distanceFromNow, Duration... moreValues) {
            return condition(FiqlParser.EQ, distanceFromNow, (Object[])moreValues);
        }


        public CompleteCondition greaterOrEqualTo(Double number) {
            return condition(FiqlParser.GE, number);
        }

        public CompleteCondition greaterOrEqualTo(Long number) {
            return condition(FiqlParser.GE, number);
        }

        public CompleteCondition greaterOrEqualTo(Integer number) {
            return condition(FiqlParser.GE, number);
        }

        public CompleteCondition greaterThan(Double number) {
            return condition(FiqlParser.GT, number);
        }

        public CompleteCondition greaterThan(Long number) {
            return condition(FiqlParser.GT, number);
        }

        public CompleteCondition greaterThan(Integer number) {
            return condition(FiqlParser.GT, number);
        }

        public CompleteCondition lessOrEqualTo(Double number) {
            return condition(FiqlParser.LE, number);
        }

        public CompleteCondition lessOrEqualTo(Long number) {
            return condition(FiqlParser.LE, number);
        }

        public CompleteCondition lessOrEqualTo(Integer number) {
            return condition(FiqlParser.LE, number);
        }

        public CompleteCondition lessThan(Double number) {
            return condition(FiqlParser.LT, number);
        }

        public CompleteCondition lessThan(Long number) {
            return condition(FiqlParser.LT, number);
        }

        public CompleteCondition lessThan(Integer number) {
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

        public CompleteCondition notEqualTo(Double number) {
            return condition(FiqlParser.NEQ, number);
        }

        public CompleteCondition notEqualTo(Long number) {
            return condition(FiqlParser.NEQ, number);
        }

        public CompleteCondition notEqualTo(Integer number) {
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

        public CompleteCondition notAfter(Duration distanceFromNow) {
            return condition(FiqlParser.LE, distanceFromNow);
        }

        public CompleteCondition notBefore(Duration distanceFromNow) {
            return condition(FiqlParser.GE, distanceFromNow);
        }

        public CompleteCondition notEqualTo(Duration distanceFromNow) {
            return condition(FiqlParser.NEQ, distanceFromNow);
        }

        protected CompleteCondition condition(String operator, Object value, Object...moreValues) {
            String name = result;
            result += operator + toString(value);
            if (moreValues != null && moreValues.length > 0) {
                for (Object next : moreValues) {
                    result += "," + name + operator + toString(next);
                }
                currentCompositeOp = FiqlParser.OR;
            }
            return this;
        }

        public PartialCondition and() {
            if (FiqlParser.OR.equals(currentCompositeOp) 
                || parent != null && FiqlParser.OR.equals(parent.currentCompositeOp)) {
                if (parent != null) {
                    parent.result = "(" + parent.result;
                    result += ")";
                } else {
                    wrap();
                }
                currentCompositeOp = FiqlParser.AND;
            }
            result += FiqlParser.AND;
            return this;
        }

        public Property and(String name) {
            return and().is(name);
        }

        public PartialCondition or() {
            if (FiqlParser.AND.equals(currentCompositeOp) 
                || parent != null && FiqlParser.AND.equals(parent.currentCompositeOp)) {
                if (parent != null) {
                    parent.result = "(" + parent.result;
                    result += ")";
                } else {
                    wrap();
                }
                currentCompositeOp = FiqlParser.OR;
            }
            result += FiqlParser.OR;
            return this;
        }

        public Property or(String name) {
            return or().is(name);
        }

        public CompleteCondition wrap() {
            result = "(" + result + ")";
            this.currentCompositeOp = null;
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

        public CompleteCondition and(List<CompleteCondition> conditions) {
            return conditionsList(FiqlParser.AND, conditions);
        }

        public CompleteCondition or(List<CompleteCondition> conditions) {
            return conditionsList(FiqlParser.OR, conditions);
        }

        protected CompleteCondition conditionsList(String op, List<CompleteCondition> conditions) {
            if (conditions.size() == 1) {
                result += ((Builder)conditions.get(0)).buildPartial(this);
            } else {
                result += "(";

                for (Iterator<CompleteCondition> it = conditions.iterator(); it.hasNext();) {
                    result += ((Builder)it.next()).buildPartial(this);
                    if (it.hasNext()) {
                        result += op;
                    }
                }
                result += ")";
            }

            return this;
        }


        public Property is(String property) {
            Builder b = new Builder(this);
            b.result = property;
            return b;
        }

        protected String toString(Object value) {
            if (value == null) {
                return null;
            }
            if (value.getClass() == Date.class) {
                String s = df.format((Date)value);
                if (timeZoneSupported) {
                    // zone in XML is "+01:00" in Java is "+0100"; adding semicolon
                    int len = s.length();
                    return s.substring(0, len - 2) + ":" + s.substring(len - 2, len);
                }
                return s;
            }
            return value.toString();
        }

        protected String toFiqlPrimitiveCondition(ConditionType type) {
            String fiqlType = FiqlParser.CONDITION_MAP.get(type);
            if (fiqlType == null) {
                throw new IllegalArgumentException("Only primitive condition types are supported");
            }
            return fiqlType;
        }
    }

}
