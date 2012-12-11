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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.OrSearchCondition;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractSearchConditionVisitor;

public abstract class AbstractJPATypedQueryVisitor<T, T1, E> 
    extends AbstractSearchConditionVisitor<T, E> {

    private EntityManager em;
    private Class<T> tClass;
    private Class<T1> queryClass;
    private Root<T> root;
    private CriteriaBuilder builder;
    private CriteriaQuery<T1> cq;
    private Stack<List<Predicate>> predStack = new Stack<List<Predicate>>();
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
        this.joinProperties = joinProps == null ? null : new HashSet<String>(joinProps);
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
            predStack.push(new ArrayList<Predicate>());
        }
        PrimitiveStatement statement = sc.getStatement();
        if (statement != null) {
            if (statement.getProperty() != null) {
                predStack.peek().add(buildPredicate(sc.getConditionType(), 
                                                    statement.getProperty(), 
                                                    statement.getValue(),
                                                    statement.getValueType()));
            }
        } else {
            predStack.push(new ArrayList<Predicate>());
            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                condition.accept(this);
            }
            List<Predicate> predsList = predStack.pop();
            Predicate[] preds = predsList.toArray(new Predicate[predsList.size()]);
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
            cq.where(predsList.toArray(new Predicate[predsList.size()]));
            criteriaFinalized = true;
        }
        return cq;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate buildPredicate(ConditionType ct, String name, Object value, Type valueType) {

        name = super.getRealPropertyName(name);
        ClassValue cv = getPrimitiveFieldClass(name, value.getClass(), valueType, value); 
        
        Class<? extends Comparable> clazz = (Class<? extends Comparable>)cv.getCls();
        value = cv.getValue();    
        
        Path<?> path = getPath(root, name, cv);
        
        Predicate pred = null;
        switch (ct) {
        case GREATER_THAN:
            pred = builder.greaterThan(path.as(clazz), clazz.cast(value));
            break;
        case EQUALS:
            if (clazz.equals(String.class)) {
                String theValue = value.toString();
                if (theValue.contains("*")) {
                    theValue = ((String)value).replaceAll("\\*", "");
                }
                pred = builder.like(path.as(String.class), "%" + theValue + "%");
            } else {
                pred = builder.equal(path.as(clazz), clazz.cast(value));
            }
            break;
        case NOT_EQUALS:
            pred = builder.notEqual(path.as(clazz), 
                                    clazz.cast(value));
            break;
        case LESS_THAN:
            pred = builder.lessThan(path.as(clazz), 
                                    clazz.cast(value));
            break;
        case LESS_OR_EQUALS:
            pred = builder.lessThanOrEqualTo(path.as(clazz), 
                                             clazz.cast(value));
            break;
        case GREATER_OR_EQUALS:
            pred = builder.greaterThanOrEqualTo(path.as(clazz), 
                                                clazz.cast(value));
            break;
        default: 
            break;
        }
        return pred;
    }

    private Path<?> getPath(Path<?> element, String name, ClassValue cv) {
        if (name.contains(".")) {
            String pre = name.substring(0, name.indexOf('.'));
            String post = name.substring(name.indexOf('.') + 1);
            return getPath(getNextPath(element, pre, cv), 
                           post, 
                           cv);
        } else {
            return getNextPath(element, name, cv);
        }
    }

    private Path<?> getNextPath(Path<?> element, String name, ClassValue cv) {
        if ((cv.isCollection(name) || isJoinProperty(name)) && (element == root || element instanceof Join)) {
            return element == root ? root.join(name) : ((Join<?, ?>)element).join(name);
        } else {
            return element.get(name);
        }
    }
    
    private boolean isJoinProperty(String prop) {
        return joinProperties == null ? false : joinProperties.contains(prop);
    }
    
}
