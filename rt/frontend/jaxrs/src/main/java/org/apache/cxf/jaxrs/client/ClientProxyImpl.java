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
package org.apache.cxf.jaxrs.client;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ParameterType;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;

public class ClientProxyImpl extends AbstractClient implements InvocationHandler {

    private ClassResourceInfo cri;
    private boolean inheritHeaders;
    
    public ClientProxyImpl(URI baseURI, URI currentURI, ClassResourceInfo cri, boolean inheritHeaders) {
        super(baseURI, currentURI);
        this.cri = cri;
        this.inheritHeaders = inheritHeaders;
    }
    
    public Object invoke(Object o, Method m, Object[] params) throws Throwable {
        
        resetResponse();
        
        Class<?> declaringClass = m.getDeclaringClass();
        if (Client.class == declaringClass) {
            return m.invoke(this, params);
        }
        
        OperationResourceInfo ori = cri.getMethodDispatcher().getOperationResourceInfo(m);
        if (ori == null) {
            throw new WebApplicationException(400);
        }
        
        
        MultivaluedMap<ParameterType, Parameter> types = 
            getParametersInfo(ori, m, params);
        List<Object> pathParams = getParamValues(types, params, ParameterType.PATH);
        
        int bodyIndex = getBodyIndex(types, ori.isSubResourceLocator());
        
        UriBuilder builder = getCurrentBuilder().clone(); 
        if (cri.isRoot()) {
            builder.path(ori.getClassResourceInfo().getServiceClass());
        }
        builder.path(m);
        handleMatrixes(types, params, builder);
        handleQueries(types, params, builder);
        
        URI uri = builder.build(pathParams.toArray()).normalize();
        
        MultivaluedMap<String, String> headers = getHeaders();
        MultivaluedMap<String, String> paramHeaders = new MetadataMap<String, String>();
        handleHeaders(paramHeaders, types, params);
        handleCookies(paramHeaders, types, params);
                
        if (ori.isSubResourceLocator()) {
            ClassResourceInfo subCri = cri.getSubResource(m.getReturnType(), m.getReturnType());
            if (subCri == null) {
                throw new WebApplicationException();
            }
            ClientProxyImpl proxyImpl = new ClientProxyImpl(getBaseURI(), uri, subCri, inheritHeaders);
            proxyImpl.setBus(bus);
            proxyImpl.setConduitSelector(conduitSelector);
            proxyImpl.setInInterceptors(inInterceptors);
            proxyImpl.setOutInterceptors(outInterceptors);
            
            Object proxy = JAXRSClientFactory.create(m.getReturnType(), proxyImpl);
            if (inheritHeaders) {
                WebClient.client(proxy).headers(headers);
            }
            WebClient.client(proxy).headers(paramHeaders);
            return proxy;
        } 
        
        headers.putAll(paramHeaders);
        setRequestHeaders(headers, ori, types.containsKey(ParameterType.FORM), 
            bodyIndex == -1 ? null : params[bodyIndex].getClass(), m.getReturnType());
        
        if (conduitSelector == null) {
            return doDirectInvocation(uri, headers, ori, params, bodyIndex, types);
        } else {
            return doChainedInvocation(uri, headers, ori, params, bodyIndex, types);
        }
    }

    private static MultivaluedMap<ParameterType, Parameter> getParametersInfo(OperationResourceInfo ori, 
                                                                              Method m, Object[] params) {
        MultivaluedMap<ParameterType, Parameter> map = new MetadataMap<ParameterType, Parameter>();
        Annotation[][] paramAnns = m.getParameterAnnotations();
        if (paramAnns.length == 0) {
            return map;
        }
        for (int i = 0; i < paramAnns.length; i++) {
            Parameter p = getParameter(i, paramAnns[i], ori);
            map.add(p.getType(), p);
        }
        if (map.containsKey(ParameterType.REQUEST_BODY)) {
            if (map.get(ParameterType.REQUEST_BODY).size() > 1) {
                throw new WebApplicationException();
            }
            if (map.containsKey(ParameterType.FORM)) {
                throw new WebApplicationException();
            }
        }
        return map;
    }
    
