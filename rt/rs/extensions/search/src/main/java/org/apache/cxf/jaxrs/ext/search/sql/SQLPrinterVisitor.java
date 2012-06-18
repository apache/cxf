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
package org.apache.cxf.jaxrs.ext.search.sql;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.search.AbstractSearchConditionVisitor;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;


public class SQLPrinterVisitor<T> extends AbstractSearchConditionVisitor<T> {

    private StringBuilder sb;
    private String table;
    private String tableAlias;
    private List<String> columns;
    
    public SQLPrinterVisitor(String table, String... columns) {
        this(null, table, Arrays.asList(columns));
    }
    
    public SQLPrinterVisitor(Map<String, String> fieldMap, 
                             String table,
                             List<String> columns) {
        this(fieldMap, table, null, columns);
    }
    
    public SQLPrinterVisitor(Map<String, String> fieldMap, 
                             String table, 
                             String tableAlias,
                             List<String> columns) {
        super(fieldMap);
        this.columns = columns;
        this.table = table;
        this.tableAlias = tableAlias;
    }
    
    public void visit(SearchCondition<T> sc) {
        
        if (sb == null) {
            sb = new StringBuilder();
            if (table != null) {
                SearchUtils.startSqlQuery(sb, table, tableAlias, columns);
            }
        }
        
        PrimitiveStatement statement = sc.getStatement();
        if (statement != null) {
            if (statement.getProperty() != null) {
                String rvalStr = statement.getValue().toString().replaceAll("\\*", "%");
                String name = getRealPropertyName(statement.getProperty());
               
                if (tableAlias != null) {
                    name = tableAlias + "." + name;
                }
                
                sb.append(name).append(" ").append(
                            SearchUtils.conditionTypeToSqlOperator(sc.getConditionType(), rvalStr))
                            .append(" ").append("'").append(rvalStr).append("'");
            }
        } else {
            boolean first = true;
            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                if (!first) {
                    sb.append(" ").append(sc.getConditionType().toString()).append(" ");
                } else {
                    first = false;
                }
                sb.append("(");
                condition.accept(this);
                sb.append(")");
            }
        }
    }
    

    @Deprecated
    public String getResult() {
        return getQuery();
    }
    
    public String getQuery() {
        return sb == null ? null : sb.toString();
    }
}
