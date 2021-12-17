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
package org.apache.cxf.jaxrs.ext.search.jpa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.OrSearchCondition;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckInfo;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;

public abstract class AbstractJPATypedQueryVisitor<T, T1, E>
    extends AbstractSearchConditionVisitor<T, E> {

    private EntityManager em;
    private Class<T> tClass;
    private Class<T1> queryClass;
    private Root<T> root;
    private CriteriaBuilder builder;
    private CriteriaQuery<T1> cq;
    private final Deque<List<Predicate>> predStack = new ArrayDeque<>();
    private boolean criteriaFinalized;
    private Set<String> joinProperties;

    protected AbstractJPATypedQueryVisitor(EntityManager em, Class<T> tClass) {
        this(em, tClass, null, null, null);
    }

    protected AbstractJPATypedQueryVisitor(EntityManager em, Class<T> tClass, Class<T1> queryClass) {
        this(em, tClass, queryClass, null, null);
    }

    protected AbstractJPATypedQueryVisitor(EntityManager em,
                                        Class<T> tClass,
                                        Map<String, String> fieldMap) {
        this(em, tClass, null, fieldMap, null);
    }

    protected AbstractJPATypedQueryVisitor(EntityManager em,
                                           Class<T> tClass,
                                           List<String> joinProps) {
        this(em, tClass, null, null, joinProps);
    }

    protected AbstractJPATypedQueryVisitor(EntityManager em,
                                        Class<T> tClass,
                                        Map<String, String> fieldMap,
                                        List<String> joinProps) {
        this(em, tClass, null, fieldMap, joinProps);
    }

    protected AbstractJPATypedQueryVisitor(EntityManager em,
                                           Class<T> tClass,
                                           Class<T1> queryClass,
                                           Map<String, String> fieldMap) {
        this(em, tClass, queryClass, fieldMap, null);
    }

    protected AbstractJPATypedQueryVisitor(EntityManager em,
                                           Class<T> tClass,
                                           Class<T1> queryClass,
                                           Map<String, String> fieldMap,
                                           List<String> joinProps) {
        super(fieldMap);
        this.em = em;
        this.tClass = tClass;
        this.queryClass = toQueryClass(queryClass, tClass);
        this.joinProperties = joinProps == null ? null : new HashSet<>(joinProps);
    }

    @SuppressWarnings("unchecked")
    private static <E> Class<E> toQueryClass(Class<E> queryClass, Class<?> tClass) {
        return queryClass != null ? queryClass : (Class<E>)tClass;
    }

    protected EntityManager getEntityManager() {
        return em;
    }

    public void visit(SearchCondition<T> sc) {
        if (builder == null) {
            builder = em.getCriteriaBuilder();
            cq = builder.createQuery(queryClass);
            root = cq.from(tClass);
            predStack.push(new ArrayList<>());
        }
        if (sc.getStatement() != null) {
            predStack.peek().add(buildPredicate(sc.getStatement()));
        } else {
            predStack.push(new ArrayList<>());
            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                condition.accept(this);
            }
            List<Predicate> predsList = predStack.pop();
            Predicate[] preds = predsList.toArray(new Predicate[0]);
            Predicate newPred;
            if (sc instanceof OrSearchCondition) {
                newPred = builder.or(preds);
            } else {
                newPred = builder.and(preds);
            }
            predStack.peek().add(newPred);
        }
    }

    protected CriteriaBuilder getCriteriaBuilder() {
        return builder;
    }

    protected Class<T1> getQueryClass() {
        return queryClass;
    }

    public Root<T> getRoot() {
        return root;
    }

    public TypedQuery<T1> getTypedQuery() {
        return em.createQuery(getCriteriaQuery());
    }

    public CriteriaQuery<T1> getCriteriaQuery() {
        if (!criteriaFinalized) {
            List<Predicate> predsList = predStack.pop();
            cq.where(predsList.toArray(new Predicate[0]));
            criteriaFinalized = true;
        }
        return cq;
    }

    private Predicate buildPredicate(PrimitiveStatement ps) {
        String name = ps.getProperty();
        Object propertyValue = ps.getValue();
        validatePropertyValue(name, propertyValue);

        name = super.getRealPropertyName(name);
        ClassValue cv = getPrimitiveFieldClass(ps,
                                               name,
                                               ps.getValue().getClass(),
                                               ps.getValueType(),
                                               propertyValue);
        CollectionCheckInfo collInfo = cv.getCollectionCheckInfo();
        Path<?> path = getPath(root, name, cv, collInfo);

        return collInfo == null
            ? doBuildPredicate(ps.getCondition(), path, cv.getCls(), cv.getValue())
            : doBuildCollectionPredicate(ps.getCondition(), path, collInfo);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate doBuildPredicate(ConditionType ct, Path<?> path, Class<?> valueClazz, Object value) {

        Class<? extends Comparable> clazz = (Class<? extends Comparable>)valueClazz;
        Expression<? extends Comparable> exp = path.as(clazz);

        Predicate pred = null;
        switch (ct) {
        case GREATER_THAN:
            pred = builder.greaterThan(exp, clazz.cast(value));
            break;
        case EQUALS:
            if (clazz.equals(String.class)) {
                final String originalValue = value.toString();

                String theValue = SearchUtils.toSqlWildcardString(originalValue, isWildcardStringMatch());
                if (SearchUtils.containsWildcard(originalValue)) {
                    if (SearchUtils.containsEscapedChar(theValue)) {
                        pred = builder.like((Expression<String>)exp, theValue, '\\');
                    } else {
                        pred = builder.like((Expression<String>)exp, theValue);
                    }
                } else {
                    pred = builder.equal(exp, clazz.cast(value));
                }
            } else {
                pred = builder.equal(exp, clazz.cast(value));
            }
            break;
        case NOT_EQUALS:
            if (clazz.equals(String.class)) {
                final String originalValue = value.toString();

                String theValue = SearchUtils.toSqlWildcardString(originalValue, isWildcardStringMatch());
                if (SearchUtils.containsWildcard(originalValue)) {
                    if (SearchUtils.containsEscapedChar(theValue)) {
                        pred = builder.notLike((Expression<String>)exp, theValue, '\\');
                    } else {
                        pred = builder.notLike((Expression<String>)exp, theValue);
                    }
                } else {
                    pred = builder.notEqual(exp, clazz.cast(value));
                }
            } else {
                pred = builder.notEqual(exp, clazz.cast(value));
            }
            break;
        case LESS_THAN:
            pred = builder.lessThan(exp, clazz.cast(value));
            break;
        case LESS_OR_EQUALS:
            pred = builder.lessThanOrEqualTo(exp, clazz.cast(value));
            break;
        case GREATER_OR_EQUALS:
            pred = builder.greaterThanOrEqualTo(exp, clazz.cast(value));
            break;
        default:
            break;
        }
        return pred;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate doBuildCollectionPredicate(ConditionType ct, Path<?> path, CollectionCheckInfo collInfo) {
        Predicate pred = null;

        Expression<Integer> exp = builder.size((Expression<? extends Collection>)path);
        Integer value = Integer.valueOf(collInfo.getCollectionCheckValue().toString());

        switch (ct) {
        case GREATER_THAN:
            pred = builder.greaterThan(exp, value);
            break;
        case EQUALS:
            pred = builder.equal(exp, value);
            break;
        case NOT_EQUALS:
            pred = builder.notEqual(exp, value);
            break;
        case LESS_THAN:
            pred = builder.lessThan(exp, value);
            break;
        case LESS_OR_EQUALS:
            pred = builder.lessThanOrEqualTo(exp, value);
            break;
        case GREATER_OR_EQUALS:
            pred = builder.greaterThanOrEqualTo(exp, value);
            break;
        default:
            break;
        }
        return pred;
    }

    private Path<?> getPath(Path<?> element, String name, ClassValue cv, CollectionCheckInfo collSize) {
        if (name.contains(".")) {
            String pre = name.substring(0, name.indexOf('.'));
            String post = name.substring(name.indexOf('.') + 1);
            final Path<?> nextPath = getNextPath(element, pre, post, cv, null);
            return getPath(nextPath, post, cv, collSize);
        }
        return getNextPath(element, name, null, cv, collSize);
    }

    private Path<?> getNextPath(Path<?> element, String name, String postName,
        ClassValue cv, CollectionCheckInfo collSize) {
        final boolean isCollectionOrJoin = collSize == null
            && (cv.isCollection(name) || isJoinProperty(name) || existingCollectionInPostName(cv, postName))
            && (element == root || element instanceof Join);
        if (isCollectionOrJoin) {
            final Path<?> path = getExistingJoinProperty((From<?, ?>)element, name);
            if (path != null) {
                return path;
            }
            return element == root ? root.join(name) : ((Join<?, ?>)element).join(name);
        }
        return element.get(name);
    }

    private boolean isJoinProperty(String prop) {
        return joinProperties != null && joinProperties.contains(prop);
    }

    private Path<?> getExistingJoinProperty(From<?, ?> element, String prop) {
        final Set<?> joins = element.getJoins();
        for (Object object : joins) {
            Join<?, ?> join = (Join<?, ?>) object;
            if (join.getAttribute().getName().equals(prop)) {
                return join;
            }
        }
        return null;
    }

    private boolean existingCollectionInPostName(ClassValue cv, String postName) {
        if (postName != null && postName.indexOf('.') != -1) {
            final String[] splitName = postName.split("\\.");
            for (String name : splitName) {
                if (cv.isCollection(name)) {
                    return true;
                }
            }
        }
        return false;
    }
}
