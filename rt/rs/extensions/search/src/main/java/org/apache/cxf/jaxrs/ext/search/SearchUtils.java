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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.search.sql.SQLPrinterVisitor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;

public final class SearchUtils {
    public static final String TIMESTAMP_NO_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String TIMESTAMP_WITH_TIMEZONE_Z = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATETIME_FORMAT = TIMESTAMP_NO_TIMEZONE;
    public static final String DEFAULT_OFFSET_DATETIME_FORMAT = TIMESTAMP_WITH_TIMEZONE_Z;
    public static final String DEFAULT_ZONE_DATETIME_FORMAT = TIMESTAMP_WITH_TIMEZONE_Z + "'['z']'";
    public static final String DATE_FORMAT_PROPERTY = "search.date-format";
    public static final String TIMEZONE_SUPPORT_PROPERTY = "search.timezone.support";
    public static final String LAX_PROPERTY_MATCH = "search.lax.property.match";
    public static final String BEAN_PROPERTY_MAP = "search.bean.property.map";
    public static final String BEAN_PROPERTY_CONVERTER = "search.bean.property.converter";
    public static final String SEARCH_VISITOR_PROPERTY = "search.visitor";
    public static final String DECODE_QUERY_VALUES = "search.decode.values";
    public static final String ESCAPE_UNDERSCORE_CHAR = "search.escape.underscore.char";

    private static final Logger LOG = LogUtils.getL7dLogger(SearchUtils.class);

    private SearchUtils() {

    }

    public static SimpleDateFormat getContextualDateFormat() {
        Message m = PhaseInterceptorChain.getCurrentMessage();

        if (m != null) {
            return getDateFormat((String)m.getContextualProperty(DATE_FORMAT_PROPERTY));
        }

        return null;
    }

    private static boolean escapeUnderscoreChar() {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        return MessageUtils.getContextualBoolean(m, ESCAPE_UNDERSCORE_CHAR, true);
    }

    public static SimpleDateFormat getContextualDateFormatOrDefault(final String pattern) {
        final SimpleDateFormat format = getContextualDateFormat();
        return format != null ? format : new SimpleDateFormat(pattern);
    }


    public static Date dateFromStringWithContextProperties(String value) {
        try {
            final SimpleDateFormat format = getContextualDateFormat();
            if (format != null) {
                return format.parse(value);
            }
        } catch (ParseException ex) {
            LOG.log(Level.FINE, "Unable to parse date using contextual date format specification", ex);
        }

        return dateFromStringWithDefaultFormats(value);
    }

    public static SimpleDateFormat getDateFormatOrDefault(Map<String, String> properties, String pattern) {
        return getDateFormatOrDefault(properties.get(DATE_FORMAT_PROPERTY), pattern);
    }

    public static SimpleDateFormat getDateFormat(Map<String, String> properties) {
        return getDateFormat(properties.get(DATE_FORMAT_PROPERTY));
    }

    public static SimpleDateFormat getDateFormatOrDefault(String dfProperty, String pattern) {
        return new SimpleDateFormat(dfProperty == null ? pattern : dfProperty);
    }

    public static SimpleDateFormat getDateFormat(String dfProperty) {
        return getDateFormatOrDefault(dfProperty, DEFAULT_DATE_FORMAT);
    }

    public static boolean isTimeZoneSupported(Map<String, String> properties, Boolean defaultValue) {
        String tzProperty = properties.get(SearchUtils.TIMEZONE_SUPPORT_PROPERTY);
        return tzProperty == null ? defaultValue : Boolean.valueOf(tzProperty);
    }

    public static <T> String toSQL(SearchCondition<T> sc, String table, String... columns) {
        SQLPrinterVisitor<T> visitor = new SQLPrinterVisitor<>(table, columns);
        sc.accept(visitor);
        return visitor.getQuery();
    }

    public static String toSqlWildcardString(String value, boolean alwaysWildcard) {
        if (value.contains("\\")) {
            value = value.replaceAll("\\\\", "\\\\\\\\");
        }
        if (value.contains("_") && escapeUnderscoreChar()) {
            value = value.replaceAll("_", "\\\\_");
        }
        if (value.contains("%")) {
            value = value.replaceAll("%", "\\\\%");
        }
        if (!containsWildcard(value)) {
            return alwaysWildcard ? "%" + value + "%" : value;
        }
        value = value.replaceAll("\\*", "%");
        return value;
    }

    public static String duplicateSingleQuoteIfNeeded(String value) {
        if (value.indexOf('\'') != -1 && value.indexOf("\'\'") == -1) {
            value = value.replaceAll("\'", "\'\'");
        }
        return value;
    }

    public static boolean containsEscapedChar(String value) {
        return containsEscapedPercent(value) || value.contains("\\\\") || value.contains("\\_");
    }

    public static boolean containsWildcard(String value) {
        return value.contains("*");
    }

    public static boolean containsEscapedPercent(String value) {
        return value.contains("\\%");
    }

    public static void startSqlQuery(StringBuilder sb,
                                     String table,
                                     String tableAlias,
                                     List<String> columns) {
        sb.append("SELECT ");
        if (columns != null && !columns.isEmpty()) {
            for (int i = 0; i < columns.size(); i++) {
                sb.append(columns.get(i));
                if (i + 1 < columns.size()) {
                    sb.append(',');
                }
            }
        } else {
            sb.append('*');
        }
        sb.append(" FROM ").append(table);
        if (tableAlias != null) {
            sb.append(' ').append(tableAlias);
        }
    }

    public static String conditionTypeToSqlOperator(ConditionType ct, String value, String originalValue) {
        // TODO : if we have the same column involved, ex a >= 123 and a <=244 then
        // we may try to use IN or BETWEEN, depending on the values
        final boolean wildcardAvailable = SearchUtils.containsWildcard(originalValue);
        String op;
        switch (ct) {
        case EQUALS:
            op = wildcardAvailable ? "LIKE" : "=";
            break;
        case NOT_EQUALS:
            op = wildcardAvailable ? "NOT LIKE" : "<>";
            break;
        case GREATER_THAN:
            op = ">";
            break;
        case GREATER_OR_EQUALS:
            op = ">=";
            break;
        case LESS_THAN:
            op = "<";
            break;
        case LESS_OR_EQUALS:
            op = "<=";
            break;
        default:
            String msg = String.format("Condition type %s is not supported", ct.name());
            throw new RuntimeException(msg);
        }
        return op;
    }

    public static Date dateFromStringWithDefaultFormats(String value) {
        value = value.replaceAll("Z$", "+0000");
        Date date = timestampFromString(new SimpleDateFormat(TIMESTAMP_WITH_TIMEZONE_Z), value);

        if (date == null) {
            date = timestampFromString(new SimpleDateFormat(TIMESTAMP_NO_TIMEZONE), value);
        }

        if (date == null) {
            date = timestampFromString(getContextualDateFormatOrDefault(DEFAULT_DATE_FORMAT), value);
        }

        return date;
    }

    private static Date timestampFromString(final SimpleDateFormat formatter, final String value) {
        try {
            return formatter.parse(value);
        } catch (final ParseException ex) {
            LOG.log(Level.FINE, "Unable to parse date using format specification: " + formatter.toPattern(), ex);
            return null;
        }
    }



}
