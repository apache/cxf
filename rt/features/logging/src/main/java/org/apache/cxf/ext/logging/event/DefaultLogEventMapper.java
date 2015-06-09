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
package org.apache.cxf.ext.logging.event;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.security.auth.Subject;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;

public class DefaultLogEventMapper implements LogEventMapper {
    private static final Set<String> BINARY_CONTENT_MEDIA_TYPES;
    static {
        BINARY_CONTENT_MEDIA_TYPES = new HashSet<String>();
        BINARY_CONTENT_MEDIA_TYPES.add("application/octet-stream");
        BINARY_CONTENT_MEDIA_TYPES.add("image/png");
        BINARY_CONTENT_MEDIA_TYPES.add("image/jpeg");
        BINARY_CONTENT_MEDIA_TYPES.add("image/gif");
    }

    public LogEvent map(Message message) {
        final LogEvent event = new LogEvent();
        event.setMessageId(getMessageId(message));
        event.setExchangeId((String)message.getExchange().get(LogEvent.KEY_EXCHANGE_ID));
        event.setType(getEventType(message));
        if (!Boolean.TRUE.equals(message.get(Message.DECOUPLED_CHANNEL_MESSAGE))) {
            // avoid logging the default responseCode 200 for the decoupled responses
            Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
            if (responseCode != null) {
                event.setResponseCode(responseCode.toString());
            }
        }

        event.setEncoding(safeGet(message, Message.ENCODING));
        event.setHttpMethod(safeGet(message, Message.HTTP_REQUEST_METHOD));
        event.setContentType(safeGet(message, Message.CONTENT_TYPE));

        Map<String, String> headerMap = getHeaders(message);
        event.setHeaders(headerMap);

        String uri = getUri(message);
        if (uri != null) {
            event.setAddress(uri);
        }

        event.setPrincipal(getPrincipal(message));
        event.setBinaryContent(isBinaryContent(message));
        setEpInfo(message, event);
        return event;
    }

    private String getPrincipal(Message message) {
        String principal = getJAASPrincipal();
        if (principal != null) {
            return principal;
        }
        SecurityContext sc = message.get(SecurityContext.class);
        if (sc != null && sc.getUserPrincipal() != null) {
            return sc.getUserPrincipal().getName();
        }

        AuthorizationPolicy authPolicy = message.get(AuthorizationPolicy.class);
        if (authPolicy != null) {
            return authPolicy.getUserName();
        }
        return null;
    }

    private String getJAASPrincipal() {
        StringBuilder principals = new StringBuilder();
        Iterator<? extends Object> principalIt = getJAASPrincipals();
        while (principalIt.hasNext()) {
            principals.append(principalIt.next());
            if (principalIt.hasNext()) {
                principals.append(",");
            }
        }
        if (principals.length() == 0) {
            return null;
        }
        return principals.toString();
    }

    private Iterator<? extends Object> getJAASPrincipals() {
        Subject subject = Subject.getSubject(AccessController.getContext());
        return subject != null && subject.getPrincipals() != null
            ? subject.getPrincipals().iterator() : Collections.emptyIterator();
    }

