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
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.ext.ResponseHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;

/**
 * An single class that provides both an input and an output filter for CORS, following
 * http://www.w3.org/TR/cors/. The input examines the input headers. If the request is valid, it stores the
 * information in the Exchange to allow the response handler to add the appropriate headers to the response.
 * If you need complex or subtle control of the behavior here (e.g. clearing the prefight cache) you might be
 * better off reading the source of this and implementing this inside your service.
 */
public class CrossOriginResourceSharingFilter implements RequestHandler, ResponseHandler {

    @Context
    private HttpHeaders headers;

    /**
     * This would be a rather painful list to maintain for real, since it's entirely dependent on the
     * deployment.
     */
    private List<String> allowOrigins = Collections.emptyList();
    private List<String> allowMethods = Collections.emptyList();
    private List<String> allowHeaders = Collections.emptyList();
    private boolean allowAllOrigins;
    private boolean allowCredentials;
    private List<String> exposeHeaders = Collections.emptyList();
    private Integer maxAge;

    private CrossOriginResourceSharing getAnnotation(OperationResourceInfo ori) {
        return ReflectionUtil.getAnnotationForMethodOrContainingClass(ori.getAnnotatedMethod(),
                                                                      CrossOriginResourceSharing.class);
    }

    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
        OperationResourceInfo opResInfo = m.getExchange().get(OperationResourceInfo.class);
        CrossOriginResourceSharing annotation = getAnnotation(opResInfo);