    private static int getBodyIndex(MultivaluedMap<ParameterType, Parameter> map, boolean subresource) {
        List<Parameter> list = map.get(ParameterType.REQUEST_BODY);
        int index  = list == null ? -1 : list.get(0).getIndex(); 
        if (subresource && index != -1) {
            throw new WebApplicationException();
        }
        return index;
    }
    
    private static void checkResponse(Method m, Response r) throws Throwable {
        
        int status = r.getStatus();
        
        if (status >= 400) {
            
            ProviderFactory pf = ProviderFactory.getInstance();
            for (Class<?> exType : m.getExceptionTypes()) {
                ResponseExceptionMapper<?> mapper = pf.createResponseExceptionMapper(exType);
                if (mapper != null) {
                    Throwable t = mapper.fromResponse(r);
                    if (t != null) {
                        throw t;
                    }
                }
            }
            
            throw new WebApplicationException(r);
        }
    }
    
    
    private MultivaluedMap<String, String> setRequestHeaders(MultivaluedMap<String, String> headers,          
                                                             OperationResourceInfo ori,
                                                             boolean formParams,
                                                             Class<?> bodyClass,
                                                             Class<?> responseClass) {
        if (headers.getFirst(HttpHeaders.CONTENT_TYPE) == null) {
            if (formParams || bodyClass != null && MultivaluedMap.class.isAssignableFrom(bodyClass)) {
                headers.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            } else {
                String cType = 
                    bodyClass != null && InjectionUtils.isPrimitive(bodyClass) 
                        ? MediaType.TEXT_PLAIN : ori.getConsumeTypes().isEmpty() 
                    || ori.getConsumeTypes().get(0).equals(WILDCARD) 
                    ? MediaType.APPLICATION_XML : ori.getConsumeTypes().get(0).toString();   
                headers.putSingle(HttpHeaders.CONTENT_TYPE, cType);
            }
        }
            
        
        List<MediaType> accepts = getAccept();
        if (accepts == null) {
            accepts = InjectionUtils.isPrimitive(responseClass) 
                ? Collections.singletonList(MediaType.TEXT_PLAIN_TYPE)
                : ori.getProduceTypes().size() == 0 
                || ori.getConsumeTypes().get(0).equals(WILDCARD) 
                ? Collections.singletonList(MediaType.APPLICATION_XML_TYPE) : ori.getProduceTypes();
            for (MediaType mt : accepts) {
                headers.add(HttpHeaders.ACCEPT, mt.toString());
            }
        }
            
        return headers;
    }
    
    private static List<Object> getParamValues(MultivaluedMap<ParameterType, Parameter> map, 
                                               Object[] params, ParameterType key) {
        List<Parameter> indexList =  getParameters(map, key);
        List<Object> list = new ArrayList<Object>(indexList.size());
        for (Parameter p : indexList) {
            list.add(JAXRSUtils.encode(p.isEncoded(), params[p.getIndex()].toString()));
        }
        return list;
    }
    
    @SuppressWarnings("unchecked")
    private static List<Parameter> getParameters(MultivaluedMap<ParameterType, Parameter> map, 
                                           ParameterType key) {
        return  map.get(key) == null ? Collections.EMPTY_LIST : map.get(key);
    }
    
    private static void handleQueries(MultivaluedMap<ParameterType, Parameter> map, 
                                      Object[] params,
                                      UriBuilder ub) {
        List<Parameter> qs = getParameters(map, ParameterType.QUERY);
        for (Parameter p : qs) {
            if (params[p.getIndex()] != null) {
                ub.queryParam(p.getValue(), 
                               JAXRSUtils.encode(p.isEncoded(), params[p.getIndex()].toString()));
            }
        }
    }
    
    private static void handleMatrixes(MultivaluedMap<ParameterType, Parameter> map, Object[] params,
                                UriBuilder ub) {
        List<Parameter> mx = getParameters(map, ParameterType.MATRIX);
        for (Parameter p : mx) {
            if (params[p.getIndex()] != null) {
                ub.matrixParam(p.getValue(), 
                                JAXRSUtils.encode(p.isEncoded(), params[p.getIndex()].toString()));
            }
        }
    }

