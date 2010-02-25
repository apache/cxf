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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.Destination;

public class MessageImpl extends StringMapImpl implements Message {
    private static final Class<?> DEFAULT_CONTENTS[];
    private static final int DEFAULT_CONTENTS_LENGTH;
    
    static {
        Class<?> tmps[];
        
        try {
            //if SAAJ is there, give it a slot
            Class<?> cls = Class.forName("javax.xml.soap.SOAPMessage");
            tmps = new Class<?>[] {
                XMLStreamReader.class, XMLStreamWriter.class,
                InputStream.class, OutputStream.class,
                List.class, Exception.class, Node.class, DelegatingInputStream.class,
                cls
            };
        } catch (Throwable e) {
            tmps = new Class<?>[] {
                XMLStreamReader.class, XMLStreamWriter.class,
                InputStream.class, OutputStream.class,
                List.class, Exception.class, Node.class, DelegatingInputStream.class
            };
        }
        DEFAULT_CONTENTS = tmps;
        DEFAULT_CONTENTS_LENGTH = tmps.length;
    }
    
    
    private Exchange exchange;
    private String id;
    private InterceptorChain interceptorChain;
    
    private Object[] defaultContents = new Object[DEFAULT_CONTENTS_LENGTH];
    private Map<Class<?>, Object> contents;
    
    public MessageImpl() {
        //nothing
    }
    
    public Collection<Attachment> getAttachments() {
        return CastUtils.cast((Collection<?>)get(ATTACHMENTS));
    }

    public void setAttachments(Collection<Attachment> attachments) {
        put(ATTACHMENTS, attachments);
    }

    public String getAttachmentMimeType() {
        //for sub class overriding
        return null;
    }
    
    public Destination getDestination() {
        return get(Destination.class);
    }

    public Exchange getExchange() {
        return exchange;
    }

    public String getId() {
        return id;
    }

    public InterceptorChain getInterceptorChain() {
        return this.interceptorChain;
    }

    @SuppressWarnings("unchecked")
    public <T> T getContent(Class<T> format) {
        for (int x = 0; x < DEFAULT_CONTENTS_LENGTH; x++) {
            if (DEFAULT_CONTENTS[x] == format) {
                return (T)defaultContents[x];
            }
        }
        return contents == null ? null : (T)contents.get(format);
    }

    public <T> void setContent(Class<T> format, Object content) {
        for (int x = 0; x < DEFAULT_CONTENTS_LENGTH; x++) {
            if (DEFAULT_CONTENTS[x] == format) {
                defaultContents[x] = content;
                return;
            }
        }
        if (contents == null) {
            contents = new IdentityHashMap<Class<?>, Object>(6);
        }
        contents.put(format, content);
    }
    
    public <T> void removeContent(Class<T> format) {
        for (int x = 0; x < DEFAULT_CONTENTS_LENGTH; x++) {
            if (DEFAULT_CONTENTS[x] == format) {
                defaultContents[x] = null;
                return;
            }
        }
        if (contents != null) {
            contents.remove(format);
        }
    }

    public Set<Class<?>> getContentFormats() {
        
        Set<Class<?>> c;
        if (contents == null) {
            c = new HashSet<Class<?>>();
        } else {
            c = new HashSet<Class<?>>(contents.keySet());
        }
        for (int x = 0; x < DEFAULT_CONTENTS_LENGTH; x++) {
            if (defaultContents[x] != null) {
                c.add(DEFAULT_CONTENTS[x]);
            }
        }
        return c;
    }

    public void setDestination(Destination d) {
        put(Destination.class, d);
    }

    public void setExchange(Exchange e) {
        this.exchange = e;
    }

    public void setId(String i) {
        this.id = i;
    }

    public void setInterceptorChain(InterceptorChain ic) {
        this.interceptorChain = ic;
    }

    public Object getContextualProperty(String key) {
        Object val = get(key);
        
        Exchange ex = getExchange();
        if (val == null && ex != null) {
            val = ex.get(key);
        }
        
        if (val == null) {
            OperationInfo ep = get(OperationInfo.class); 
            if (ep != null) {
                val = ep.getProperty(key);
            }
        }
        
        if (val == null && ex != null) {
            Endpoint ep = ex.get(Endpoint.class); 
            if (ep != null) {
                val = ep.get(key);
                
                if (val == null) {
                    val = ep.getEndpointInfo().getProperty(key);
                }

                if (val == null) {
                    val = ep.getEndpointInfo().getBinding().getProperty(key);
                }

            }
            if (val == null) {
                Service sv = ex.get(Service.class); 
                if (sv != null) {
                    val = sv.get(key);
                }
                if (val == null) {
                    Bus bus = ex.get(Bus.class);
                    if (bus != null) {
                        val = bus.getProperty(key);
                    }
                }
            }
        }
        
        return val;
    }
    
    public static void copyContent(Message m1, Message m2) {
        for (Class<?> c : m1.getContentFormats()) {
            m2.setContent(c, m1.getContent(c));
        }
    }
}
