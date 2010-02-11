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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;

/**
 * Proxy-based client implementation
 *
 */
public class ClientProxyImpl extends AbstractClient implements InvocationHandlerAware, InvocationHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(ClientProxyImpl.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ClientProxyImpl.class);
    
    private ClassResourceInfo cri;
    private boolean inheritHeaders;
    private boolean isRoot;
    private Map<String, Object> valuesMap;
    
    public ClientProxyImpl(URI baseURI, ClassResourceInfo cri, boolean isRoot, 
                           boolean inheritHeaders, Object... varValues) {
        super(baseURI);
        this.cri = cri;
        this.isRoot = isRoot;
        this.inheritHeaders = inheritHeaders;
        initValuesMap(varValues);
    }
    
    public ClientProxyImpl(ClientState initialState, ClassResourceInfo cri, boolean isRoot, 
                           boolean inheritHeaders, Object... varValues) {
        super(initialState);
        this.cri = cri;
        this.isRoot = isRoot;
        this.inheritHeaders = inheritHeaders;
        initValuesMap(varValues);
    }
    
    private void initValuesMap(Object... varValues) {
        if (isRoot && varValues.length != 0) {
            valuesMap = new LinkedHashMap<String, Object>();
            List<String> vars = cri.getURITemplate().getVariables();
            for (int i = 0; i < vars.size(); i++) {
                if (i < varValues.length) {
                    valuesMap.put(vars.get(i), varValues[i]);
                } else {
                    org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                         "ROOT_VARS_MISMATCH", BUNDLE, vars.size(), varValues.length);
                    LOG.info(msg.toString());
                    break;
                }
            }
        } else {
            valuesMap = Collections.emptyMap();
        }
    }
    
    /**
     * Updates the current state if Client method is invoked, otherwise 
     * does the remote invocation or returns a new proxy if subresource 
     * method is invoked. Can throw an expected exception if ResponseExceptionMapper
     * is registered     
     */
    public Object invoke(Object o, Method m, Object[] params) throws Throwable {
        
        Class<?> declaringClass = m.getDeclaringClass();
        if (Client.class == declaringClass || InvocationHandlerAware.class == declaringClass
            || Object.class == declaringClass) {
            return m.invoke(this, params);
        }
        resetResponse();
        OperationResourceInfo ori = cri.getMethodDispatcher().getOperationResourceInfo(m);
        if (ori == null) {
            reportInvalidResourceMethod(m, "INVALID_RESOURCE_METHOD");
        }
        
        MultivaluedMap<ParameterType, Parameter> types = getParametersInfo(ori);
        List<Object> pathParams = getPathParamValues(types, params, ori);
        
        int bodyIndex = getBodyIndex(types, ori);
        
        UriBuilder builder = getCurrentBuilder().clone(); 
        if (isRoot) {
            builder.path(ori.getClassResourceInfo().getURITemplate().getValue());
        }
        builder.path(ori.getURITemplate().getValue());
        handleMatrixes(types, params, builder);
        handleQueries(types, params, builder);
        
        URI uri = builder.buildFromEncoded(pathParams.toArray()).normalize();
        
        MultivaluedMap<String, String> headers = getHeaders();
        MultivaluedMap<String, String> paramHeaders = new MetadataMap<String, String>();
        handleHeaders(paramHeaders, types, params);
        handleCookies(paramHeaders, types, params);
                
        if (ori.isSubResourceLocator()) {
            ClassResourceInfo subCri = cri.getSubResource(m.getReturnType(), m.getReturnType());
            if (subCri == null) {
                reportInvalidResourceMethod(m, "INVALID_SUBRESOURCE");
            }
            
            MultivaluedMap<String, String> subHeaders = paramHeaders;
            if (inheritHeaders) {
                subHeaders.putAll(headers);    
            }
            
            ClientState newState = getState().newState(uri, headers);
            ClientProxyImpl proxyImpl = new ClientProxyImpl(newState, subCri, false, inheritHeaders);
            proxyImpl.setConfiguration(getConfiguration());
            return JAXRSClientFactory.create(m.getReturnType(), proxyImpl);
        } 
        
        headers.putAll(paramHeaders);
        setRequestHeaders(headers, ori, types.containsKey(ParameterType.FORM), 
            bodyIndex == -1 ? null : params[bodyIndex].getClass(), m.getReturnType());
        
        return doChainedInvocation(uri, headers, ori, params, bodyIndex, types, pathParams);
        
    }

    private static MultivaluedMap<ParameterType, Parameter> getParametersInfo(OperationResourceInfo ori) {
        MultivaluedMap<ParameterType, Parameter> map = 
            new MetadataMap<ParameterType, Parameter>();
        
        List<Parameter> parameters = ori.getParameters();
        if (parameters.size() == 0) {
            return map;
        }
        for (Parameter p : parameters) {
            if (p.getType() == ParameterType.CONTEXT) {
                reportInvalidResourceMethod(ori.getMethodToInvoke(), "NO_CONTEXT_PARAMETERS");
            }
            map.add(p.getType(), p);
        }
        if (map.containsKey(ParameterType.REQUEST_BODY)) {
            if (map.get(ParameterType.REQUEST_BODY).size() > 1) {
                reportInvalidResourceMethod(ori.getMethodToInvoke(), "SINGLE_BODY_ONLY");
            }
            if (map.containsKey(ParameterType.FORM)) {
                reportInvalidResourceMethod(ori.getMethodToInvoke(), "ONLY_FORM_ALLOWED");
            }
        }
        return map;
    }
    
    private static int getBodyIndex(MultivaluedMap<ParameterType, Parameter> map, 
                                    OperationResourceInfo ori) {
        List<Parameter> list = map.get(ParameterType.REQUEST_BODY);
        int index  = list == null ? -1 : list.get(0).getIndex(); 
        if (ori.isSubResourceLocator() && index != -1) {
            reportInvalidResourceMethod(ori.getMethodToInvoke(), "NO_BODY_IN_SUBRESOURCE");
        }
        return index;
    }
    
    private void checkResponse(Method m, Response r, Message inMessage) throws Throwable {
        Throwable t = null;
        int status = r.getStatus();
        
        if (status >= 400) {
            if (m.getReturnType() == Response.class && m.getExceptionTypes().length == 0) {
                return;
            }            
            ResponseExceptionMapper<?> mapper = findExceptionMapper(m, inMessage);
            if (mapper != null) {
                t = mapper.fromResponse(r);
                if (t != null) {
                    throw t;
                }
            } 
                        
            if (t == null) {
                t = new WebApplicationException(r);
            }

            
            if (inMessage.getExchange().get(Message.RESPONSE_CODE) == null) {
                throw t;
            }
            
            Endpoint ep = inMessage.getExchange().get(Endpoint.class);
            inMessage.getExchange().put(InterceptorProvider.class, getConfiguration());
            inMessage.setContent(Exception.class, new Fault(t));
            inMessage.getInterceptorChain().abort();
            if (ep.getInFaultObserver() != null) {
                ep.getInFaultObserver().onMessage(inMessage);
            }
            
            throw t;
            
        }
    }
    
    private static ResponseExceptionMapper<?> findExceptionMapper(Method m, Message message) {
        ProviderFactory pf = ProviderFactory.getInstance(message);
        for (Class<?> exType : m.getExceptionTypes()) {
            ResponseExceptionMapper<?> mapper = pf.createResponseExceptionMapper(exType);
            if (mapper != null) {
                return mapper;
            }
        }
        return null;
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
                    || ori.getConsumeTypes().get(0).equals(MediaType.WILDCARD_TYPE) 
                    ? MediaType.APPLICATION_XML : ori.getConsumeTypes().get(0).toString();   
                headers.putSingle(HttpHeaders.CONTENT_TYPE, cType);
            }
        }
        
        List<MediaType> accepts = getAccept(headers);
        if (accepts == null) {
            accepts = InjectionUtils.isPrimitive(responseClass) 
                ? Collections.singletonList(MediaType.TEXT_PLAIN_TYPE)
                : ori.getProduceTypes().size() == 0 
                || ori.getProduceTypes().get(0).equals(MediaType.WILDCARD_TYPE) 
                ? Collections.singletonList(MediaType.APPLICATION_XML_TYPE) : ori.getProduceTypes();
            for (MediaType mt : accepts) {
                headers.add(HttpHeaders.ACCEPT, mt.toString());
            }
        }
            
        return headers;
    }
    
    private List<MediaType> getAccept(MultivaluedMap<String, String> allHeaders) {
        List<String> headers = allHeaders.get(HttpHeaders.ACCEPT);
        if (headers == null || headers.size() == 0) {
            return null;
        }
        List<MediaType> types = new ArrayList<MediaType>();
        for (String s : headers) {
            types.add(MediaType.valueOf(s));
        }
        return types;
    }
    
    private List<Object> getPathParamValues(MultivaluedMap<ParameterType, Parameter> map,
                                            Object[] params,
                                            OperationResourceInfo ori) {
        List<Parameter> paramsList =  getParameters(map, ParameterType.PATH);
        List<Object> list = new LinkedList<Object>();
        if (isRoot) {
            list.addAll(valuesMap.values());
        }
        List<String> vars = ori.getURITemplate().getVariables();
        // TODO : unfortunately, UriBuilder will lose a method-scoped parameter 
        // if a same name variable exists in a class scope which is an api bug.
        // It's a rare case but we might want just to use UriBuilderImpl() directly 
        // on the client side and tell it to choose the last variable value
        for (Parameter p : paramsList) {
            if (valuesMap.containsKey(p.getName()) && !vars.contains(p.getName())) {
                int index = 0; 
                for (Iterator<String> it = valuesMap.keySet().iterator(); it.hasNext(); index++) {
                    if (it.next().equals(p.getName())) {
                        list.remove(index);
                        list.add(index, params[p.getIndex()]);
                        break;
                    }
                }
            } else {
                String paramName = p.getName();
                if (!"".equals(paramName)) {
                    list.add(params[p.getIndex()]);
                } else {
                    MultivaluedMap<String, Object> values = 
                        InjectionUtils.extractValuesFromBean(params[p.getIndex()], "");
                    for (String var : vars) {
                        list.addAll(values.get(var));
                    }
                }
                
            }
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
                addParametersToBuilder(ub, p.getName(), params[p.getIndex()], ParameterType.QUERY);
            }
        }
    }
    
    private static void handleMatrixes(MultivaluedMap<ParameterType, Parameter> map, Object[] params,
                                UriBuilder ub) {
        List<Parameter> mx = getParameters(map, ParameterType.MATRIX);
        for (Parameter p : mx) {
            if (params[p.getIndex()] != null) {
                addParametersToBuilder(ub, p.getName(), params[p.getIndex()], ParameterType.MATRIX);
            }
        }
    }

    private MultivaluedMap<String, Object> handleForm(MultivaluedMap<ParameterType, Parameter> map, 
                                                      Object[] params) {
        
        MultivaluedMap<String, Object> form = new MetadataMap<String, Object>();
        
        List<Parameter> fm = getParameters(map, ParameterType.FORM);
        for (Parameter p : fm) {
            if (params[p.getIndex()] != null) {
                FormUtils.addPropertyToForm(form, p.getName(), params[p.getIndex()].toString());
            }
        }
        
        return form;
    }
    
    private void handleHeaders(MultivaluedMap<String, String> headers,
                               MultivaluedMap<ParameterType, Parameter> map, Object[] params) {
        List<Parameter> hs = getParameters(map, ParameterType.HEADER);
        for (Parameter p : hs) {
            if (params[p.getIndex()] != null) {
                headers.add(p.getName(), params[p.getIndex()].toString());
            }
        }
    }
    
    private void handleCookies(MultivaluedMap<String, String> headers,
                               MultivaluedMap<ParameterType, Parameter> map, Object[] params) {
        List<Parameter> cs = getParameters(map, ParameterType.COOKIE);
        for (Parameter p : cs) {
            if (params[p.getIndex()] != null) {
                headers.add(HttpHeaders.COOKIE, p.getName() + '=' + params[p.getIndex()].toString());
            }
        }
    }
    
    private Object doChainedInvocation(URI uri, MultivaluedMap<String, String> headers, 
                          OperationResourceInfo ori, Object[] params, int bodyIndex, 
                          MultivaluedMap<ParameterType, Parameter> types,
                          List<Object> pathParams) throws Throwable {
        Message outMessage = createMessage(ori.getHttpMethod(), headers, uri);
        outMessage.getExchange().setOneWay(ori.isOneway());
        if (pathParams.size() != 0) { 
            List<String> vars = ori.getURITemplate().getVariables();
            MultivaluedMap<String, String> templatesMap =  new MetadataMap<String, String>(vars.size());
            for (int i = 0; i < vars.size(); i++) {
                if (i < pathParams.size()) {
                    templatesMap.add(vars.get(i), pathParams.get(i).toString());
                }
            }
            outMessage.put(URITemplate.TEMPLATE_PARAMETERS, templatesMap);
        }
        outMessage.setContent(OperationResourceInfo.class, ori);
        setPlainOperationNameProperty(outMessage, ori.getMethodToInvoke().getName());
        outMessage.getExchange().put(Method.class, ori.getMethodToInvoke());
        boolean isForm = types.containsKey(ParameterType.FORM);
        if (bodyIndex != -1 || isForm) {
            outMessage.put("BODY_INDEX", bodyIndex);
            Object body = isForm ? handleForm(types, params) : params[bodyIndex];
            MessageContentsList contents = new MessageContentsList(new Object[]{body});
            outMessage.setContent(List.class, contents);
            outMessage.getInterceptorChain().add(new BodyWriter());
        } else {
            setEmptyRequestProperty(outMessage, ori.getHttpMethod());
        }
        
        // execute chain    
        try {
            outMessage.getInterceptorChain().doIntercept(outMessage);
        } catch (Throwable ex) {
            // we'd like a user to get the whole Response anyway if needed
        }
        
        // TODO : this needs to be done in an inbound chain instead
        HttpURLConnection connect = (HttpURLConnection)outMessage.get(HTTPConduit.KEY_HTTP_CONNECTION);
        return handleResponse(connect, outMessage, ori);
        
    }
    
    protected Object handleResponse(HttpURLConnection connect, Message outMessage, OperationResourceInfo ori) 
        throws Throwable {
        Response r = setResponseBuilder(connect, outMessage.getExchange()).build();
        Method method = ori.getMethodToInvoke();
        checkResponse(method, r, outMessage);
        if (method.getReturnType() == Void.class) { 
            return null;
        }
        if (method.getReturnType() == Response.class
            && (r.getEntity() == null || InputStream.class.isAssignableFrom(r.getEntity().getClass())
                && ((InputStream)r.getEntity()).available() == 0)) {
            return r;
        }
        
        return readBody(r, connect, outMessage, method.getReturnType(), 
                        method.getGenericReturnType(), method.getDeclaredAnnotations());
    }

    public Object getInvocationHandler() {
        return this;
    }
    
    protected static void reportInvalidResourceMethod(Method m, String name) {
        org.apache.cxf.common.i18n.Message errorMsg = 
            new org.apache.cxf.common.i18n.Message(name, 
                                                   BUNDLE,
                                                   m.getDeclaringClass().getName(), 
                                                   m.getName());
        LOG.severe(errorMsg.toString());
        throw new WebApplicationException(405);
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
        public void handleMessage(Message outMessage) throws Fault {
            
            OperationResourceInfo ori = outMessage.getContent(OperationResourceInfo.class);
            OutputStream os = outMessage.getContent(OutputStream.class);
            if (os == null || ori == null) {
                return;
            }
            MessageContentsList objs = MessageContentsList.getContentsList(outMessage);
            if (objs == null || objs.size() == 0) {
                return;
            }
            MultivaluedMap<String, String> headers = 
                (MultivaluedMap)outMessage.get(Message.PROTOCOL_HEADERS);
            Method method = ori.getMethodToInvoke();
            int bodyIndex = (Integer)outMessage.get("BODY_INDEX");
            Method aMethod = ori.getAnnotatedMethod();
            Annotation[] anns = aMethod == null || bodyIndex == -1 ? new Annotation[0] 
                                                  : aMethod.getParameterAnnotations()[bodyIndex];
            Object body = objs.get(0);
            try {
                if (bodyIndex != -1) {
                    writeBody(body, outMessage, body.getClass(), 
                              method.getGenericParameterTypes()[bodyIndex],
                              anns, headers, os);
                } else {
                    writeBody(body, outMessage, body.getClass(), body.getClass(), 
                              anns, headers, os);
                }
                os.flush();
            } catch (Exception ex) {
                throw new Fault(ex);
            }
            
        }
        
    }

    
}