    private Map<String, String> getHeaders(Message message) {
        Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        Map<String, String> result = new HashMap<>();
        if (headers == null) {
            return result;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getValue().size() == 1) {
                result.put(entry.getKey(), entry.getValue().get(0));
            } else {
                String[] valueAr = entry.getValue().toArray(new String[] {});
                result.put(entry.getKey(), valueAr.toString());
            }
        }
        return result;
    }

    private String getUri(Message message) {
        String uri = safeGet(message, Message.REQUEST_URL);
        if (uri == null) {
            String address = safeGet(message, Message.ENDPOINT_ADDRESS);
            uri = safeGet(message, Message.REQUEST_URI);
            if (uri != null && uri.startsWith("/")) {
                if (address != null && !address.startsWith(uri)) {
                    if (address.endsWith("/") && address.length() > 1) {
                        address = address.substring(0, address.length());
                    }
                    uri = address + uri;
                }
            } else {
                uri = address;
            }
        }
        String query = safeGet(message, Message.QUERY_STRING);
        if (query != null) {
            return uri + "?" + query;
        } else {
            return uri;
        }
    }

    private boolean isBinaryContent(Message message) {
        String contentType = safeGet(message, Message.CONTENT_TYPE);
        return contentType != null && BINARY_CONTENT_MEDIA_TYPES.contains(contentType);
    }

    /**
     * check if a Message is a Rest Message
     *
     * @param message
     * @return
     */
    private boolean isSOAPMessage(Message message) {
        Binding binding = message.getExchange().getBinding();
        return binding != null && binding.getClass().getSimpleName().equals("SoapBinding");
    }

    /**
     * Get MessageId from WS Addressing properties
     * 
     * @param message
     * @return message id
     */
    private String getMessageId(Message message) {
        AddressingProperties addrProp = ContextUtils.retrieveMAPs(message, false,
                                                                  MessageUtils.isOutbound(message), false);
        return (addrProp != null) ? addrProp.getMessageID().getValue() : UUID.randomUUID().toString();
    }

    private String getOperationName(Message message) {
        String operationName = null;
        BindingOperationInfo boi = null;

        boi = message.getExchange().getBindingOperationInfo();
        if (null == boi) {
            boi = getOperationFromContent(message);
        }

        if (null == boi) {
            Message inMsg = message.getExchange().getInMessage();
            if (null != inMsg) {
                Message reqMsg = inMsg.getExchange().getInMessage();
                if (null != reqMsg) {
                    boi = getOperationFromContent(reqMsg);
                }
            }
        }

        if (null != boi) {
            operationName = boi.getName().toString();
        }

        return operationName;
    }

    private BindingOperationInfo getOperationFromContent(Message message) {
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        if (xmlReader != null) {
            return ServiceModelUtil.getOperation(message.getExchange(), xmlReader.getName());
        } else {
            return null;
        }
    }

    private Message getEffectiveMessage(Message message) {
        boolean isRequestor = MessageUtils.isRequestor(message);
        boolean isOutbound = MessageUtils.isOutbound(message);
        if (isRequestor) {
            return isOutbound ? message : message.getExchange().getOutMessage();
        } else {
            return isOutbound ? message.getExchange().getInMessage() : message;
        }
    }

    private String getRestOperationName(Message curMessage) {
        Message message = getEffectiveMessage(curMessage);
        String httpMethod = safeGet(message, Message.HTTP_REQUEST_METHOD);
        if (httpMethod == null) {
            return "";
        }

        String path = "";
        String requestUri = safeGet(message, Message.REQUEST_URI);
        if (requestUri != null) {
            String basePath = safeGet(message, Message.BASE_PATH);
            int baseUriLength = (basePath != null) ? basePath.length() : 0;
            path = requestUri.substring(baseUriLength);
            if (path.isEmpty()) {
                path = "/";
            }
        }
        return new StringBuffer().append(httpMethod).append('[').append(path).append(']').toString();
    }
    
    private String safeGet(Message message, String key) {
        if (!message.containsKey(key)) {
            return null;
        }
        Object value = message.get(key);
        return (value instanceof String) ? value.toString() : null;
    }

    /**
     * Gets the event type from message.
     *
     * @param message the message
     * @return the event type
     */
    private EventType getEventType(Message message) {
        boolean isRequestor = MessageUtils.isRequestor(message);
        boolean isFault = MessageUtils.isFault(message);
        if (!isFault) {
            isFault = !isSOAPMessage(message) && isRESTFault(message);
        }
        boolean isOutbound = MessageUtils.isOutbound(message);
        if (isOutbound) {
            if (isFault) {
                return EventType.FAULT_OUT;
            } else {
                return isRequestor ? EventType.REQ_OUT : EventType.RESP_OUT;
            }
        } else {
            if (isFault) {
                return EventType.FAULT_IN;
            } else {
                return isRequestor ? EventType.RESP_IN : EventType.REQ_IN;
            }
        }
    }

    /**
     * For REST we also consider a response to be a fault if the operation is not found or the response code
     * is an error
     * 
     * @param message
     * @return
     */
    private boolean isRESTFault(Message message) {
        Object opName = message.getExchange().get("org.apache.cxf.resource.operation.name");
        if (opName == null) {
            return true;
        } else {
            Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
            return (responseCode != null) && (responseCode >= 400);
        }
    }

    private void setEpInfo(Message message, final LogEvent event) {
        EndpointInfo endpoint = getEPInfo(message);
        event.setPortName(endpoint.getName());
        event.setPortTypeName(endpoint.getName());
        String opName = isSOAPMessage(message) ? getOperationName(message) : getRestOperationName(message);
        event.setOperationName(opName);
        if (endpoint.getService() != null) {
            setServiceInfo(endpoint.getService(), event);
        }
    }

    private void setServiceInfo(ServiceInfo service, LogEvent event) {
        event.setServiceName(service.getName());
        InterfaceInfo iface = service.getInterface();
        event.setPortTypeName(iface.getName());
    }

    private EndpointInfo getEPInfo(Message message) {
        Endpoint ep = message.getExchange().getEndpoint();
        return (ep == null) ? new EndpointInfo() : ep.getEndpointInfo();
    }

}
