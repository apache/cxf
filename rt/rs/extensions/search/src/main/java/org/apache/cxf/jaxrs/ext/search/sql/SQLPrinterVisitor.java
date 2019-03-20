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
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractUntypedSearchConditionVisitor;


public class SQLPrinterVisitor<T> extends AbstractUntypedSearchConditionVisitor<T, String> {

    private String primaryTable;
    private String tableAlias;
    private List<String> columns;
    private StringBuilder topBuilder = new StringBuilder();
    private volatile boolean joinDone;
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
        this.primaryTable = table;
        this.tableAlias = tableAlias;
        prepareTopStringBuilder();
    }

    public void visit(SearchCondition<T> sc) {
        StringBuilder sb = getStringBuilder();

        PrimitiveStatement statement = sc.getStatement();
        if (statement != null) {
            if (statement.getProperty() != null) {

                String property = statement.getProperty();
                String[] properties =  property.split("\\.");
                if (properties.length > 2) {
                    throw new SearchParseException("SQL Visitor supports only a single JOIN");
                } else if (properties.length == 2) {
                    if (joinDone) {
                        throw new SearchParseException("SQL Visitor has already created JOIN");
                    }
                    joinDone = true;
                    String joinTable = getRealPropertyName(properties[0]);
                    // Joining key can be pre-configured
                    String joiningKey = primaryTable;
                    if (joiningKey.endsWith("s")) {
                        joiningKey = joiningKey.substring(0, joiningKey.length() - 1);
                    }
                    joiningKey += "_id";

                    topBuilder.append(" left join ").append(joinTable);
                    topBuilder.append(" on ").append(primaryTable).append(".id").append(" = ")
                        .append(joinTable).append('.').append(joiningKey);

                    property = joinTable + "." + getRealPropertyName(properties[1]);
                }

                String name = getRealPropertyName(property);
                String originalValue = getPropertyValue(name, statement.getValue());
                validatePropertyValue(name, originalValue);

                String value = SearchUtils.toSqlWildcardString(originalValue, isWildcardStringMatch());
                value = SearchUtils.duplicateSingleQuoteIfNeeded(value);

                if (tableAlias != null) {
                    name = tableAlias + "." + name;
                }

                sb.append(name).append(' ').append(
                            SearchUtils.conditionTypeToSqlOperator(sc.getConditionType(), value,
                                                                   originalValue))
                            .append(' ').append("'").append(value).append("'"); //NOPMD
            }
        } else {
            boolean first = true;
            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                if (!first) {
                    sb.append(' ').append(sc.getConditionType().toString()).append(' ');
                } else {
                    first = false;
                }
                sb.append('(');
                saveStringBuilder(sb);
                condition.accept(this);
                sb = getStringBuilder();
                sb.append(')');
            }
        }

        saveStringBuilder(sb);
    }

    protected StringBuilder getStringBuilder() {
        StringBuilder sb = super.getStringBuilder();
        if (sb == null) {
            sb = new StringBuilder();
        }
        return sb;
    }


    @Override
    public String getQuery() {
        StringBuilder sb = removeStringBuilder();
        return sb == null ? null : topBuilder.toString() + " WHERE " + sb.toString();
    }

    private void prepareTopStringBuilder() {
        if (primaryTable != null) {
            SearchUtils.startSqlQuery(topBuilder, primaryTable, tableAlias, columns);
        }
    }

}
