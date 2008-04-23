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

package org.apache.cxf.jaxws.support;

import java.util.Arrays;
import java.util.concurrent.Executor;

import javax.xml.ws.WebServiceFeature;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;

/**
 *
 */

public class JaxWsClientEndpointImpl extends JaxWsEndpointImpl {

    private ServiceImpl executorProvider;
    
    public JaxWsClientEndpointImpl(Bus bus, Service s, EndpointInfo ei, ServiceImpl si, 
                                   WebServiceFeature... wf)
        throws EndpointException {
        super(bus, s, ei, Arrays.asList(wf));
        executorProvider = si;
    }

    @Override
    public Executor getExecutor() {
        Executor e = executorProvider.getExecutor();
        if (null == e) {
            e = super.getExecutor();   
        }
        return e;
    }    
}
