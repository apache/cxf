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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.cxf.jaxrs.ext.search.AbstractSearchConditionVisitor;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.OrSearchCondition;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;

public class JPATypedQueryVisitor<T> extends AbstractSearchConditionVisitor<T> {

    private EntityManager em;
    private Class<T> tClass;
    private Root<T> root;
    private CriteriaBuilder builder;
    private CriteriaQuery<T> cq;
    private Stack<List<Predicate>> predStack = new Stack<List<Predicate>>();
    private boolean criteriaFinalized;
    
    public JPATypedQueryVisitor(EntityManager em, Class<T> tClass) {
        this(em, tClass, null);
    }
    
    public JPATypedQueryVisitor(EntityManager em, Class<T> tClass, Map<String, String> fieldMap) {
        super(fieldMap);
        this.em = em;
        this.tClass = tClass;
    }
    
    public void visit(SearchCondition<T> sc) {
        if (builder == null) {
            builder = em.getCriteriaBuilder();
            cq = builder.createQuery(tClass);
            root = cq.from(tClass);
            predStack.push(new ArrayList<Predicate>());
        }
        PrimitiveStatement statement = sc.getStatement();
        if (statement != null) {
            if (statement.getProperty() != null) {
                predStack.peek().add(buildPredicate(sc.getConditionType(), 
                                                    statement.getProperty(), 
                                                    statement.getValue()));
            }
        } else {
            predStack.push(new ArrayList<Predicate>());
            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                condition.accept(this);
            }
            Predicate[] preds = predStack.pop().toArray(new Predicate[0]);
            Predicate newPred;
            if (sc instanceof OrSearchCondition) {
                newPred = builder.or(preds);
            } else {
                newPred = builder.and(preds);
            }
            predStack.peek().add(newPred);
        }
    }

    public TypedQuery<T> getQuery() {
        return em.createQuery(getCriteriaQuery());
    }
    
    public CriteriaQuery<T> getCriteriaQuery() {
        if (!criteriaFinalized) {
            cq.where(predStack.pop().toArray(new Predicate[0]));
            criteriaFinalized = true;
        }
        return cq;
    }
    
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Predicate buildPredicate(ConditionType ct, String name, Object value) {

        Class<? extends Comparable> clazz = (Class<? extends Comparable>) value
                        .getClass();
        
        name = super.getRealPropertyName(name);
        
        Path<?> path = getPath(root, name);
        
        Predicate pred = null;
        switch (ct) {
        case GREATER_THAN:
            pred = builder.greaterThan(path.as(clazz), clazz.cast(value));
            break;
        case EQUALS:
            if (clazz.equals(String.class)) {
                pred = builder.like(path.as(String.class), "%"
                                    + (String) value + "%");
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

    private Path<?> getPath(Path<?> element, String name) {
        if (name.contains(".")) {
            String pre = name.substring(0, name.indexOf('.'));
            String post = name.substring(name.indexOf('.') + 1);
            Path<?> newPath = element.get(pre);
            return getPath(newPath, post);
        } else {
            return element.get(name);
        }
    }
    
    
}
