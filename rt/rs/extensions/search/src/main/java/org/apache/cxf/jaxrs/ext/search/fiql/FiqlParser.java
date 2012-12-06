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
package org.apache.cxf.jaxrs.ext.search.fiql;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.cxf.jaxrs.ext.search.AndSearchCondition;
import org.apache.cxf.jaxrs.ext.search.Beanspector;
import org.apache.cxf.jaxrs.ext.search.Beanspector.TypeInfo;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.OrSearchCondition;
import org.apache.cxf.jaxrs.ext.search.PropertyNotFoundException;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.SimpleSearchCondition;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.MessageUtils;


/**
 * Parses <a href="http://tools.ietf.org/html/draft-nottingham-atompub-fiql-00">FIQL</a> expression to
 * construct {@link SearchCondition} structure. Since this class operates on Java type T, not on XML
 * structures "selectors" part of specification is not applicable; instead selectors describes getters of type
 * T used as search condition type (see {@link SimpleSearchCondition#isMet(Object)} for details.
 * 
 * @param <T> type of search condition.
 */
public class FiqlParser<T> implements SearchConditionParser<T> {

    public static final String OR = ",";
    public static final String AND = ";";

    public static final String GT = "=gt=";
    public static final String GE = "=ge=";
    public static final String LT = "=lt=";
    public static final String LE = "=le=";
    public static final String EQ = "==";
    public static final String NEQ = "!=";
    
    private static final Pattern COMPARATORS_PATTERN; 
    private static final Map<String, ConditionType> OPERATORS_MAP;

    static {
        // operatorsMap
        OPERATORS_MAP = new HashMap<String, ConditionType>();
        OPERATORS_MAP.put(GT, ConditionType.GREATER_THAN);
        OPERATORS_MAP.put(GE, ConditionType.GREATER_OR_EQUALS);
        OPERATORS_MAP.put(LT, ConditionType.LESS_THAN);
        OPERATORS_MAP.put(LE, ConditionType.LESS_OR_EQUALS);
        OPERATORS_MAP.put(EQ, ConditionType.EQUALS);
        OPERATORS_MAP.put(NEQ, ConditionType.NOT_EQUALS);
        
        // pattern
        String comparators = GT + "|" + GE + "|" + LT + "|" + LE + "|" + EQ + "|" + NEQ;
        String s1 = "[\\p{ASCII}]+(" + comparators + ")";
        COMPARATORS_PATTERN = Pattern.compile(s1);
    }

    private Beanspector<T> beanspector;
    private Class<T> conditionClass;
    private Map<String, String> contextProperties;
    private Map<String, String> beanPropertiesMap;
    /**
     * Creates FIQL parser.
     * 
     * @param tclass - class of T used to create condition objects in built syntax tree. Class T must have
     *            accessible no-arg constructor and complementary setters to these used in FIQL expressions.
     */
    public FiqlParser(Class<T> tclass) {
        this(tclass, Collections.<String, String>emptyMap());
    }
    
    /**
     * Creates FIQL parser.
     * 
     * @param tclass - class of T used to create condition objects in built syntax tree. Class T must have
     *            accessible no-arg constructor and complementary setters to these used in FIQL expressions.
     * @param contextProperties            
     */
    public FiqlParser(Class<T> tclass, Map<String, String> contextProperties) {
        this(tclass, contextProperties, null);
    }
    
    /**
     * Creates FIQL parser.
     * 
     * @param tclass - class of T used to create condition objects in built syntax tree. Class T must have
     *            accessible no-arg constructor and complementary setters to these used in FIQL expressions.
     * @param contextProperties            
     */
    public FiqlParser(Class<T> tclass, 
                      Map<String, String> contextProperties,
                      Map<String, String> beanProperties) {
        beanspector = SearchBean.class.isAssignableFrom(tclass) 
            ? null : new Beanspector<T>(tclass);
        conditionClass = tclass;
        this.contextProperties = contextProperties == null 
            ? Collections.<String, String>emptyMap() : contextProperties;
        this.beanPropertiesMap = beanProperties;
    }
    
