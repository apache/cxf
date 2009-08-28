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
package org.apache.cxf.aegis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;

/**
 * Holds information about the message request and response. Applications should not need to 
 * work with this class.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @since Feb 13, 2004
 */
public class Context {
    private AegisContext globalContext;
    private Collection<Attachment> attachments;
    private Fault fault;
    private Map<Class<?>, Object> properties;
    private Map<String, Object> namedProperties;
    
    public Context(AegisContext aegisContext) {
        this.globalContext = aegisContext;
        this.properties = new HashMap<Class<?>, Object>();
    }

    public TypeMapping getTypeMapping() {
        return globalContext.getTypeMapping();
    }

    public Collection<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Collection<Attachment> attachments) {
        this.attachments = attachments;
    }

    public boolean isWriteXsiTypes() {
        return globalContext.isWriteXsiTypes();
    }

    public boolean isReadXsiTypes() {
        return globalContext.isReadXsiTypes();
    }

    public void setFault(Fault fault) {
        this.fault = fault;
    }

    public Fault getFault() {
        return fault;
    }

    public AegisContext getGlobalContext() {
        return globalContext;
    }

    public boolean isMtomEnabled() {
        return globalContext.isMtomEnabled();
    }

    // bus-style properties for internal state management.
    public <T> T getProperty(Class<T> key) {
        return key.cast(properties.get(key));
    }
    
    public void setProperty(Object value) {
        properties.put(value.getClass(), value);
    }
    
    //named properties to solve other problems
    public void setProperty(String name, Object value) {
        namedProperties.put(name, value);
    }
    
    public <T> T getProperty(String name, Class<T> type) {
        return type.cast(namedProperties.get(name));
    }
}
