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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;
import org.apache.cxf.jaxrs.ext.search.visitor.ThreadLocalVisitorState;
import org.apache.cxf.jaxrs.ext.search.visitor.VisitorState;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.QueryBuilder;

import static org.apache.cxf.jaxrs.ext.search.ParamConverterUtils.getString;
import static org.apache.cxf.jaxrs.ext.search.ParamConverterUtils.getValue;

/**
 * LuceneQueryVisitor implements SearchConditionVisitor and returns corresponding Lucene query. The
 * implementations is thread-safe, however if visitor is called multiple times, each call to visit()
 * method should be preceded by reset() method call (to properly reset the visitor's internal
 * state).
 */
public class LuceneQueryVisitor<T> extends AbstractSearchConditionVisitor<T, Query> {

    private String contentsFieldName;
    private Map<String, String> contentsFieldMap;
    private boolean caseInsensitiveMatch;
    private final VisitorState< Deque< List< Query > > > state = new ThreadLocalVisitorState<>();
    private QueryBuilder queryBuilder;

    public LuceneQueryVisitor() {
        this(Collections.<String, String>emptyMap());
    }

    public LuceneQueryVisitor(Analyzer analyzer) {
        this(Collections.<String, String>emptyMap(), null, analyzer);
    }


    public LuceneQueryVisitor(String contentsFieldAlias, String contentsFieldName) {
        this(Collections.singletonMap(contentsFieldAlias, contentsFieldName));
    }

    public LuceneQueryVisitor(String contentsFieldName) {
        this(Collections.<String, String>emptyMap(), contentsFieldName);
    }

    public LuceneQueryVisitor(String contentsFieldName, Analyzer analyzer) {
        this(Collections.<String, String>emptyMap(), contentsFieldName, analyzer);
    }

    public LuceneQueryVisitor(Map<String, String> fieldsMap) {
        this(fieldsMap, null);
    }

    public LuceneQueryVisitor(Map<String, String> fieldsMap, String contentsFieldName) {
        this(fieldsMap, contentsFieldName, null);
    }

    public LuceneQueryVisitor(String contentsFieldAlias, String contentsFieldName, Analyzer analyzer) {
        this(Collections.singletonMap(contentsFieldAlias, contentsFieldName), null, analyzer);
    }

    public LuceneQueryVisitor(Map<String, String> fieldsMap, String contentsFieldName, Analyzer analyzer) {
        super(fieldsMap);
        this.contentsFieldName = contentsFieldName;

        if (analyzer != null) {
            queryBuilder = new QueryBuilder(analyzer);
        }

    }

    public void setContentsFieldMap(Map<String, String> map) {
        this.contentsFieldMap = map;
    }

    /**
     * Resets visitor's internal state. If the instance of the visitor is intended to be used many times,
     * each call to visit() method should be preceded by reset() method call.
     */
    public void reset() {
        state.set(new ArrayDeque<List<Query>>());
        state.get().push(new ArrayList<>());
    }

