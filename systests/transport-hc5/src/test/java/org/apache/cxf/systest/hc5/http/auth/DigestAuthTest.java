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

package org.apache.cxf.systest.hc5.http.auth;


import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPException;
import org.apache.cxf.transport.http.asyncclient.hc5.AsyncHTTPConduit;
import org.apache.cxf.transport.http.auth.DigestAuthSupplier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.services.SOAPService;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.cxf.systest.hc5.IsAsyncHttpConduit.isInstanceOfAsyncHttpConduit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class DigestAuthTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(DigestServer.class);

    private final QName serviceName =
        new QName("http://apache.org/hello_world", "SOAPService");
    private final QName mortimerQ =
        new QName("http://apache.org/hello_world", "Mortimer");

    @BeforeClass
    public static void startServer() throws Exception {
        launchServer(DigestServer.class, true);
        createStaticBus();
    }

    @Test
    public void testDigestAuth() throws Exception {
        final Greeter mortimer = setupClient(false);
        final String answer = mortimer.sayHi();
        assertEquals("Unexpected answer: " + answer, "Hi", answer);
    }

    @Test
    public void testDigestAuthAsync() throws Exception {
        final Greeter mortimer = setupClient(true);
        final String answer = mortimer.sayHi();
        assertEquals("Unexpected answer: " + answer, "Hi", answer);
    }

    @Test
    public void testNoAuth() throws Exception {
        URL wsdl = getClass().getResource("../greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter mortimer = service.getPort(mortimerQ, Greeter.class);
        assertNotNull("Port is null", mortimer);

        TestUtil.setAddress(mortimer, "http://localhost:" + PORT + "/digestauth/greeter");

        try {
            String answer = mortimer.sayHi();
            fail("Unexpected reply (" + answer + "). Should throw exception");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertEquals(HTTPException.class, cause.getClass());
            HTTPException he = (HTTPException)cause;
            assertEquals(401, he.getResponseCode());
        }
    }

    private Greeter setupClient(boolean async) throws Exception {
        URL wsdl = getClass().getResource("../greeting.wsdl");
        assertNotNull("WSDL is null", wsdl);
        
        SOAPService service = new SOAPService(wsdl, serviceName);
        assertNotNull("Service is null", service);

        Greeter mortimer = service.getPort(mortimerQ, Greeter.class);
        assertNotNull("Port is null", mortimer);

        BindingProvider bp = (BindingProvider)mortimer;
        ClientProxy.getClient(mortimer).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(mortimer).getOutInterceptors().add(new LoggingOutInterceptor());
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                "http://localhost:" + PORT + "/digestauth/greeter");
        HTTPConduit cond = (HTTPConduit)ClientProxy.getClient(mortimer).getConduit();
        HTTPClientPolicy client = new HTTPClientPolicy();
        client.setConnectionTimeout(600000);
        client.setReceiveTimeout(600000);
        cond.setClient(client);
        if (async) {
            assertThat("Not an async conduit", cond, isInstanceOfAsyncHttpConduit());
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials("foo", "bar".toCharArray());
            bp.getRequestContext().put(Credentials.class.getName(), creds);
            bp.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);
            client.setAutoRedirect(true);
        } else {
            bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "foo");
            bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "bar");
            cond.setAuthSupplier(new DigestAuthSupplier());
        }

        ClientProxy.getClient(mortimer).getOutInterceptors()
            .add(new AbstractPhaseInterceptor<Message>(Phase.PRE_STREAM_ENDING) {

                public void handleMessage(Message message) throws Fault {
                    Map<String, ?> headers = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
                    if (headers.containsKey("Proxy-Authorization")) {
                        throw new RuntimeException("Should not have Proxy-Authorization");
                    }
                }
            });
        client.setAllowChunking(false);
        return mortimer;
    }
}

