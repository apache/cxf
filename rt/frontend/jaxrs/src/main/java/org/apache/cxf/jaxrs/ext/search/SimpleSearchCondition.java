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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleSearchCondition<T> implements SearchCondition<T> {

    private static Set<ConditionType> supportedTypes = new HashSet<ConditionType>();
    static {
        supportedTypes.add(ConditionType.EQUALS);
        supportedTypes.add(ConditionType.GREATER_THAN);
        supportedTypes.add(ConditionType.GREATER_OR_EQUALS);
        supportedTypes.add(ConditionType.LESS_THAN);
        supportedTypes.add(ConditionType.LESS_OR_EQUALS);
    }
    private ConditionType cType;
    private Map<String, ConditionType> getters2operators;
    private T condition;
    private List<Method> getters;

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
        this.cType = cType;
        this.getters2operators = null;
        this.condition = condition;
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
        if (isPrimitive(condition)) {
            throw new IllegalArgumentException("mapped operators strategy is "
                                               + "not supported for primitive type "
                                               + condition.getClass().getName());
        }
        for (ConditionType ct : getters2operators.values()) {
            if (!supportedTypes.contains(ct)) {
                throw new IllegalArgumentException("unsupported condition type: " + ct.name());
            }
        }
        this.cType = null;
        this.getters2operators = getters2operators;
        this.condition = condition;
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
        return cType;
    }

    public List<SearchCondition<T>> getConditions() {
        return null;
    }

    /**
     * Compares given object against template condition object.
     * <p>
     * For primitive type T like String, Number (precisely, from type T located in subpackage of
     * "java.lang.*") given object is directly compared with template object. Comparison for
     * {@link ConditionType#EQUALS} requires correct implementation of {@link Object#equals(Object)}, using
     * inequalities requires type T implementing {@link Comparable}.
     * <p>
     * For other types comparison of given object against template object is done using these <b>getters</b>;
     * returned "is met" value is <b>conjunction (*and*)</b> of comparisons per each getter. Getters of
     * template object that return null or throw exception are not used in comparison, in extreme if all
     * getters are excluded it means every given pojo object matches. If
     * {@link #SimpleSearchCondition(ConditionType, Object) constructor with shared operator} was used, then
     * getters are compared using the same operator. If {@link #SimpleSearchCondition(Map, Object) constructor
     * with map of operators} was used then for every getter specified operator is used (getters for missing
     * mapping are ignored). The way that comparison per getter is done depends on operator type per getter -
     * comparison for {@link ConditionType#EQUALS} requires correct implementation of
     * {@link Object#equals(Object)}, using inequalities requires that getter type implements
     * {@link Comparable}.
     * <p>
     * <b>Example:</b>
     * 
     * <pre>
     * class Entity {
     *   public String getName() {...
     *   public int getLevel() {...
     *   public String getMessage() {...
     * }
     * 
     * Entity template = new Entity("bbb", 10, null);
     * SimpleSearchCondition&lt;Entity> ssc = new SimpleSearchCondition&lt;Entity>(
     *   ConditionType.GREATER_THAN, template);    
     * 
     * ssc.isMet(new Entity("aaa", 20, "some mesage")); 
     * // false: is not met, expression '"aaa">"bbb" and 20>10' is not true  
     * // since "aaa" is not greater than "bbb"; not that message is null in template hence ingored
     * 
     * ssc.isMet(new Entity("ccc", 30, "other message"));
     * // true: is met, expression '"ccc">"bbb" and 30>10' is true
     * 
     * Map&lt;String,ConditionType> map;
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
        if (isPrimitive(pojo)) {
            return compare(pojo, cType, condition);
        } else {
            boolean matches = false;
            for (Method getter : loadGetters()) {
                ConditionType ct = cType;
                if (ct == null) {
                    ct = getters2operators.get(getterName(getter));
                    if (ct == null) {
                        continue;
                    }
                }
                Object lval = getValue(getter, pojo);
                Object rval = getValue(getter, condition);
                matches = compare(lval, ct, rval);
                if (!matches) {
                    break;
                }
            }
            return matches;
        }
    }

    private List<Method> loadGetters() {
        if (getters == null) {
            getters = new ArrayList<Method>();
            for (Method m : condition.getClass().getMethods()) {
                if (isGetter(m)) {
                    getters.add(m);
                }
            }
        }
        return getters;
    }

    private boolean isPrimitive(T pojo) {
        return pojo.getClass().getName().startsWith("java.lang");
    }

    private boolean isGetter(Method m) {
        return m.getParameterTypes().length == 0
               && (m.getName().startsWith("get") || m.getName().startsWith("is"));
    }
    
    private String getterName(Method m) {
        return m.getName().replace("is", "").replace("get", "").toLowerCase();
    }

    private Object getValue(Method getter, T pojo) {
        try {
            return getter.invoke(pojo);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            // getter exception is null equivalent
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean compare(Object lval, ConditionType cond, Object rval) {
        boolean compares = true;
        if (cond == ConditionType.EQUALS) {
            compares = (lval != null) ? lval.equals(rval) : true;
        } else {
            if (lval instanceof Comparable && rval instanceof Comparable) {
                Comparable lcomp = (Comparable)lval;
                Comparable rcomp = (Comparable)rval;
                int comp = lcomp.compareTo(rcomp);
                switch (cond) {
                case GREATER_THAN:
                    compares = comp > 0;
                    break;
                case GREATER_OR_EQUALS:
                    compares = comp >= 0;
                    break;
                case LESS_THAN:
                    compares = comp < 0;
                    break;
                case LESS_OR_EQUALS:
                    compares = comp <= 0;
                    break;
                default:
                    String msg = String.format("Condition type %s is not supported", cond.name());
                    throw new RuntimeException(msg);
                }
            }
        }
        return compares;
    }

    public List<T> findAll(List<T> pojos) {
        List<T> result = new ArrayList<T>();
        for (T pojo : pojos) {
            if (isMet(pojo)) {
                result.add(pojo);
            }
        }
        return result;
    }

}