    public void visit(SearchCondition<T> sc) {
        if (state.get() == null) {
            reset();
        }
        PrimitiveStatement statement = sc.getStatement();
        if (statement != null) {
            if (statement.getProperty() != null) {
                state.get().peek().add(buildSimpleQuery(sc.getConditionType(),
                                         statement.getProperty(),
                                         statement.getValue()));
            }
        } else {
            state.get().push(new ArrayList<>());
            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                condition.accept(this);
            }
            boolean orCondition = sc.getConditionType() == ConditionType.OR;
            List<Query> queries = state.get().pop();
            state.get().peek().add(createCompositeQuery(queries, orCondition));
        }
    }

    public Query getQuery() {
        List<Query> queries = state.get().peek();
        return queries.isEmpty() ? null : queries.get(0);
    }

    public void setCaseInsensitiveMatch(boolean caseInsensitiveMatch) {
        this.caseInsensitiveMatch = caseInsensitiveMatch;
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
            BooleanQuery.Builder booleanBuilder = new BooleanQuery.Builder();
            booleanBuilder.add(createEqualsQuery(clazz, name, value), BooleanClause.Occur.MUST_NOT);
            BooleanQuery booleanQuery = booleanBuilder.build();
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
        final Query query;
        if (cls == String.class) {
            String strValue = value.toString();
            if (caseInsensitiveMatch) {
                strValue = strValue.toLowerCase();
            }
            boolean isWildCard = strValue.contains("*") || super.isWildcardStringMatch();

            String theContentsFieldName = getContentsFieldName(name);
            if (theContentsFieldName == null) {
                if (!isWildCard) {
                    query = newTermQuery(name, strValue);
                } else {
                    query = new WildcardQuery(new Term(name, strValue));
                }
            } else if (!isWildCard) {
                query = newPhraseQuery(theContentsFieldName, strValue);
            } else {
                query = new WildcardQuery(new Term(theContentsFieldName, strValue));
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
            final Query query;

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
                return TermRangeQuery.newStringRange(name, null, luceneDateValue, minInclusive, maxInclusive);
            }
            return TermRangeQuery.newStringRange(name, luceneDateValue,
                DateTools.dateToString(new Date(), Resolution.MILLISECOND), minInclusive, maxInclusive);
        } else {
            return null;
        }
    }

    private Query createIntRangeQuery(final String name, final Object value,
            final ConditionType type, final boolean minInclusive, final boolean maxInclusive) {
        final Integer intValue = Integer.valueOf(value.toString());
        Integer min = getMin(type, intValue);
        if (min == null) {
            min = Integer.MIN_VALUE;
        } else if (!minInclusive) {
            min = Math.addExact(min, 1);
        }

        Integer max = getMax(type, intValue);
        if (max == null) {
            max = Integer.MAX_VALUE;
        } else if (!maxInclusive) {
            max = Math.addExact(max, -1);
        }

        return IntPoint.newRangeQuery(name, min, max);
    }

    private Query createLongRangeQuery(final String name, final Object value,
            final ConditionType type, final boolean minInclusive, final boolean maxInclusive) {
        final Long longValue = Long.valueOf(value.toString());
        Long min = getMin(type, longValue);
        if (min == null) {
            min = Long.MIN_VALUE;
        } else if (!minInclusive) {
            min = Math.addExact(min, 1L);
        }

        Long max = getMax(type, longValue);
        if (max == null) {
            max = Long.MAX_VALUE;
        } else if (!maxInclusive) {
            max = Math.addExact(max, -1L);
        }
        return LongPoint.newRangeQuery(name, min, max);
    }

    private Query createDoubleRangeQuery(final String name, final Object value,
            final ConditionType type, final boolean minInclusive, final boolean maxInclusive) {
        final Double doubleValue = Double.valueOf(value.toString());
        Double min = getMin(type, doubleValue);
        if (min == null) {
            min = Double.NEGATIVE_INFINITY;
        } else if (!minInclusive) {
            min = Math.nextUp(min);
        }

        Double max = getMax(type, doubleValue);
        if (max == null) {
            max = Double.POSITIVE_INFINITY;
        } else if (!maxInclusive) {
            max = Math.nextDown(max);
        }
        return DoublePoint.newRangeQuery(name, min, max);
    }

    private Query createFloatRangeQuery(final String name, final Object value,
            final ConditionType type, final boolean minInclusive, final boolean maxInclusive) {
        final Float floatValue = Float.valueOf(value.toString());
        Float min = getMin(type, floatValue);
        if (min == null) {
            min = Float.NEGATIVE_INFINITY;
        } else if (!minInclusive) {
            min = Math.nextUp(min);
        }

        Float max = getMax(type, floatValue);
        if (max == null) {
            max = Float.POSITIVE_INFINITY;
        } else if (!maxInclusive) {
            max = Math.nextDown(max);
        }

        return FloatPoint.newRangeQuery(name, min, max);
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

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        for (Query query : queries) {
            builder.add(query, clause);
        }

        return builder.build();
    }

    private Query newTermQuery(final String field, final String query) {
        return (queryBuilder != null) ? queryBuilder.createBooleanQuery(field, query)
            : new TermQuery(new Term(field, query));
    }

    private Query newPhraseQuery(final String field, final String query) {
        if (queryBuilder != null) {
            return queryBuilder.createPhraseQuery(field, query);
        }

        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        builder.add(new Term(field, query));
        return builder.build();
    }
}
