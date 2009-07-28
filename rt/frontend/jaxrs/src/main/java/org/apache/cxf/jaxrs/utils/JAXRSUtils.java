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

package org.apache.cxf.jaxrs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.HttpServletResponseFilter;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.impl.RequestImpl;
import org.apache.cxf.jaxrs.impl.SecurityContextImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfoComparator;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfoComparator;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

public final class JAXRSUtils {

    public static final MediaType ALL_TYPES = new MediaType();
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSUtils.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSUtils.class);
    private static final String PROPOGATE_EXCEPTION = "org.apache.cxf.propogate.exception";
    private static final String FORM_PARAM_MAP = JAXRSUtils.class.getName() + ".FORM_DATA";

    private JAXRSUtils() {        
    }
    
    public static List<PathSegment> getPathSegments(String thePath, boolean decode) {
        return getPathSegments(thePath, decode, true);
    }
    
    public static List<PathSegment> getPathSegments(String thePath, boolean decode, 
                                                    boolean ignoreLastSlash) {
        String[] segments = thePath.split("/");
        List<PathSegment> theList = new ArrayList<PathSegment>();
        for (String path : segments) {
            if (!StringUtils.isEmpty(path)) {
                theList.add(new PathSegmentImpl(path, decode));
            }
        }
        int len = thePath.length();
        if (len > 0 && thePath.charAt(len - 1) == '/') {
            String value = ignoreLastSlash ? "" : "/";
            theList.add(new PathSegmentImpl(value, false));
        }
        return theList;
    }

    @SuppressWarnings("unchecked")
    private static String[] getUserMediaTypes(Object provider, String methodName) {
        String[] values = null;
        if (AbstractConfigurableProvider.class.isAssignableFrom(provider.getClass())) {
            try {
                Method m = provider.getClass().getMethod(methodName, new Class[]{});
                List<String> types = (List<String>)m.invoke(provider, new Object[]{});
                if (types != null) {
                    values =  types.size() > 0 ? types.toArray(new String[]{})
                                               : new String[]{"*/*"};
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        return values;
    }
    
    public static List<MediaType> getProviderConsumeTypes(MessageBodyReader provider) {
        String[] values = getUserMediaTypes(provider, "getConsumeMediaTypes");
        
        if (values == null) {
            Consumes c = provider.getClass().getAnnotation(Consumes.class);
            values = c == null ? new String[]{"*/*"} : c.value();
        }
        return JAXRSUtils.getMediaTypes(values);
    }
    
    public static List<MediaType> getProviderProduceTypes(MessageBodyWriter provider) {
        String[] values = getUserMediaTypes(provider, "getProduceMediaTypes");
        
        if (values == null) {
            Produces c = provider.getClass().getAnnotation(Produces.class);
            values = c == null ? new String[]{"*/*"} : c.value();
        }
        return JAXRSUtils.getMediaTypes(values);
    }
    
    public static List<MediaType> getMediaTypes(String[] values) {
        List<MediaType> supportedMimeTypes = new ArrayList<MediaType>(values.length);
        for (int i = 0; i < values.length; i++) {
            supportedMimeTypes.add(MediaType.valueOf(values[i]));    
        }
        return supportedMimeTypes;
    }
    
    @SuppressWarnings("unchecked")
    public static void handleSetters(OperationResourceInfo ori,
                                     Object requestObject,
                                     Message message) {
        ClassResourceInfo cri = ori.getClassResourceInfo();
        InjectionUtils.injectContextMethods(requestObject, cri, message);
        // Param methods
        MultivaluedMap<String, String> values = 
            (MultivaluedMap<String, String>)message.get(URITemplate.TEMPLATE_PARAMETERS);
        for (Method m : cri.getParameterMethods()) {
            Parameter p = ResourceUtils.getParameter(0, m.getAnnotations());
            Object o = createHttpParameterValue(p, 
                                                m.getParameterTypes()[0],
                                                m.getGenericParameterTypes()[0],
                                                message,
                                                values,
                                                ori);
            if (o != null) { 
                InjectionUtils.injectThroughMethod(requestObject, m, o);
            }
        }
        // Param fields
        for (Field f : cri.getParameterFields()) {
            Parameter p = ResourceUtils.getParameter(0, f.getAnnotations());
            Object o = createHttpParameterValue(p, 
                                                f.getType(),
                                                f.getGenericType(),
                                                message,
                                                values,
                                                ori);
            if (o != null) { 
                InjectionUtils.injectFieldValue(f, requestObject, o);
            }
        }
        
    }
    
    public static ClassResourceInfo selectResourceClass(List<ClassResourceInfo> resources,
                                                 String path, 
                                                 MultivaluedMap<String, String> values) {
        
        if (resources.size() == 1) { 
            return resources.get(0).getURITemplate().match(path, values)
                   ? resources.get(0) : null;
        }
        
        SortedMap<ClassResourceInfo, MultivaluedMap<String, String>> candidateList = 
            new TreeMap<ClassResourceInfo, MultivaluedMap<String, String>>(
                new ClassResourceInfoComparator());
        
        for (ClassResourceInfo resource : resources) {
            MultivaluedMap<String, String> map = new MetadataMap<String, String>();
            if (resource.getURITemplate().match(path, map)) {
                candidateList.put(resource, map);
            }
        }
        
        if (!candidateList.isEmpty()) {
            Map.Entry<ClassResourceInfo, MultivaluedMap<String, String>> firstEntry = 
                candidateList.entrySet().iterator().next();
            values.putAll(firstEntry.getValue());
            return firstEntry.getKey();
        }
        
        return null;
    }

    public static OperationResourceInfo findTargetMethod(ClassResourceInfo resource, 
                                                         String path,
                                                         String httpMethod, 
                                                         MultivaluedMap<String, String> values, 
                                                         String requestContentType, 
                                                         List<MediaType> acceptContentTypes) {
        return JAXRSUtils.findTargetMethod(resource, path, httpMethod, values, requestContentType, 
                                           acceptContentTypes, true);
    }
    
    public static OperationResourceInfo findTargetMethod(ClassResourceInfo resource, 
                                                         String path,
                                                         String httpMethod, 
                                                         MultivaluedMap<String, String> values, 
                                                         String requestContentType, 
                                                         List<MediaType> acceptContentTypes,
                                                         boolean logNow) {
        SortedMap<OperationResourceInfo, MultivaluedMap<String, String>> candidateList = 
            new TreeMap<OperationResourceInfo, MultivaluedMap<String, String>>(
                new OperationResourceInfoComparator());
        MediaType requestType = requestContentType == null 
                                ? ALL_TYPES : MediaType.valueOf(requestContentType);
        
        int pathMatched = 0;
        int methodMatched = 0;
        int consumeMatched = 0;
        int produceMatched = 0;
        
        boolean subresourcesOnly = true;
        for (MediaType acceptType : acceptContentTypes) {
            for (OperationResourceInfo ori : resource.getMethodDispatcher().getOperationResourceInfos()) {
                URITemplate uriTemplate = ori.getURITemplate();
                MultivaluedMap<String, String> map = new MetadataMap<String, String>(values);
                if (uriTemplate != null && uriTemplate.match(path, map)) {
                    if (ori.isSubResourceLocator()) {
                        candidateList.put(ori, map);
                    } else {
                        String finalGroup = map.getFirst(URITemplate.FINAL_MATCH_GROUP);
                        if (finalGroup == null || StringUtils.isEmpty(finalGroup)
                            || finalGroup.equals("/")) {
                            pathMatched++;
                            boolean mMatched = matchHttpMethod(ori.getHttpMethod(), httpMethod);
                            boolean cMatched = matchConsumeTypes(requestType, ori);
                            boolean pMatched = matchProduceTypes(acceptType, ori);
                            if (mMatched && cMatched && pMatched) {
                                subresourcesOnly = false;
                                candidateList.put(ori, map);    
                            } else {
                                methodMatched = mMatched ? methodMatched + 1 : methodMatched;
                                produceMatched = pMatched ? produceMatched + 1 : produceMatched;
                                consumeMatched = cMatched ? consumeMatched + 1 : consumeMatched;
                            }
                        }
                    }
                }
            }
            if (!candidateList.isEmpty() && !subresourcesOnly) {
                break;
            }
        }
        if (!candidateList.isEmpty()) {
            Map.Entry<OperationResourceInfo, MultivaluedMap<String, String>> firstEntry = 
                candidateList.entrySet().iterator().next();
            values.clear();
            values.putAll(firstEntry.getValue());
            OperationResourceInfo ori = firstEntry.getKey();
            if (headMethodPossible(ori.getHttpMethod(), httpMethod)) {
                LOG.info(new org.apache.cxf.common.i18n.Message("GET_INSTEAD_OF_HEAD", 
                         BUNDLE, resource.getServiceClass().getName(), 
                         ori.getMethodToInvoke().getName()).toString());
            }
            return ori;
        }
        
        int status = pathMatched == 0 ? 404 : methodMatched == 0 ? 405 
                     : consumeMatched == 0 ? 415 : produceMatched == 0 ? 406 : 404;
        String name = resource.isRoot() ? "NO_OP_EXC" : "NO_SUBRESOURCE_METHOD_FOUND";
        org.apache.cxf.common.i18n.Message errorMsg = 
            new org.apache.cxf.common.i18n.Message(name, 
                                                   BUNDLE, 
                                                   path,
                                                   requestType.toString(),
                                                   convertTypesToString(acceptContentTypes));
        if (!"OPTIONS".equalsIgnoreCase(httpMethod) && logNow) {
            LOG.warning(errorMsg.toString());
        }
        ResponseBuilder rb = createResponseBuilder(resource, status, methodMatched == 0);
        throw new WebApplicationException(rb.build());
        
    }    

    public static ResponseBuilder createResponseBuilder(ClassResourceInfo cri, int status, boolean addAllow) {
        ResponseBuilder rb = Response.status(status);
        if (addAllow) {
            for (String m : cri.getAllowedMethods()) {
                rb.header("Allow", m);
            }
        }
        return rb;
    }
    
    private static boolean matchHttpMethod(String expectedMethod, String httpMethod) {
        if (expectedMethod.equalsIgnoreCase(httpMethod) 
            || headMethodPossible(expectedMethod, httpMethod)) {
            return true;
        }
        return false;
    }
    
    public static boolean headMethodPossible(String expectedMethod, String httpMethod) {
        return "HEAD".equalsIgnoreCase(httpMethod) && "GET".equals(expectedMethod);        
    }
    
    private static String convertTypesToString(List<MediaType> types) {
        StringBuilder sb = new StringBuilder();
        for (MediaType type : types) {
            sb.append(type.toString()).append(',');
        }
        return sb.toString();
    }
    
    public static List<MediaType> getConsumeTypes(Consumes cm) {
        return cm == null ? Collections.singletonList(ALL_TYPES)
                          : getMediaTypes(cm.value());
    }
    
    public static List<MediaType> getProduceTypes(Produces pm) {
        return pm == null ? Collections.singletonList(ALL_TYPES)
                          : getMediaTypes(pm.value());
    }
    
    public static int compareSortedMediaTypes(List<MediaType> mts1, List<MediaType> mts2) {
        int size1 = mts1.size();
        int size2 = mts2.size();
        for (int i = 0; i < size1 && i < size2; i++) {
            int result = compareMediaTypes(mts1.get(i), mts2.get(i));
            if (result != 0) {
                return result;
            }
        }
        return size1 == size2 ? 0 : size1 < size2 ? -1 : 1;
    }
    
    public static int compareMediaTypes(MediaType mt1, MediaType mt2) {
        
        if (mt1.isWildcardType() && !mt2.isWildcardType()) {
            return 1;
        }
        if (!mt1.isWildcardType() && mt2.isWildcardType()) {
            return -1;
        }
         
        
        if (mt1.isWildcardSubtype() && !mt2.isWildcardSubtype()) {
            return 1;
        }
        if (!mt1.isWildcardSubtype() && mt2.isWildcardSubtype()) {
            return -1;
        }       
        
        return compareMediaTypesQualityFactors(mt1, mt2);
    }
    
    public static int compareMediaTypesQualityFactors(MediaType mt1, MediaType mt2) {
        float q1 = getMediaTypeQualityFactor(mt1.getParameters().get("q"));
        float q2 = getMediaTypeQualityFactor(mt2.getParameters().get("q"));
        return Float.compare(q1, q2) * -1;
    }
    

    public static float getMediaTypeQualityFactor(String q) {
        if (q == null) {
            return 1;
        }
        if (q.charAt(0) == '.') {
            q = '0' + q;
        }
        try {
            return Float.parseFloat(q);
        } catch (NumberFormatException ex) {
            // default value will do
        }
        return 1;
    }
    
    //Message contains following information: PATH, HTTP_REQUEST_METHOD, CONTENT_TYPE, InputStream.
    public static List<Object> processParameters(OperationResourceInfo ori, 
                                                 MultivaluedMap<String, String> values, 
                                                 Message message) {
        
        
        Method method = ori.getMethodToInvoke();
        Class[] parameterTypes = method.getParameterTypes();
        Parameter[] paramsInfo = ori.getParameters().toArray(new Parameter[]{});  
        Method annotatedMethod = ori.getAnnotatedMethod();
        Type[] genericParameterTypes = annotatedMethod == null ? method.getGenericParameterTypes() 
                                      : annotatedMethod.getGenericParameterTypes();
        Annotation[][] anns = annotatedMethod == null ? null : annotatedMethod.getParameterAnnotations();
        List<Object> params = new ArrayList<Object>(parameterTypes.length);

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> param = parameterTypes[i]; 
            Type genericParam = genericParameterTypes[i];
            if (param == Object.class && genericParam instanceof TypeVariable) {
                genericParam = InjectionUtils.getSuperType(ori.getClassResourceInfo().getServiceClass(), 
                                                           (TypeVariable)genericParam);
                param = (Class)genericParam; 
            }
            
            Object paramValue = processParameter(param, 
                                                 genericParam,
                                                 anns == null ? new Annotation[0] : anns[i],
                                                 paramsInfo[i], 
                                                 values, 
                                                 message,
                                                 ori);
            params.add(paramValue);
        }

        return params;
    }

    private static Object processParameter(Class<?> parameterClass, 
                                           Type parameterType,
                                           Annotation[] parameterAnns,
                                           Parameter parameter, 
                                           MultivaluedMap<String, String> values,
                                           Message message,
                                           OperationResourceInfo ori) {
        InputStream is = message.getContent(InputStream.class);

        if (parameter.getType() == ParameterType.REQUEST_BODY) {
            
            String contentType = (String)message.get(Message.CONTENT_TYPE);

            if (contentType == null) {
                org.apache.cxf.common.i18n.Message errorMsg = 
                    new org.apache.cxf.common.i18n.Message("NO_CONTENT_TYPE_SPECIFIED", 
                                                           BUNDLE, 
                                                           ori.getHttpMethod());
                LOG.fine(errorMsg.toString());
                contentType = MediaType.WILDCARD;
            }

            return readFromMessageBody(parameterClass,
                                       parameterType,
                                       parameterAnns,
                                       is, 
                                       MediaType.valueOf(contentType),
                                       ori.getConsumeTypes(),
                                       message);
        } else if (parameter.getType() == ParameterType.CONTEXT) {
            return createContextValue(message, parameterType, parameterClass);
        } else {
            
            return createHttpParameterValue(parameter,
                                            parameterClass,
                                            parameterType,
                                            message,
                                            values,
                                            ori);
        }
    }
    
    public static Object createHttpParameterValue(Parameter parameter, 
                                            Class<?> parameterClass, 
                                            Type genericParam,
                                            Message message,
                                            MultivaluedMap<String, String> values,
                                            OperationResourceInfo ori) {
       
        boolean isEncoded = parameter.isEncoded() || ori != null && ori.isEncodedEnabled();
        String defaultValue = parameter.getDefaultValue();
        if (defaultValue == null && ori != null) {
            defaultValue = ori.getDefaultParameterValue();
        }
        
        Object result = null;
        
        if (parameter.getType() == ParameterType.PATH) {
            result = readFromUriParam(message, parameter.getName(), parameterClass, genericParam, 
                                      values, defaultValue, !isEncoded);
        } 
        
        if (parameter.getType() == ParameterType.QUERY) {
            result = readQueryString(parameter.getName(), parameterClass, genericParam, message, 
                                   defaultValue, !isEncoded);
        }
        
        if (parameter.getType() == ParameterType.MATRIX) {
            result = processMatrixParam(message, parameter.getName(), parameterClass, genericParam, 
                                      defaultValue, !isEncoded);
        }
        
        if (parameter.getType() == ParameterType.FORM) {
            result = processFormParam(message, parameter.getName(), parameterClass, genericParam, 
                                      defaultValue, !isEncoded);
        }
        
        if (parameter.getType() == ParameterType.COOKIE) {
            result = processCookieParam(message, parameter.getName(), parameterClass, genericParam,
                                        defaultValue);
        } 
        
        if (parameter.getType() == ParameterType.HEADER) {
            result = processHeaderParam(message, parameter.getName(), parameterClass, genericParam, 
                                        defaultValue);
        } 

        return result;
    }
    
    private static Object processMatrixParam(Message m, String key, 
                                             Class<?> pClass, Type genericType,
                                             String defaultValue,
                                             boolean decode) {
        List<PathSegment> segments = JAXRSUtils.getPathSegments(
                                      (String)m.get(Message.REQUEST_URI), decode);
        if (segments.size() > 0) {
            MultivaluedMap<String, String> params = new MetadataMap<String, String>(); 
            for (PathSegment ps : segments) {
                MultivaluedMap<String, String> matrix = ps.getMatrixParameters();
                for (Map.Entry<String, List<String>> entry : matrix.entrySet()) {
                    for (String value : entry.getValue()) {                    
                        params.add(entry.getKey(), value);
                    }
                }
            }
            
            if ("".equals(key)) {
                return InjectionUtils.handleBean(pClass, params, ParameterType.MATRIX, m, false);
            } else {
                List<String> values = params.get(key);
                return InjectionUtils.createParameterObject(values, 
                                                            pClass, 
                                                            genericType,
                                                            defaultValue,
                                                            false,
                                                            ParameterType.MATRIX,
                                                            m);
            }
        }
        
        return null;
    }
    
    private static Object processFormParam(Message m, String key, 
                                           Class<?> pClass, Type genericType,
                                           String defaultValue,
                                           boolean decode) {
        
        MessageContext mc = new MessageContextImpl(m);
        MediaType mt = mc.getHttpHeaders().getMediaType();
        
        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> params = (MultivaluedMap<String, String>)m.get(FORM_PARAM_MAP); 
        
        if (params == null) {
            params = new MetadataMap<String, String>();
            m.put(FORM_PARAM_MAP, params);
        
            if (mt == null || mt.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE)) {
                String body = (String)m.get("org.apache.cxf.jaxrs.provider.form.body");
                if (body == null) {
                    body = FormUtils.readBody(m.getContent(InputStream.class));
                    m.put("org.apache.cxf.jaxrs.provider.form.body", body);
                }
                HttpServletRequest request = (HttpServletRequest)m.get(AbstractHTTPDestination.HTTP_REQUEST);
                FormUtils.populateMapFromString(params, (String)body, decode, request);
            } else {
                MultipartBody body = AttachmentUtils.getMultipartBody(mc);
                FormUtils.populateMapFromMultipart(params, body, decode);
            }
        }
        
        if ("".equals(key)) {
            return InjectionUtils.handleBean(pClass, params, ParameterType.FORM, m, false);
        } else {
            List<String> results = params.get(key);
    
            return InjectionUtils.createParameterObject(results, 
                                                        pClass, 
                                                        genericType,
                                                        defaultValue,
                                                        false,
                                                        ParameterType.FORM,
                                                        m);
             
        }
    }
    
    
    public static MultivaluedMap<String, String> getMatrixParams(String path, boolean decode) {
        int index = path.indexOf(';');
        return index == -1 ? new MetadataMap<String, String>()
                           : JAXRSUtils.getStructuredParams(path.substring(index + 1), ";", decode);
    }
    
    private static Object processHeaderParam(Message m, 
                                             String header, 
                                             Class<?> pClass, 
                                             Type genericType, 
                                             String defaultValue) {
        
        List<String> values = new HttpHeadersImpl(m).getRequestHeader(header);
        if (values != null && values.isEmpty()) {
            values = null;
        }
        return InjectionUtils.createParameterObject(values, 
                                                    pClass, 
                                                    genericType,
                                                    defaultValue,
                                                    false,
                                                    ParameterType.HEADER,
                                                    m);
             
        
    }

    private static Object processCookieParam(Message m, String cookieName, 
                              Class<?> pClass, Type genericType, String defaultValue) {
        List<String> values = new HttpHeadersImpl(m).getRequestHeader(HttpHeaders.COOKIE);
        String value = values.size() == 1 && values.get(0).contains(cookieName + '=') 
                       ? values.get(0) : defaultValue != null ? cookieName + '=' + defaultValue : null;
        
        if (value == null) {
            return null;
        }
        
        Cookie c = Cookie.valueOf(value);
        if (pClass.isAssignableFrom(Cookie.class)) {
            return c;
        }
        
        return InjectionUtils.handleParameter(c.getValue(), pClass, ParameterType.COOKIE, m);
    }
    
    public static <T> T createContextValue(Message m, Type genericType, Class<T> clazz) {
 
        Message contextMessage = m.getExchange() != null ? m.getExchange().getInMessage() : m;
        if (contextMessage == null && Boolean.FALSE.equals(m.get(Message.INBOUND_MESSAGE))) {
            contextMessage = m;
        }
        Object o = null;
        if (UriInfo.class.isAssignableFrom(clazz)) {
            o = createUriInfo(contextMessage);
        } else if (HttpHeaders.class.isAssignableFrom(clazz)) {
            o = new HttpHeadersImpl(contextMessage);
        } else if (Request.class.isAssignableFrom(clazz)) {
            o = new RequestImpl(contextMessage);
        } else if (SecurityContext.class.isAssignableFrom(clazz)) {
            o = new SecurityContextImpl(contextMessage);
        } else if (Providers.class.isAssignableFrom(clazz)) {
            o = new ProvidersImpl(contextMessage);
        } else if (ContextResolver.class.isAssignableFrom(clazz)) {
            o = createContextResolver(genericType, contextMessage);
        } else if (MessageContext.class.isAssignableFrom(clazz)) {
            o = new MessageContextImpl(m);
        }
        
        o = o == null ? createServletResourceValue(contextMessage, clazz) : o;
        return clazz.cast(o);
    }
    
    @SuppressWarnings("unchecked")
    private static UriInfo createUriInfo(Message m) {
        if (MessageUtils.isRequestor(m)) {
            m = m.getExchange() != null ? m.getExchange().getOutMessage() : m;
        }
        MultivaluedMap<String, String> templateParams =
            (MultivaluedMap<String, String>)m.get(URITemplate.TEMPLATE_PARAMETERS);
        return new UriInfoImpl(m, templateParams);
    }
    
    public static ContextResolver<?> createContextResolver(Type genericType, Message m) {
        if (genericType instanceof ParameterizedType) {
            return ProviderFactory.getInstance(m).createContextResolver(
                      ((ParameterizedType)genericType).getActualTypeArguments()[0], m);
        }
        return null;
    }

    public static Object createResourceValue(Message m, Type genericType, Class<?> clazz) {
                
        // lets assume we're aware of servlet types only that can be @Resource-annotated
        return createContextValue(m, genericType, clazz);
    }
    
    public static <T> T createServletResourceValue(Message m, Class<T> clazz) {
        
        Object value = null; 
        if (HttpServletRequest.class.isAssignableFrom(clazz)) {
            value = m.get(AbstractHTTPDestination.HTTP_REQUEST);
        }
        if (HttpServletResponse.class.isAssignableFrom(clazz)) {
            value = new HttpServletResponseFilter(
                            (HttpServletResponse)m.get(AbstractHTTPDestination.HTTP_RESPONSE), m);
        }
        if (ServletContext.class.isAssignableFrom(clazz)) {
            value = m.get(AbstractHTTPDestination.HTTP_CONTEXT);
        }
        if (ServletConfig.class.isAssignableFrom(clazz)) {
            value = m.get(AbstractHTTPDestination.HTTP_CONFIG);
        }
        
        return clazz.cast(value);
    }

    private static Object readFromUriParam(Message m,
                                           String parameterName,
                                           Class<?> paramType,
                                           Type genericType,
                                           MultivaluedMap<String, String> values,
                                           String defaultValue,
                                           boolean decoded) {
        
        if ("".equals(parameterName)) {
            return InjectionUtils.handleBean(paramType, values, ParameterType.PATH, m, decoded);
        } else {
            List<String> results = values.get(parameterName);
            return InjectionUtils.createParameterObject(results, 
                                                    paramType, 
                                                    genericType,
                                                    defaultValue,
                                                    decoded,
                                                    ParameterType.PATH,
                                                    m);
        }
    }
    
    
    
    //TODO : multiple query string parsing, do it once
    private static Object readQueryString(String queryName,
                                          Class<?> paramType,
                                          Type genericType,
                                          Message m, 
                                          String defaultValue,
                                          boolean decode) {
        if ("".equals(queryName)) {
            return InjectionUtils.handleBean(paramType, new UriInfoImpl(m, null).getQueryParameters(),
                                             ParameterType.QUERY, m, decode);
        } else {
            List<String> results = getStructuredParams((String)m.get(Message.QUERY_STRING),
                                       "&",
                                       decode).get(queryName);
    
            return InjectionUtils.createParameterObject(results, 
                                                        paramType, 
                                                        genericType,
                                                        defaultValue,
                                                        false,
                                                        ParameterType.QUERY, m);
             
        }
    }

    
    
    /**
     * Retrieve map of query parameters from the passed in message
     * @param message
     * @return a Map of query parameters.
     */
    public static MultivaluedMap<String, String> getStructuredParams(String query, 
                                                                    String sep, 
                                                                    boolean decode) {
        MultivaluedMap<String, String> queries = 
            new MetadataMap<String, String>(new LinkedHashMap<String, List<String>>());
        
        if (!StringUtils.isEmpty(query)) {            
            List<String> parts = Arrays.asList(query.split(sep));
            for (String part : parts) {
                String[] values = part.split("=");
                String value = null;
                if (values.length == 1) {
                    value = "";
                } else if (decode) {
                    value = "&".equals(sep) ? HttpUtils.urlDecode(values[1]) 
                                            : HttpUtils.pathDecode(values[1]); 
                } else {
                    value = values[1];
                }
                queries.add(values[0], value);
            }
        }
        return queries;
    }

    @SuppressWarnings("unchecked")
    private static <T> Object readFromMessageBody(Class<T> targetTypeClass,
                                                  Type parameterType,
                                                  Annotation[] parameterAnnotations,
                                                  InputStream is, 
                                                  MediaType contentType, 
                                                  List<MediaType> consumeTypes,
                                                  Message m) {
        
        List<MediaType> types = JAXRSUtils.intersectMimeTypes(consumeTypes, contentType);
        
        MessageBodyReader provider = null;
        for (MediaType type : types) { 
            provider = ProviderFactory.getInstance(m)
                .createMessageBodyReader(targetTypeClass,
                                         parameterType,
                                         parameterAnnotations,
                                         type,
                                         m);
            if (provider != null) {
                try {
                    HttpHeaders headers = new HttpHeadersImpl(m);
                    return provider.readFrom(
                              targetTypeClass, parameterType, parameterAnnotations, contentType,
                              headers.getRequestHeaders(), is);
                } catch (IOException e) {
                    String errorMessage = "Error deserializing input stream into target class "
                                          + targetTypeClass.getSimpleName() 
                                           + ", content type : " + contentType;
                    LOG.severe(errorMessage);
                    throw new WebApplicationException(e);
                } catch (WebApplicationException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new WebApplicationException(ex);
                }
            } else {
                String errorMessage = new org.apache.cxf.common.i18n.Message("NO_MSG_READER",
                                                       BUNDLE,
                                                       targetTypeClass.getSimpleName(),
                                                       contentType).toString();
                LOG.warning(errorMessage);
                throw new WebApplicationException(Response.Status.UNSUPPORTED_MEDIA_TYPE);
            }
        }

        return null;
    }

    

    public static boolean matchConsumeTypes(MediaType requestContentType, 
                                            OperationResourceInfo ori) {
        
        return intersectMimeTypes(ori.getConsumeTypes(), requestContentType).size() != 0;
    }
    
    public static boolean matchProduceTypes(MediaType acceptContentType, 
                                            OperationResourceInfo ori) {
        
        return intersectMimeTypes(ori.getProduceTypes(), acceptContentType).size() != 0;
    }
    
    public static boolean matchMimeTypes(MediaType requestContentType, 
                                         MediaType acceptContentType, 
                                         OperationResourceInfo ori) {
        
        if (intersectMimeTypes(ori.getConsumeTypes(), requestContentType).size() != 0
            && intersectMimeTypes(ori.getProduceTypes(), acceptContentType).size() != 0) {
            return true;
        }
        return false;
    }

    public static List<MediaType> parseMediaTypes(String types) {
        List<MediaType> acceptValues = new ArrayList<MediaType>();
        
        if (types != null) {
            while (types.length() > 0) {
                String tp = types;
                int index = types.indexOf(',');
                if (index != -1) {
                    tp = types.substring(0, index);
                    types = types.substring(index + 1).trim();
                } else {
                    types = "";
                }
                acceptValues.add(MediaType.valueOf(tp));
            }
        } else {
            acceptValues.add(ALL_TYPES);
        }
        
        return acceptValues;
    }
    
    /**
     * intersect two mime types
     * 
     * @param mimeTypesA 
     * @param mimeTypesB 
     * @return return a list of intersected mime types
     */   
    public static List<MediaType> intersectMimeTypes(List<MediaType> requiredMediaTypes, 
                                                     List<MediaType> userMediaTypes) {
        Set<MediaType> supportedMimeTypeList = new LinkedHashSet<MediaType>();

        for (MediaType requiredType : requiredMediaTypes) {
            for (MediaType userType : userMediaTypes) {
                boolean isCompatible = 
                    requiredType.isCompatible(userType) || userType.isCompatible(requiredType);
                if (!isCompatible && requiredType.getType().equalsIgnoreCase(userType.getType())) {
                    // check if we have composite subtypes
                    String[] subtypes1 = requiredType.getSubtype().split("\\+");
                    String[] subtypes2 = userType.getSubtype().split("\\+");
                    if (subtypes1.length == 2 && subtypes2.length == 2
                        && subtypes1[1].equalsIgnoreCase(subtypes2[1])
                        && (subtypes1[0].equals("*") || subtypes2[0].equals("*"))) {
                        isCompatible = true;
                    }
                }
                if (isCompatible) {
                    boolean parametersMatched = true;
                    for (Map.Entry<String, String> entry : userType.getParameters().entrySet()) {
                        String value = requiredType.getParameters().get(entry.getKey());
                        if (value != null && !value.equals(entry.getValue())) {
                            parametersMatched = false;
                            break;
                        }
                    }
                    if (!parametersMatched) {
                        continue;
                    }
                    
                    String type = requiredType.getType().equals(MediaType.MEDIA_TYPE_WILDCARD) 
                                      ? userType.getType() : requiredType.getType();
                    String subtype = requiredType.getSubtype().equals(MediaType.MEDIA_TYPE_WILDCARD) 
                                      ? userType.getSubtype() : requiredType.getSubtype();                  
                    supportedMimeTypeList.add(new MediaType(type, subtype, userType.getParameters()));
                }
            }
        }

        return new ArrayList<MediaType>(supportedMimeTypeList);
        
    }
    
    public static List<MediaType> intersectMimeTypes(List<MediaType> mimeTypesA, 
                                                     MediaType mimeTypeB) {
        return intersectMimeTypes(mimeTypesA, 
                                  Collections.singletonList(mimeTypeB));
    }
    
    public static List<MediaType> intersectMimeTypes(String mimeTypesA, 
                                                     String mimeTypesB) {
        return intersectMimeTypes(parseMediaTypes(mimeTypesA),
                                  parseMediaTypes(mimeTypesB));
    }
    
    public static List<MediaType> sortMediaTypes(String mediaTypes) {
        return sortMediaTypes(JAXRSUtils.parseMediaTypes(mediaTypes));
    }
    
    public static List<MediaType> sortMediaTypes(List<MediaType> types) {
        if (types.size() > 1) {
            Collections.sort(types, new Comparator<MediaType>() {

                public int compare(MediaType mt1, MediaType mt2) {
                    return JAXRSUtils.compareMediaTypes(mt1, mt2);
                }
                
            });
        }
        return types;
    }
    
    @SuppressWarnings("unchecked")
    public static Response convertFaultToResponse(Throwable ex, Message inMessage) {
        
        ExceptionMapper mapper = 
            ProviderFactory.getInstance(inMessage).createExceptionMapper(ex.getClass(), inMessage);
        if (mapper != null) {
            try {
                return mapper.toResponse(ex);
            } catch (Exception mapperEx) {
                mapperEx.printStackTrace();
                return Response.serverError().build();
            }
        }
        
        return null;
        
    }
    
    public static String removeMediaTypeParameter(MediaType mt, String paramName) {
        StringBuilder sb = new StringBuilder();
        sb.append(mt.getType()).append('/').append(mt.getSubtype());
        if (mt.getParameters().size() > 1) {
            for (String key : mt.getParameters().keySet()) {
                if (!paramName.equals(key)) {
                    sb.append(';').append(key).append('=').append(mt.getParameters().get(key));
                }
            }
        }    
        return sb.toString();
    }
        
    public static boolean propogateException(Message m) {
        
        Object value = m.getContextualProperty(PROPOGATE_EXCEPTION);
        
        if (value == null) {
            return true;
        }

        if (Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(value.toString())) {
            return true;
        }
        
        return false;
    }
    
    public static QName getClassQName(Class<?> type) {
        String nsURI = PackageUtils.getNamespace(PackageUtils.getPackageName(type));
        if (nsURI.endsWith("/")) {
            nsURI = nsURI.substring(0, nsURI.length() - 1);
        }
        return new QName(nsURI, type.getSimpleName(), "ns1"); 
    }
    
}