        if ("OPTIONS".equals(m.get(Message.HTTP_REQUEST_METHOD))) {
            // what if someone wants to use options for something else, and also for preflight?
            // in that case, they set the localPreflight flag, and we bow out.
            if (opResInfo != null && (annotation == null || annotation.localPreflight())) {
                return null; // continue handling
            }
            return preflightRequest(m, annotation, resourceClass);
        }
        return simpleRequest(m, annotation);
    }

    private Response simpleRequest(Message m, CrossOriginResourceSharing ann) {
        List<String> values = headers.getRequestHeader(CorsHeaderConstants.HEADER_ORIGIN);
        // 5.1.1 there has to be an origin
        if (values == null || values.size() == 0) {
            return null;
        }
        // 5.1.2 check all the origins
        if (!effectiveAllowAllOrigins(ann) && !effectiveAllowOrigins(ann).containsAll(values)) {
            return null;
        }
        // 5.1.3 credentials lives in the output filter
        // in any case
        if (effectiveAllowAllOrigins(ann)) {
            m.getExchange().put(CorsHeaderConstants.HEADER_ORIGIN, Arrays.asList(new String[] {
                "*"
            }));
        } else {
            m.getExchange().put(CorsHeaderConstants.HEADER_ORIGIN, values);
        }

        // 5.1.4 expose headers lives on the output side.

        // note what kind of processing we're doing.
        m.getExchange().put(CrossOriginResourceSharingFilter.class.getName(), "simple");
        return null;
    }

    /**
     * handle preflight.
     * 
     * @param m the incoming message.
     * @param ann the annotation, if any, derived from a method that matched the OPTIONS request for the
     *            preflight. probably completely useless.
     * @param resourceClass the resource class passed into the filter.
     * @return
     */
    private Response preflightRequest(Message m, CrossOriginResourceSharing optionAnn,
                                      ClassResourceInfo resourceClass) {
        /*
         * CORS doesn't send enough information with a preflight to accurately identity the single method
         * that will handle the request. So the code uses annotations from the containing class,
         * only. 
         */
        CrossOriginResourceSharing ann 
            = resourceClass.getResourceClass().getAnnotation(CrossOriginResourceSharing.class);
        
        List<String> values = headers.getRequestHeader(CorsHeaderConstants.HEADER_ORIGIN);
        String origin;
        // 5.2.1 -- must have origin, must have one origin.
        if (values == null || values.size() != 1) {
            return null;
        }
        origin = values.get(0);
        // 5.2.2 must be on the list or we must be matching *.
        boolean effectiveAllowAllOrigins = effectiveAllowAllOrigins(ann);
        if (!effectiveAllowAllOrigins && !effectiveAllowOrigins(ann).contains(origin)) {
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
        List<String> effectiveAllowMethods = effectiveAllowMethods(ann);

        if (!effectiveAllowMethods.contains(requestMethod)) {
            return null;
        }

        // 5.2.6 reject if the header is not listed.
        if (!effectiveAllowHeaders(ann).containsAll(requestHeaders)) {
            return null;
        }

        // 5.2.7: add allow credentials and allow-origin as required: this lives in the Output filter
        if (effectiveAllowAllOrigins(ann)) {
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
        m.getExchange().put(CrossOriginResourceSharingFilter.class.getName(), "preflight");
        // and allow things to proceed to the output filter.
        return Response.ok().build();
    }

    public Response handleResponse(Message m, OperationResourceInfo ori, Response response) {
        String op = (String)m.getExchange().get(CrossOriginResourceSharingFilter.class.getName());
        if (op == null) {
            return response; // we're not here.
        }
        CrossOriginResourceSharing annotation;

        List<String> originHeader = getHeadersFromInput(m, CorsHeaderConstants.HEADER_ORIGIN);
        ResponseBuilder rbuilder = Response.fromResponse(response);
        if ("simple".equals(op)) {
            annotation = getAnnotation(ori);
            // 5.1.3: add Allow-Origin supplied from the input side, plus allow-credentials as requested
            addHeaders(rbuilder, CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN, originHeader);
            rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS,
                            Boolean.toString(effectiveAllowCredentials(annotation)));
            // 5.1.4 add allowed headers
            List<String> rqAllowedHeaders = getHeadersFromInput(m,
                                                                CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS);
            if (rqAllowedHeaders != null) {
                addHeaders(rbuilder, CorsHeaderConstants.HEADER_AC_ALLOW_METHODS, rqAllowedHeaders);
            }
            
            List<String> effectiveExposeHeaders = effectiveExposeHeaders(annotation);
            if (effectiveExposeHeaders.size() > 0) {
                addHeaders(rbuilder, CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS, effectiveExposeHeaders);
            }
            // if someone wants to clear the cache, we can't help them.
            return rbuilder.build();
        } else {
            annotation = ori.getAnnotatedMethod().getDeclaringClass()
                    .getAnnotation(CrossOriginResourceSharing.class);
            // preflight
            // 5.2.7 add Allow-Origin supplied from the input side, plus allow-credentials as requested
            addHeaders(rbuilder, CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN, originHeader);
            rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS,
                            Boolean.toString(allowCredentials));
            // 5.2.8 max-age
            if (effectiveMaxAge(annotation) != null) {
                rbuilder.header(CorsHeaderConstants.HEADER_AC_MAX_AGE, 
                                effectiveMaxAge(annotation).toString());
            }
            // 5.2.9 add allowed methods
            /*
             * Currently, input side just lists the one requested method, and spec endorses that.
             */
            addHeaders(rbuilder, CorsHeaderConstants.HEADER_AC_ALLOW_METHODS,
                       getHeadersFromInput(m, CorsHeaderConstants.HEADER_AC_ALLOW_METHODS));
            // 5.2.10 add allowed headers
            List<String> rqAllowedHeaders = getHeadersFromInput(m,
                                                                CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS);
            if (rqAllowedHeaders != null) {
                addHeaders(rbuilder, CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS, rqAllowedHeaders);
            }
            return rbuilder.build();

        }
    }

    private boolean effectiveAllowAllOrigins(CrossOriginResourceSharing ann) {
        if (ann != null) {
            return ann.allowAllOrigins();
        } else {
            return allowAllOrigins;
        }
    }

    private boolean effectiveAllowCredentials(CrossOriginResourceSharing ann) {
        if (ann != null) {
            return ann.allowCredentials();
        } else {
            return allowCredentials;
        }
    }

    private List<String> effectiveAllowOrigins(CrossOriginResourceSharing ann) {
        if (ann != null) {
            if (ann.allowOrigins() == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(ann.allowOrigins());
        } else {
            return allowOrigins;
        }
    }
    
    private List<String> effectiveAllowMethods(CrossOriginResourceSharing ann) {
        if (ann != null) {
            if (ann.allowMethods() == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(ann.allowMethods());
        } else {
            return allowMethods;
        }
    }

    private List<String> effectiveAllowHeaders(CrossOriginResourceSharing ann) {
        if (ann != null) {
            if (ann.allowHeaders() == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(ann.allowHeaders());
        } else {
            return allowHeaders;
        }
    }

    private List<String> effectiveExposeHeaders(CrossOriginResourceSharing ann) {
        if (ann != null) {
            if (ann.exposeHeaders() == null) {
                return Collections.emptyList();
            }
            return Arrays.asList(ann.exposeHeaders());
        } else {
            return exposeHeaders;
        }
    }

    private Integer effectiveMaxAge(CrossOriginResourceSharing ann) {
        if (ann != null) {
            int ma = ann.maxAge();
            if (ma < 0) {
                return null;
            } else {
                return Integer.valueOf(ma);
            }
        } else {
            return maxAge;
        }
    }

    /**
     * The origin strings to allow. Call {@link #setAllowAllOrigins(boolean)} to enable '*'.
     * 
     * @param allowedOrigins a list of case-sensitive origin strings.
     */
    public void setAllowOrigins(List<String> allowedOrigins) {
        this.allowOrigins = allowedOrigins;
    }

    public List<String> getAllowOrigins() {
        return allowOrigins;
    }

    /**
     * Whether to implement Access-Control-Allow-Origin: *
     * 
     * @param allowAllOrigins if true, all origins are accepted and * is returned in the header. Sections
     *            5.1.1 and 5.1.2, and 5.2.1 and 5.2.2. If false, then the list of allowed origins must be
     */
    public void setAllowAllOrigins(boolean allowAllOrigins) {
        this.allowAllOrigins = allowAllOrigins;
    }

    public List<String> getAllowMethods() {
        return allowMethods;
    }

    /**
     * The list of allowed non-simple methods for preflight checks. Section 5.2.3.
     * 
     * @param allowedMethods a list of case-sensitive HTTP method names.
     */
    public void setAllowMethods(List<String> allowedMethods) {
        this.allowMethods = allowedMethods;
    }

    public List<String> getAllowHeaders() {
        return allowHeaders;
    }

    /**
     * The list of allowed headers for preflight checks. Section 5.2.6
     * 
     * @param allowedHeaders a list of permitted headers.
     */
    public void setAllowHeaders(List<String> allowedHeaders) {
        this.allowHeaders = allowedHeaders;
    }

    public List<String> getExposeHeaders() {
        return exposeHeaders;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    /**
     * The value for the Access-Control-Allow-Credentials header. If false, no header is added. If true, the
     * header is added with the value 'true'.
     * 
     * @param allowCredentials
     */
    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    /**
     * A list of non-simple headers to be exposed via Access-Control-Expose-Headers.
     * 
     * @param exposeHeaders the list of (case-sensitive) header names.
     */
    public void setExposeHeaders(List<String> exposeHeaders) {
        this.exposeHeaders = exposeHeaders;
    }

    /**
     * The value for Access-Control-Max-Age.
     * 
     * @param maxAge An integer 'delta-seconds' or null. If null, no header is added.
     */
    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    @SuppressWarnings("unchecked")
    List<String> getHeadersFromInput(Message m, String key) {
        Object obj = m.getExchange().get(key);
        if (obj instanceof List<?>) {
            return (List<String>)obj;
        }
        return null;
    }

    private void addHeaders(ResponseBuilder rb, String key, List<String> vals) {
        for (String v : vals) {
            rb.header(key, v);
        }
    }

}
