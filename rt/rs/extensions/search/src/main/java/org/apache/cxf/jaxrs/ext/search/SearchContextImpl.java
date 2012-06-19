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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

public class SearchContextImpl implements SearchContext {

    public static final String SEARCH_QUERY = "_search";
    public static final String SHORT_SEARCH_QUERY = "_s";
    private static final Logger LOG = LogUtils.getL7dLogger(SearchContextImpl.class);
    private Message message;
    
    public SearchContextImpl(Message message) {
        this.message = message;
    }
    
    public <T> SearchCondition<T> getCondition(Class<T> cls) {
        
        if (InjectionUtils.isPrimitive(cls)) {
            String errorMessage = "Primitive condition types are not supported"; 
            LOG.warning(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        
        SearchConditionParser<T> parser = getParser(cls);
        
        String expression = getSearchExpression();
        if (expression != null) {
            try {
                return parser.parse(expression);
            } catch (SearchParseException ex) {
                return null;
            }
        } else {
            return null;
        }
        
    }

    public String getSearchExpression() {
        String queryStr = (String)message.get(Message.QUERY_STRING);
        if (queryStr != null 
            && (queryStr.contains(SHORT_SEARCH_QUERY) || queryStr.contains(SEARCH_QUERY))) {
            MultivaluedMap<String, String> params = 
                JAXRSUtils.getStructuredParams(queryStr, "&", true, false);
            if (params.containsKey(SHORT_SEARCH_QUERY)) {
                return params.getFirst(SHORT_SEARCH_QUERY);
            } else {
                return params.getFirst(SEARCH_QUERY);
            }
        } else {
            return null;
        }
    }
    
    private <T> FiqlParser<T> getParser(Class<T> cls) {
        // we can use this method as a parser factory, ex
        // we can get parsers capable of parsing XQuery and other languages
        // depending on the properties set by a user
        Map<String, String> props = new LinkedHashMap<String, String>(2);
        props.put(SearchUtils.DATE_FORMAT_PROPERTY, 
                  (String)message.getContextualProperty(SearchUtils.DATE_FORMAT_PROPERTY));
        props.put(SearchUtils.TIMEZONE_SUPPORT_PROPERTY, 
                  (String)message.getContextualProperty(SearchUtils.TIMEZONE_SUPPORT_PROPERTY));
        return new FiqlParser<T>(cls, props); 
    }
}
