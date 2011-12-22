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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.ext.ResponseHandler;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;

/**
 * An single class that provides both an input and an output filter for CORS, following
 * http://www.w3.org/TR/cors/. The input examines the input headers. If the request is valid, it stores the
 * information in the Exchange to allow the response handler to add the appropriate headers to the response.
 * If you need complex or subtle control of the behavior here (e.g. clearing the prefight cache) you might be
 * better off reading the source of this and implementing this inside your service.
 * 
 * This class will perform preflight processing even if there is a resource method annotated 
 * to handle @OPTIONS,
 * <em>unless</em> that method is annotated as follows:
 * <pre>
 *   @CrossOriginResourceSharing(localPreflight = true)
 * </pre>
 * or unless the <tt>defaultOptionsMethodsHandlePreflight</tt> property of this class is set to <tt>true</tt>.
 */
public class CrossOriginResourceSharingFilter implements RequestHandler, ResponseHandler {
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
    private static final Pattern FIELD_COMMA_PATTERN = Pattern.compile(",\\w*");
    
    private static final String PREFLIGHT_PASSED = "preflight_passed";
    private static final String PREFLIGHT_FAILED = "preflight_failed";
    private static final String SIMPLE_REQUEST = "simple_request";
    
    @Context
    private HttpHeaders headers;

    /**
     * This would be a rather painful list to maintain for real, since it's entirely dependent on the
     * deployment.
     */
    private List<String> allowOrigins = Collections.emptyList();
    private List<String> allowHeaders = Collections.emptyList();
    private boolean allowAllOrigins;
    private boolean allowCredentials;
    private List<String> exposeHeaders = Collections.emptyList();
    private Integer maxAge;
    private Integer preflightFailStatus = 200;
    private boolean defaultOptionsMethodsHandlePreflight;
    private boolean allowAnyHeaders;
    
    
    private CrossOriginResourceSharing getAnnotation(OperationResourceInfo ori) {
        if (ori == null) {
            return null;
        }
        return ReflectionUtil.getAnnotationForMethodOrContainingClass(ori.getAnnotatedMethod(),
                                                                      CrossOriginResourceSharing.class);
    }

    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
        OperationResourceInfo opResInfo = m.getExchange().get(OperationResourceInfo.class);
        /*
         * If there is an actual method annotated with @OPTIONS, this is the annotation (if any) from it.
         * The lookup falls back to it.
         */
        CrossOriginResourceSharing annotation = getAnnotation(opResInfo);
        /*
         * If we don't have an annotation on the target method or an @OPTION method, perhaps
         * we've got one on the class?
         */
        if (annotation == null) {
            annotation = resourceClass.getServiceClass().getAnnotation(CrossOriginResourceSharing.class);
        }

