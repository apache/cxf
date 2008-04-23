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

package org.apache.cxf.binding;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.transport.Destination;

/**
 * A factory interface for creating Bindings from BindingInfo metadata.
 */
public interface BindingFactory {
    
    /**
     * Create a Binding from the BindingInfo metadata.
     * 
     * @param binding
     * @return the Binding object
     */
    Binding createBinding(BindingInfo binding);

    /**
     * Create a "default" BindingInfo object for the service. Can return a subclass. 
     * @param service
     * @param namespace
     * @param configObject - binding specific configuration object
     * @return the BindingInfo object
     */
    BindingInfo createBindingInfo(Service service, String namespace, Object configObject);
    
    /**
     * Set the destionation's message observer which is created by using the endpoint to
     * listen the incoming message
     * @param d the destination that will be set the MessageObserver 
     * @param e the endpoint to build up the MessageObserver      
     */
    void addListener(Destination d, Endpoint e);
}
