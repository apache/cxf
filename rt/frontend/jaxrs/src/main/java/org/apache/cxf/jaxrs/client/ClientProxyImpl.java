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
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;

/**
 * Proxy-based client implementation
 *
 */
public class ClientProxyImpl extends AbstractClient implements 
    InvocationHandlerAware, InvocationHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(ClientProxyImpl.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ClientProxyImpl.class);
    private static final String SLASH = "/";
    
    private ClassResourceInfo cri;
    private ClassLoader proxyLoader;
    private boolean inheritHeaders;
    private boolean isRoot;
    private Map<String, Object> valuesMap = Collections.emptyMap();
    
    public ClientProxyImpl(URI baseURI,
                           ClassLoader loader,
                           ClassResourceInfo cri, 
                           boolean isRoot, 
                           boolean inheritHeaders, 
                           Object... varValues) {
        this(new LocalClientState(baseURI), loader, cri, isRoot, inheritHeaders, varValues);
    }
    
    public ClientProxyImpl(ClientState initialState,
                           ClassLoader loader,
                           ClassResourceInfo cri, 
                           boolean isRoot, 
                           boolean inheritHeaders, 
                           Object... varValues) {
        super(initialState);
        this.proxyLoader = loader;
        this.cri = cri;
        this.isRoot = isRoot;
        this.inheritHeaders = inheritHeaders;
        initValuesMap(varValues);
    }
    
    private void initValuesMap(Object... varValues) {
        if (isRoot) {
            List<String> vars = cri.getURITemplate().getVariables();
            valuesMap = new LinkedHashMap<String, Object>();
            for (int i = 0; i < vars.size(); i++) {
                if (varValues.length > 0) {
                    if (i < varValues.length) {
                        valuesMap.put(vars.get(i), varValues[i]);
                    } else {
                        org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                             "ROOT_VARS_MISMATCH", BUNDLE, vars.size(), varValues.length);
                        LOG.info(msg.toString());
                        break;
                    }
                } else {
                    valuesMap.put(vars.get(i), "");
                }
            }
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
        
        MultivaluedMap<ParameterType, Parameter> types = getParametersInfo(params, ori);
        List<Object> pathParams = getPathParamValues(types, params, ori);
        
        int bodyIndex = getBodyIndex(types, ori);
        
        UriBuilder builder = getCurrentBuilder().clone(); 
        if (isRoot) {
            addNonEmptyPath(builder, ori.getClassResourceInfo().getURITemplate().getValue());
        }
        addNonEmptyPath(builder, ori.getURITemplate().getValue());
        
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
            
            ClientState newState = getState().newState(uri, subHeaders, 
                 getTemplateParametersMap(ori.getURITemplate(), pathParams));
            ClientProxyImpl proxyImpl = 
                new ClientProxyImpl(newState, proxyLoader, subCri, false, inheritHeaders);
            proxyImpl.setConfiguration(getConfiguration());
            return JAXRSClientFactory.createProxy(m.getReturnType(), proxyLoader, proxyImpl);
        } 
        
        headers.putAll(paramHeaders);
        setRequestHeaders(headers, ori, types.containsKey(ParameterType.FORM), 
            bodyIndex == -1 ? null : params[bodyIndex].getClass(), m.getReturnType());
        
        getState().setTemplates(getTemplateParametersMap(ori.getURITemplate(), pathParams));
        
        Object body = null;
        if (bodyIndex != -1) {
            body = params[bodyIndex];
        } else if (types.containsKey(ParameterType.FORM))  {
            body = handleForm(types, params);
        } else if (types.containsKey(ParameterType.REQUEST_BODY))  {
            body = handleMultipart(types, ori, params);
        }
        
        return doChainedInvocation(uri, headers, ori, body, bodyIndex, null, null);
        
    }

    private void addNonEmptyPath(UriBuilder builder, String pathValue) {
        if (!SLASH.equals(pathValue)) {
            builder.path(pathValue);
        }
    }
    
    private static MultivaluedMap<ParameterType, Parameter> getParametersInfo(
        Object[] params, OperationResourceInfo ori) {
        MultivaluedMap<ParameterType, Parameter> map = 
            new MetadataMap<ParameterType, Parameter>();
        
        List<Parameter> parameters = ori.getParameters();
        if (parameters.size() == 0) {
            return map;
        }
        int requestBodyParam = 0;
        int multipartParam = 0;
        for (Parameter p : parameters) {
            if (p.getType() == ParameterType.CONTEXT) {
                // ignore
                continue;
            }
            if (p.getType() == ParameterType.REQUEST_BODY) {
                requestBodyParam++;
                if (getMultipart(ori, p.getIndex()) != null) {
                    multipartParam++;    
                }
            }
            map.add(p.getType(), p);
        }
        
        if (map.containsKey(ParameterType.REQUEST_BODY)) {
            if (requestBodyParam > 1 && requestBodyParam != multipartParam) {
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
        int index = list == null || list.size() > 1 ? -1 : list.get(0).getIndex();
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
                t = new ServerWebApplicationException(r);
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
                String cType = ori.getConsumeTypes().isEmpty() 
                    || ori.getConsumeTypes().get(0).equals(MediaType.WILDCARD_TYPE) 
                    ? MediaType.APPLICATION_XML : ori.getConsumeTypes().get(0).toString();   
                headers.putSingle(HttpHeaders.CONTENT_TYPE, cType);
            }
        }
        
        List<MediaType> accepts = getAccept(headers);
        if (accepts == null) {
            boolean produceWildcard = ori.getProduceTypes().size() == 0 
                || ori.getProduceTypes().get(0).equals(MediaType.WILDCARD_TYPE);
            if (produceWildcard) {
                accepts = InjectionUtils.isPrimitive(responseClass)
                    ? Collections.singletonList(MediaType.TEXT_PLAIN_TYPE)
                    : Collections.singletonList(MediaType.APPLICATION_XML_TYPE);        
            } else if (responseClass == Void.class) {
                accepts = Collections.singletonList(MediaType.WILDCARD_TYPE);
            } else {
                accepts = ori.getProduceTypes();
            }
            
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
        List<Object> list = new LinkedList<Object>();
        if (isRoot) {
            list.addAll(valuesMap.values());
        }
        List<String> methodVars = ori.getURITemplate().getVariables();
        
        List<Parameter> paramsList =  getParameters(map, ParameterType.PATH);
        Map<String, Parameter> paramsMap = new LinkedHashMap<String, Parameter>();
        for (Parameter p : paramsList) {
            if (p.getName().length() == 0) {
                MultivaluedMap<String, Object> values = 
                    InjectionUtils.extractValuesFromBean(params[p.getIndex()], "");
                for (String var : methodVars) {
                    list.addAll(values.get(var));
                }
            } else {
                paramsMap.put(p.getName(), p);
            }
        }
        
        for (String varName : methodVars) {
            Parameter p = paramsMap.remove(varName);
            if (p != null) {
                list.add(params[p.getIndex()]);
            }
        }
        
        for (Parameter p : paramsMap.values()) {
            if (valuesMap.containsKey(p.getName())) {
                int index = 0; 
                for (Iterator<String> it = valuesMap.keySet().iterator(); it.hasNext(); index++) {
                    if (it.next().equals(p.getName()) && index < list.size()) {
                        list.remove(index);
                        list.add(index, params[p.getIndex()]);
                        break;
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

    private MultivaluedMap<String, String> handleForm(MultivaluedMap<ParameterType, Parameter> map, 
                                                      Object[] params) {
        
        MultivaluedMap<String, String> form = new MetadataMap<String, String>();
        
        List<Parameter> fm = getParameters(map, ParameterType.FORM);
        for (Parameter p : fm) {
            Object pValue = params[p.getIndex()];
            if (pValue != null) {
                if (InjectionUtils.isSupportedCollectionOrArray(pValue.getClass())) {
                    Collection<?> c = pValue.getClass().isArray() 
                        ? Arrays.asList((Object[]) pValue) : (Collection) pValue;
                    for (Iterator<?> it = c.iterator(); it.hasNext();) {
                        FormUtils.addPropertyToForm(form, p.getName(), it.next());
                    }
                } else { 
                    FormUtils.addPropertyToForm(form, p.getName(), pValue); 
                }
                
            }
        }
        
        return form;
    }
    
    private List<Attachment> handleMultipart(MultivaluedMap<ParameterType, Parameter> map,
                                             OperationResourceInfo ori,
                                             Object[] params) {
        
        List<Attachment> atts = new LinkedList<Attachment>();
        List<Parameter> fm = getParameters(map, ParameterType.REQUEST_BODY);
        for (Parameter p : fm) {
            Multipart part = getMultipart(ori, p.getIndex());
            if (part != null) {
                atts.add(new Attachment(part.value(), part.type(), params[p.getIndex()]));
            }
        }
        return atts;        
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
    
    private static Multipart getMultipart(OperationResourceInfo ori, int index) {
        Method aMethod = ori.getAnnotatedMethod();
        return aMethod != null ? AnnotationUtils.getAnnotation(
            aMethod.getParameterAnnotations()[index], Multipart.class) : null;
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
    
    private Object doChainedInvocation(URI uri, 
                                       MultivaluedMap<String, String> headers, 
                                       OperationResourceInfo ori, 
                                       Object body, 
                                       int bodyIndex,
                                       Exchange exchange,
                                       Map<String, Object> invocationContext) throws Throwable {
        
        Message outMessage = createMessage(body, ori.getHttpMethod(), headers, uri, 
                                           exchange, invocationContext, true);
        
        outMessage.getExchange().setOneWay(ori.isOneway());
        outMessage.setContent(OperationResourceInfo.class, ori);
        setPlainOperationNameProperty(outMessage, ori.getMethodToInvoke().getName());
        outMessage.getExchange().put(Method.class, ori.getMethodToInvoke());
        
        if (body != null) {
            outMessage.put("BODY_INDEX", bodyIndex);
            outMessage.getInterceptorChain().add(new BodyWriter());
        }

        Map<String, Object> reqContext = getRequestContext(outMessage);
        reqContext.put(OperationResourceInfo.class.getName(), ori);
        reqContext.put("BODY_INDEX", bodyIndex);
        
        // execute chain    
        try {
            outMessage.getInterceptorChain().doIntercept(outMessage);
        } catch (Exception ex) {
            outMessage.setContent(Exception.class, ex);
        }
        
        Object[] results = preProcessResult(outMessage);
        if (results != null && results.length == 1) {
            // this can happen if a connection exception has occurred and
            // failover feature used this client to invoke on a different address  
            return results[0];
        }
        
        Object response = null;
        try {
            response = handleResponse(outMessage);
            return response;
        } catch (Exception ex) {
            response = ex;
            throw ex;
        } finally {
            completeExchange(response, outMessage.getExchange());
        }
        
    }
    
    @Override
    protected Object retryInvoke(URI newRequestURI, 
                                 MultivaluedMap<String, String> headers,
                                 Object body,
                                 Exchange exchange, 
                                 Map<String, Object> invContext) throws Throwable {
        
        Map<String, Object> reqContext = CastUtils.cast((Map)invContext.get(REQUEST_CONTEXT));
        int bodyIndex = body != null ? (Integer)reqContext.get("BODY_INDEX") : -1;
        OperationResourceInfo ori = 
            (OperationResourceInfo)reqContext.get(OperationResourceInfo.class.getName());
        return doChainedInvocation(newRequestURI, headers, ori, 
                                   body, bodyIndex, exchange, invContext);
    }
    
    protected Object handleResponse(Message outMessage) 
        throws Throwable {
        try {
            Response r = setResponseBuilder(outMessage, outMessage.getExchange()).build();
            Method method = outMessage.getExchange().get(Method.class);
            checkResponse(method, r, outMessage);
            if (method.getReturnType() == Void.class) { 
                return null;
            }
            if (method.getReturnType() == Response.class
                && (r.getEntity() == null || InputStream.class.isAssignableFrom(r.getEntity().getClass())
                    && ((InputStream)r.getEntity()).available() == 0)) {
                return r;
            }
            
            return readBody(r, outMessage, method.getReturnType(), 
                            method.getGenericReturnType(), method.getDeclaredAnnotations());
        } finally {
            ProviderFactory.getInstance(outMessage).clearThreadLocalProxies();
        }
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
        throw new ClientWebApplicationException(errorMsg.toString());
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
            if ((os == null && outMessage.getContent(XMLStreamWriter.class) == null)
                || ori == null) {
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
                    Class<?> paramClass = method.getParameterTypes()[bodyIndex];
                    Type paramType = method.getGenericParameterTypes()[bodyIndex];
                    
                    boolean isAssignable = paramClass.isAssignableFrom(body.getClass());
                    writeBody(body, outMessage,
                              isAssignable ? paramClass : body.getClass(),
                              isAssignable ? paramType : body.getClass(),
                              anns, headers, os);
                } else {
                    writeBody(body, outMessage, body.getClass(), body.getClass(), 
                              anns, headers, os);
                }
            } catch (Exception ex) {
                throw new Fault(ex);
            }
            
        }
        
    }
    
}
