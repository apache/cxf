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

package org.apache.cxf.jaxws.context;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;

public class WrappedMessageContext implements MessageContext {
    public static final String SCOPES = WrappedMessageContext.class.getName() + ".SCOPES";
    
    private static Map<String, String> cxf2jaxwsMap = new HashMap<String, String>();
    private static Map<String, String> jaxws2cxfMap = new HashMap<String, String>();
    
    static {
        cxf2jaxwsMap.put(Message.ENDPOINT_ADDRESS, 
                          BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        cxf2jaxwsMap.put(Message.MAINTAIN_SESSION,
                         BindingProvider.SESSION_MAINTAIN_PROPERTY);
        
        cxf2jaxwsMap.put(Message.HTTP_REQUEST_METHOD,
                          MessageContext.HTTP_REQUEST_METHOD);
        cxf2jaxwsMap.put(Message.RESPONSE_CODE, 
                          MessageContext.HTTP_RESPONSE_CODE);        
        cxf2jaxwsMap.put(Message.PATH_INFO, 
                          MessageContext.PATH_INFO);
        cxf2jaxwsMap.put(Message.QUERY_STRING, 
                          MessageContext.QUERY_STRING);
        cxf2jaxwsMap.put("HTTP.REQUEST", 
                         MessageContext.SERVLET_REQUEST);
        cxf2jaxwsMap.put("HTTP.RESPONSE", 
                         MessageContext.SERVLET_RESPONSE);
        cxf2jaxwsMap.put("HTTP.CONTEXT", 
                         MessageContext.SERVLET_CONTEXT);
       
        jaxws2cxfMap.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                         Message.ENDPOINT_ADDRESS);
        jaxws2cxfMap.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, 
                         Message.MAINTAIN_SESSION);
                
        jaxws2cxfMap.put(MessageContext.HTTP_REQUEST_METHOD,
                         Message.HTTP_REQUEST_METHOD);
        jaxws2cxfMap.put(MessageContext.HTTP_RESPONSE_CODE,
                         Message.RESPONSE_CODE);        
        jaxws2cxfMap.put(MessageContext.PATH_INFO,
                         Message.PATH_INFO);
        jaxws2cxfMap.put(MessageContext.QUERY_STRING,
                         Message.QUERY_STRING);
        
        jaxws2cxfMap.put(MessageContext.SERVLET_REQUEST, 
                         "HTTP.REQUEST"); 
        jaxws2cxfMap.put(MessageContext.SERVLET_RESPONSE, 
                         "HTTP.RESPONSE");
        jaxws2cxfMap.put(MessageContext.SERVLET_CONTEXT, 
                        "HTTP.CONTEXT");
        