    private MultivaluedMap<String, String> handleForm(MultivaluedMap<ParameterType, Parameter> map, 
                                                      Object[] params) {
        
        MultivaluedMap<String, String> form = new MetadataMap<String, String>();
        
        List<Parameter> fm = getParameters(map, ParameterType.FORM);
        for (Parameter p : fm) {
            if (params[p.getIndex()] != null) {
                form.add(p.getValue(), params[p.getIndex()].toString());
            }
        }
        
        return form;
    }
    
    private void handleHeaders(MultivaluedMap<String, String> headers,
                               MultivaluedMap<ParameterType, Parameter> map, Object[] params) {
        List<Parameter> hs = getParameters(map, ParameterType.HEADER);
        for (Parameter p : hs) {
            if (params[p.getIndex()] != null) {
                headers.add(p.getValue(), params[p.getIndex()].toString());
            }
        }
    }
    
    private void handleCookies(MultivaluedMap<String, String> headers,
                               MultivaluedMap<ParameterType, Parameter> map, Object[] params) {
        List<Parameter> cs = getParameters(map, ParameterType.COOKIE);
        for (Parameter p : cs) {
            if (params[p.getIndex()] != null) {
                headers.add(HttpHeaders.COOKIE, p.getValue() + '=' + params[p.getIndex()].toString());
            }
        }
    }
    
    private static Parameter getParameter(int index, Annotation[] anns, OperationResourceInfo ori) {
        
        Context ctx = AnnotationUtils.getAnnotation(anns, Context.class);
        if (ctx != null) {
            throw new WebApplicationException();
        }
        
        boolean isEncoded = AnnotationUtils.isEncoded(anns, ori);
        PathParam a = AnnotationUtils.getAnnotation(anns, PathParam.class); 
        if (a != null) {
            return new Parameter(ParameterType.PATH, index, a.value(), isEncoded);
        } 
        
        QueryParam q = AnnotationUtils.getAnnotation(anns, QueryParam.class);
        if (q != null) {
            return new Parameter(ParameterType.QUERY, index, q.value(), isEncoded);
        }
        
        MatrixParam m = AnnotationUtils.getAnnotation(anns, MatrixParam.class);
        if (m != null) {
            return new Parameter(ParameterType.MATRIX, index, m.value(), isEncoded);
        }  
    
        HeaderParam h = AnnotationUtils.getAnnotation(anns, HeaderParam.class);
        if (h != null) {
            return new Parameter(ParameterType.HEADER, index, h.value(), isEncoded);
        }  
        
        Parameter p = null;
        CookieParam c = AnnotationUtils.getAnnotation(anns, CookieParam.class);
        if (c != null) {
            p = new Parameter(ParameterType.COOKIE, index, c.value(), isEncoded);
        } else {
            p = new Parameter(ParameterType.REQUEST_BODY, index, null, isEncoded); 
        }
        
        return p;
        
    }
    
    protected Object doDirectInvocation(URI uri, MultivaluedMap<String, String> headers, 
        OperationResourceInfo ori, Object[] params, int bodyIndex, 
        MultivaluedMap<ParameterType, Parameter> types) throws Throwable {

        // TODO : we need to refactor bits of HTTPConduit such that it can be reused
        
        HttpURLConnection connect = createHttpConnection(uri, ori.getHttpMethod());
        setAllHeaders(headers, connect);
        Method m = ori.getMethodToInvoke();
        if (bodyIndex != -1 || types.containsKey(ParameterType.FORM)) {
            if (bodyIndex != -1) {
                writeBody(params[bodyIndex], params[bodyIndex].getClass(), 
                          m.getGenericParameterTypes()[bodyIndex],
                          m.getParameterAnnotations()[bodyIndex], headers, connect.getOutputStream());
            } else {
                MultivaluedMap<String, String> form = handleForm(types, params);
                writeBody(form, form.getClass(), form.getClass(), m.getDeclaredAnnotations(),
                          headers, connect.getOutputStream());
            }
        }
        
        return handleResponse(connect, ori);
        
    }
    
