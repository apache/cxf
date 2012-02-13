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

import javax.jms.ConnectionFactory;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

import org.apache.cxf.wsn.jms.JmsNotificationBroker;
import org.apache.cxf.wsn.jms.JmsPublisher;
import org.apache.cxf.wsn.jms.JmsSubscription;

@WebService(endpointInterface = "org.oasis_open.docs.wsn.brw_2.NotificationBroker",
            targetNamespace = "http://docs.oasis-open.org/wsn/brw-2",
            serviceName = "NotificationBroker",
            portName = "NotificationBrokerPort")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class JaxwsNotificationBroker extends JmsNotificationBroker 
    implements JaxwsNotificationBrokerMBean {

    public JaxwsNotificationBroker(String name) {
        super(name);
        manager = new JaxwsEndpointManager();
    }

    public JaxwsNotificationBroker(String name, ConnectionFactory connectionFactory) {
        super(name, connectionFactory);
        manager = new JaxwsEndpointManager();
    }

    @Override
    protected JmsSubscription createJmsSubscription(String name) {
        return new JaxwsSubscription(name);
    }

    @Override
    protected JmsPublisher createJmsPublisher(String name) {
        return new JaxwsPublisher(name, this);
    }
    
}