        if ("OPTIONS".equals(m.get(Message.HTTP_REQUEST_METHOD))) {
          
            return preflightRequest(m, annotation, opResInfo, resourceClass);
        }
        return simpleRequest(m, annotation);
    }

    private Response simpleRequest(Message m, CrossOriginResourceSharing ann) {
        List<String> values = getHeaderValues(CorsHeaderConstants.HEADER_ORIGIN, true);
        // 5.1.1 there has to be an origin
        if (values == null || values.size() == 0) {
            return null;
        }
        
        // 5.1.2 check all the origins
        if (!effectiveAllowAllOrigins(ann) && !effectiveAllowOrigins(ann).containsAll(values)) {
            return null;
        }
        
        String originResponse;
        // 5.1.3 credentials lives in the output filter
        // in any case
        if (effectiveAllowAllOrigins(ann)) {
            originResponse = "*";
        } else {
            originResponse = concatValues(values, true);
        }

        // handle 5.1.3
        commonRequestProcessing(m, ann, originResponse);
        
        // 5.1.4
        List<String> effectiveExposeHeaders = effectiveExposeHeaders(ann);
        if (effectiveExposeHeaders != null && effectiveExposeHeaders.size() != 0) {
            m.getExchange().put(CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS, effectiveExposeHeaders);
        }

        // note what kind of processing we're doing.
        m.getExchange().put(CrossOriginResourceSharingFilter.class.getName(), SIMPLE_REQUEST);
        return null;
    }

    /**
     * handle preflight.
     * 
     * Note that preflight is a bit of a parasite on OPTIONS. The class may still have an options method,
     * and, if it does, it will be invoked, and it will respond however it likes. The response will
     * have additional headers based on what happens here.
     * 
     * @param m the incoming message.
     * @param opResInfo 
     * @param ann the annotation, if any, derived from a method that matched the OPTIONS request for the
     *            preflight. probably completely useless.
     * @param resourceClass the resource class passed into the filter.
     * @return
     */
    //CHECKSTYLE:OFF
    private Response preflightRequest(Message m, CrossOriginResourceSharing optionAnn,
                                      OperationResourceInfo opResInfo, ClassResourceInfo resourceClass) {

        /*
         * What to do if the resource class indeed has a method annotated with @OPTIONS 
         * that is matched by this request? We go ahead and do this job unless the request
         * has one of our annotations on it (or its parent class) indicating 'localPreflight' --
         * or the defaultOptionsMethodsHandlePreflight flag is true.
         */
        if (opResInfo != null && ((optionAnn == null && defaultOptionsMethodsHandlePreflight) 
            || (optionAnn != null && optionAnn.localPreflight()))) {
            return null; // let the resource method take all responsibility.
        }
        
        List<String> headerOriginValues = getHeaderValues(CorsHeaderConstants.HEADER_ORIGIN, true);
        String origin;
        // 5.2.1 -- must have origin, must have one origin.
        if (headerOriginValues == null || headerOriginValues.size() != 1) {
            return null;
        }
        origin = headerOriginValues.get(0);

        List<String> requestMethodValues = getHeaderValues(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, false);

        // 5.2.3 must have access-control-request-method, must be single-valued
        // we should reject parse errors but we cannot.
        if (requestMethodValues == null || requestMethodValues.size() != 1) {
            return createPreflightResponse(m, false);
        }
        String requestMethod = requestMethodValues.get(0);
        /*
         * CORS doesn't send enough information with a preflight to accurately identity the single method
         * that will handle the request. We ask the JAX-RS runtime to find the matching method which is
         * expected to have a CrossOriginResourceSharing annotation set.
         */
        
        Method method = getPreflightMethod(m, requestMethod);
        if (method == null) {
            return null;
        }
        CrossOriginResourceSharing ann = method.getAnnotation(CrossOriginResourceSharing.class);
        ann = ann == null ? optionAnn : ann;
        
        /* We aren't required to have any annotation at all. If no annotation,
         * the properties of this filter make all the decisions.
         */

        // 5.2.2 must be on the list or we must be matching *.
        boolean effectiveAllowAllOrigins = effectiveAllowAllOrigins(ann);
        if (!effectiveAllowAllOrigins && !effectiveAllowOrigins(ann).contains(origin)) {
            return createPreflightResponse(m, false);
        }

        // 5.2.4 get list of request headers. we should reject parse errors but we cannot.
        List<String> requestHeaders = getHeaderValues(CorsHeaderConstants.HEADER_AC_REQUEST_HEADERS, false);

        // 5.2.5 reject if the method is not on the list.
        // This was indirectly enforced by getCorsMethod()

        // 5.2.6 reject if the header is not listed.
        if (!effectiveAllowAnyHeaders(ann) && !effectiveAllowHeaders(ann).containsAll(requestHeaders)) {
            return createPreflightResponse(m, false);
        }

        // 5.2.7: add allow credentials and allow-origin as required: this lives in the Output filter
        String originResponse;
        if (effectiveAllowAllOrigins(ann)) {
            originResponse = "*";
        } else {
            originResponse = origin;
        }
        // 5.2.9 add allow-methods; we pass them from here to the output filter which actually adds them.
        m.getExchange().put(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS, Arrays.asList(requestMethod));
        
        // 5.2.10 add allow-headers; we pass them from here to the output filter which actually adds them.
        m.getExchange().put(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS, requestHeaders);
        
        // 5.2.8 max-age lives in the output filter.
        if (effectiveMaxAge(ann) != null) {
            m.getExchange().put(CorsHeaderConstants.HEADER_AC_MAX_AGE,effectiveMaxAge(ann).toString());
        }

        // 5.2.7 is in here.
        commonRequestProcessing(m, ann, originResponse);

        return createPreflightResponse(m, true);
    }
    //CHECKSTYLE:ON

    private Response createPreflightResponse(Message m, boolean passed) {
        m.getExchange().put(CrossOriginResourceSharingFilter.class.getName(), 
                            passed ? PREFLIGHT_PASSED : PREFLIGHT_FAILED);
        int status = passed ? 200 : preflightFailStatus;
        return Response.status(status).build();
    }
    
    private Method getPreflightMethod(Message m, String httpMethod) {
        String requestUri = HttpUtils.getPathToMatch(m, true);
        
        Service service = m.getExchange().get(Service.class);
        List<ClassResourceInfo> resources = ((JAXRSServiceImpl)service).getClassResourceInfos();
        MultivaluedMap<String, String> values = new MetadataMap<String, String>();
        ClassResourceInfo resource = JAXRSUtils.selectResourceClass(resources, 
                                                                    requestUri, 
                                                                    values,
                                                                    m);
        if (resource == null) {
            return null;
        }
        OperationResourceInfo ori = findPreflightMethod(resource, requestUri, httpMethod, values, m);
        return ori == null ? null : ori.getAnnotatedMethod();
    }
    
    
    private OperationResourceInfo findPreflightMethod(ClassResourceInfo resource, 
                                                      String requestUri,
                                                      String httpMethod,
                                                      MultivaluedMap<String, String> values, 
                                                      Message m) {
        final String contentType = MediaType.WILDCARD;
        final MediaType acceptType = MediaType.WILDCARD_TYPE;
        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(resource, 
                                    m, httpMethod, values, 
                                    contentType, 
                                    Collections.singletonList(acceptType), 
                                    true); 
        if (ori == null) {
            return null;
        }
        if (ori.isSubResourceLocator()) {
            Class<?> cls = ori.getMethodToInvoke().getReturnType();
            ClassResourceInfo subcri = resource.getSubResource(cls, cls);
            if (subcri == null) {
                return null;
            } else {
                MultivaluedMap<String, String> newValues = new MetadataMap<String, String>();
                newValues.putAll(values);
                return findPreflightMethod(subcri, 
                                           values.getFirst(URITemplate.FINAL_MATCH_GROUP),
                                           httpMethod, 
                                           newValues, 
                                           m);
            }
        } else {
            return ori;
        }
    }
    
    private void commonRequestProcessing(Message m, CrossOriginResourceSharing ann, String origin) {
        
        m.getExchange().put(CorsHeaderConstants.HEADER_ORIGIN, origin);
        m.getExchange().put(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS, effectiveAllowCredentials(ann));
    }

    public Response handleResponse(Message m, OperationResourceInfo ori, Response response) {
        String op = (String)m.getExchange().get(CrossOriginResourceSharingFilter.class.getName());
        if (op == null || op == PREFLIGHT_FAILED) {
            return response;
        }

        ResponseBuilder rbuilder = Response.fromResponse(response);
        
        /* Common to simple and preflight */
        rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN, 
                        (String)m.getExchange().get(CorsHeaderConstants.HEADER_ORIGIN));
        rbuilder.header(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS,
                        Boolean.toString(allowCredentials));
        
        if (SIMPLE_REQUEST.equals(op)) {
            /* 5.1.4 expose headers */
            List<String> effectiveExposeHeaders 
                = getHeadersFromInput(m, CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS);
            if (effectiveExposeHeaders != null) {
                addHeaders(rbuilder, CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS, 
                           effectiveExposeHeaders, false);
            }
            // if someone wants to clear the cache, we can't help them.
            return rbuilder.build();
        } else {
            // 5.2.8 max-age
            String maValue = (String)m.getExchange().get(CorsHeaderConstants.HEADER_AC_MAX_AGE);
            if (maValue != null) {
                rbuilder.header(CorsHeaderConstants.HEADER_AC_MAX_AGE, maValue);
            }
            // 5.2.9 add allowed methods
            /*
             * Currently, input side just lists the one requested method, and spec endorses that.
             */
            addHeaders(rbuilder, CorsHeaderConstants.HEADER_AC_ALLOW_METHODS,
                       getHeadersFromInput(m, CorsHeaderConstants.HEADER_AC_ALLOW_METHODS), false);
            // 5.2.10 add allowed headers
            List<String> rqAllowedHeaders = getHeadersFromInput(m,
                                                                CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS);
            if (rqAllowedHeaders != null) {
                addHeaders(rbuilder, CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS, rqAllowedHeaders, false);
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
    
    private boolean effectiveAllowAnyHeaders(CrossOriginResourceSharing ann) {
        if (ann != null) {
            return ann.allowAnyHeaders();
        } else {
            return allowAnyHeaders;
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
     * Function called to grab a list of strings left behind by the input side.
     * @param m
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<String> getHeadersFromInput(Message m, String key) {
        Object obj = m.getExchange().get(key);
        if (obj instanceof List<?>) {
            return (List<String>)obj;
        }
        return null;
    }

    /**
     * CORS uses one header containing space-separated values (Origin) and then
     * a raft of #field-name productions, which parse on commas and optional spaces.
     * @param m
     * @param key
     * @return
     */
    private List<String> getHeaderValues(String key, boolean spaceSeparated) {
        List<String> values = headers.getRequestHeader(key);
        Pattern splitPattern;
        if (spaceSeparated) {
            splitPattern = SPACE_PATTERN;
        } else {
            splitPattern = FIELD_COMMA_PATTERN;
        }
        List<String> results = new ArrayList<String>();
        for (String value : values) {
            String[] items = splitPattern.split(value);
            for (String item : items) {
                results.add(item);
            }
        }
        return results;
    }
    
    private void addHeaders(ResponseBuilder rb, String key, List<String> values, boolean spaceSeparated) {
        String sb = concatValues(values, spaceSeparated);
        rb.header(key, sb);
    }

    private String concatValues(List<String> values, boolean spaceSeparated) {
        StringBuffer sb = new StringBuffer();
        for (int x = 0; x < values.size(); x++) {
            sb.append(values.get(x));
            if (x != values.size() - 1) {
                if (spaceSeparated) {
                    sb.append(" ");
                } else {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
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
     * @param allowAllOrigins if true, all origins are accepted and 
     * "*" is returned in the header. Sections
     * 5.1.1 and 5.1.2, and 5.2.1 and 5.2.2. If false, then the list of allowed origins must be
     */
    public void setAllowAllOrigins(boolean allowAllOrigins) {
        this.allowAllOrigins = allowAllOrigins;
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
    
    /**
     * Preflight error response status, default is 200.
     * 
     * @param status HTTP status code.
     */
    public void setPreflightErrorStatus(Integer status) {
        this.preflightFailStatus = status;
    }


    public boolean isDefaultOptionsMethodsHandlePreflight() {
        return defaultOptionsMethodsHandlePreflight;
    }

    /**
     * What to do when a preflight request comes along for a resource that has a handler method for
     * \@OPTIONS and there is no <tt>@{@link CrossResourceSharing}(localPreflight = val)</tt>
     * annotation on the method. If this is <tt>true</tt>, then the filter 
     * defers to the resource class method.
     * If this is false, then this filter performs preflight processing.
     * @param defaultOptionsMethodsHandlePreflight true to defer to resource methods.
     */
    public void setDefaultOptionsMethodsHandlePreflight(boolean defaultOptionsMethodsHandlePreflight) {
        this.defaultOptionsMethodsHandlePreflight = defaultOptionsMethodsHandlePreflight;
    }

    public boolean isAllowAnyHeaders() {
        return allowAnyHeaders;
    }

    /**
     * Completely relax the Access-Control-Request-Headers check. 
     * Any headers in this header will be permitted. Handy for 
     * dealing with Chrome / Firefox / Safari incompatibilities.
     * @param allowAnyHeader whether to allow any header. If <tt>false</tt>,
     * respect the allowHeaders property.
     */
    public void setAllowAnyHeaders(boolean allowAnyHeader) {
        this.allowAnyHeaders = allowAnyHeader;
    }

}
