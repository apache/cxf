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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.search.sql.SQLPrinterVisitor;

public final class SearchUtils {
    
    public static final String DATE_FORMAT_PROPERTY = "search.date-format";
    public static final String TIMEZONE_SUPPORT_PROPERTY = "search.timezone.support";
    
    private SearchUtils() {
        
    }
    
    public static SimpleDateFormat getDateFormat(Map<String, String> properties, String defaultFormat) {
        String dfProperty = properties.get(DATE_FORMAT_PROPERTY);
        return new SimpleDateFormat(dfProperty == null ? defaultFormat : dfProperty);    
    }
    
    public static boolean isTimeZoneSupported(Map<String, String> properties, Boolean defaultValue) {
        String tzProperty = properties.get(SearchUtils.TIMEZONE_SUPPORT_PROPERTY);
        return tzProperty == null ? defaultValue : Boolean.valueOf(tzProperty);    
    }
    
    public static <T> String toSQL(SearchCondition<T> sc, String table, String... columns) {
        SQLPrinterVisitor<T> visitor = new SQLPrinterVisitor<T>(table, columns);
        sc.accept(visitor);
        return visitor.getQuery();
    }
    
    public static void startSqlQuery(StringBuilder sb, 
                                     String table,
                                     String tableAlias,
                                     List<String> columns) {
        sb.append("SELECT ");
        if (columns != null && columns.size() > 0) {
            for (int i = 0; i < columns.size(); i++) {
                sb.append(columns.get(i));
                if (i + 1 < columns.size()) {
                    sb.append(",");
                }
            }
        } else {
            sb.append("*");
        }
        sb.append(" FROM ").append(table);
        if (tableAlias != null) {
            sb.append(" " + tableAlias);
        }
        sb.append(" WHERE ");
    }
    
    public static String conditionTypeToSqlOperator(ConditionType ct, String value) {
        // TODO : if we have the same column involved, ex a >= 123 and a <=244 then 
        // we may try to use IN or BETWEEN, depending on the values
        String op;
        switch (ct) {
        case EQUALS:
            op = value.contains("%") ? "LIKE" : "=";
            break;
        case NOT_EQUALS:
            op = value.contains("%") ? "NOT LIKE" : "<>";
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
}
