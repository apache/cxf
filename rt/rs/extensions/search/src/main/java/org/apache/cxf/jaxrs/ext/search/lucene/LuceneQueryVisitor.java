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
package org.apache.cxf.jaxrs.ext.search.lucene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;

import static org.apache.cxf.jaxrs.ext.search.ParamConverterUtils.getString;
import static org.apache.cxf.jaxrs.ext.search.ParamConverterUtils.getValue;

public class LuceneQueryVisitor<T> extends AbstractSearchConditionVisitor<T, Query> {

    private String contentsFieldName;
    private Map<String, String> contentsFieldMap;
    private boolean caseInsensitiveMatch;
    private Stack<List<Query>> queryStack = new Stack<List<Query>>();
    public LuceneQueryVisitor() {
        this(Collections.<String, String>emptyMap());        
    }
    
    public LuceneQueryVisitor(String contentsFieldAlias, String contentsFieldName) {
        this(Collections.singletonMap(contentsFieldAlias, contentsFieldName));
    }
     
    public LuceneQueryVisitor(String contentsFieldName) {
        this(Collections.<String, String>emptyMap(), contentsFieldName);
    }
    
    public LuceneQueryVisitor(Map<String, String> fieldsMap) {
        this(fieldsMap, null);
    }
    
    public LuceneQueryVisitor(Map<String, String> fieldsMap, String contentsFieldName) {
        super(fieldsMap);
        this.contentsFieldName = contentsFieldName;
        queryStack.push(new ArrayList<Query>());
    }
    