    /**
     * Parses expression and builds search filter. Names used in FIQL expression are names of getters/setters
     * in type T.
     * <p>
     * Example:
     * 
     * <pre>
     * class Condition {
     *   public String getFoo() {...}
     *   public void setFoo(String foo) {...}
     *   public int getBar() {...}
     *   public void setBar(int bar) {...}
     * }
     * 
     * FiqlParser&lt;Condition> parser = new FiqlParser&lt;Condition&gt;(Condition.class);
     * parser.parse("foo==mystery*;bar=ge=10");
     * </pre>
     * 
     * @param fiqlExpression expression of filter.
     * @return tree of {@link SearchCondition} objects representing runtime search structure.
     * @throws SearchParseException when expression does not follow FIQL grammar
     */
    public SearchCondition<T> parse(String fiqlExpression) throws SearchParseException {
        ASTNode<T> ast = parseAndsOrsBrackets(fiqlExpression);
        // System.out.println(ast);
        return ast.build();
    }

    private ASTNode<T> parseAndsOrsBrackets(String expr) throws SearchParseException {
        List<String> subexpressions = new ArrayList<String>();
        List<String> operators = new ArrayList<String>();
        int level = 0;
        int lastIdx = 0;
        int idx = 0;
        for (idx = 0; idx < expr.length(); idx++) {
            char c = expr.charAt(idx);
            if (c == '(') {
                level++;
            } else if (c == ')') {
                level--;
                if (level < 0) {
                    throw new SearchParseException(String.format("Unexpected closing bracket at position %d",
                                                               idx));
                }
            }
            String cs = Character.toString(c);
            boolean isOperator = AND.equals(cs) || OR.equals(cs);
            if (level == 0 && isOperator) {
                String s1 = expr.substring(lastIdx, idx);
                String s2 = expr.substring(idx, idx + 1);
                subexpressions.add(s1);
                operators.add(s2);
                lastIdx = idx + 1;
            }
            boolean isEnd = idx == expr.length() - 1;
            if (isEnd) {
                String s1 = expr.substring(lastIdx, idx + 1);
                subexpressions.add(s1);
                operators.add(null);
                lastIdx = idx + 1;
            }
        }
        if (level != 0) {
            throw new SearchParseException(String
                .format("Unmatched opening and closing brackets in expression: %s", expr));
        }
        if (operators.get(operators.size() - 1) != null) {
            String op = operators.get(operators.size() - 1);
            String ex = subexpressions.get(subexpressions.size() - 1);
            throw new SearchParseException("Dangling operator at the end of expression: ..." + ex + op);
        }
        // looking for adjacent ANDs then group them into ORs
        // Note: in case not ANDs is found (e.g only ORs) every single subexpression is
        // treated as "single item group of ANDs"
        int from = 0;
        int to = 0;
        SubExpression ors = new SubExpression(OR);
        while (to < operators.size()) {
            while (to < operators.size() && AND.equals(operators.get(to))) {
                to++;
            }
            SubExpression ands = new SubExpression(AND);
            for (; from <= to; from++) {
                String subex = subexpressions.get(from);
                ASTNode<T> node = null;
                if (subex.startsWith("(")) {
                    node = parseAndsOrsBrackets(subex.substring(1, subex.length() - 1));
                } else {
                    node = parseComparison(subex);
                }
                if (node != null) {
                    ands.add(node);
                }
            }
            to = from;
            if (ands.getSubnodes().size() == 1) {
                ors.add(ands.getSubnodes().get(0));
            } else {
                ors.add(ands);
            }
        }
        if (ors.getSubnodes().size() == 1) {
            return ors.getSubnodes().get(0);
        } else {
            return ors;
        }
    }

    private Comparison parseComparison(String expr) throws SearchParseException {
        Matcher m = COMPARATORS_PATTERN.matcher(expr);
        if (m.find()) {
            String name = expr.substring(0, m.start(1));
            String operator = m.group(1);
            String value = expr.substring(m.end(1));
            if ("".equals(value)) {
                throw new SearchParseException("Not a comparison expression: " + expr);
            }
            
            String beanPropertyName = beanPropertiesMap == null ? null : beanPropertiesMap.get(name);
            if (beanPropertyName != null) {
                name = beanPropertyName;
            }
            
            TypeInfoObject castedValue = parseType(name, value);
            if (castedValue != null) {
                return new Comparison(name, operator, castedValue);
            } else if (MessageUtils.isTrue(contextProperties.get(SearchUtils.LAX_PROPERTY_MATCH))) {
                return null;
            } else {
                throw new PropertyNotFoundException(name, value);
            }
        } else {
            throw new SearchParseException("Not a comparison expression: " + expr);
        }
    }

    
    private TypeInfoObject parseType(String setter, String value) throws SearchParseException {
        String name = getSetter(setter);
        
        try {
            TypeInfo typeInfo = 
                beanspector != null ? beanspector.getAccessorTypeInfo(name) 
                    : new TypeInfo(String.class, String.class);
            Object object = parseType(null, null, setter, typeInfo, value);
            return new TypeInfoObject(object, typeInfo);
        } catch (Exception e) {
            return null;
        }
        
    }

