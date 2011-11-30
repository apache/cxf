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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;

/**
 * An input filter for CORS, following http://www.w3.org/TR/cors/. 
 * This examines the input headers. If the request is valid, it stores
 * the information in the Exchange to allow {@link CrossOriginOutputFilter}
 * to add the appropriate headers to the response.
 * 
 *  If you need complex or subtle control of the behavior here (e.g. clearing
 *  the prefight cache) you might be better off reading the source of this
 *  and implementing this inside your service.
 */
public class CrossOriginInputFilter implements RequestHandler {
    
    @Context
    private HttpHeaders headers;

    /**
     * This would be a rather painful list to maintain for real, since it's entirely dependent on the
     * deployment.
     */
    private List<String> allowedOrigins = Collections.emptyList();
    private List<String> allowedMethods = Collections.emptyList();
    private List<String> allowedHeaders = Collections.emptyList();
    private boolean allowAllOrigins;

    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
        if ("OPTIONS".equals(m.get(Message.HTTP_REQUEST_METHOD))) {
            OperationResourceInfo opResInfo = m.getExchange().get(OperationResourceInfo.class);
            if (opResInfo != null) { // OPTIONS method defined in service bean
                return null; // continue handling
            }
            return preflight(m, resourceClass);
        }
        List<String> values = headers.getRequestHeader(CorsHeaderConstants.HEADER_ORIGIN);
        // 5.1.1 there has to be an origin
        if (values == null || values.size() == 0) {
            return null;
        }
        // 5.1.2 check all the origins
        if (!allowAllOrigins && !allowedOrigins.containsAll(values)) {
            return null;
        }
        // 5.1.3 credentials lives in the output filter
        // in any case
        if (allowAllOrigins) {
            m.getExchange().put(CorsHeaderConstants.HEADER_ORIGIN, Arrays.asList(new String[] {
                "*"
            }));
        } else {
            m.getExchange().put(CorsHeaderConstants.HEADER_ORIGIN, values);
        }

        // 5.1.4 expose headers lives on the output side.
        
        // note what kind of processing we're doing.
        m.getExchange().put(CrossOriginOutputFilter.class.getName(), "simple");
        return null;
    }

    private Response preflight(Message m, ClassResourceInfo resourceClass) {

        List<String> values = headers.getRequestHeader(CorsHeaderConstants.HEADER_ORIGIN);
        String origin;
        // 5.2.1 -- must have origin, must have one origin.
        if (values == null || values.size() != 1) {
            return null;
        }
        origin = values.get(0);
        // 5.2.2 must be on the list or we must be matching *.
        if (!allowAllOrigins && !allowedOrigins.contains(origin)) {
            return null;
        }

        values = headers.getRequestHeader(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD);

        // 5.2.3 must have access-control-request-method, must be single-valued
        // we should reject parse errors but we cannot.
        if (values == null || values.size() != 1) {
            return null;
        }

        String requestMethod = values.get(0);

        // 5.2.4 get list of request headers. we should reject parse errors but we cannot.
        List<String> requestHeaders = headers.getRequestHeader(CorsHeaderConstants.HEADER_AC_REQUEST_HEADERS);

        // 5.2.5 reject if the method is not on the list.
        if (allowedMethods.size() != 0 && !allowedMethods.contains(requestMethod)) {
            return null;
        }

        // 5.2.6 reject if the header is not listed.
        if (allowedHeaders.size() != 0 && !allowedHeaders.containsAll(requestHeaders)) {
            return null;
        }

        // 5.2.7: add allow credentials and allow-origin as required: this lives in the Output filter
        if (allowAllOrigins) {
            m.getExchange().put(CorsHeaderConstants.HEADER_ORIGIN, Arrays.asList(new String[] {
                "*"
            }));
        } else {
            m.getExchange().put(CorsHeaderConstants.HEADER_ORIGIN, origin);
        }
        // 5.2.8 max-age lives in the output filter.
        // 5.2.9 add allow-methods; we pass them from here to the output filter which actually adds them.
        m.getExchange().put(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS, Arrays.asList(new String[] {
            requestMethod
        }));
        // 5.2.10 add allow-headers; we pass them from here to the output filter which actually adds them.
        m.getExchange().put(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS, requestHeaders);
        m.getExchange().put(CrossOriginOutputFilter.class.getName(), "preflight");
        // and allow things to proceed to the output filter.
        return Response.ok().build();
    }

    /**
     * The origin strings to allow. Call {@link #setAllowAllOrigins(boolean)} to enable '*'.
     * @param allowedOrigins a list of case-sensitive origin strings. 
     */
    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
    
    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * Whether to implement Access-Control-Allow-Origin: *
     * @param allowAllOrigins if true, all origins are accepted and * is returned in the header.
     * Sections 5.1.1 and 5.1.2, and 5.2.1 and 5.2.2.
     * If false, then the list of allowed origins must be 
     */
    public void setAllowAllOrigins(boolean allowAllOrigins) {
        this.allowAllOrigins = allowAllOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    /**
     * The list of allowed non-simple methods for preflight checks.
     * Section 5.2.3.
     * @param allowedMethods a list of case-sensitive HTTP method names.
     */
    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    /**
     * The list of allowed headers for preflight checks.
     * Section 5.2.6
     * @param allowedHeaders a list of permitted headers.
     */
    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

}
