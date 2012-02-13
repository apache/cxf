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
package org.apache.cxf.wsn;

import javax.xml.ws.wsaddressing.W3CEndpointReference;

public abstract class AbstractEndpoint implements EndpointMBean {

    protected final String name;

    protected String address;

    protected EndpointManager manager;

    protected Object endpoint;

    public AbstractEndpoint(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void register() throws EndpointRegistrationException {
        endpoint = manager.register(getAddress(), this);
    }

    public void unregister() throws EndpointRegistrationException {
        if (endpoint != null) {
            manager.unregister(endpoint);
        }
    }

    public W3CEndpointReference getEpr() {
        if (endpoint != null) {
            return manager.getEpr(endpoint);
        }
        return null;
    }

    public EndpointManager getManager() {
        return manager;
    }

    public void setManager(EndpointManager manager) {
        this.manager = manager;
    }

}
