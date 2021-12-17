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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.cxf.jaxrs.ext.search.client.SearchConditionBuilder;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

public class SearchContextImpl implements SearchContext {

    public static final String SEARCH_QUERY = "_search";
    public static final String SHORT_SEARCH_QUERY = "_s";
    public static final String CUSTOM_SEARCH_PARSER_PROPERTY = "search.parser";
    public static final String CUSTOM_SEARCH_PARSER_CLASS_PROPERTY = "search.parser.class";
    public static final String CUSTOM_SEARCH_QUERY_PARAM_NAME = "search.query.parameter.name";
    private static final String USE_PLAIN_QUERY_PARAMETERS = "search.use.plain.queries";
    private static final String USE_ALL_QUERY_COMPONENT = "search.use.all.query.component";
    private static final String BLOCK_SEARCH_EXCEPTION = "search.block.search.exception";
    private static final String KEEP_QUERY_ENCODED = "search.keep.query.encoded";
    private static final Logger LOG = LogUtils.getL7dLogger(SearchContextImpl.class);
    private Message message;

    public SearchContextImpl(Message message) {
        this.message = message;
    }

    public <T> SearchCondition<T> getCondition(Class<T> cls) {
        return getCondition(null, cls);
    }

    public <T> SearchCondition<T> getCondition(Class<T> cls, Map<String, String> beanProperties) {
        return getCondition(null, cls, beanProperties);
    }

    public <T> SearchCondition<T> getCondition(Class<T> cls,
                                               Map<String, String> beanProperties,
                                               Map<String, String> parserProperties) {
        return getCondition(null, cls, beanProperties, parserProperties);
    }

    public <T> SearchCondition<T> getCondition(String expression, Class<T> cls) {
        return getCondition(expression, cls, null);
    }

    public <T> SearchCondition<T> getCondition(String expression,
                                               Class<T> cls,
                                               Map<String, String> beanProperties) {
        return getCondition(expression, cls, beanProperties, null);
    }

