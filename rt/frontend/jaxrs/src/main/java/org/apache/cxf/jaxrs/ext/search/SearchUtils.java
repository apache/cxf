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

import org.apache.cxf.jaxrs.ext.search.sql.SQLPrinterVisitor;

public final class SearchUtils {
    
    private SearchUtils() {
        
    }
    
    public static <T> String toSQL(SearchCondition<T> sc, String table, String... columns) {
        SearchConditionVisitor<T> visitor = new SQLPrinterVisitor<T>(table, columns);
        sc.accept(visitor);
        return visitor.getResult();
    }
    
    public static void startSqlQuery(StringBuilder sb, String table, String... columns) {
        sb.append("SELECT ");
        if (columns.length > 0) {
            for (int i = 0; i < columns.length; i++) {
                sb.append(columns[i]);
                if (i + 1 < columns.length) {
                    sb.append(",");
                }
            }
        } else {
            sb.append("*");
        }
        sb.append(" FROM ").append(table).append(" WHERE ");
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
