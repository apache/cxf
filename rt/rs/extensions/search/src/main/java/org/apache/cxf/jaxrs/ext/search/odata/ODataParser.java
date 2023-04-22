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
package org.apache.cxf.jaxrs.ext.search.odata;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.jaxrs.ext.search.AbstractSearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.AndSearchCondition;
import org.apache.cxf.jaxrs.ext.search.Beanspector.TypeInfo;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.OrSearchCondition;
import org.apache.cxf.jaxrs.ext.search.PrimitiveSearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckCondition;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckInfo;
import org.apache.olingo.odata2.api.edm.EdmLiteral;
import org.apache.olingo.odata2.api.edm.EdmLiteralKind;
import org.apache.olingo.odata2.api.edm.EdmSimpleType;
import org.apache.olingo.odata2.api.edm.EdmSimpleTypeException;
import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataMessageException;
import org.apache.olingo.odata2.api.uri.expression.BinaryExpression;
import org.apache.olingo.odata2.api.uri.expression.BinaryOperator;
import org.apache.olingo.odata2.api.uri.expression.ExpressionVisitor;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.LiteralExpression;
import org.apache.olingo.odata2.api.uri.expression.MemberExpression;
import org.apache.olingo.odata2.api.uri.expression.MethodExpression;
import org.apache.olingo.odata2.api.uri.expression.MethodOperator;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderExpression;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;
import org.apache.olingo.odata2.api.uri.expression.SortOrder;
import org.apache.olingo.odata2.api.uri.expression.UnaryExpression;
import org.apache.olingo.odata2.api.uri.expression.UnaryOperator;
import org.apache.olingo.odata2.core.uri.expression.FilterParser;
import org.apache.olingo.odata2.core.uri.expression.FilterParserImpl;

public class ODataParser<T> extends AbstractSearchConditionParser<T> {
    private final FilterParser parser;

    private static class TypedProperty {
        private final TypeInfo typeInfo;
        private final String propertyName;

        TypedProperty(final TypeInfo typeInfo, final String propertyName) {
            this.typeInfo = typeInfo;
            this.propertyName = propertyName;
        }
    }

    private static class TypedValue {
        private final Object value;
        private final String literal;
        private final Class< ? > typeClass;

        TypedValue(final Class< ? > typeClass, final String literal, final Object value) {
            this.literal = literal;
            this.value = value;
            this.typeClass = typeClass;
        }
    }

    private class FilterExpressionVisitor implements ExpressionVisitor {
        private final T condition;

        FilterExpressionVisitor(final T condition) {
            this.condition = condition;
        }