    public <T> SearchCondition<T> getCondition(String expression,
                                               Class<T> cls,
                                               Map<String, String> beanProperties,
                                               Map<String, String> parserProperties) {
        if (InjectionUtils.isPrimitive(cls)) {
            String errorMessage = "Primitive condition types are not supported";
            LOG.warning(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        SearchConditionParser<T> parser = getParser(cls, beanProperties, parserProperties);

        String theExpression = expression == null
            ? getSearchExpression() : expression;
        if (theExpression != null) {
            try {
                return parser.parse(theExpression);
            } catch (SearchParseException ex) {
                if (PropertyUtils.isTrue(message.getContextualProperty(BLOCK_SEARCH_EXCEPTION))) {
                    return null;
                }
                throw ex;
            }
        }
        return null;

    }

    public String getSearchExpression() {

        String queryStr = (String)message.get(Message.QUERY_STRING);
        if (queryStr != null) {
            if (MessageUtils.getContextualBoolean(message, USE_ALL_QUERY_COMPONENT)) {
                return queryStr;
            }
            boolean encoded = PropertyUtils.isTrue(getKeepEncodedProperty());

            MultivaluedMap<String, String> params =
                JAXRSUtils.getStructuredParams(queryStr, "&", !encoded, false);
            String customQueryParamName = (String)message.getContextualProperty(CUSTOM_SEARCH_QUERY_PARAM_NAME);
            if (customQueryParamName != null) {
                return params.getFirst(customQueryParamName);
            }
            if (queryStr.contains(SHORT_SEARCH_QUERY) || queryStr.contains(SEARCH_QUERY)) {
                if (params.containsKey(SHORT_SEARCH_QUERY)) {
                    return params.getFirst(SHORT_SEARCH_QUERY);
                }
                return params.getFirst(SEARCH_QUERY);
            } else if (MessageUtils.getContextualBoolean(message, USE_PLAIN_QUERY_PARAMETERS)) {
                return convertPlainQueriesToFiqlExp(params);
            }
        }
        return null;

    }

    private String convertPlainQueriesToFiqlExp(MultivaluedMap<String, String> params) {
        SearchConditionBuilder builder = SearchConditionBuilder.instance();
        List<CompleteCondition> list = new ArrayList<>(params.size());

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            list.add(getOrCondition(builder, entry));
        }
        return builder.and(list).query();
    }

    private String getKeepEncodedProperty() {
        return (String)message.getContextualProperty(KEEP_QUERY_ENCODED);
    }

    private CompleteCondition getOrCondition(SearchConditionBuilder builder,
                                             Map.Entry<String, List<String>> entry) {
        String key = entry.getKey();
        final ConditionType ct;
        if (key.endsWith("From")) {
            ct = ConditionType.GREATER_OR_EQUALS;
            key = key.substring(0, key.length() - 4);
        } else if (key.endsWith("Till")) {
            ct = ConditionType.LESS_OR_EQUALS;
            key = key.substring(0, key.length() - 4);
        } else {
            ct = ConditionType.EQUALS;
        }

        List<CompleteCondition> list = new ArrayList<>(entry.getValue().size());
        for (String value : entry.getValue()) {
            list.add(builder.is(key).comparesTo(ct, value));
        }
        return builder.or(list);
    }



    private <T> SearchConditionParser<T> getParser(Class<T> cls,
                                                   Map<String, String> beanProperties,
                                                   Map<String, String> parserProperties) {

        Object parserProp = message.getContextualProperty(CUSTOM_SEARCH_PARSER_PROPERTY);
        if (parserProp != null) {
            return getCustomParser(parserProp);
        }

        final Map<String, String> props;
        if (parserProperties == null) {
            props = new HashMap<>(5);
            props.put(SearchUtils.DATE_FORMAT_PROPERTY,
                      (String)message.getContextualProperty(SearchUtils.DATE_FORMAT_PROPERTY));
            props.put(SearchUtils.TIMEZONE_SUPPORT_PROPERTY,
                      (String)message.getContextualProperty(SearchUtils.TIMEZONE_SUPPORT_PROPERTY));
            props.put(SearchUtils.LAX_PROPERTY_MATCH,
                      (String)message.getContextualProperty(SearchUtils.LAX_PROPERTY_MATCH));
            props.put(SearchUtils.DECODE_QUERY_VALUES,
                      (String)message.getContextualProperty(SearchUtils.DECODE_QUERY_VALUES));
            // FIQL specific
            props.put(FiqlParser.SUPPORT_SINGLE_EQUALS,
                      (String)message.getContextualProperty(FiqlParser.SUPPORT_SINGLE_EQUALS));
        } else {
            props = parserProperties;
        }

        final Map<String, String> beanProps;

        if (beanProperties == null) {
            beanProps = CastUtils.cast((Map<?, ?>)message.getContextualProperty(SearchUtils.BEAN_PROPERTY_MAP));
        } else {
            beanProps = beanProperties;
        }

        String parserClassProp = (String) message.getContextualProperty(CUSTOM_SEARCH_PARSER_CLASS_PROPERTY);
        if (parserClassProp != null) {
            try {
                final Class<?> parserClass = ClassLoaderUtils.loadClass(parserClassProp, SearchContextImpl.class);
                final Constructor<?> constructor = parserClass.getConstructor(Class.class, Map.class, Map.class);
                @SuppressWarnings("unchecked")
                SearchConditionParser<T> customParser =
                    (SearchConditionParser<T>)constructor.newInstance(cls, props, beanProps);
                return customParser;
            } catch (Exception ex) {
                throw new SearchParseException(ex);
            }
        }
        return new FiqlParser<T>(cls, props, beanProps);
    }

    @SuppressWarnings("unchecked")
    private <T> SearchConditionParser<T> getCustomParser(Object parserProp) {
        return (SearchConditionParser<T>)parserProp;
    }
}
