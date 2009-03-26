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

package org.apache.cxf.message;

import java.util.Collection;
import java.util.Set;

import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.transport.Destination;

/**
 * The base interface for all all message implementations. 
 * All message objects passed to interceptors use this interface.
 */
public interface Message extends StringMap {
    
    String TRANSPORT = "org.apache.cxf.transport";    
    String REQUESTOR_ROLE = "org.apache.cxf.client";

    String INBOUND_MESSAGE = "org.apache.cxf.message.inbound";
    String INVOCATION_CONTEXT = "org.apache.cxf.invocation.context";
    
    String MIME_HEADERS = "org.apache.cxf.mime.headers";
    
    String ASYNC_POST_RESPONSE_DISPATCH =
        "org.apache.cxf.async.post.response.dispatch";

    String DECOUPLED_CHANNEL_MESSAGE = "decoupled.channel.message";
    String PARTIAL_RESPONSE_MESSAGE = "org.apache.cxf.partial.response";
    
    String PROTOCOL_HEADERS = Message.class.getName() + ".PROTOCOL_HEADERS";
    String RESPONSE_CODE = Message.class.getName() + ".RESPONSE_CODE";
    String ENDPOINT_ADDRESS = Message.class.getName() + ".ENDPOINT_ADDRESS";
    String HTTP_REQUEST_METHOD = Message.class.getName() + ".HTTP_REQUEST_METHOD";
    String PATH_INFO = Message.class.getName() + ".PATH_INFO";
    String REQUEST_URI = Message.class.getName() + ".REQUEST_URI";
    String QUERY_STRING = Message.class.getName() + ".QUERY_STRING";
    String MTOM_ENABLED = "mtom-enabled";
    String MTOM_THRESHOLD = "mtom-threshold";
    String SCHEMA_VALIDATION_ENABLED = "schema-validation-enabled";
    String FAULT_STACKTRACE_ENABLED = "faultStackTraceEnabled";
    String CONTENT_TYPE = "Content-Type";    
    String ACCEPT_CONTENT_TYPE = "Accept";
    String BASE_PATH = Message.class.getName() + ".BASE_PATH";
    String ENCODING = Message.class.getName() + ".ENCODING";
    String FIXED_PARAMETER_ORDER = Message.class.getName() + "FIXED_PARAMETER_ORDER";
    String MAINTAIN_SESSION = Message.class.getName() + ".MAINTAIN_SESSION";
    String ATTACHMENTS = Message.class.getName() + ".ATTACHMENTS";

    String WSDL_DESCRIPTION = "javax.xml.ws.wsdl.description";
    String WSDL_SERVICE = "javax.xml.ws.wsdl.service";
    String WSDL_PORT = "javax.xml.ws.wsdl.port";
    String WSDL_INTERFACE = "javax.xml.ws.wsdl.interface";
    String WSDL_OPERATION = "javax.xml.ws.wsdl.operation";

    
    String getId();
    void setId(String id);
    
    /**
     * Returns a live copy of the messages interceptor chain. This is 
     * useful when an interceptor wants to modify the interceptor chain on the 
     * fly.
     * 
     * @return the interceptor chain used to process the message
     */
    InterceptorChain getInterceptorChain();
    void setInterceptorChain(InterceptorChain chain);

    /**
     * @return the associated Destination if message is inbound, null otherwise
     */
    Destination getDestination();
    
    Exchange getExchange();

    void setExchange(Exchange exchange);
    
    Collection<Attachment> getAttachments();

    void setAttachments(Collection<Attachment> attachments);
    
    /**
     * Retrieve the encapsulated content as a particular type (a result type
     * if message is outbound, a source type if message is inbound)
     * 
     * @param format the expected content format 
     * @return the encapsulated content
     */    
    <T> T getContent(Class<T> format);

    /**
     * Provide the encapsulated content as a particular type (a result type
     * if message is outbound, a source type if message is inbound)
     * 
     * @param format the provided content format 
     * @param content the content to be encapsulated
     */    
    <T> void setContent(Class<T> format, Object content);
    
    /**
     * @return the set of currently encapsulated content formats
     */
    Set<Class<?>> getContentFormats();
    
    /**
     * Removes a content from a message.  If some contents are completely consumed,
     * removing them is a good idea
     * @param format the format to remove
     */
    <T> void removeContent(Class<T> format);
    
    /**
     * Queries the Message object's metadata for a specific property.
     * 
     * @param key the Message interface's property strings that 
     * correlates to the desired property 
     * @return the property's value
     */
    Object getContextualProperty(String key);   
}
