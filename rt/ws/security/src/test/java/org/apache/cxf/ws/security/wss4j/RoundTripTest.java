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
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.ws.security.handler.WSHandlerConstants;

import org.junit.Before;
import org.junit.Test;

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
        service.getOutInterceptors().add(new SAAJOutInterceptor());

        wsIn = new WSS4JInInterceptor();
        wsIn.setProperty(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/insecurity.properties");
        wsIn.setProperty(WSHandlerConstants.DEC_PROP_FILE, "META-INF/cxf/insecurity.properties");
        wsIn.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());

        service.getInInterceptors().add(wsIn);

        wsOut = new WSS4JOutInterceptor();
        wsOut.setProperty(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        wsOut.setProperty(WSHandlerConstants.ENC_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        wsOut.setProperty(WSHandlerConstants.USER, "myalias");
        wsOut.setProperty("password", "myAliasPassword");
        wsOut.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());
        service.getOutInterceptors().add(wsOut);

        // Create the client
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setServiceClass(Echo.class);
        proxyFac.setAddress("local://Echo");
        proxyFac.getClientFactoryBean().setTransportId(LocalTransportFactory.TRANSPORT_ID);
        
        echo = (Echo)proxyFac.create();

        client = ClientProxy.getClient(echo);
        client.getInInterceptors().add(wsIn);
        client.getInInterceptors().add(new SAAJInInterceptor());
        client.getOutInterceptors().add(wsOut);
        client.getOutInterceptors().add(new SAAJOutInterceptor());
    }

    @Test
    public void testSignature() throws Exception {
        wsIn.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        wsOut.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);

        assertEquals("test", echo.echo("test"));
    }

    @Test
    public void testEncryptionPlusSig() throws Exception {
        wsIn.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT + " "
                                                    + WSHandlerConstants.SIGNATURE);
        wsOut.setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT + " "
                                                     + WSHandlerConstants.SIGNATURE);

        assertEquals("test", echo.echo("test"));
    }
    @Test
    public void testUsernameToken() throws Exception {
        String actions = WSHandlerConstants.ENCRYPT + " " + WSHandlerConstants.SIGNATURE + " "
                         + WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.USERNAME_TOKEN;

        wsIn.setProperty(WSHandlerConstants.ACTION, actions);
        wsOut.setProperty(WSHandlerConstants.ACTION, actions);

        assertEquals("test", echo.echo("test"));
    }
}