    public void setContentsFieldMap(Map<String, String> map) {
        this.contentsFieldMap = map;
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
            queryStack.push(new ArrayList<Query>());
            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                condition.accept(this);
            }
            boolean orCondition = sc.getConditionType() == ConditionType.OR;
            List<Query> queries = queryStack.pop();
            queryStack.peek().add(createCompositeQuery(queries, orCondition));
        }    
    }
    
    public Query getQuery() {
        List<Query> queries = queryStack.peek();
        return queries.isEmpty() ? null : queries.get(0);
    }
    
    private Query buildSimpleQuery(ConditionType ct, String name, Object value) {
        name = super.getRealPropertyName(name);
        validatePropertyValue(name, value);
        
        Class<?> clazz = getPrimitiveFieldClass(name, value.getClass());
        
        
        Query query = null;
        switch (ct) {
        case EQUALS:
            query = createEqualsQuery(clazz, name, value);
            break;
        case NOT_EQUALS:
            BooleanQuery booleanQuery = new BooleanQuery();
            booleanQuery.add(createEqualsQuery(clazz, name, value),
                             BooleanClause.Occur.MUST_NOT);
            query = booleanQuery;
            break;
        case GREATER_THAN:
            query = createRangeQuery(clazz, name, value, ct);
            break;
        case GREATER_OR_EQUALS:
            query = createRangeQuery(clazz, name, value, ct);
            break;
        case LESS_THAN:
            query = createRangeQuery(clazz, name, value, ct);
            break;
        case LESS_OR_EQUALS:
            query = createRangeQuery(clazz, name, value, ct);
            break;
        default: 
            break;
        }
        return query;
    }
    
    private Query createEqualsQuery(Class<?> cls, String name, Object value) {
        Query query = null;
        if (cls == String.class) {
            String strValue = value.toString();
            if (caseInsensitiveMatch) {
                strValue = strValue.toLowerCase();
            }
            boolean isWildCard = strValue.contains("*") || super.isWildcardStringMatch(); 
            
            String theContentsFieldName = getContentsFieldName(name);
            if (theContentsFieldName == null) {
                Term term = new Term(name, strValue);
                
                if (!isWildCard) {
                    query = new TermQuery(term);
                } else {
                    query = new WildcardQuery(term);
                } 
            } else if (!isWildCard) {
                PhraseQuery pquery = new PhraseQuery();
                pquery.add(new Term(theContentsFieldName, name));
                pquery.add(new Term(theContentsFieldName, strValue));
                query = pquery;
            } else {
                BooleanQuery pquery = new BooleanQuery();
                pquery.add(new TermQuery(new Term(theContentsFieldName, name)),
                           BooleanClause.Occur.MUST);
                pquery.add(new WildcardQuery(new Term(theContentsFieldName, strValue)),
                           BooleanClause.Occur.MUST);
                query = pquery;                
            }
        } else {
            query = createRangeQuery(cls, name, value, ConditionType.EQUALS);
        }
        return query;
    }
    
    private String getContentsFieldName(String name) {
        String fieldName = null;
        if (contentsFieldMap != null) {
            fieldName = contentsFieldMap.get(name); 
        }
        if (fieldName == null) {
            fieldName = contentsFieldName;
        }
        return fieldName;
    }
    
    private Query createRangeQuery(Class<?> cls, String name, Object value, ConditionType type) {
        
        boolean minInclusive = type == ConditionType.GREATER_OR_EQUALS || type == ConditionType.EQUALS;
        boolean maxInclusive = type == ConditionType.LESS_OR_EQUALS || type == ConditionType.EQUALS;
        
        if (String.class.isAssignableFrom(cls) || Number.class.isAssignableFrom(cls)) {
            Query query = null;
            
            if (Double.class.isAssignableFrom(cls)) {
                query = createDoubleRangeQuery(name, value, type, minInclusive, maxInclusive);
            } else if (Float.class.isAssignableFrom(cls)) {
                query = createFloatRangeQuery(name, value, type, minInclusive, maxInclusive);
            } else if (Long.class.isAssignableFrom(cls)) {
                query = createLongRangeQuery(name, value, type, minInclusive, maxInclusive);
            } else {
                query = createIntRangeQuery(name, value, type, minInclusive, maxInclusive);
            }
        
            return query;
        } else if (Date.class.isAssignableFrom(cls)) {
            final Date date = getValue(Date.class, getFieldTypeConverter(), value.toString());           
            final String luceneDateValue = getString(Date.class, getFieldTypeConverter(), date);
                
            if (type == ConditionType.LESS_THAN || type == ConditionType.LESS_OR_EQUALS) {
                return TermRangeQuery.newStringRange(name, "", luceneDateValue, minInclusive, maxInclusive);
            } else {
                return TermRangeQuery.newStringRange(name, luceneDateValue, 
                    DateTools.dateToString(new Date(), Resolution.MILLISECOND), minInclusive, maxInclusive);
            }
        } else {
            return null;
        }
    }

    private Query createIntRangeQuery(final String name, final Object value,
            final ConditionType type, final boolean minInclusive, final boolean maxInclusive) {        
        final Integer intValue = Integer.valueOf(value.toString());        
        
        return NumericRangeQuery.newIntRange(name, getMin(type, intValue), 
            getMax(type, intValue),  minInclusive, maxInclusive);        
    }
    
    private Query createLongRangeQuery(final String name, final Object value,
            final ConditionType type, final boolean minInclusive, final boolean maxInclusive) {        
        final Long longValue = Long.valueOf(value.toString());        
        
        return NumericRangeQuery.newLongRange(name, getMin(type, longValue), 
            getMax(type, longValue),  minInclusive, maxInclusive);        
    }
    
    private Query createDoubleRangeQuery(final String name, final Object value,
            final ConditionType type, final boolean minInclusive, final boolean maxInclusive) {        
        final Double doubleValue = Double.valueOf(value.toString());        
        
        return NumericRangeQuery.newDoubleRange(name, getMin(type, doubleValue), 
            getMax(type, doubleValue),  minInclusive, maxInclusive);        
    }
    
    private Query createFloatRangeQuery(final String name, final Object value,
            final ConditionType type, final boolean minInclusive, final boolean maxInclusive) {        
        final Float floatValue = Float.valueOf(value.toString());        
        
        return NumericRangeQuery.newFloatRange(name, getMin(type, floatValue), 
            getMax(type, floatValue),  minInclusive, maxInclusive);        
    }

    private< N > N getMax(final ConditionType type, final N value) {
        return type == ConditionType.GREATER_THAN || type == ConditionType.GREATER_OR_EQUALS 
            ? null : value;
    }

    private< N > N getMin(final ConditionType type, final N value) {
        return type == ConditionType.LESS_THAN || type == ConditionType.LESS_OR_EQUALS ? null : value;
    }
    
    private Query createCompositeQuery(List<Query> queries, boolean orCondition) {
        
        BooleanClause.Occur clause = orCondition 
            ? BooleanClause.Occur.SHOULD : BooleanClause.Occur.MUST;
        
        BooleanQuery booleanQuery = new BooleanQuery();
        
        for (Query query : queries) {
            booleanQuery.add(query, clause);
        }
        
        return booleanQuery;
    }

    public void setCaseInsensitiveMatch(boolean caseInsensitiveMatch) {
        this.caseInsensitiveMatch = caseInsensitiveMatch;
    }    
}
