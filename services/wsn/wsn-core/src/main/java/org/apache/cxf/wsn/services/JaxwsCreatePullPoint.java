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
package org.apache.cxf.wsn.services;

import java.net.URI;

import jakarta.jms.ConnectionFactory;
import jakarta.jws.WebService;
import jakarta.xml.ws.BindingType;
import org.apache.cxf.wsn.AbstractPullPoint;
import org.apache.cxf.wsn.EndpointManager;
import org.apache.cxf.wsn.jms.JmsCreatePullPoint;

@WebService(endpointInterface = "org.oasis_open.docs.wsn.bw_2.CreatePullPoint",
            targetNamespace = "http://cxf.apache.org/wsn/jaxws",
            serviceName = "CreatePullPointService",
            portName = "CreatePullPointPort")
@BindingType(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class JaxwsCreatePullPoint extends JmsCreatePullPoint
    implements JaxwsCreatePullPointMBean {

    public JaxwsCreatePullPoint(String name) {
        this(name, null, null);
    }
    public JaxwsCreatePullPoint(String name, ConnectionFactory connectionFactory) {
        this(name, connectionFactory, null);
    }

    public JaxwsCreatePullPoint(String name,
                                ConnectionFactory connectionFactory,
                                EndpointManager epManager) {
        super(name, connectionFactory);
        if (epManager == null) {
            manager = new JaxwsEndpointManager();
        } else {
            manager = epManager;
        }
    }

    @Override
    protected AbstractPullPoint createPullPoint(String name) {
        JaxwsPullPoint pullPoint = new JaxwsPullPoint(name);
        pullPoint.setManager(getManager());
        pullPoint.setConnection(connection);
        pullPoint.setAddress(URI.create(getAddress()).resolve("pullpoints/" + name).toString());
        return pullPoint;
    }
}
