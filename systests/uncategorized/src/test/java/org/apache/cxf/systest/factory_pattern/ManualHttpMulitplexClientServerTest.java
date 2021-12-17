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

package org.apache.cxf.systest.factory_pattern;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.factory_pattern.IsEvenResponse;
import org.apache.cxf.factory_pattern.Number;
import org.apache.cxf.factory_pattern.NumberFactory;
import org.apache.cxf.factory_pattern.NumberFactoryService;
import org.apache.cxf.factory_pattern.NumberService;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.apache.cxf.jaxws.support.ServiceDelegateAccessor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ManualHttpMulitplexClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = TestUtil.getPortNumber(ManualHttpMulitplexClientServerTest.class);
    public static final String FACTORY_ADDRESS =
        "http://localhost:" + PORT + "/NumberFactoryService/NumberFactoryPort";

    public static class Server extends AbstractBusTestServerBase {
        Endpoint ep;
        ManualNumberFactoryImpl implementor;
        protected void run() {
            setBus(BusFactory.getDefaultBus());
            implementor = new ManualNumberFactoryImpl(getBus(), PORT);
            ep = Endpoint.publish(FACTORY_ADDRESS, implementor);
        }
        public void tearDown() throws Exception {
            ep.stop();
            ep = null;
            implementor.stop();
            implementor = null;
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, true));
        createStaticBus();
    }


    @Test
    public void testWithManualMultiplexEprCreation() throws Exception {

        NumberFactoryService service = new NumberFactoryService();
        NumberFactory nfact = service.getNumberFactoryPort();
        updateAddressPort(nfact, PORT);

        W3CEndpointReference w3cEpr = nfact.create("2");
        assertNotNull("reference", w3cEpr);

        // use the epr info only
        // no wsdl so default generated soap/http binding will be used
        // address url must come from the calling context
        EndpointReferenceType epr = ProviderImpl.convertToInternal(w3cEpr);
        QName serviceName = EndpointReferenceUtils.getServiceName(epr, bus);
        Service numService = Service.create(serviceName);

        String portString = EndpointReferenceUtils.getPortName(epr);
        QName portName = new QName(serviceName.getNamespaceURI(), portString);
        numService.addPort(portName, SoapBindingFactory.SOAP_11_BINDING, "http://foo");
        Number num = numService.getPort(portName, Number.class);

        setupContextWithEprAddress(epr, num);

        IsEvenResponse numResp = num.isEven();
        assertTrue("2 is even", numResp.isEven());

        // try again with the address from another epr
        w3cEpr = nfact.create("3");
        epr = ProviderImpl.convertToInternal(w3cEpr);
        setupContextWithEprAddress(epr, num);
        numResp = num.isEven();
        assertFalse("3 is not even", numResp.isEven());

        // try again with the address from another epr
        w3cEpr = nfact.create("6");
        epr = ProviderImpl.convertToInternal(w3cEpr);
        setupContextWithEprAddress(epr, num);
        numResp = num.isEven();
        assertTrue("6 is even", numResp.isEven());
    }

    @Test
    public void testWithGetPortExtensionHttp() throws Exception {

        NumberFactoryService service = new NumberFactoryService();
        NumberFactory factory = service.getNumberFactoryPort();
        updateAddressPort(factory, PORT);


        W3CEndpointReference w3cEpr = factory.create("20");
        EndpointReferenceType numberTwoRef = ProviderImpl.convertToInternal(w3cEpr);
        assertNotNull("reference", numberTwoRef);

        // use getPort with epr api on service
        NumberService numService = new NumberService();
        ServiceImpl serviceImpl = ServiceDelegateAccessor.get(numService);

        Number num = serviceImpl.getPort(numberTwoRef, Number.class);
        assertTrue("20 is even", num.isEven().isEven());
        w3cEpr = factory.create("23");
        EndpointReferenceType numberTwentyThreeRef = ProviderImpl.convertToInternal(w3cEpr);
        num = serviceImpl.getPort(numberTwentyThreeRef, Number.class);
        assertFalse("23 is not even", num.isEven().isEven());
    }

    private void setupContextWithEprAddress(EndpointReferenceType epr, Number num) {

        String address = EndpointReferenceUtils.getAddress(epr);

        InvocationHandler handler = Proxy.getInvocationHandler(num);
        BindingProvider bp = null;
        if (handler instanceof BindingProvider) {
            bp = (BindingProvider)handler;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);
        }
    }
}
