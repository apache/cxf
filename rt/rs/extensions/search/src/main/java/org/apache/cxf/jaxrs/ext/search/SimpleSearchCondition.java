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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple search condition comparing primitive objects or complex object by its getters. For details see
 * {@link #isMet(Object)} description.
 * 
 * @param <T> type of search condition.
 * 
 */
public class SimpleSearchCondition<T> implements SearchCondition<T> {

    private static Set<ConditionType> supportedTypes = new HashSet<ConditionType>();
    static {
        supportedTypes.add(ConditionType.EQUALS);
        supportedTypes.add(ConditionType.NOT_EQUALS);
        supportedTypes.add(ConditionType.GREATER_THAN);
        supportedTypes.add(ConditionType.GREATER_OR_EQUALS);
        supportedTypes.add(ConditionType.LESS_THAN);
        supportedTypes.add(ConditionType.LESS_OR_EQUALS);
    }
    private ConditionType joiningType = ConditionType.AND;
    private T condition;
    
    private List<SearchCondition<T>> scts;
    
    /**
     * Creates search condition with same operator (equality, inequality) applied in all comparison; see
     * {@link #isMet(Object)} for details of comparison.
     * 
     * @param cType shared condition type
     * @param condition template object
     */
    public SimpleSearchCondition(ConditionType cType, T condition) {
        if (cType == null) {
            throw new IllegalArgumentException("cType is null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("condition is null");
        }
        if (!supportedTypes.contains(cType)) {
            throw new IllegalArgumentException("unsupported condition type: " + cType.name());
        }
        this.condition = condition;
        scts = createConditions(null, cType);
                
    }

    /**
     * Creates search condition with different operators (equality, inequality etc) specified for each getter;
     * see {@link #isMet(Object)} for details of comparison. Cannot be used for primitive T type due to
     * per-getter comparison strategy.
     * 
     * @param getters2operators getters names and operators to be used with them during comparison
     * @param condition template object
     */
    public SimpleSearchCondition(Map<String, ConditionType> getters2operators, T condition) {
        if (getters2operators == null) {
            throw new IllegalArgumentException("getters2operators is null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("condition is null");
        }
        if (isBuiltIn(condition)) {
            throw new IllegalArgumentException("mapped operators strategy is "
                                               + "not supported for primitive type "
                                               + condition.getClass().getName());
        }
        this.condition = condition;
        for (ConditionType ct : getters2operators.values()) {
            if (!supportedTypes.contains(ct)) {
                throw new IllegalArgumentException("unsupported condition type: " + ct.name());
            }
        }
        scts = createConditions(getters2operators, null);
    }

    public T getCondition() {
        return condition;
    }

    /**
     * {@inheritDoc}
     * <p>
     * When constructor with map is used it returns null.
     */
    public ConditionType getConditionType() {
        if (scts.size() > 1) {
            return joiningType;
        } else {
            return scts.get(0).getStatement().getCondition();
        }
    }

    public List<SearchCondition<T>> getSearchConditions() {
        if (scts.size() > 1) {
            return Collections.unmodifiableList(scts);
        } else {
            return null;
        }
    }

    private List<SearchCondition<T>> createConditions(Map<String, ConditionType> getters2operators, 
                                                      ConditionType sharedType) {
        if (isBuiltIn(condition)) {
            return Collections.singletonList(
                (SearchCondition<T>)new PrimitiveSearchCondition<T>(null, condition, sharedType, condition));
        } else {
            List<SearchCondition<T>> list = new ArrayList<SearchCondition<T>>();
            Map<String, Object> get2val = getGettersAndValues();
            
            Set<String> keySet = get2val != null ? get2val.keySet()
                : ((SearchBean)condition).getKeySet();
            
            for (String getter : keySet) {
                ConditionType ct = getters2operators == null ? sharedType : getters2operators.get(getter);
                if (ct == null) {
                    continue;
                }
                Object rval = get2val != null 
                    ? get2val.get(getter) : ((SearchBean)condition).get(getter);
                if (rval == null) {
                    continue;
                }
                list.add(new PrimitiveSearchCondition<T>(getter, rval, ct, condition));
                
            }
            if (list.isEmpty()) {
                throw new IllegalStateException("This search condition is empty and can not be used");
            }
            return list;
        }
    }
    
    /**
     * Compares given object against template condition object.
     * <p>
     * For built-in type T like String, Number (precisely, from type T located in subpackage of "java.lang.*")
     * given object is directly compared with template object. Comparison for {@link ConditionType#EQUALS}
     * requires correct implementation of {@link Object#equals(Object)}, using inequalities requires type T
     * implementing {@link Comparable}.
     * <p>
     * For other types the comparison of given object against template object is done using its
     * <b>getters</b>; Value returned by {@linkplain #isMet(Object)} operation is <b>conjunction ('and'
     * operator)</b> of comparisons of each getter accessible in object of type T. Getters of template object
     * that return null or throw exception are not used in comparison. Finally, if all getters
     * return nulls (are excluded) it is interpreted as no filter (match every pojo).
     * <p>
     * If {@link #SimpleSearchCondition(ConditionType, Object) constructor with shared operator} was used,
     * then getters are compared using the same operator. If {@link #SimpleSearchCondition(Map, Object)
     * constructor with map of operators} was used then for every getter specified operator is used (getters
     * for missing mapping are ignored). The way that comparison per-getter is done depending on operator type
     * per getter - comparison for {@link ConditionType#EQUALS} requires correct implementation of
     * {@link Object#equals(Object)}, using inequalities requires that getter type implements
     * {@link Comparable}.
     * <p>
     * For equality comparison and String type in template object (either being built-in or getter from client
     * provided type) it is allowed to used asterisk at the beginning or at the end of text as wild card (zero
     * or more of any characters) e.g. "foo*", "*foo" or "*foo*". Inner asterisks are not interpreted as wild
     * cards.
     * <p>
     * <b>Example:</b>
     * 
     * <pre>
     * SimpleSearchCondition&lt;Integer&gt; ssc = new SimpleSearchCondition&lt;Integer&gt;(
     *   ConditionType.GREATER_THAN, 10);    
     * ssc.isMet(20);
     * // true since 20&gt;10 
     * 
     * class Entity {
     *   public String getName() {...
     *   public int getLevel() {...
     *   public String getMessage() {...
     * }
     * 
     * Entity template = new Entity("bbb", 10, null);
     * ssc = new SimpleSearchCondition&lt;Entity&gt;(
     *   ConditionType.GREATER_THAN, template);    
     * 
     * ssc.isMet(new Entity("aaa", 20, "some mesage")); 
     * // false: is not met, expression '"aaa"&gt;"bbb" and 20&gt;10' is not true  
     * // since "aaa" is not greater than "bbb"; not that message is null in template hence ingored
     * 
     * ssc.isMet(new Entity("ccc", 30, "other message"));
     * // true: is met, expression '"ccc"&gt;"bbb" and 30&gt;10' is true
     * 
     * Map&lt;String,ConditionType&gt; map;
     * map.put("name", ConditionType.EQUALS);
     * map.put("level", ConditionType.GREATER_THAN);
     * ssc = new SimpleSearchCondition&lt;Entity&gt;(
     *   ConditionType.GREATER_THAN, template);
     *   
     * ssc.isMet(new Entity("ccc", 30, "other message"));
     * // false due to expression '"aaa"=="ccc" and 30&gt;10"' (note different operators)
     * 
     * </pre>
     * 
     * @throws IllegalAccessException when security manager disallows reflective call of getters.
     */
    public boolean isMet(T pojo) {
        for (SearchCondition<T> sc : scts) {
            if (!sc.isMet(pojo)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates cache of getters from template (condition) object and its values returned during one-pass
     * invocation. Method isMet() will use its keys to introspect getters of passed pojo object, and values
     * from map in comparison.
     * 
     * @return template (condition) object getters mapped to their non-null values
     */
    private Map<String, Object> getGettersAndValues() {
        if (!SearchBean.class.isAssignableFrom(condition.getClass())) {
            Map<String, Object> getters2values = new HashMap<String, Object>();
            Beanspector<T> beanspector = new Beanspector<T>(condition);
            for (String getter : beanspector.getGettersNames()) {
                Object value = getValue(beanspector, getter, condition);
                getters2values.put(getter, value);
            }
            //we do not need compare class objects
            getters2values.keySet().remove("class");
            return getters2values; 
        } else {
            return null;
        }
    }

    private Object getValue(Beanspector<T> beanspector, String getter, T pojo) {
        try {
            return beanspector.swap(pojo).getValue(getter);
        } catch (Throwable e) {
            return null;
        }
    }

    private boolean isBuiltIn(T pojo) {
        return pojo.getClass().getName().startsWith("java.lang");
    }


    public List<T> findAll(Collection<T> pojos) {
        List<T> result = new ArrayList<T>();
        for (T pojo : pojos) {
            if (isMet(pojo)) {
                result.add(pojo);
            }
        }
        return result;
    }

    public String toSQL(String table, String... columns) {
        return SearchUtils.toSQL(this, table, columns);
    }
    
    public PrimitiveStatement getStatement() {
        if (scts.size() == 1) {
            return scts.get(0).getStatement();
        } else {
            return null;
        }
    }

    public void accept(SearchConditionVisitor<T> visitor) {
        visitor.visit(this);
    }
    
    
}