    private Object parseType(Object ownerBean, Object lastCastedValue, String setter, 
                             TypeInfo typeInfo, String value) throws SearchParseException {
        Class<?> valueType = typeInfo.getTypeClass();
        boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(valueType);
        Class<?> actualType = isCollection ? InjectionUtils.getActualType(typeInfo.getGenericType()) : valueType;
        
        int index = setter.indexOf(".");
        if (index == -1) {
            Object castedValue = value;
            if (Date.class.isAssignableFrom(valueType)) {
                castedValue = convertToDate(value);
            } else {
                if (ownerBean == null || InjectionUtils.isPrimitive(valueType) || valueType.isEnum()) {
                    try {
                        castedValue = InjectionUtils.convertStringToPrimitive(value, actualType);
                        if (isCollection) {
                            castedValue = getCollectionSingleton(valueType, castedValue);
                        }
                    } catch (Exception e) {
                        throw new SearchParseException("Cannot convert String value \"" + value
                                                     + "\" to a value of class " + valueType.getName(), e);
                    }
                } else {
                    Class<?> classType = isCollection ? valueType : value.getClass(); 
                    try {
                        Method setterM = valueType.getMethod("set" + getMethodNameSuffix(setter),
                                                             new Class[]{classType});
                        Object objectValue = !isCollection ? value : getCollectionSingleton(valueType, value);
                        setterM.invoke(ownerBean, new Object[]{objectValue});
                        castedValue = objectValue; 
                    } catch (Throwable ex) {
                        throw new SearchParseException("Cannot convert String value \"" + value
                                                       + "\" to a value of class " + valueType.getName(), ex);
                    }
                    
                }
            }
            if (lastCastedValue != null) {
                castedValue = lastCastedValue;
            }
            return castedValue;
        } else {
            String[] names = setter.split("\\.");
            try {
                String nextPart = getMethodNameSuffix(names[1]);
                Method getterM = actualType.getMethod("get" + nextPart, new Class[]{});   
                Class<?> returnType = getterM.getReturnType();
                boolean returnCollection = InjectionUtils.isSupportedCollectionOrArray(returnType);
                
                boolean isPrimitive = InjectionUtils.isPrimitive(returnType) || returnType.isEnum();
                boolean lastTry = names.length == 2 
                    && (isPrimitive || returnType == Date.class || returnCollection);
                
                Object valueObject = lastTry && ownerBean != null ? ownerBean : actualType.newInstance();
                Object nextObject;
                
                if (lastTry) {
                    if (!returnCollection) {
                        nextObject = isPrimitive ? InjectionUtils.convertStringToPrimitive(value, returnType) 
                            : convertToDate(value);
                    } else {
                        nextObject = Collections.singletonList(value);
                    }
                } else {
                    nextObject = returnType.newInstance();
                }
                
                Method setterM = actualType.getMethod("set" + nextPart, new Class[]{returnType});
                setterM.invoke(valueObject, new Object[]{nextObject});
                
                lastCastedValue = lastCastedValue == null ? valueObject : lastCastedValue;
                if (lastTry) {
                    return isCollection ? Collections.singletonList(lastCastedValue) : lastCastedValue;
                } 
                
                TypeInfo nextTypeInfo = new TypeInfo(nextObject.getClass(), getterM.getGenericReturnType()); 
                return parseType(nextObject, lastCastedValue, setter.substring(index + 1), 
                                 nextTypeInfo, value);
            } catch (Throwable e) {
                throw new SearchParseException("Cannot convert String value \"" + value
                                               + "\" to a value of class " + valueType.getName(), e);
            }
        }
    }
    
    private Object getCollectionSingleton(Class<?> collectionCls, Object value) {
        if (Set.class.isAssignableFrom(collectionCls)) {
            return Collections.singleton(value);
        } else {
            return Collections.singletonList(value);
        }
    }
    