        jaxws2cxfMap.put(BindingProvider.SOAPACTION_URI_PROPERTY, SoapBindingConstants.SOAP_ACTION);
    }

    private final Map<String, Object> message;
    private final Map<String, Object> reqMessage;
    private final Exchange exchange;
    private Map<String, Scope> scopes;
    private Scope defaultScope;

    public WrappedMessageContext(Message m) {
        this(m, Scope.HANDLER);
    }
    public WrappedMessageContext(Message m, Scope defScope) {
        this(m, m.getExchange(), defScope);
    }    
    public WrappedMessageContext(Map<String, Object> m, Exchange ex, Scope defScope) {
        message = m;
        exchange = ex;
        defaultScope = defScope;
        scopes = CastUtils.cast((Map<?, ?>)message.get(SCOPES));
        
        if (isResponse() && exchange != null) {
            if (isRequestor()) {
                reqMessage = exchange.getOutMessage();
            } else {
                reqMessage = exchange.getInMessage();
            }
        } else {
            reqMessage = null;
        }
        
        if (scopes == null && reqMessage != null) {
            scopes = CastUtils.cast((Map<?, ?>)reqMessage.get(SCOPES));
            if (scopes != null) {
                m.put(SCOPES, scopes);
                copyScoped(reqMessage);
            }
        }
        if (scopes == null) {
            scopes = new HashMap<String, Scope>();
            message.put(SCOPES, scopes);
        }
    }
    private void copyScoped(Map<String, Object> msg) {
        for (String s : scopes.keySet()) {
            message.put(s, msg.get(s));
        }
    }
    
    private String mapKey(String key) {
        String k2 = jaxws2cxfMap.get(key);
        if (k2 != null) {
            return k2;
        }
        return key;
    }
    private String mapKeyReverse(String key) {
        String k2 = cxf2jaxwsMap.get(key);
        if (k2 != null) {
            return k2;
        }
        if (Message.PROTOCOL_HEADERS.equals(key)) {
            return isResponse() ? MessageContext.HTTP_RESPONSE_HEADERS : MessageContext.HTTP_REQUEST_HEADERS;
        }
        return key;
    }
    
    
    protected final boolean isResponse() {
        return isOutbound() ^ isRequestor();
    }
    protected final boolean isRequestor() {
        return Boolean.TRUE.equals(message.containsKey(Message.REQUESTOR_ROLE));
    }
    protected final boolean isOutbound() {
        return message != null 
            && exchange != null
            && (message == exchange.getOutMessage()
                || message == exchange.getOutFaultMessage());
    }
    
    public final Message getWrappedMessage() {
        return message instanceof Message ? (Message)message : null;
    }
    public final Map<String, Object> getWrappedMap() {
        return message;
    }
    
    public void clear() {
        //just clear the JAXWS things....
        for (String key : jaxws2cxfMap.keySet()) {
            remove(key);
        }
    }

    public final boolean containsKey(Object key) {
        return message.containsKey(mapKey((String)key));
    }

    public final boolean containsValue(Object value) {
        return message.containsValue(value);
    }

    public Object get(Object key) {
        String mappedkey = mapKey((String)key);
        Object ret = message.get(mappedkey);
        if (ret == null) {
            if (Message.class.getName().equals(mappedkey)) {
                return message;
            }
            if (exchange != null) {
                ret = exchange.get(mappedkey);
                if (ret != null) {
                    return ret;
                }
            }
            if (MessageContext.INBOUND_MESSAGE_ATTACHMENTS.equals(key)) {
                if (isRequestor() && isOutbound()) {
                    ret = null;
                } else if (isOutbound()) {
                    ret = createAttachments(reqMessage, 
                                            MessageContext.INBOUND_MESSAGE_ATTACHMENTS);
                } else {
                    ret = createAttachments(message, 
                                            MessageContext.INBOUND_MESSAGE_ATTACHMENTS);                    
                }
            } else if (MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS.equals(key)) {
                if (isRequestor() && !isOutbound()) {
                    ret = createAttachments(reqMessage, MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS);
                } else {
                    ret = createAttachments(isRequestor() ? getWrappedMessage() : createResponseMessage(),
                        MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS);
                }
            } else if (MessageContext.MESSAGE_OUTBOUND_PROPERTY.equals(key)) {
                ret = isOutbound();
            } else if (MessageContext.HTTP_REQUEST_HEADERS.equals(key)) {
                if (!isResponse()) {
                    ret = message.get(Message.PROTOCOL_HEADERS);
                } else if (reqMessage != null && !isRequestor()) {
                    ret = reqMessage.get(Message.PROTOCOL_HEADERS);
                }
            } else if (MessageContext.HTTP_RESPONSE_HEADERS.equals(key)) {
                Map mp = null;
                if (isResponse()) {
                    mp = (Map)message.get(Message.PROTOCOL_HEADERS);
                } else if (exchange != null) {
                    //may have to create the out message and add the headers
                    Message tmp = createResponseMessage();
                    if (tmp != null) {
                        ret = (Map)tmp.get(Message.PROTOCOL_HEADERS);
                    }
                }
                ret = mp;
            } else if (BindingProvider.USERNAME_PROPERTY.equals(key)) {
                AuthorizationPolicy authPolicy =
                    (AuthorizationPolicy)message.get(AuthorizationPolicy.class.getName());
                if (authPolicy != null) {
                    ret = authPolicy.getUserName();
                }
            } else if (BindingProvider.PASSWORD_PROPERTY.equals(key)) {
                AuthorizationPolicy authPolicy =
                    (AuthorizationPolicy)message.get(AuthorizationPolicy.class.getName());
                if (authPolicy != null) {
                    ret = authPolicy.getPassword();
                }
            } else if (Message.WSDL_OPERATION.equals(key)) {
                BindingOperationInfo boi = getBindingOperationInfo(exchange);
                if (boi != null) {
                    ret = boi.getName();
                }
            } else if (Message.WSDL_SERVICE.equals(key)) {
                BindingOperationInfo boi = getBindingOperationInfo(exchange);
                if (boi != null) {
                    ret = boi.getBinding().getService().getName();
                }
            } else if (Message.WSDL_INTERFACE.equals(key)) {
                BindingOperationInfo boi = getBindingOperationInfo(exchange);
                if (boi != null) {
                    ret = boi.getBinding().getService().getInterface().getName();
                }
            } else if (Message.WSDL_PORT.equals(key)) {
                EndpointInfo endpointInfo = getEndpointInfo(exchange);
                if (endpointInfo != null) {
                    ret = endpointInfo.getName();
                }
            } else if (Message.WSDL_DESCRIPTION.equals(key)) {
                EndpointInfo endpointInfo = getEndpointInfo(exchange);
                if (endpointInfo != null) {
                    URI wsdlDescription = endpointInfo.getProperty("URI", URI.class);
                    if (wsdlDescription == null) {
                        String address = endpointInfo.getAddress();
                        try {
                            wsdlDescription = new URI(address + "?wsdl");
                        } catch (URISyntaxException e) {
                            // do nothing
                        }
                        endpointInfo.setProperty("URI", wsdlDescription);
                    }
                    ret = wsdlDescription;
                }
            }

            
            if (ret == null && reqMessage != null) { 
                ret = reqMessage.get(mappedkey);
            }
        }
        return ret;
    }
    
    private static BindingOperationInfo getBindingOperationInfo(Exchange exchange) {
        if (exchange != null && exchange.get(BindingOperationInfo.class) != null) {
            return exchange.get(BindingOperationInfo.class);
        }
        return null;
    }

    private static EndpointInfo getEndpointInfo(Exchange exchange) {
        if (exchange != null) {
            Endpoint endpoint = exchange.get(Endpoint.class);
            if (endpoint != null) {
                return endpoint.getEndpointInfo();
            }
        }
        return null;
    }

    private Message createResponseMessage() {
        if (exchange == null || exchange.isOneWay()) {
            return null;
        }
        if (isResponse()) {
            return getWrappedMessage();
        }
        Message m = null;
        if (isRequestor()) {
            m = exchange.getInFaultMessage();
            if (m == null) {
                m = exchange.getInMessage();
            }
            if (m == null) {
                Endpoint ep = exchange.get(Endpoint.class);
                m = new org.apache.cxf.message.MessageImpl();
                m.setExchange(exchange);
                m = ep.getBinding().createMessage(m);
                exchange.setInMessage(m);
            }
        } else {
            m = exchange.getOutMessage();
            if (m == null) {
                m = exchange.getOutFaultMessage();
            }
            if (m == null) {
                Endpoint ep = exchange.get(Endpoint.class);
                m = ep.getBinding().createMessage();
                exchange.setOutMessage(m);
            }
        }
        return m;
    }
    private Object createAttachments(Map<String, Object> mc, String propertyName) {
        if (mc == null) {
            return null;
        }
        Collection<Attachment> attachments = CastUtils.cast((Collection<?>)mc.get(Message.ATTACHMENTS));
        Map<String, DataHandler> dataHandlers = 
            AttachmentUtil.getDHMap(attachments);
        mc.put(propertyName, 
               dataHandlers);
        scopes.put(propertyName, Scope.APPLICATION);
        return dataHandlers;
    }    
        
    public final boolean isEmpty() {
        return message.isEmpty();
    }

    // map to jaxws
    public final Set<String> keySet() {
        Set<String> set = new HashSet<String>();
        for (String s : message.keySet()) {
            set.add(s);
            set.add(mapKeyReverse(s));
        }
        return Collections.unmodifiableSet(set);
    }
    public final Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> set = new HashSet<Entry<String, Object>>();
        for (Map.Entry<String, Object> s : message.entrySet()) {
            set.add(s);
            
            final String s2 = mapKeyReverse(s.getKey());
            final Object o = s.getValue();
            if (s2.equals(s.getKey())) {
                Map.Entry<String, Object> entry = new Map.Entry<String, Object>() {
                    public String getKey() {
                        return s2;
                    }
                    public Object getValue() {
                        return o;
                    }
                    public Object setValue(Object value) {
                        throw new UnsupportedOperationException();
                    }
                };
                set.add(entry);
            }
        }
        return Collections.unmodifiableSet(set);
    }


    public final Object put(String key, Object value) {
        return put(key, value, defaultScope);
    }
    public final Object put(String key, Object value, Scope scope) {
        String mappedKey = mapKey(key);
        if (!MessageContext.MESSAGE_OUTBOUND_PROPERTY.equals(mappedKey)) {
            scopes.put(mappedKey, scope);
        }
        Object ret = null;
        if ((MessageContext.HTTP_RESPONSE_HEADERS.equals(key)
            || MessageContext.HTTP_RESPONSE_CODE.equals(key)
            || MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS.equals(key)
            || MessageContext.HTTP_RESPONSE_CODE.equals(key))
            && !isResponse() && !isRequestor()) { 
            Message tmp = createResponseMessage();
            if (tmp != null) {
                if (MessageContext.HTTP_RESPONSE_HEADERS.equals(key)) {
                    return tmp.put(Message.PROTOCOL_HEADERS, value);
                } else {
                    return tmp.put(mappedKey, value);
                }
            }
        } else if (BindingProvider.USERNAME_PROPERTY.equals(key)) {
            AuthorizationPolicy authPolicy =
                (AuthorizationPolicy)message.get(AuthorizationPolicy.class.getName());
            if (authPolicy == null) {
                authPolicy = new AuthorizationPolicy();
                message.put(AuthorizationPolicy.class.getName(), authPolicy);
            }
            ret = authPolicy.getUserName();
            authPolicy.setUserName((String)value);
        } else if (BindingProvider.PASSWORD_PROPERTY.equals(key)) {
            AuthorizationPolicy authPolicy =
                (AuthorizationPolicy)message.get(AuthorizationPolicy.class.getName());
            if (authPolicy == null) {
                authPolicy = new AuthorizationPolicy();
                message.put(AuthorizationPolicy.class.getName(), authPolicy);
            }
            ret = authPolicy.getPassword();
            authPolicy.setPassword((String)value);
        } else if (MessageContext.HTTP_REQUEST_HEADERS.equals(key)) {
            ret = message.put(Message.PROTOCOL_HEADERS, value);
        } else if (SoapBindingConstants.SOAP_ACTION.equals(mappedKey)
            && !isRequestor() && exchange != null) {
            Message tmp = createResponseMessage();
            if (tmp != null) {
                tmp.put(mappedKey, value);
            }
        } else {
            ret = message.put(mappedKey, value);
        }
        return ret;
    }

    public final void putAll(Map<? extends String, ? extends Object> t) {
        for (Map.Entry<? extends String, ? extends Object> s : t.entrySet()) {
            put(s.getKey(), s.getValue());
        }
    }

    public final Object remove(Object key) {
        key = mapKey((String)key);
        scopes.remove(key);
        if (BindingProvider.PASSWORD_PROPERTY.equals(key) 
            || BindingProvider.USERNAME_PROPERTY.equals(key)) {
            message.remove(AuthorizationPolicy.class.getName());
        }
        return message.remove(key);
    }

    public final int size() {
        return message.size();
    }

    public final Collection<Object> values() {
        return message.values();
    }

    public final void setScope(String key, Scope arg1) {
        if (!this.containsKey(key)) {
            throw new IllegalArgumentException("non-existant property-" + key + "is specified");    
        }
        scopes.put(key, arg1);        
    }

    public final Scope getScope(String key) {
        if (containsKey(key)) {
            if (scopes.containsKey(key)) {
                return scopes.get(key);
            } else {
                return defaultScope;
            }
        }
        throw new IllegalArgumentException("non-existant property-" + key + "is specified");
    }
    
    public final Map<String, Scope> getScopes() {
        return scopes;
    }
    
}
