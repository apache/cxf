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

package org.apache.cxf.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.interceptor.JAXRSInInterceptor;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.HttpHeadersImpl;
import org.apache.cxf.jaxrs.provider.PathSegmentImpl;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.RequestImpl;
import org.apache.cxf.jaxrs.provider.SecurityContextImpl;
import org.apache.cxf.jaxrs.provider.UriInfoImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

public final class JAXRSUtils {

    public static final MediaType ALL_TYPES = new MediaType();
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSUtils.class);

    private JAXRSUtils() {        
    }
    
    public static String getHttpMethodValue(Method m) {
        for (Annotation a : m.getAnnotations()) {
            HttpMethod httpM = a.annotationType().getAnnotation(HttpMethod.class);
            if (httpM != null) {
                return httpM.value();
            }
        }
        // TODO : make it shorter
        for (Class<?> i : m.getDeclaringClass().getInterfaces()) {
            try {
                Method interfaceMethod = i.getMethod(m.getName(), m.getParameterTypes());
                if (interfaceMethod != null) {
                    return getHttpMethodValue(interfaceMethod);
                }
            } catch (NoSuchMethodException ex) {
                //ignore
            }
        }
        Class<?> superC = m.getDeclaringClass().getSuperclass();
        if (superC != null) {
            try {
                Method cMethod = superC.getMethod(m.getName(), m.getParameterTypes());
                if (cMethod != null) {
                    return getHttpMethodValue(cMethod);
                }
            } catch (NoSuchMethodException ex) {
                //ignore
            }
        }
        
        return null;
    }
    
    public static Annotation getMethodAnnotation(Method m,
                                                 Class<? extends Annotation> aClass) {
        Annotation a = m.getAnnotation(aClass);
        if (a != null) {
            return a;
        }
        
        for (Class<?> i : m.getDeclaringClass().getInterfaces()) {
            a = getClassMethodAnnotation(m, i, aClass);
            if (a != null) {
                return a;
            }
        }
        Class<?> superC = m.getDeclaringClass().getSuperclass();
        if (superC != null) {
            return getClassMethodAnnotation(m, superC, aClass);
        }
        
        return null;
    }
    
    private static Annotation getClassMethodAnnotation(Method m,
                                                       Class<?> c, 
                                                       Class<? extends Annotation> aClass) {
        try {
            Method interfaceMethod = c.getMethod(m.getName(), m.getParameterTypes());
            if (interfaceMethod != null) {
                return getMethodAnnotation(interfaceMethod, aClass);
            }
        } catch (NoSuchMethodException ex) {
            //ignore
        }
        return null;
    }
    
    public static Annotation getClassAnnotation(Class<?> c, 
                                                Class<? extends Annotation> aClass) {
        if (c == null) {
            return null;
        }
        Annotation p = c.getAnnotation(aClass);
        if (p != null) {
            return p;
        }
        return getClassAnnotation(c.getSuperclass(), aClass);
    }
    
    public static List<PathSegment> getPathSegments(String thePath, boolean decode) {
        String[] segments = thePath.split("/");
        List<PathSegment> theList = new ArrayList<PathSegment>();
        for (String path : segments) {
            if (!StringUtils.isEmpty(path)) {
                theList.add(new PathSegmentImpl(path, decode));
            }
        }
        return theList;
    }

    public static List<MediaType> getMediaTypes(String[] values) {
        List<MediaType> supportedMimeTypes = new ArrayList<MediaType>(values.length);
        for (int i = 0; i < values.length; i++) {
            supportedMimeTypes.add(MediaType.parse(values[i]));    
        }
        return supportedMimeTypes;
    }
    
    public static ClassResourceInfo findSubResourceClass(ClassResourceInfo resource,
                                                         Class subResourceClassType) {
        for (ClassResourceInfo subCri : resource.getSubClassResourceInfo()) {
            if (subCri.getResourceClass() == subResourceClassType) {
                return subCri;
            }
        }
        return null;
    }

    public static OperationResourceInfo findTargetResourceClass(List<ClassResourceInfo> resources,
                                                                String path, 
                                                                String httpMethod,
                                                                MultivaluedMap<String, String> values,
                                                                String requestContentType, 
                                                                String acceptContentTypes) {
        
        for (ClassResourceInfo resource : resources) {
            URITemplate uriTemplate = resource.getURITemplate();
            MultivaluedMap<String, String> map = new MetadataMap<String, String>();
            if (uriTemplate.match(path, map)) {
                String subResourcePath = map.getFirst(URITemplate.FINAL_MATCH_GROUP);
                OperationResourceInfo ori = findTargetMethod(resource, subResourcePath, httpMethod, map,
                                                             requestContentType, acceptContentTypes);
                if (ori != null) {
                    values.putAll(map);
                    return ori;
                }
            }
        }
        return null;
    }

    public static OperationResourceInfo findTargetMethod(ClassResourceInfo resource, 
                                                         String path,
                                                         String httpMethod, 
                                                         MultivaluedMap<String, String> values, 
                                                         String requestContentType, 
                                                         String acceptContentTypes) {
        SortedMap<OperationResourceInfo, MultivaluedMap<String, String>> candidateList = 
            new TreeMap<OperationResourceInfo, MultivaluedMap<String, String>>(
                new OperationResourceInfoComparator());
        MediaType requestType = requestContentType == null 
                                ? ALL_TYPES : MediaType.parse(requestContentType);
        List<MediaType> acceptTypes = JAXRSUtils.sortMediaTypes(acceptContentTypes);
       
        for (MediaType acceptType : acceptTypes) {
            for (OperationResourceInfo ori : resource.getMethodDispatcher().getOperationResourceInfos()) {
                
                URITemplate uriTemplate = ori.getURITemplate();
                MultivaluedMap<String, String> map = cloneMap(values);
                if (uriTemplate != null && uriTemplate.match(path, map)) {
                    if (ori.isSubResourceLocator() && matchMimeTypes(requestType, acceptType, ori)) {
                        candidateList.put(ori, map);
                    } else if (ori.getHttpMethod().equalsIgnoreCase(httpMethod)
                               && matchMimeTypes(requestType, acceptType, ori)) {
                        String finalGroup = map.getFirst(URITemplate.FINAL_MATCH_GROUP);
                        if (finalGroup == null || StringUtils.isEmpty(finalGroup)
                            || finalGroup.equals("/")) {
                            candidateList.put(ori, map);    
                        }
                    }
                }
            }
            if (!candidateList.isEmpty()) {
                Map.Entry<OperationResourceInfo, MultivaluedMap<String, String>> firstEntry = 
                    candidateList.entrySet().iterator().next();
                values.clear();
                values.putAll(firstEntry.getValue());
                return firstEntry.getKey();
            }
        }

        return null;
    }    

    public static List<MediaType> getConsumeTypes(ConsumeMime cm) {
        return cm == null ? Collections.singletonList(ALL_TYPES)
                          : getMediaTypes(cm.value());
    }
    
    public static List<MediaType> getProduceTypes(ProduceMime pm) {
        return pm == null ? Collections.singletonList(ALL_TYPES)
                          : getMediaTypes(pm.value());
    }
    
    private static class OperationResourceInfoComparator implements Comparator<OperationResourceInfo> {
        public int compare(OperationResourceInfo e1, OperationResourceInfo e2) {
            
            List<MediaType> mimeType1 = 
                getConsumeTypes(e1.getMethod().getAnnotation(ConsumeMime.class));
            List<MediaType> mimeType2 = 
                getConsumeTypes(e2.getMethod().getAnnotation(ConsumeMime.class));
            
            // TODO: we actually need to check all consume and produce types here ?
            int result = JAXRSUtils.compareMediaTypes(mimeType1.get(0), 
                                                      mimeType2.get(0));
            if (result == 0) {
                //use the media type of output data as the secondary key.
                List<MediaType> mimeTypeP1 = 
                    getProduceTypes(e1.getMethod().getAnnotation(ProduceMime.class));

                List<MediaType> mimeTypeP2 = 
                    getProduceTypes(e2.getMethod().getAnnotation(ProduceMime.class));    

                return JAXRSUtils.compareMediaTypes(mimeTypeP1.get(0), 
                                                    mimeTypeP2.get(0));
            } else {
                return result;
            }

        }
        
    }
    
    public static int compareMediaTypes(MediaType mt1, MediaType mt2) {
        
        if (mt1.equals(mt2)) {
            float q1 = getMediaTypeQualityFactor(mt1);
            float q2 = getMediaTypeQualityFactor(mt2);
            int result = Float.compare(q1, q2);
            return result == 0 ? result : ~result;
        }
        
        if (mt1.isWildcardType() && !mt2.isWildcardType()) {
            return 1;
        }
        if (!mt1.isWildcardType() && mt2.isWildcardType()) {
            return -1;
        }
         
        if (mt1.getType().equals(mt2.getType())) {
            if (mt1.isWildcardSubtype() && !mt2.isWildcardSubtype()) {
                return 1;
            }
            if (!mt1.isWildcardSubtype() && mt2.isWildcardSubtype()) {
                return -1;
            }       
        }
        return mt1.toString().compareTo(mt2.toString());
        
    }

    private static float getMediaTypeQualityFactor(MediaType mt) {
        String q = mt.getParameters().get("q");
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
        
        
        Method method = ori.getMethod();
        Class[] parameterTypes = method.getParameterTypes();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        List<Object> params = new ArrayList<Object>(parameterTypes.length);

        for (int i = 0; i < parameterTypes.length; i++) {
            Object param = processParameter(parameterTypes[i], 
                                            genericParameterTypes[i],
                                            parameterAnnotations[i], 
                                            values, 
                                            message,
                                            ori);
            params.add(param);
        }

        return params;
    }

    private static Object processParameter(Class<?> parameterClass, 
                                           Type parameterType,
                                           Annotation[] parameterAnnotations, 
                                           MultivaluedMap<String, String> values,
                                           Message message,
                                           OperationResourceInfo ori) {
        InputStream is = message.getContent(InputStream.class);

        String path = (String)message.get(JAXRSInInterceptor.RELATIVE_PATH);
        
        if ((parameterAnnotations == null || parameterAnnotations.length == 0)
            && ("PUT".equals(ori.getHttpMethod()) || "POST".equals(ori.getHttpMethod()))) {
            String contentType = (String)message.get(Message.CONTENT_TYPE);

            if (contentType == null) {
                throw new Fault(new  org.apache.cxf.common.i18n.Message("NO_CONTENT_TYPE_SPECIFIED",
                                                                        LOG, ori.getHttpMethod()));
            }

            return readFromMessageBody(parameterClass,
                                         is, 
                                         MediaType.parse(contentType),
                                         ori.getConsumeTypes());
        } else if (parameterAnnotations[0].annotationType() == Context.class
                   && ori.getClassResourceInfo().isRoot()) {
            return createHttpContextValue(message, parameterClass);
        } else if (parameterAnnotations[0].annotationType() == PathParam.class) {
            return readFromUriParam((PathParam)parameterAnnotations[0], parameterClass, parameterType,
                                      parameterAnnotations, path, values);
        }  
        
        Object result = null;

        // TODO : deal with @DefaultValues
        if (parameterAnnotations[0].annotationType() == QueryParam.class) {
            result = readQueryString((QueryParam)parameterAnnotations[0], parameterClass, message, null);
        } else if (parameterAnnotations[0].annotationType() == MatrixParam.class) {
            result = processMatrixParam(message, ((MatrixParam)parameterAnnotations[0]).value(), 
                                        parameterClass, null);
        } else if (parameterAnnotations[0].annotationType() == HeaderParam.class) {
            result = processHeaderParam(message, ((HeaderParam)parameterAnnotations[0]).value(),
                                        parameterClass, null);
        } 

        return result;
    }
    
    private static Object processMatrixParam(Message m, String key, 
                                             Class<?> pClass, String defaultValue) {
        List<PathSegment> segments = JAXRSUtils.getPathSegments(
                                      (String)m.get(Message.PATH_INFO), true);
        String value = null;
        if (segments.size() > 0) {
            MultivaluedMap<String, String> params = 
                segments.get(segments.size() - 1).getMatrixParameters();
            List<String> values = params.get(key);
            if (values != null && values.size() > 0) {
                value = values.get(0);
            }
        }
        
        return value == null ? defaultValue : handleParameter(value, pClass);
    }
    
    public static MultivaluedMap<String, String> getMatrixParams(String path, boolean decode) {
        int index = path.indexOf(';');
        return index == -1 ? new MetadataMap<String, String>()
                           : JAXRSUtils.getStructuredParams(path.substring(index + 1), ";", decode);
    }
    
    @SuppressWarnings("unchecked")
    private static Object processHeaderParam(Message m, String header, 
                                             Class<?> pClass, String defaultValue) {
        Map<String, List<String>> headers = (Map<String, List<String>>)m.get(Message.PROTOCOL_HEADERS);
        List<String> values = headers.get(header);
        StringBuilder sb = new StringBuilder();
        if (values != null) {
            for (Iterator<String> it = values.iterator(); it.hasNext();) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
        }
        return sb.length() > 0 ? handleParameter(sb.toString(), pClass) : defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    public static Object createHttpContextValue(Message m, Class<?> clazz) {
                
        if (UriInfo.class.isAssignableFrom(clazz)) {
            MultivaluedMap<String, String> templateParams =
                (MultivaluedMap<String, String>)m.get(URITemplate.TEMPLATE_PARAMETERS);
            return new UriInfoImpl(m, templateParams);
        }
        if (HttpHeaders.class.isAssignableFrom(clazz)) {
            return new HttpHeadersImpl(m);
        }
        if (Request.class.isAssignableFrom(clazz)) {
            return new RequestImpl(m);
        }
        if (SecurityContext.class.isAssignableFrom(clazz)) {
            return new SecurityContextImpl(m);
        }
        
        return null;
    }

    public static Object createServletResourceValue(Message m, Class<?> clazz) {
                
        if (HttpServletRequest.class.isAssignableFrom(clazz)) {
            return (HttpServletRequest) m.get(AbstractHTTPDestination.HTTP_REQUEST);
        }
        if (HttpServletResponse.class.isAssignableFrom(clazz)) {
            return (HttpServletResponse) m.get(AbstractHTTPDestination.HTTP_RESPONSE);
        }
        if (ServletContext.class.isAssignableFrom(clazz)) {
            return (ServletContext) m.get(AbstractHTTPDestination.HTTP_CONTEXT);
        }
        
        return null;
    }

    private static Object readFromUriParam(PathParam uriParamAnnotation,
                                           Class<?> parameter,
                                           Type parameterType,
                                           Annotation[] parameterAnnotations,
                                           String path,
                                           MultivaluedMap<String, String> values) {
        String parameterName = uriParamAnnotation.value();
        if (parameterName == null || parameterName.length() == 0) {
            // Invalid URI parameter name
            return null;
        }

        String result = null;
        List<String> results = values.get(parameterName);
        if (values != null && values.size() > 0) {
            result = results.get(results.size() - 1);
        }
        if (result != null) {
            return handleParameter(result, parameter);
        }
        return result;
    }
    
    private static Object handleParameter(String value, Class<?> pClass) {
        if (pClass.isPrimitive()) {
            return PrimitiveUtils.read(value, pClass);
        }
        // check constructors accepting a single String value
        try {
            Constructor<?> c = pClass.getConstructor(new Class<?>[]{String.class});
            if (c !=  null) {
                return c.newInstance(new Object[]{value});
            }
        } catch (Exception ex) {
            // try valueOf
        }
        // check for valueOf(String) static methods
        try {
            Method m = pClass.getMethod("valueOf", new Class<?>[]{String.class});
            if (m != null && Modifier.isStatic(m.getModifiers())) {
                return m.invoke(null, new Object[]{value});
            }
        } catch (Exception ex) {
            // no luck
        }
        return null;
    }
    
    //TODO : multiple query string parsing, do it once
    private static Object readQueryString(QueryParam queryParam, Class<?> parameter,
                                          Message m, String defaultValue) {
        String queryName = queryParam.value();

        String result = getStructuredParams((String)m.get(Message.QUERY_STRING),
                                   "&",
                                   true).getFirst(queryName);

        if (result != null) {
            return handleParameter(result, parameter);
        }
        return result;  
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
                queries.add(values[0], values.length == 1 ? "" 
                    : decode ? uriDecode(values[1]) : values[1]);
            }
        }
        return queries;
    }

    public static String uriDecode(String query) {
        try {
            query = URLDecoder.decode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //Swallow unsupported decoding exception          
        }
        return query;
    }

    @SuppressWarnings("unchecked")
    private static <T> Object readFromMessageBody(Class<T> targetTypeClass, InputStream is, 
                                                  MediaType contentType, List<MediaType> consumeTypes) {
        
        List<MediaType> types = JAXRSUtils.intersectMimeTypes(consumeTypes, contentType);
        
        MessageBodyReader provider = null;
        
        for (MediaType type : types) { 
            provider = ProviderFactory.getInstance()
                .createMessageBodyReader(targetTypeClass, type);
            // TODO : make the exceptions
            if (provider != null) {
                try {
                    return provider.readFrom(targetTypeClass, contentType, null, is);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Error deserializing input stream into target class "
                                               + targetTypeClass.getSimpleName() 
                                               + ", content type : " + contentType);
                }    
            } else {
                throw new RuntimeException("No message body reader found for target class "
                                           + targetTypeClass.getSimpleName() 
                                           + ", content type : " + contentType);
            }
        }

        return null;
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
                acceptValues.add(MediaType.parse(tp));
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
    public static List<MediaType> intersectMimeTypes(List<MediaType> mimeTypesA, 
                                                     List<MediaType> mimeTypesB) {
        Set<MediaType> supportedMimeTypeList = new LinkedHashSet<MediaType>();

        for (MediaType mimeTypeA : mimeTypesA) {
            for (MediaType mimeTypeB : mimeTypesB) {
                if (mimeTypeB.isCompatible(mimeTypeA) || mimeTypeA.isCompatible(mimeTypeB)) {
                    
                    String type = mimeTypeA.getType().equals(MediaType.MEDIA_TYPE_WILDCARD) 
                                      ? mimeTypeB.getType() : mimeTypeA.getType();
                    String subtype = mimeTypeA.getSubtype().equals(MediaType.MEDIA_TYPE_WILDCARD) 
                                      ? mimeTypeB.getSubtype() : mimeTypeA.getSubtype();                  
                    supportedMimeTypeList.add(new MediaType(type, subtype));
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
        List<MediaType> types = JAXRSUtils.parseMediaTypes(mediaTypes);
        if (types.size() > 1) {
            Collections.sort(types, new Comparator<MediaType>() {

                public int compare(MediaType mt1, MediaType mt2) {
                    return JAXRSUtils.compareMediaTypes(mt1, mt2);
                }
                
            });
        }
        return types;
    }
    
    public static void injectHttpContextValues(Object o,
                                               OperationResourceInfo ori,
                                               Message m) {
        
        for (Field f : ori.getClassResourceInfo().getHttpContexts()) {
            Object value = createHttpContextValue(m, f.getType());
            f.setAccessible(true);
            try {
                f.set(o, value);
            } catch (IllegalAccessException ex) {
                // ignore
            }
        }
    }
    
    public static void injectServletResourceValues(Object o,
                                               OperationResourceInfo ori,
                                               Message m) {
        
        for (Field f : ori.getClassResourceInfo().getResources()) {
            Object value = createServletResourceValue(m, f.getType());
            f.setAccessible(true);
            try {
                f.set(o, value);
            } catch (IllegalAccessException ex) {
                // ignore
            }
        }
    }

    private static <K, V> MultivaluedMap<K, V> cloneMap(MultivaluedMap<K, V> map1) {
        
        MultivaluedMap<K, V> map2 = new MetadataMap<K, V>();
        for (Map.Entry<K, List<V>> entry : map1.entrySet()) {
            map2.put(entry.getKey(), new ArrayList<V>(entry.getValue()));
        }
        return map2;
        
    }
}
