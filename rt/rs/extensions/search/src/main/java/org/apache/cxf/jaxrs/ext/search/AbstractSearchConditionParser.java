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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.search.Beanspector.TypeInfo;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheck;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public abstract class AbstractSearchConditionParser<T> implements SearchConditionParser<T> {

    private static final Annotation[] EMPTY_ANNOTTAIONS = new Annotation[]{};
    protected final Map<String, String> contextProperties;
    protected final Class<T> conditionClass;
    protected Beanspector<T> beanspector;
    protected Map<String, String> beanPropertiesMap;

    protected AbstractSearchConditionParser(Class<T> tclass) {
        this(tclass, Collections.<String, String>emptyMap(), null);
    }

    protected AbstractSearchConditionParser(Class<T> tclass,
                                            Map<String, String> contextProperties,
                                            Map<String, String> beanProperties) {
        this.conditionClass = tclass;
        this.contextProperties = contextProperties == null
            ? Collections.<String, String>emptyMap() : contextProperties;
        beanspector = SearchBean.class.isAssignableFrom(tclass) ? null : new Beanspector<T>(tclass);
        this.beanPropertiesMap = beanProperties;
    }

    protected String getActualSetterName(String setter) {
        String beanPropertyName = beanPropertiesMap == null ? null : beanPropertiesMap.get(setter);
        if (beanPropertyName == null) {
            Message m = JAXRSUtils.getCurrentMessage();
            if (m != null) {
                Object converterProp = m.getContextualProperty(SearchUtils.BEAN_PROPERTY_CONVERTER);
                if (converterProp != null) {
                    PropertyNameConverter converter = (PropertyNameConverter)converterProp;
                    beanPropertyName = converter.getPropertyName(setter);
                }
            }
        }
        return beanPropertyName != null ? beanPropertyName : setter;
    }

    protected Boolean isDecodeQueryValues() {
        return PropertyUtils.isTrue(contextProperties.get(SearchUtils.DECODE_QUERY_VALUES));
    }

    protected TypeInfo getTypeInfo(String setter, String value)
        throws SearchParseException, PropertyNotFoundException {

        String name = getSetter(setter);

        TypeInfo typeInfo = null;
        try {
            typeInfo = beanspector != null ? beanspector.getAccessorTypeInfo(name)
                    : new TypeInfo(String.class, String.class);
        } catch (Exception e) {
            // continue
        }
        if (typeInfo == null && !PropertyUtils.isTrue(contextProperties.get(SearchUtils.LAX_PROPERTY_MATCH))) {
            throw new PropertyNotFoundException(name, value);
        }
        return typeInfo;
    }

    protected String getSetter(String setter) {
        int index = getDotIndex(setter);
        if (index != -1) {
            return setter.substring(0, index).toLowerCase();
        }
        return setter;
    }

    @SuppressWarnings("unchecked")
    protected Object parseType(String originalPropName,
                             Object ownerBean,
                             Object lastCastedValue,
                             String setter,
                             TypeInfo typeInfo,
                             String value) throws SearchParseException {
        Class<?> valueType = typeInfo.getTypeClass();
        boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(valueType);
        Class<?> actualType = isCollection ? InjectionUtils.getActualType(typeInfo.getGenericType()) : valueType;

        int index = getDotIndex(setter);
        if (index == -1) {
            Object castedValue = value;
            if (Date.class.isAssignableFrom(valueType)) {
                castedValue = convertToDate(valueType, value);
            } else if (Temporal.class.isAssignableFrom(valueType)) {
                castedValue = convertToTemporal((Class<? extends Temporal>)valueType, value);
            } else {
                boolean isPrimitive = InjectionUtils.isPrimitive(valueType);
                boolean isPrimitiveOrEnum = isPrimitive || valueType.isEnum();
                if (ownerBean == null || isPrimitiveOrEnum) {
                    try {
                        CollectionCheck collCheck = getCollectionCheck(originalPropName, isCollection, actualType);
                        if (collCheck == null) {
                            castedValue = InjectionUtils.convertStringToPrimitive(value, actualType);
                        }
                        if (collCheck == null && isCollection) {
                            castedValue = getCollectionSingleton(valueType, castedValue);
                        } else if (isCollection) {
                            typeInfo.setCollectionCheckInfo(new CollectionCheckInfo(collCheck, castedValue));
                            castedValue = getEmptyCollection(valueType);
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
        }
        String[] names = setter.split("\\.");
        try {
            String nextPart = getMethodNameSuffix(names[1]);
            Method getterM = actualType.getMethod("get" + nextPart, new Class[]{});
            Class<?> returnType = getterM.getReturnType();
            boolean returnCollection = InjectionUtils.isSupportedCollectionOrArray(returnType);
            Class<?> actualReturnType = !returnCollection ? returnType
                : InjectionUtils.getActualType(getterM.getGenericReturnType());

            boolean isPrimitive = !returnCollection
                && InjectionUtils.isPrimitive(returnType) || returnType.isEnum();
            boolean lastTry = names.length == 2
                && (isPrimitive
                    ||
                    Date.class.isAssignableFrom(returnType)
                    || Temporal.class.isAssignableFrom(returnType)
                    || returnCollection
                    || paramConverterAvailable(returnType));

            Object valueObject = ownerBean != null ? ownerBean
                : actualType.isInterface()
                ? Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                         new Class[]{actualType},
                                         new InterfaceProxy())
                : actualType.getDeclaredConstructor().newInstance();
            Object nextObject;

            if (lastTry) {
                if (!returnCollection) {
                    if (isPrimitive) {
                        nextObject = InjectionUtils.convertStringToPrimitive(value, returnType);
                    } else if (Temporal.class.isAssignableFrom(returnType)) {
                        nextObject = convertToTemporal((Class<? extends Temporal>)returnType, value);
                    } else {
                        nextObject = convertToDate(returnType, value);
                    }
                } else {
                    CollectionCheck collCheck = getCollectionCheck(originalPropName, true, actualReturnType);
                    if (collCheck == null) {
                        nextObject = getCollectionSingleton(valueType, value);
                    } else {
                        typeInfo.setCollectionCheckInfo(new CollectionCheckInfo(collCheck, value));
                        nextObject = getEmptyCollection(valueType);
                    }
                }
            } else if (!returnCollection) {
                nextObject = returnType.getDeclaredConstructor().newInstance();
            } else {
                nextObject = actualReturnType.getDeclaredConstructor().newInstance();
            }
            Method setterM = actualType.getMethod("set" + nextPart, new Class[]{returnType});
            final Object valueObjectValue;
            if (lastTry || !returnCollection) {
                valueObjectValue = nextObject;
            } else {
                Class<?> collCls = Collection.class.isAssignableFrom(valueType) ? valueType : returnType;
                valueObjectValue = getCollectionSingleton(collCls, nextObject);
            }
            setterM.invoke(valueObject, new Object[]{valueObjectValue});

            if (lastTry) {
                lastCastedValue = lastCastedValue == null ? valueObject : lastCastedValue;
                return isCollection ? getCollectionSingleton(valueType, lastCastedValue) : lastCastedValue;
            }
            lastCastedValue = valueObject;

            TypeInfo nextTypeInfo = new TypeInfo(valueObjectValue.getClass(), getterM.getGenericReturnType());
            Object response = parseType(originalPropName,
                             nextObject,
                             lastCastedValue,
                             setter.substring(index + 1),
                             nextTypeInfo,
                             value);
            if (ownerBean == null) {
                return isCollection ? getCollectionSingleton(valueType, lastCastedValue) : lastCastedValue;
            }
            return response;
        } catch (Throwable e) {
            throw new SearchParseException("Cannot convert String value \"" + value
                                           + "\" to a value of class " + valueType.getName(), e);
        }
    }

    private boolean paramConverterAvailable(final Class<?> pClass) {
        Message m = JAXRSUtils.getCurrentMessage();
        ServerProviderFactory pf = m == null ? null : ServerProviderFactory.getInstance(m);
        return pf != null && pf.createParameterHandler(pClass, pClass, EMPTY_ANNOTTAIONS, m) != null;
    }

    private CollectionCheck getCollectionCheck(String propName, boolean isCollection, Class<?> actualCls) {
        if (isCollection) {
            if (InjectionUtils.isPrimitive(actualCls)) {
                if (isCount(propName)) {
                    return CollectionCheck.SIZE;
                }
            } else {
                return CollectionCheck.SIZE;
            }
        }
        return null;
    }

    protected boolean isCount(String propName) {
        return false;
    }

    private Object getCollectionSingleton(Class<?> collectionCls, Object value) {
        if (Set.class.isAssignableFrom(collectionCls)) {
            return Collections.singleton(value);
        }
        return Collections.singletonList(value);
    }

    private Object getEmptyCollection(Class<?> collectionCls) {
        if (Set.class.isAssignableFrom(collectionCls)) {
            return Collections.emptySet();
        }
        return Collections.emptyList();
    }

    private Object convertToDate(Class<?> valueType, String value) throws SearchParseException {

        Message m = JAXRSUtils.getCurrentMessage();
        Object obj = InjectionUtils.createFromParameterHandler(value, valueType, valueType,
                                                               new Annotation[]{}, m);
        if (obj != null) {
            return obj;
        }

        try {
            if (Timestamp.class.isAssignableFrom(valueType)) {
                return convertToTimestamp(value);
            } else if (Time.class.isAssignableFrom(valueType)) {
                return convertToTime(value);
            } else {
                return convertToDefaultDate(value);
            }
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
    
    private Temporal convertToTemporal(Class<? extends Temporal> valueType, String value) throws SearchParseException {

        Message m = JAXRSUtils.getCurrentMessage();
        Temporal obj = InjectionUtils.createFromParameterHandler(value, valueType, valueType,
                                                               new Annotation[]{}, m);
        if (obj != null) {
            return obj;
        }

        try {
            if (LocalTime.class.isAssignableFrom(valueType)) {
                return LocalTime.parse(value);
            } else if (LocalDate.class.isAssignableFrom(valueType)) {
                return LocalDate.from(convertToDefaultDate(value).toInstant().atZone(ZoneId.systemDefault()));
            } else if (LocalDateTime.class.isAssignableFrom(valueType)) {
                return convertTo(value, SearchUtils.DEFAULT_DATETIME_FORMAT, Boolean.FALSE, LocalDateTime::parse);
            } else if (OffsetTime.class.isAssignableFrom(valueType)) {
                return OffsetTime.parse(value);
            } else if (OffsetDateTime.class.isAssignableFrom(valueType)) {
                return convertTo(value, SearchUtils.DEFAULT_OFFSET_DATETIME_FORMAT,
                    Boolean.TRUE, OffsetDateTime::parse);
            } else if (ZonedDateTime.class.isAssignableFrom(valueType)) {
                return convertTo(value, SearchUtils.DEFAULT_ZONE_DATETIME_FORMAT,
                    Boolean.TRUE, ZonedDateTime::parse);
            } else {
                return convertToDefaultDate(value).toInstant();
            }
        } catch (ParseException | DateTimeParseException e) {
            // is that duration?
            try {
                Date now = new Date();
                DatatypeFactory.newInstance().newDuration(value).addTo(now);
                return LocalDateTime.from(now.toInstant().atZone(ZoneId.systemDefault()));
            } catch (DatatypeConfigurationException e1) {
                throw new SearchParseException(e1);
            } catch (IllegalArgumentException e1) {
                throw new SearchParseException("Can parse " + value + " neither as date nor duration", e);
            }
        }
    }

    private Timestamp convertToTimestamp(String value) throws ParseException {
        Date date = convertToDefaultDate(value);
        return new Timestamp(date.getTime());
    }

    private Time convertToTime(String value) throws ParseException {
        Date date = convertToDefaultDate(value);
        return new Time(date.getTime());
    }

    private Date convertToDefaultDate(String value) throws ParseException {
        DateFormat df = SearchUtils.getDateFormat(contextProperties);
        String dateValue = normalizeTimeZone(value);
        return df.parse(dateValue);
    }

    private <S extends Temporal> S convertTo(String value, String pattern, Boolean timezoneSupported,
            BiFunction<String, DateTimeFormatter, S> parser) throws ParseException {
        final DateTimeFormatter df = DateTimeFormatter.ofPattern(SearchUtils
            .getDateFormatOrDefault(contextProperties, pattern)
            .toPattern());
        final String dateValue = normalizeTimeZone(value, timezoneSupported);
        return parser.apply(dateValue, df);
    }

    private String normalizeTimeZone(String value) {
        return normalizeTimeZone(value, Boolean.FALSE);
    }

    private String normalizeTimeZone(String value, Boolean timezoneSupported) {
        String dateValue = value;
        if (SearchUtils.isTimeZoneSupported(contextProperties, timezoneSupported)) {
            // zone in XML is "+01:00" in Java is "+0100"; stripping semicolon
            int idx = value.lastIndexOf(':');
            if (idx != -1) {
                dateValue = value.substring(0, idx) + value.substring(idx + 1);
            }
        }
        return dateValue;
    }

    private static String getMethodNameSuffix(String name) {
        return StringUtils.capitalize(name);
    }

    private int getDotIndex(String setter) {
        return this.conditionClass == SearchBean.class ? -1 : setter.indexOf('.');
    }
}
