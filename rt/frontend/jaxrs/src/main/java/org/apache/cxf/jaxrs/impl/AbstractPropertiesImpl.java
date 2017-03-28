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
package org.apache.cxf.jaxrs.impl;

import java.util.Collection;

import org.apache.cxf.jaxrs.impl.PropertyHolderFactory.PropertyHolder;
import org.apache.cxf.message.Message;

public abstract class AbstractPropertiesImpl {
    protected Message m;
    private PropertyHolder holder;
    public AbstractPropertiesImpl(Message message) {
        holder = PropertyHolderFactory.getPropertyHolder(message);
        this.m = message;
    }

    public Object getProperty(String name) {
        return holder.getProperty(name);
    }

    public void removeProperty(String name) {
        holder.removeProperty(name);
    }


    public void setProperty(String name, Object value) {
        holder.setProperty(name, value);
    }

    public Collection<String> getPropertyNames() {
        return holder.getPropertyNames();
    }
    
    public Message getMessage() {
        return m;
    }
}
