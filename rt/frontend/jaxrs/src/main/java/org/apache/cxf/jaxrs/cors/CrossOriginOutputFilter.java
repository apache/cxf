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

package org.apache.cxf.jaxrs.cors;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.jaxrs.ext.ResponseHandler;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;

/**
 * An output filter for CORS, following http://www.w3.org/TR/cors/. 
 * This looks at the information left by the {@link CrossOriginInputFilter}
 * and its properties to add appropriate headers to the response.
 */
public class CrossOriginOutputFilter implements ResponseHandler {
    private boolean allowCredentials;
    private List<String> exposeHeaders;
    private Integer maxAge;
    
    public CrossOriginOutputFilter() {
        exposeHeaders = Collections.emptyList();
    }
    
    @SuppressWarnings("unchecked")
    List<String> getHeadersFromInput(Message m, String key) {
        Object obj = m.getExchange().get(key);
        if (obj instanceof List<?>) {
            return (List<String>)obj;
        }
        return null;
    }

    public Response handleResponse(Message m, OperationResourceInfo ori, Response response) {
        String op = (String)m.getExchange().get(CrossOriginOutputFilter.class.getName());
        if (op == null) {
            return response; // we're not here.
        }
        List<String> originHeader = getHeadersFromInput(m, CorsHeaderConstants.HEADER_ORIGIN);
        ResponseBuilder rbuilder = Response.fromResponse(response);
        if ("simple".equals(op)) {
            // 5.1.3: add Allow-Origin supplied from the input side, plus allow-credentials as requested
            rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN, originHeader);
            rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS, 
                                Boolean.toString(allowCredentials));
            // 5.1.4 add allowed headers
            List<String> allowedHeaders 
                = getHeadersFromInput(m, CorsHeaderConstants.HEADER_AC_ALLOW_METHODS);
            if (allowedHeaders != null) {
                rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS, allowedHeaders);
            }
            if (exposeHeaders.size() > 0) {
                rbuilder.header(CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS, exposeHeaders);
            }
            // if someone wants to clear the cache, we can't help them.
            return rbuilder.build();
        } else {
            // preflight
            // 5.2.7 add Allow-Origin supplied from the input side, plus allow-credentials as requested
            rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN, originHeader);
            rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS, 
                                Boolean.toString(allowCredentials));
            // 5.2.8 max-age
            if (maxAge != null) {
                rbuilder.header(CorsHeaderConstants.HEADER_AC_MAX_AGE, maxAge.toString());
            }
            // 5.2.9 add allowed methods
            /*
             * Currently, input side just lists the one requested method, and spec endorses that.
             */
            rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS, 
                            getHeadersFromInput(m, CorsHeaderConstants.HEADER_AC_ALLOW_METHODS));
            // 5.2.10 add allowed headers
            List<String> allowedHeaders 
                = getHeadersFromInput(m, CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS);
            if (allowedHeaders != null && allowedHeaders.size() > 0) {
                rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS, allowedHeaders);
            }
            return rbuilder.build();
            
        }
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    /**
     * The value for the Access-Control-Allow-Credentials header. If false, no header is added.
     * If true, the header is added with the value 'true'.
     * @param allowCredentials
     */
    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public List<String> getExposeHeaders() {
        return exposeHeaders;
    }

    /**
     * A list of non-simple headers to be exposed via Access-Control-Expose-Headers.
     * @param exposeHeaders the list of (case-sensitive) header names.
     */
    public void setExposeHeaders(List<String> exposeHeaders) {
        this.exposeHeaders = exposeHeaders;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    /**
     * The value for Access-Control-Max-Age.
     * @param maxAge An integer 'delta-seconds' or null. If null, no header is added.
     */
    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

}