        @Override
        public Object visitFilterExpression(FilterExpression filterExpression, String expressionString,
                Object expression) {
            return expression;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object visitBinary(BinaryExpression binaryExpression, BinaryOperator operator,
                Object leftSide, Object rightSide) {

            // AND / OR operate on search conditions
            if (operator == BinaryOperator.AND || operator == BinaryOperator.OR) {
                if (leftSide instanceof SearchCondition && rightSide instanceof SearchCondition) {
                    final List< SearchCondition< T > > conditions = new ArrayList<>(2);
                    conditions.add((SearchCondition< T >)leftSide);
                    conditions.add((SearchCondition< T >)rightSide);

                    if (operator == BinaryOperator.AND) {
                        return new AndSearchCondition< T >(conditions);
                    } else if (operator == BinaryOperator.OR) {
                        return new OrSearchCondition< T >(conditions);
                    }
                } else {
                    throw new SearchParseException(
                        "Unsupported binary operation arguments (SearchCondition expected): "
                            + leftSide + ", " + rightSide);
                }
            }

            // Property could be either on left side (Name eq 'Tom') or
            // right side ('Tom' eq Name)
            final TypedValue value;
            final TypedProperty property;

            if (leftSide instanceof TypedProperty && rightSide instanceof TypedValue) {
                property = (TypedProperty)leftSide;
                value = (TypedValue)rightSide;
            } else if (rightSide instanceof TypedProperty && leftSide instanceof TypedValue) {
                property = (TypedProperty)rightSide;
                value = (TypedValue)leftSide;
            } else {
                throw new SearchParseException(
                    "Unsupported binary operation arguments (TypedValue or TypedProperty expected): "
                        + leftSide + ", " + rightSide);
            }

            ConditionType conditionType = null;
            switch (operator) {
            case EQ:
                conditionType = ConditionType.EQUALS;
                break;
            case NE:
                conditionType = ConditionType.NOT_EQUALS;
                break;
            case LT:
                conditionType = ConditionType.LESS_THAN;
                break;
            case LE:
                conditionType = ConditionType.LESS_OR_EQUALS;
                break;
            case GT:
                conditionType = ConditionType.GREATER_THAN;
                break;
            case GE:
                conditionType = ConditionType.GREATER_OR_EQUALS;
                break;
            default:
                throw new SearchParseException("Unsupported binary operation: " + operator);
            }

            final Object typedValue;
            // If property type and value type are compatible, just use them
            if (property.typeInfo.getWrappedTypeClass().isAssignableFrom(value.typeClass)) {
                typedValue = value.value;
            } else { // Property type and value type are not compatible and convert / cast are required
                String valueStr = value.literal;
                if (isDecodeQueryValues()) {
                    valueStr = UrlUtils.urlDecode(valueStr);
                }
                typedValue = parseType(property.propertyName, null, null, property.propertyName,
                    property.typeInfo, valueStr);
            }

            final CollectionCheckInfo checkInfo = property.typeInfo.getCollectionCheckInfo();
            if (checkInfo != null) {
                return new CollectionCheckCondition< T >(property.propertyName, typedValue,
                    property.typeInfo.getGenericType(), conditionType, condition, checkInfo);
            }

            return new PrimitiveSearchCondition< T >(property.propertyName, typedValue,
                property.typeInfo.getGenericType(), conditionType, condition);
        }

        @Override
        public Object visitLiteral(LiteralExpression literal, EdmLiteral edmLiteral) {
            try {
                final EdmSimpleType type = edmLiteral.getType();

                final Object value = type.valueOfString(edmLiteral.getLiteral(),
                    EdmLiteralKind.DEFAULT, null, type.getDefaultType());

                return new TypedValue(type.getDefaultType(), edmLiteral.getLiteral(), value);
            } catch (EdmSimpleTypeException ex) {
                throw new SearchParseException("Failed to convert literal to a typed form: " + literal, ex);
            }
        }

        @Override
        public Object visitProperty(PropertyExpression propertyExpression, String uriLiteral, EdmTyped edmProperty) {
            String setter = getActualSetterName(uriLiteral);
            final TypeInfo typeInfo = ODataParser.this.getTypeInfo(setter, null);
            return new TypedProperty(typeInfo, setter);
        }

        @Override
        public Object visitMethod(MethodExpression methodExpression, MethodOperator method, List<Object> parameters) {
            throw new SearchParseException("Unsupported operation visitMethod: " + methodExpression
                + "," + method + "," + parameters);
        }

        @Override
        public Object visitMember(MemberExpression memberExpression, Object path, Object property) {
            throw new SearchParseException("Unsupported operation visitMember: "
                + memberExpression + "," + path + "," + property);
        }

        @Override
        public Object visitUnary(UnaryExpression unaryExpression, UnaryOperator operator, Object operand) {
            throw new SearchParseException("Unsupported operation visitUnary: " + unaryExpression
                + "," + operator + "," + operand);
        }

        @Override
        public Object visitOrderByExpression(OrderByExpression orderByExpression, String expressionString,
                List<Object> orders) {
            throw new SearchParseException("Unsupported operation visitOrderByExpression: "
                + orderByExpression + "," + expressionString + "," + orders);
        }

        @Override
        public Object visitOrder(OrderExpression orderExpression, Object filterResult, SortOrder sortOrder) {
            throw new SearchParseException("Unsupported operation visitOrder: " + orderExpression
                + "," + filterResult + "," + sortOrder);
        }
    }

    /**
     * Creates OData parser.
     *
     * @param conditionClass - class of T used to create condition objects. Class T must have
     *            accessible no-arguments constructor and complementary setters to these used in
     *            OData $filter expressions.
     */
    public ODataParser(final Class< T > conditionClass) {
        this(conditionClass, Collections.<String, String>emptyMap());
    }

    /**
     * Creates OData parser.
     *
     * @param tclass - class of T used to create condition objects in built syntax tree. Class T must have
     *            accessible no-arg constructor and complementary setters to these used in
     *            OData $filter expressions.
     * @param contextProperties
     */
    public ODataParser(Class<T> tclass, Map<String, String> contextProperties) {
        this(tclass, contextProperties, null);
    }

    /**
     * Creates OData parser.
     *
     * @param tclass - class of T used to create condition objects in built syntax tree. Class T must have
     *            accessible no-arg constructor and complementary setters to these used in
     *            OData $filter expressions.
     * @param contextProperties
     */
    public ODataParser(Class<T> tclass,
                      Map<String, String> contextProperties,
                      Map<String, String> beanProperties) {
        super(tclass, contextProperties, beanProperties);

        this.parser = new FilterParserImpl(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SearchCondition<T> parse(String searchExpression) throws SearchParseException {
        try {
            final T condition = conditionClass.getDeclaredConstructor().newInstance();
            final FilterExpression expression = parser.parseFilterString(searchExpression);
            final FilterExpressionVisitor visitor = new FilterExpressionVisitor(condition);
            return (SearchCondition< T >)expression.accept(visitor);
        } catch (ODataMessageException | ODataApplicationException
            | InstantiationException | IllegalAccessException | IllegalArgumentException 
            | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            throw new SearchParseException(ex);
        }
    }
}
