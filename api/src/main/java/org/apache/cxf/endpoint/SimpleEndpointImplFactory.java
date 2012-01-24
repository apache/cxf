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

package org.apache.cxf.endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;

/**
 * Create ordinary EndpointImpl objects.
 */
public class SimpleEndpointImplFactory implements EndpointImplFactory {
    
    private static EndpointImplFactory singleton 
        = new SimpleEndpointImplFactory();

    /** {@inheritDoc}
     */
    public EndpointImpl newEndpointImpl(Bus bus, 
                                        Service service, EndpointInfo endpointInfo) throws EndpointException {
        return new EndpointImpl(bus, service, endpointInfo);
    }

    /**
     * Avoid the need to contruct these objects over and over
     * in cases where the code knows that it needs the basic
     * case.
     * @return
     */
    public static EndpointImplFactory getSingleton() {
        return singleton;
    }
    
}
