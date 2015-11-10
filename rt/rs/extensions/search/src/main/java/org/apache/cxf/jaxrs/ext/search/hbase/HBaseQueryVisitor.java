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
package org.apache.cxf.jaxrs.ext.search.hbase;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.ByteArrayComparable;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

public class HBaseQueryVisitor<T> extends AbstractSearchConditionVisitor<T, Filter> {

    private Stack<List<Filter>> queryStack = new Stack<List<Filter>>();
    private String family;
    private Map<String, String> familyMap;
    public HBaseQueryVisitor(String family) {
        this(family, Collections.<String, String>emptyMap());
    }
    public HBaseQueryVisitor(String family, Map<String, String> fieldsMap) {
        super(fieldsMap);
        this.family = family;
        queryStack.push(new ArrayList<Filter>());
    }
    public HBaseQueryVisitor(Map<String, String> familyMap) {
        this(familyMap, Collections.<String, String>emptyMap());
    }
    public HBaseQueryVisitor(Map<String, String> familyMap,
                             Map<String, String> fieldsMap) {
        super(fieldsMap);
        this.familyMap = familyMap;
        queryStack.push(new ArrayList<Filter>());
    }
    
    public void visit(SearchCondition<T> sc) {
        PrimitiveStatement statement = sc.getStatement();
        if (statement != null) {
            if (statement.getProperty() != null) {
                queryStack.peek().add(buildSimpleQuery(sc.getConditionType(), 
                                         statement.getProperty(), 
                                         statement.getValue()));
            }
        } else {
            queryStack.push(new ArrayList<Filter>());
            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                condition.accept(this);
            }
            boolean orCondition = sc.getConditionType() == ConditionType.OR;
            List<Filter> queries = queryStack.pop();
            queryStack.peek().add(createCompositeQuery(queries, orCondition));
        }    
    }

    public Filter getQuery() {
        List<Filter> queries = queryStack.peek();
        return queries.isEmpty() ? null : queries.get(0);
    }
    
    private Filter buildSimpleQuery(ConditionType ct, String name, Object value) {
        name = super.getRealPropertyName(name);
        validatePropertyValue(name, value);
        Class<?> clazz = getPrimitiveFieldClass(name, value.getClass());
        CompareOp compareOp = null;
        boolean regexCompRequired = false;
        switch (ct) {
        case EQUALS:
            compareOp = CompareOp.EQUAL;
            regexCompRequired = String.class == clazz && value.toString().endsWith("*");
            break;
        case NOT_EQUALS:
            compareOp = CompareOp.NOT_EQUAL;
            regexCompRequired = String.class == clazz && value.toString().endsWith("*");
            break;
        case GREATER_THAN:
            compareOp = CompareOp.GREATER;
            break;
        case GREATER_OR_EQUALS:
            compareOp = CompareOp.GREATER_OR_EQUAL;
            break;
        case LESS_THAN:
            compareOp = CompareOp.LESS;
            break;
        case LESS_OR_EQUALS:
            compareOp = CompareOp.LESS_OR_EQUAL;
            break;
        default: 
            break;
        }
        String qualifier = name;
        String theFamily = family != null ? family : familyMap.get(qualifier);
        ByteArrayComparable byteArrayComparable = regexCompRequired 
            ? new RegexStringComparator(value.toString().replace("*", "."))
            : new BinaryComparator(value.toString().getBytes(StandardCharsets.UTF_8));
        
        Filter query = new SingleColumnValueFilter(theFamily.getBytes(StandardCharsets.UTF_8),
                                                   qualifier.getBytes(StandardCharsets.UTF_8),
                                                   compareOp,
                                                   byteArrayComparable);
        return query;
    }
    
    private Filter createCompositeQuery(List<Filter> queries, boolean orCondition) {
        
        FilterList.Operator oper = orCondition ? FilterList.Operator.MUST_PASS_ONE
            : FilterList.Operator.MUST_PASS_ALL;
        FilterList list = new FilterList(oper);
        for (Filter query : queries) {
            list.addFilter(query);
        }
        return list;
    }
    
}
