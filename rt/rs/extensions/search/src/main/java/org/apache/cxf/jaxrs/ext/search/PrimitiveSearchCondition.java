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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

public class PrimitiveSearchCondition<T> implements SearchCondition<T> {

    private String propertyName;
    private Object propertyValue;
    private Type propertyType;
    private T condition;
    private ConditionType cType;
    private Beanspector<T> beanspector;

    public PrimitiveSearchCondition(String propertyName,
                                    Object propertyValue,
                                    ConditionType ct,
                                    T condition) {
        this(propertyName, propertyValue, propertyValue.getClass(), ct, condition);
    }

    public PrimitiveSearchCondition(String propertyName,
                                    Object propertyValue,
                                    Type propertyType,
                                    ConditionType ct,
                                    T condition) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        this.propertyType = propertyType;
        this.condition = condition;
        this.cType = ct;
        if (propertyName != null) {
            this.beanspector = SearchBean.class.isAssignableFrom(condition.getClass())
                ? null : new Beanspector<T>(condition);
        }
    }

    public List<T> findAll(Collection<T> pojos) {
        List<T> result = new ArrayList<>();
        for (T pojo : pojos) {
            if (isMet(pojo)) {
                result.add(pojo);
            }
        }
        return result;
    }

    public T getCondition() {
        return condition;
    }

    public ConditionType getConditionType() {
        return cType;
    }

    protected String getPropertyName() {
        return propertyName;
    }

    protected Object getPropertyValue() {
        return propertyValue;
    }

    protected Type getPropertyType() {
        return propertyType;
    }

    public List<SearchCondition<T>> getSearchConditions() {
        return null;
    }

    public PrimitiveStatement getStatement() {
        return new PrimitiveStatement(propertyName, propertyValue, propertyType, cType);
    }

    public boolean isMet(T pojo) {
        if (isPrimitive(pojo)) {
            return compare(pojo, cType, propertyValue);
        }
        Object lValue = getValue(propertyName, pojo);
        Object rValue = getPrimitiveValue(propertyName, propertyValue);
        return lValue != null && compare(lValue, cType, rValue);
    }

    private Object getValue(String getter, T pojo) {
        String thePropertyName;
        int index = getter.indexOf('.');
        if (index != -1) {
            thePropertyName = getter.substring(0, index);
        } else {
            thePropertyName = getter;
        }

        Object value;
        try {
            if (beanspector != null) {
                value = beanspector.swap(pojo).getValue(thePropertyName.toLowerCase());
            } else {
                value = ((SearchBean)pojo).get(getter);
            }
            return getPrimitiveValue(getter, value);
        } catch (Throwable e) {
            return null;
        }
    }

    public String toSQL(String table, String... columns) {
        return SearchUtils.toSQL(this, table, columns);
    }

    public void accept(SearchConditionVisitor<T, ?> visitor) {
        visitor.visit(this);
    }

    private boolean isPrimitive(T pojo) {
        return pojo.getClass().getName().startsWith("java.lang");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean compare(Object lval, ConditionType cond, Object rval) {
        boolean compares = true;
        if (cond == ConditionType.EQUALS || cond == ConditionType.NOT_EQUALS) {
            if (rval == null) {
                compares = true;
            } else if (lval == null) {
                compares = false;
            } else {
                if (lval instanceof String) {
                    compares = textCompare((String)lval, (String)rval);
                } else {
                    compares = lval.equals(rval);
                }
                if (cond == ConditionType.NOT_EQUALS) {
                    compares = !compares;
                }
            }
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

    private boolean textCompare(String lval, String rval) {
        // check wild cards
        boolean starts = false;
        boolean ends = false;
        if (rval.charAt(0) == '*') {
            starts = true;
            rval = rval.substring(1);
            if (rval.isEmpty()) {
                throw new SearchParseException("A single wildcard is not a valid search condition");
            }
        }
        if (rval.charAt(rval.length() - 1) == '*') {
            ends = true;
            rval = rval.substring(0, rval.length() - 1);
        }
        if (starts || ends) {
            // wild card tests
            if (starts && !ends) {
                return lval.endsWith(rval);
            } else if (ends && !starts) {
                return lval.startsWith(rval);
            } else {
                return lval.contains(rval);
            }
        }
        return lval.equals(rval);
    }

    protected static Object getPrimitiveValue(String name, Object value) {

        int index = name.indexOf('.');
        if (index != -1) {
            String[] names = name.split("\\.");
            name = name.substring(index + 1);
            if (value != null && !InjectionUtils.isPrimitive(value.getClass())) {
                try {
                    String nextPart = StringUtils.capitalize(names[1]);
                    Method m = value.getClass().getMethod("get" + nextPart, new Class[]{});
                    value = m.invoke(value, new Object[]{});
                } catch (Throwable ex) {
                    throw new RuntimeException();
                }
            }
            return getPrimitiveValue(name, value);
        }
        return value;

    }
}