    private Object convertToDate(String value) throws SearchParseException {
        try {
            DateFormat df = SearchUtils.getDateFormat(contextProperties);
            String dateValue = value;
            if (SearchUtils.isTimeZoneSupported(contextProperties, Boolean.FALSE)) {
                // zone in XML is "+01:00" in Java is "+0100"; stripping semicolon
                int idx = value.lastIndexOf(':');
                if (idx != -1) {
                    dateValue = value.substring(0, idx) + value.substring(idx + 1);
                }
            }
            return df.parse(dateValue);
        } catch (ParseException e) {
            // is that duration?
            try {
                Date now = new Date();
                DatatypeFactory.newInstance().newDuration(value).addTo(now);
                return now;
            } catch (DatatypeConfigurationException e1) {
                throw new SearchParseException(e1);
            } catch (IllegalArgumentException e1) {
                throw new SearchParseException("Can parse " + value + " neither as date nor duration", e);
            }
        }
    }
    
    private String getSetter(String setter) {
        int index = setter.indexOf(".");
        if (index != -1) {
            return setter.substring(0, index).toLowerCase();
        } else {
            return setter;
        }
    }
    
    private String getMethodNameSuffix(String name) {
        if (name.length() == 1) {
            return name.toUpperCase();
        } else {
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }
    
    // node of abstract syntax tree
    private interface ASTNode<T> {
        SearchCondition<T> build() throws SearchParseException;
    }

    private class SubExpression implements ASTNode<T> {
        private String operator;
        private List<ASTNode<T>> subnodes = new ArrayList<ASTNode<T>>();

        public SubExpression(String operator) {
            this.operator = operator;
        }

        public void add(ASTNode<T> node) {
            subnodes.add(node);
        }

        public List<ASTNode<T>> getSubnodes() {
            return Collections.unmodifiableList(subnodes);
        }

        @Override
        public String toString() {
            String s = operator.equals(AND) ? "AND" : "OR";
            s += ":[";
            for (int i = 0; i < subnodes.size(); i++) {
                s += subnodes.get(i);
                if (i < subnodes.size() - 1) {
                    s += ", ";
                }
            }
            s += "]";
            return s;
        }

        public SearchCondition<T> build() throws SearchParseException {
            List<SearchCondition<T>> scNodes = new ArrayList<SearchCondition<T>>();
            for (ASTNode<T> node : subnodes) {
                scNodes.add(node.build());
            }
            if (OR.equals(operator)) {
                return new OrSearchCondition<T>(scNodes);
            } else {
                return new AndSearchCondition<T>(scNodes);
            }
            
        }
    }

    private class Comparison implements ASTNode<T> {
        private String name;
        private String operator;
        private TypeInfoObject tvalue;

        public Comparison(String name, String operator, TypeInfoObject value) {
            this.name = name;
            this.operator = operator;
            this.tvalue = value;
        }

        @Override
        public String toString() {
            return name + " " + operator + " " + tvalue.getObject() 
                + " (" + tvalue.getObject().getClass().getSimpleName() + ")";
        }

        public SearchCondition<T> build() throws SearchParseException {
            String templateName = getSetter(name);
            T cond = createTemplate(templateName);
            ConditionType ct = OPERATORS_MAP.get(operator);
            
            if (isPrimitive(cond)) {
                return new SimpleSearchCondition<T>(ct, cond); 
            } else {
                return new SimpleSearchCondition<T>(Collections.singletonMap(templateName, ct),
                                                    Collections.singletonMap(templateName, name),
                                                    Collections.singletonMap(templateName, tvalue.getTypeInfo()),
                                                    cond);
            }
        }

        private boolean isPrimitive(T pojo) {
            return pojo.getClass().getName().startsWith("java.lang");
        }
        
        @SuppressWarnings("unchecked")
        private T createTemplate(String setter) throws SearchParseException {
            try {
                if (beanspector != null) {
                    beanspector.instantiate().setValue(setter, tvalue.getObject());
                    return beanspector.getBean();
                } else {
                    SearchBean bean = (SearchBean)conditionClass.newInstance();
                    bean.set(setter, tvalue.getObject().toString());
                    return (T)bean;
                }
            } catch (Throwable e) {
                throw new SearchParseException(e);
            }
        }
    }
    
    static class TypeInfoObject {
        private Object object;
        private TypeInfo typeInfo;
        
        public TypeInfoObject(Object object, TypeInfo typeInfo) {
            this.object = object;
            this.typeInfo = typeInfo;
        }

        public TypeInfo getTypeInfo() {
            return typeInfo;
        }

        public Object getObject() {
            return object;
        }
                
    }
}
