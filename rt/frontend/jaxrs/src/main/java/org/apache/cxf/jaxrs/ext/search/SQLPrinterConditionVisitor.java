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


public class SQLPrinterConditionVisitor<T> implements SearchConditionVisitor<T> {

    private StringBuilder sb;
    private String table;
    private String[] columns;

    
    public SQLPrinterConditionVisitor(String table, String... columns) {
        this.columns = columns;
        this.table = table;
    }
    
    
    @Override
    public void visit(SearchCondition<T> sc) {
        
        if (sb == null) {
            sb = new StringBuilder();
            if (table != null) {
                SearchUtils.startSqlQuery(sb, table, columns);
            }
        }
        
        PrimitiveStatement statement = sc.getStatement();
        if (statement != null) {
            if (statement.getProperty() != null) {
                String rvalStr = statement.getValue().toString().replaceAll("\\*", "%");
                sb.append(statement.getProperty()).append(" ").append(
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
    

    public String getResult() {
        return sb == null ? null : sb.toString();
    }
}
