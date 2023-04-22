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

package org.apache.cxf.systest.ws.rm;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.handler.MessageContext;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.systest.ws.util.ConnectionHelper;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.ws.rm.RMEndpoint;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class SequenceTimeoutTest extends AbstractBusClientServerTestBase {
    public static final String PORT = TestUtil.getPortNumber(SequenceTimeoutTest.class);
    private static final String ADDRESS = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
    private static final QName GREETME_NAME
        = new QName("http://cxf.apache.org/greeter_control", "greetMe");
    private static final QName GREETME_SERVICE_NAME
        = new QName("http://cxf.apache.org/greeter_control", "GreeterService");

    private static RMManager rmManager;

    private Bus greeterBus;
    private Greeter greeter;


    public static class Server extends AbstractBusTestServerBase {
        Endpoint endpoint;

        protected void run()  {
            SpringBusFactory bf = new SpringBusFactory();
            System.setProperty("db.name", "rdbm");
            Bus bus = bf.createBus("org/apache/cxf/systest/ws/rm/rminterceptors.xml");
            System.clearProperty("db.name");
            BusFactory.setDefaultBus(bus);

            setBus(bus);

            rmManager = bus.getExtension(RMManager.class);
            rmManager.getConfiguration().setInactivityTimeout(1000L);

            //System.out.println("Created control bus " + bus);
            GreeterImpl greeterImplementor = new GreeterImpl();
            endpoint = Endpoint.publish(ADDRESS, greeterImplementor);

            BusFactory.setDefaultBus(null);
            BusFactory.setThreadDefaultBus(null);
        }

        public void tearDown() throws Exception {
            endpoint.stop();
        }
    }


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }


    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    private void init(String cfgResource, boolean useDispatchClient) {
        init(cfgResource, useDispatchClient, null);
    }

    private void init(String cfgResource,
                      boolean useDispatchClient,
                      Executor executor) {

        SpringBusFactory bf = new SpringBusFactory();
        initGreeterBus(bf, cfgResource);
        if (useDispatchClient) {
            initDispatch();
        } else {
            initProxy(executor);
        }
    }
    private void initGreeterBus(SpringBusFactory bf,
                                String cfgResource) {
        greeterBus = bf.createBus(cfgResource);
        BusFactory.setDefaultBus(greeterBus);
    }


    private Dispatch<DOMSource> initDispatch() {
        GreeterService gs = new GreeterService();
        Dispatch<DOMSource> dispatch = gs.createDispatch(GreeterService.GreeterPort,
                                                         DOMSource.class,
                                                         Service.Mode.MESSAGE);
        try {
            updateAddressPort(dispatch, PORT);
        } catch (Exception e) {
            //ignore
        }
        dispatch.getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, Boolean.FALSE);
        dispatch.getRequestContext().put(MessageContext.WSDL_OPERATION, GREETME_NAME);

        return dispatch;
    }

    private void initProxy(Executor executor) {
        GreeterService gs = new GreeterService();

        if (null != executor) {
            gs.setExecutor(executor);
        }

        greeter = gs.getGreeterPort();
        try {
            updateAddressPort(greeter, PORT);
        } catch (Exception e) {
            //ignore
        }

        ConnectionHelper.setKeepAliveConnection(greeter, true);
    }
    @Test
    public void testTimeout() throws Exception {
        init("org/apache/cxf/systest/ws/rm/rminterceptors.xml", true);

        List<Dispatch<DOMSource>> dispatches = new ArrayList<>(5);
        int count = 5;
        for (int x = 0; x < count; x++) {
            Dispatch<DOMSource> dispatch = initDispatch();
            AcksPolicyType ap = new AcksPolicyType();
            //don't send the acks to cause a memory leak - CXF-7096
            ap.setImmediaAcksTimeout(500000L);
            greeterBus.getExtension(RMManager.class).getDestinationPolicy().setAcksPolicy(ap);
            dispatch.invoke(getDOMRequest("One"));
            dispatches.add(dispatch);
        }
        RMEndpoint ep = rmManager.findReliableEndpoint(GREETME_SERVICE_NAME);
        Assert.assertNotNull(ep);
        Assert.assertEquals(count, ep.getDestination().getAllSequences().size());
        Assert.assertEquals(count, ep.getSource().getAllSequences().size());
        Thread.sleep(2500);
        System.gc();
        Assert.assertEquals(0, ep.getDestination().getAllSequences().size());
        Assert.assertEquals(0, ep.getSource().getAllSequences().size());
        try {
            dispatches.get(0).invoke(getDOMRequest("One"));
            fail("The sequence should have been terminated");
        } catch (Throwable t) {
            //expected
            assertTrue(t.getMessage().contains("not a known Sequence identifier"));
        }
        rmManager.getStore();
    }
    private DOMSource getDOMRequest(String n)
        throws Exception {
        InputStream is =
            getClass().getResourceAsStream("twoway"
                                           + "Req" + n + ".xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document newDoc = builder.parse(is);
        return new DOMSource(newDoc);
    }
}
