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
import org.apache.lucene.search.WildcardQuery;

public class LuceneQueryVisitor<T> extends AbstractSearchConditionVisitor<T, Query> {

    //private Analyzer analyzer;
    private String contentsFieldName;
    private Map<String, String> contentsFieldMap;
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

    //public void setAnalyzer(Analyzer a) {
    //    this.analyzer = a;
    //}
    
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
    
    private Query createRangeQuery(Class<?> cls, String name, Object value,
                                   ConditionType type) {
        if (String.class.isAssignableFrom(cls) || Number.class.isAssignableFrom(cls)) {
            // If needed, long and double can be supported too
            // Also, perhaps Strings may optionally be compared with string comparators 
            Integer intValue = Integer.valueOf(value.toString());
            Integer min = type == ConditionType.LESS_THAN || type == ConditionType.LESS_OR_EQUALS ? null : intValue;
            Integer max = type == ConditionType.GREATER_THAN || type == ConditionType.GREATER_OR_EQUALS 
                ? null : intValue;
            boolean minInclusive = 
                type == ConditionType.GREATER_OR_EQUALS || type == ConditionType.EQUALS;
            boolean maxInclusive =
                type == ConditionType.LESS_OR_EQUALS || type == ConditionType.EQUALS;
            Query query = NumericRangeQuery.newIntRange(name, min, max, 
                                                        minInclusive, maxInclusive);
            return query;
        } else if (Date.class.isAssignableFrom(cls)) {
            // This code has not been tested - most likely needs to be fixed  
            // Resolution should be configurable ?
            String luceneDateValue = DateTools.dateToString((Date)value, Resolution.MILLISECOND);
            String expression = null;
            if (type == ConditionType.LESS_THAN) {
                // what is the base date here ?
                expression = "[" + ""
                    + " TO " + luceneDateValue + "]";    
            } else {
                expression = "[" + luceneDateValue + " TO " 
                    + DateTools.dateToString(new Date(), Resolution.MILLISECOND) + "]";
            }
            return parseExpression(name, expression);
        } else {
            return null;
        }
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
    
    protected Query parseExpression(String fieldName, String expression) {
        //QueryParser parser = new QueryParser(Version.LUCENE_40, name, analyzer);
        // return parse.parse(expression);
        return null;
    }
}