    private Object doChainedInvocation(URI uri, MultivaluedMap<String, String> headers, 
                          OperationResourceInfo ori, Object[] params, int bodyIndex, 
                          MultivaluedMap<ParameterType, Parameter> types) throws Throwable {
        Message m = createMessage(ori.getHttpMethod(), headers, uri.toString());
        
        if (bodyIndex != -1 || types.containsKey(ParameterType.FORM)) {
            m.setContent(OperationResourceInfo.class, ori);
            m.put("BODY_INDEX", bodyIndex);
            Object body = bodyIndex != -1 ? params[bodyIndex] : handleForm(types, params); 
            MessageContentsList contents = new MessageContentsList(new Object[]{body});
            m.setContent(List.class, contents);
            m.getInterceptorChain().add(new BodyWriter());
        }
        
        // execute chain        
        try {
            m.getInterceptorChain().doIntercept(m);
        } catch (Throwable ex) {
            // we'd like a user to get the whole Response anyway if needed
        }
        
        // TODO : this needs to be done in an inbound chain instead
        HttpURLConnection connect = (HttpURLConnection)m.get(HTTPConduit.KEY_HTTP_CONNECTION);
        
        return handleResponse(connect, ori);
        
    }
    
    protected Object handleResponse(HttpURLConnection connect, OperationResourceInfo ori) 
        throws Throwable {
        Response r = setResponseBuilder(connect).clone().build();
        Method method = ori.getMethodToInvoke();
        checkResponse(method, r);
        if (method.getReturnType() == Void.class) { 
            return null;
        }
        
        return readBody(r, connect, method.getReturnType(), 
                        method.getGenericReturnType(), method.getDeclaredAnnotations());
    }
    
    private static class Parameter {
        private ParameterType type;
        private int ind;
        private String aValue;
        private boolean isEncoded;
        
        public Parameter(ParameterType type, int ind, String aValue, boolean encoded) {
            this.type = type;
            this.ind = ind;
            this.aValue = aValue; 
            this.isEncoded = encoded;
        }
        
        public int getIndex() {
            return ind;
        }
        
        public String getValue() {
            return aValue;
        }
        
        public ParameterType getType() {
            return type;
        }
        
        public boolean isEncoded() {
            return isEncoded;
        }
    }

    // TODO : what we really need to do is to refactor JAXRSOutInterceptor so that
    // it can handle both client requests and server responses - it may need to be split into
    // several interceptors - in fact we need to do the same for JAXRSInInterceptor so that we can do
    // on onMessage() properly
    
    private class BodyWriter extends AbstractOutDatabindingInterceptor {

        public BodyWriter() {
            super(Phase.WRITE);
        }
        
        @SuppressWarnings("unchecked")
        public void handleMessage(Message m) throws Fault {
            
            OperationResourceInfo ori = m.getContent(OperationResourceInfo.class);
            OutputStream os = m.getContent(OutputStream.class);
            if (os == null || ori == null) {
                return;
            }
            MessageContentsList objs = MessageContentsList.getContentsList(m);
            if (objs == null || objs.size() == 0) {
                return;
            }
            MultivaluedMap<String, String> headers = (MultivaluedMap)m.get(Message.PROTOCOL_HEADERS);
            Method method = ori.getMethodToInvoke();
            int bodyIndex = (Integer)m.get("BODY_INDEX");
            Object body = objs.get(0);
            try {
                if (bodyIndex != -1) {
                    writeBody(body, body.getClass(), 
                          method.getGenericParameterTypes()[bodyIndex],
                          method.getParameterAnnotations()[bodyIndex], headers, os);
                } else {
                    writeBody(body, body.getClass(), body.getClass(), method.getDeclaredAnnotations(),
                            headers, os);
                }
                os.flush();
            } catch (Exception ex) {
                throw new Fault(ex);
            }
            
        }
        
    }
}
