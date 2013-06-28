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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractUntypedSearchConditionVisitor;


public class SQLPrinterVisitor<T> extends AbstractUntypedSearchConditionVisitor<T, String> {

    private String table;
    private String tableAlias;
    private List<String> columns;
    
    // Can be useful when some other code will build Select and From clauses.
    public SQLPrinterVisitor() {
        this(null, null, Collections.<String>emptyList());
    }
    
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
        StringBuilder sb = getStringBuilder();
        
        PrimitiveStatement statement = sc.getStatement();
        if (statement != null) {
            if (statement.getProperty() != null) {
                String name = getRealPropertyName(statement.getProperty());
                String value = getPropertyValue(name, statement.getValue());
                validatePropertyValue(name, value);
                String rvalStr = value.replaceAll("\\*", "%");
                
                                
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
                saveStringBuilder(sb);
                condition.accept(this);
                sb = getStringBuilder();
                sb.append(")");
            }
        }
        
        saveStringBuilder(sb);
    }
    
    protected StringBuilder getStringBuilder() {
        StringBuilder sb = super.getStringBuilder();
        if (sb == null) {
            sb = new StringBuilder();
            if (table != null) {
                SearchUtils.startSqlQuery(sb, table, tableAlias, columns);
            }
        }
        return sb;
    }
    
    
    
}
