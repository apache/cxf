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

import java.util.Collections;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.interceptor.security.SimpleAuthorizingInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;

import org.junit.Test;

public class UserNameTokenAuthorizationTest extends AbstractSecurityTest {
    private SimpleSubjectCreatingInterceptor wsIn;
    private WSS4JOutInterceptor wsOut;
    private Echo echo;
    private Client client;

    public void setUpService(String expectedRoles,
                             boolean digest,
                             boolean encryptUsernameTokenOnly) throws Exception {
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

        wsIn = new SimpleSubjectCreatingInterceptor();
        wsIn.setSupportDigestPasswords(digest);
        wsIn.setProperty(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/insecurity.properties");
        wsIn.setProperty(WSHandlerConstants.DEC_PROP_FILE, "META-INF/cxf/insecurity.properties");
        wsIn.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());

        service.getInInterceptors().add(wsIn);
         
        SimpleAuthorizingInterceptor sai = new SimpleAuthorizingInterceptor();
        sai.setMethodRolesMap(Collections.singletonMap("echo", expectedRoles));
        service.getInInterceptors().add(sai);
        
        
        wsOut = new WSS4JOutInterceptor();
        wsOut.setProperty(WSHandlerConstants.SIG_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        wsOut.setProperty(WSHandlerConstants.ENC_PROP_FILE, "META-INF/cxf/outsecurity.properties");
        wsOut.setProperty(WSHandlerConstants.USER, "myalias");
        if (digest) {
            wsOut.setProperty("password", "myAliasPassword");
        } else {
            wsOut.setProperty(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        }
        
        if (encryptUsernameTokenOnly) {
            wsOut.setProperty(WSHandlerConstants.ENCRYPTION_USER, "myalias");
            wsOut.setProperty(
                WSHandlerConstants.ENCRYPTION_PARTS, 
                "{Content}{" + WSConstants.WSSE_NS + "}UsernameToken"
            );
        }
        wsOut.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, TestPwdCallback.class.getName());
        service.getOutInterceptors().add(wsOut);

        // Create the client
        JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
        proxyFac.setServiceClass(Echo.class);
        proxyFac.setAddress("local://Echo");
        proxyFac.getClientFactoryBean().setTransportId(LocalTransportFactory.TRANSPORT_ID);
        
        echo = (Echo)proxyFac.create();

        ((BindingProvider)echo).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);

        
        client = ClientProxy.getClient(echo);
        
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getInInterceptors().add(wsIn);
        client.getInInterceptors().add(new SAAJInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());
        client.getOutInterceptors().add(wsOut);
        client.getOutInterceptors().add(new SAAJOutInterceptor());
    }

    
    @Test
    public void testDigestPasswordAuthorized() throws Exception {
        setUpService("developers", true, false);
        String actions = WSHandlerConstants.ENCRYPT + " " + WSHandlerConstants.SIGNATURE + " "
                         + WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.USERNAME_TOKEN;

        wsIn.setProperty(WSHandlerConstants.ACTION, actions);
        
        wsOut.setProperty(WSHandlerConstants.ACTION, actions);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testDigestPasswordUnauthorized() throws Exception {
        setUpService("managers", true, false);
        String actions = WSHandlerConstants.ENCRYPT + " " + WSHandlerConstants.SIGNATURE + " "
                         + WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.USERNAME_TOKEN;

        wsIn.setProperty(WSHandlerConstants.ACTION, actions);
        
        wsOut.setProperty(WSHandlerConstants.ACTION, actions);

        try {
            echo.echo("test");
            fail("Exception expected");
        } catch (Exception ex) {
            assertEquals("Unauthorized", ex.getMessage());
        }
    }
    
    @Test
    public void testEncryptedDigestPasswordAuthorized() throws Exception {
        setUpService("developers", true, true);
        String actions = WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.ENCRYPT;

        wsIn.setProperty(WSHandlerConstants.ACTION, actions);
        wsOut.setProperty(WSHandlerConstants.ACTION, actions);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testClearPasswordAuthorized() throws Exception {
        setUpService("developers", false, false);
        String actions = WSHandlerConstants.USERNAME_TOKEN;

        wsIn.setProperty(WSHandlerConstants.ACTION, actions);
        wsOut.setProperty(WSHandlerConstants.ACTION, actions);

        assertEquals("test", echo.echo("test"));
    }
    
    @Test
    public void testEncyptedClearPasswordAuthorized() throws Exception {
        setUpService("developers", false, true);
        String actions = WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.ENCRYPT;

        wsIn.setProperty(WSHandlerConstants.ACTION, actions);
        wsOut.setProperty(WSHandlerConstants.ACTION, actions);

        assertEquals("test", echo.echo("test"));
    }
}
