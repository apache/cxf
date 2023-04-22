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

package org.apache.cxf.rs.security.cors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * A single class that provides both an input and an output filter for CORS, following
 * http://www.w3.org/TR/cors/. The input filter examines the input headers. If the request is valid, it stores the
 * information in the Exchange to allow the response handler to add the appropriate headers to the response.
 * If you need complex or subtle control of the behavior here (e.g. clearing the prefight cache) you might be
 * better off reading the source of this class and implementing this inside your service.
 *
 * This class will perform preflight processing even if there is a resource method annotated
 * to handle @OPTIONS,
 * <em>unless</em> that method is annotated as follows:
 * <pre>
 *   @LocalPreflight
 * </pre>
 * or unless the <tt>defaultOptionsMethodsHandlePreflight</tt> property of this class is set to <tt>true</tt>.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION - 1)
public class CrossOriginResourceSharingFilter implements ContainerRequestFilter,
    ContainerResponseFilter {
    private static final String SPACE_PATTERN = " ";
    private static final String FIELD_COMMA_PATTERN = ",";

    private static final String LOCAL_PREFLIGHT = "local_preflight";
    private static final String LOCAL_PREFLIGHT_ORIGIN = "local_preflight.origin";
    private static final String LOCAL_PREFLIGHT_METHOD = "local_preflight.method";

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
    private boolean allowCredentials;
    private List<String> exposeHeaders = Collections.emptyList();
    private Integer maxAge;
    private Integer preflightFailStatus = 200;
    private boolean defaultOptionsMethodsHandlePreflight;
    private boolean findResourceMethod = true;
    private boolean blockCorsIfUnauthorized;

    private <T extends Annotation> T  getAnnotation(Method m,
                                                    Class<T> annClass) {
        if (m == null) {
            return null;
        }
        return ReflectionUtil.getAnnotationForMethodOrContainingClass(m,  annClass);
    }

    @Override
    public void filter(ContainerRequestContext context) {
        Message m = JAXRSUtils.getCurrentMessage();

        String httpMethod = (String)m.get(Message.HTTP_REQUEST_METHOD);
        if (HttpMethod.OPTIONS.equals(httpMethod)) {
            Response r = preflightRequest(m);
            if (r != null) {
                context.abortWith(r);
            }
        } else if (findResourceMethod) {
            Method method = getResourceMethod(m, httpMethod);
            simpleRequest(m, method);
        } else {
            m.getInterceptorChain().add(new CorsInInterceptor());
        }

    }

    private Response simpleRequest(Message m, Method resourceMethod) {
        CrossOriginResourceSharing ann =
            getAnnotation(resourceMethod, CrossOriginResourceSharing.class);
        List<String> headerOriginValues = getHeaderValues(CorsHeaderConstants.HEADER_ORIGIN, true);
        // 5.1.1 there has to be an origin
        if (headerOriginValues == null || headerOriginValues.isEmpty()) {
            return null;
        }

        // 5.1.2 check all the origins
        if (!effectiveAllowOrigins(ann, headerOriginValues)) {
            return null;
        }

        // handle 5.1.3
        setAllowOriginAndCredentials(m, ann, headerOriginValues);

        // 5.1.4
        List<String> effectiveExposeHeaders = effectiveExposeHeaders(ann);
        if (effectiveExposeHeaders != null && !effectiveExposeHeaders.isEmpty()) {
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
     * @return
     */
    //CHECKSTYLE:OFF
    private Response preflightRequest(Message m) {

        // Validate main CORS preflight properties (origin, method)
        // even if Local preflight is requested

        // 5.2.1 -- must have origin, must have one origin.
        List<String> headerOriginValues = getHeaderValues(CorsHeaderConstants.HEADER_ORIGIN, true);
        if (headerOriginValues == null || headerOriginValues.size() != 1) {
            return null;
        }
        String origin = headerOriginValues.get(0);

        // 5.2.3 must have access-control-request-method, must be single-valued
        // we should reject parse errors but we cannot.
        List<String> requestMethodValues = getHeaderValues(CorsHeaderConstants.HEADER_AC_REQUEST_METHOD, false);
        if (requestMethodValues == null || requestMethodValues.size() != 1) {
            return createPreflightResponse(m, false);
        }
        String requestMethod = requestMethodValues.get(0);

        /*
         * Ask JAX-RS runtime to validate that the matching resource method actually exists.
         */

        Method method = null;
        if (findResourceMethod) {
            method = getResourceMethod(m, requestMethod);
            if (method == null) {
                return null;
            }
        }

        /*
         * What to do if the resource class indeed has a method annotated with @OPTIONS
         * that is matched by this request? We go ahead and do this job unless the request
         * has one of our annotations on it (or its parent class) indicating 'localPreflight' --
         * or the defaultOptionsMethodsHandlePreflight flag is true.
         */
        LocalPreflight preflightAnnotation = null;
        if (!defaultOptionsMethodsHandlePreflight) {
            Method optionsMethod = getResourceMethod(m, "OPTIONS");
            if (optionsMethod != null) {
                preflightAnnotation = getAnnotation(optionsMethod, LocalPreflight.class);
            }
        }

        if (preflightAnnotation != null || defaultOptionsMethodsHandlePreflight) {
            m.put(LOCAL_PREFLIGHT, "true");
            m.put(LOCAL_PREFLIGHT_ORIGIN, origin);
            m.put(LOCAL_PREFLIGHT_METHOD, method);
            return null; // let the resource method take all responsibility.
        }

        CrossOriginResourceSharing ann = getAnnotation(method, CrossOriginResourceSharing.class);

        /* We aren't required to have any annotation at all. If no annotation,
         * the properties of this filter make all the decisions.
         */

        // 5.2.2 must be on the list or we must be matching *.
        if (!effectiveAllowOrigins(ann, Collections.singletonList(origin))) {
            return createPreflightResponse(m, false);
        }

        // 5.2.4 get list of request headers. we should reject parse errors but we cannot.
        List<String> requestHeaders = getHeaderValues(CorsHeaderConstants.HEADER_AC_REQUEST_HEADERS, false);

        // 5.2.5 reject if the method is not on the list.
        // This was indirectly enforced by getCorsMethod()

        // 5.2.6 reject if the header is not listed.
        if (!effectiveAllowHeaders(ann, requestHeaders)) {
            return createPreflightResponse(m, false);
        }

        // 5.2.9 add allow-methods; we pass them from here to the output filter which actually adds them.
        m.getExchange().put(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS, Arrays.asList(requestMethod));

        // 5.2.10 add allow-headers; we pass them from here to the output filter which actually adds them.
        m.getExchange().put(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS, requestHeaders);

        // 5.2.8 max-age lives in the output filter.
        if (effectiveMaxAge(ann) != null) {
            m.getExchange().put(CorsHeaderConstants.HEADER_AC_MAX_AGE, effectiveMaxAge(ann).toString());
        }

        // 5.2.7 is in here.
        setAllowOriginAndCredentials(m, ann, headerOriginValues);

        return createPreflightResponse(m, true);
    }
    //CHECKSTYLE:ON

    private Response createPreflightResponse(Message m, boolean passed) {
        m.getExchange().put(CrossOriginResourceSharingFilter.class.getName(),
                            passed ? PREFLIGHT_PASSED : PREFLIGHT_FAILED);
        int status = passed ? 200 : preflightFailStatus;
        return Response.status(status).build();
    }

    private Method getResourceMethod(Message m, String httpMethod) {
        String requestUri = HttpUtils.getPathToMatch(m, true);

        List<ClassResourceInfo> resources = JAXRSUtils.getRootResources(m);
        Map<ClassResourceInfo, MultivaluedMap<String, String>> matchedResources =
            JAXRSUtils.selectResourceClass(resources, requestUri, m);
        if (matchedResources == null) {
            return null;
        }
        MultivaluedMap<String, String> values = new MetadataMap<>();
        OperationResourceInfo ori = findPreflightMethod(matchedResources, requestUri, httpMethod, values, m);
        return ori == null ? null : ori.getAnnotatedMethod();
    }


    private OperationResourceInfo findPreflightMethod(
        Map<ClassResourceInfo, MultivaluedMap<String, String>> matchedResources,
                                                      String requestUri,
                                                      String httpMethod,
                                                      MultivaluedMap<String, String> values,
                                                      Message m) {
        final String contentType = MediaType.WILDCARD;
        final MediaType acceptType = MediaType.WILDCARD_TYPE;
        OperationResourceInfo ori = JAXRSUtils.findTargetMethod(matchedResources,
                                    m, httpMethod, values,
                                    contentType,
                                    Collections.singletonList(acceptType),
                                    false,
                                    false);
        if (ori == null) {
            return null;
        }
        if (ori.isSubResourceLocator()) {
            Class<?> cls = ori.getMethodToInvoke().getReturnType();
            ClassResourceInfo subcri = ori.getClassResourceInfo().getSubResource(cls, cls);
            if (subcri == null) {
                return null;
            }
            MultivaluedMap<String, String> newValues = new MetadataMap<>();
            newValues.putAll(values);
            return findPreflightMethod(Collections.singletonMap(subcri, newValues),
                                       values.getFirst(URITemplate.FINAL_MATCH_GROUP),
                                       httpMethod,
                                       newValues,
                                       m);
        }
        return ori;
    }

    private void setAllowOriginAndCredentials(Message m,
                                              CrossOriginResourceSharing ann,
                                              List<String> headerOriginValues) {

        boolean allowCreds = effectiveAllowCredentials(ann);
        m.getExchange().put(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS, allowCreds);

        String originResponse;
        if (!allowCreds && effectiveAllowAllOrigins(ann)) {
            originResponse = "*";
        } else {
            originResponse = concatValues(headerOriginValues, true);
        }

        m.getExchange().put(CorsHeaderConstants.HEADER_ORIGIN, originResponse);

    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {

        Message m = JAXRSUtils.getCurrentMessage();
        String op = (String)m.getExchange().get(CrossOriginResourceSharingFilter.class.getName());
        if (op == null || PREFLIGHT_FAILED.equals(op)) {
            return;
        }
        if (responseContext.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()
            && blockCorsIfUnauthorized) {
            return;
        }

        /* Common to simple and preflight */
        responseContext.getHeaders().putSingle(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN,
                        m.getExchange().get(CorsHeaderConstants.HEADER_ORIGIN));
        responseContext.getHeaders().putSingle(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS,
                        m.getExchange().get(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS));

        if (SIMPLE_REQUEST.equals(op)) {
            /* 5.1.4 expose headers */
            List<String> effectiveExposeHeaders
                = getHeadersFromInput(m, CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS);
            if (effectiveExposeHeaders != null) {
                addHeaders(responseContext, CorsHeaderConstants.HEADER_AC_EXPOSE_HEADERS,
                           effectiveExposeHeaders, false);
            }
            // if someone wants to clear the cache, we can't help them.
        } else {
            // 5.2.8 max-age
            String maValue = (String)m.getExchange().get(CorsHeaderConstants.HEADER_AC_MAX_AGE);
            if (maValue != null) {
                responseContext.getHeaders().putSingle(CorsHeaderConstants.HEADER_AC_MAX_AGE, maValue);
            }
            // 5.2.9 add allowed methods
            /*
             * Currently, input side just lists the one requested method, and spec endorses that.
             */
            addHeaders(responseContext, CorsHeaderConstants.HEADER_AC_ALLOW_METHODS,
                       getHeadersFromInput(m, CorsHeaderConstants.HEADER_AC_ALLOW_METHODS), false);
            // 5.2.10 add allowed headers
            List<String> rqAllowedHeaders = getHeadersFromInput(m,
                                                                CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS);
            if (rqAllowedHeaders != null) {
                addHeaders(responseContext, CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS, rqAllowedHeaders, false);
            }
        }
    }

    private boolean effectiveAllowAllOrigins(CrossOriginResourceSharing ann) {
        if (ann != null) {
            return ann.allowAllOrigins();
        }
        return allowOrigins.isEmpty();
    }

    private boolean effectiveAllowCredentials(CrossOriginResourceSharing ann) {
        if (ann != null) {
            return ann.allowCredentials();
        }
        return allowCredentials;
    }

    private boolean effectiveAllowOrigins(CrossOriginResourceSharing ann, List<String> origins) {
        if (effectiveAllowAllOrigins(ann)) {
            return true;
        }
        List<String> actualOrigins = Collections.emptyList();
        if (ann != null) {
            actualOrigins = Arrays.asList(ann.allowOrigins());
        }

        if (actualOrigins.isEmpty()) {
            actualOrigins = allowOrigins;
        }

        return actualOrigins.containsAll(origins);
    }

    private boolean effectiveAllowAnyHeaders(CrossOriginResourceSharing ann) {
        if (ann != null) {
            return ann.allowHeaders().length == 0;
        }
        return allowHeaders.isEmpty();
    }

    private boolean effectiveAllowHeaders(CrossOriginResourceSharing ann, List<String> aHeaders) {
        if (effectiveAllowAnyHeaders(ann)) {
            return true;
        }
        Set<String> actualHeadersSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        actualHeadersSet.addAll(ann != null ? Arrays.asList(ann.allowHeaders()) : allowHeaders);
        return actualHeadersSet.containsAll(aHeaders);
    }

    private List<String> effectiveExposeHeaders(CrossOriginResourceSharing ann) {
        return ann != null ? Arrays.asList(ann.exposeHeaders()) : exposeHeaders;
    }

    private Integer effectiveMaxAge(CrossOriginResourceSharing ann) {
        if (ann != null) {
            int ma = ann.maxAge();
            if (ma < 0) {
                return null;
            }
            return Integer.valueOf(ma);
        }
        return maxAge;
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
     * @param key
     * @param spaceSeparated
     * @return
     */
    private List<String> getHeaderValues(String key, boolean spaceSeparated) {
        List<String> values = headers.getRequestHeader(key);
        String splitPattern;
        if (spaceSeparated) {
            splitPattern = SPACE_PATTERN;
        } else {
            splitPattern = FIELD_COMMA_PATTERN;
        }
        final List<String> results;
        if (values != null) {
            results = new ArrayList<>();
            for (String value : values) {
                for (String item : value.split(splitPattern)) {
                    results.add(item.trim());
                }
            }
        } else {
            results = Collections.emptyList();
        }
        return results;
    }

    private void addHeaders(ContainerResponseContext responseContext,
                            String key, List<String> values, boolean spaceSeparated) {
        String sb = concatValues(values, spaceSeparated);
        responseContext.getHeaders().putSingle(key, sb);
    }

    private String concatValues(List<String> values, boolean spaceSeparated) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < values.size(); x++) {
            sb.append(values.get(x));
            if (x != values.size() - 1) {
                if (spaceSeparated) {
                    sb.append(' ');
                } else {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    /**
     * The origin strings to allow. An empty list allows all origins.
     *
     * @param allowedOrigins a list of case-sensitive origin strings.
     */
    public void setAllowOrigins(List<String> allowedOrigins) {
        this.allowOrigins = allowedOrigins;
    }

    /** @return the list of allowed origins. */
    public List<String> getAllowOrigins() {
        return allowOrigins;
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

    public void setFindResourceMethod(boolean findResourceMethod) {
        this.findResourceMethod = findResourceMethod;
    }

    public void setBlockCorsIfUnauthorized(boolean blockCorsIfUnauthorized) {
        this.blockCorsIfUnauthorized = blockCorsIfUnauthorized;
    }

    private class CorsInInterceptor extends AbstractPhaseInterceptor<Message> {

        CorsInInterceptor() {
            super(Phase.PRE_INVOKE);
        }

        @Override
        public void handleMessage(Message message) {
            OperationResourceInfo ori = message.getExchange().get(OperationResourceInfo.class);
            simpleRequest(message, ori.getAnnotatedMethod());
        }
    }
}
