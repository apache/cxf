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
package org.apache.cxf.ws.security.wss4j;

import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.wss4j.common.ConfigurationConstants;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RoundTripTest extends AbstractSecurityTest {
    private WSS4JInInterceptor wsIn;
    private WSS4JOutInterceptor wsOut;
    private Echo echo;
    private Client client;

    @Before
    public void setUpService() throws Exception {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceBean(new EchoImpl());
        factory.setAddress("local://Echo");
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        Server server = factory.create();
        Service service = server.getEndpoint().getService();

        service.getInInterceptors().add(new SAAJInInterceptor());
        service.getInInterceptors().add(new LoggingInInterceptor());
        service.getOutInterceptors().add(new SAAJOutInterceptor());
        service.getOutInterceptors().add(new LoggingOutInterceptor());

        wsIn = new WSS4JInInterceptor();
        wsIn.setProperty(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");
        wsIn.setProperty(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        wsIn.setProperty(ConfigurationConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());

        service.getInInterceptors().add(wsIn);

        wsOut = new WSS4JOutInterceptor();
        wsOut.setProperty(ConfigurationConstants.SIG_PROP_FILE, "outsecurity.properties");
        wsOut.setProperty(ConfigurationConstants.ENC_PROP_FILE, "outsecurity.properties");
        wsOut.setProperty(ConfigurationConstants.USER, "myalias");
        wsOut.setProperty("password", "myAliasPassword");
        wsOut.setProperty(ConfigurationConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());
        service.getOutInterceptors().add(wsOut);

        // Create the client
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setServiceClass(Echo.class);
        proxyFac.setAddress("local://Echo");
        proxyFac.getClientFactoryBean().setTransportId(LocalTransportFactory.TRANSPORT_ID);

        echo = (Echo)proxyFac.create();

        client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getInInterceptors().add(wsIn);
        client.getInInterceptors().add(new SAAJInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        client.getOutInterceptors().add(wsOut);
        client.getOutInterceptors().add(new SAAJOutInterceptor());
    }

    @Test
    public void testSignature() throws Exception {
        wsIn.setProperty(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        wsOut.setProperty(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptionPlusSig() throws Exception {
        wsIn.setProperty(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION + " "
                                                    + ConfigurationConstants.SIGNATURE);
        wsOut.setProperty(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION + " "
                                                     + ConfigurationConstants.SIGNATURE);

        assertEquals("test", echo.echo("test"));
    }
    @Test
    public void testUsernameToken() throws Exception {
        String actions = ConfigurationConstants.ENCRYPTION + " " + ConfigurationConstants.SIGNATURE + " "
                         + ConfigurationConstants.TIMESTAMP + " " + ConfigurationConstants.USERNAME_TOKEN;

        wsIn.setProperty(ConfigurationConstants.ACTION, actions);
        wsOut.setProperty(ConfigurationConstants.ACTION, actions);

        assertEquals("test", echo.echo("test"));
    }
}
