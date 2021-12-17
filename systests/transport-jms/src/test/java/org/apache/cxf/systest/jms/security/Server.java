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
package org.apache.cxf.systest.jms.security;

import java.util.HashMap;
import java.util.Map;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;

public class Server extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(Server.class);

    EmbeddedJMSBrokerLauncher broker;
    public Server(EmbeddedJMSBrokerLauncher b) {
        broker = b;
    }

    protected void run()  {
        Bus bus = BusFactory.getDefaultBus();
        setBus(bus);

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SAML_TOKEN_SIGNED);
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new KeystorePasswordCallback());
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "bob.properties");

        WSS4JInInterceptor inInterceptor = new WSS4JInInterceptor(inProperties);

        bus.getInInterceptors().add(inInterceptor);

        broker.updateWsdl(bus, "testutils/jms_test.wsdl");

        Endpoint.publish(null, new SecurityGreeterImplTwoWayJMS());
    }

}
