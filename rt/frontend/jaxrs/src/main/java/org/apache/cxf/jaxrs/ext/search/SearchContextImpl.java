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

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;

public class SearchContextImpl implements SearchContext {

    public static final String SEARCH_QUERY = "_search";
    public static final String SHORT_SEARCH_QUERY = "_s";
    private Message message;
    
    public SearchContextImpl(Message message) {
        this.message = message;
    }
    
    public <T> SearchCondition<T> getCondition(Class<T> cls) {
        FiqlParser<T> parser = getParser(cls);
        
        String expression = getExpression();
        if (expression != null) {
            try {
                return parser.parse(expression);
            } catch (FiqlParseException ex) {
                return null;
            }
        } else {
            return null;
        }
        
    }

    private String getExpression() {
        String queryStr = (String)message.get(Message.QUERY_STRING);
        if (queryStr != null 
            && (queryStr.startsWith(SEARCH_QUERY) || queryStr.startsWith(SHORT_SEARCH_QUERY))) {
            int ind = queryStr.indexOf('=');
            if (ind + 1 < queryStr.length()) {
                return HttpUtils.urlDecode(queryStr.substring(ind + 1));
            }
        }
        return null;
    }
    
    private <T> FiqlParser<T> getParser(Class<T> cls) {
        
        // we can use this method as a parser factory, ex
        // we can get parsers capable of parsing XQuery and other languages
        // depending on the properties set by a user
        
        return new FiqlParser<T>(cls); 
    }
}
