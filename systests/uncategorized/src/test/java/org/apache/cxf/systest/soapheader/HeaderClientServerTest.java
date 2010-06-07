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



package org.apache.cxf.systest.soapheader;

import java.net.URL;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;

import org.apache.cxf.pizza.Pizza;
import org.apache.cxf.pizza.PizzaService;
import org.apache.cxf.pizza.types.CallerIDHeaderType;
import org.apache.cxf.pizza.types.OrderPizzaResponseType;
import org.apache.cxf.pizza.types.OrderPizzaType;
import org.apache.cxf.pizza.types.ToppingsListType;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class HeaderClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = Server.PORT;

    private final QName serviceName = new QName("http://cxf.apache.org/pizza", "PizzaService");

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testBasicConnection() throws Exception {
        Pizza port = getPort();
        updateAddressPort(port, PORT);
        OrderPizzaType req = new OrderPizzaType();
        ToppingsListType t = new ToppingsListType();
        t.getTopping().add("test");
        req.setToppings(t);

        CallerIDHeaderType header = new CallerIDHeaderType();
        header.setName("mao");
        header.setPhoneNumber("108");

        OrderPizzaResponseType res =  port.orderPizza(req, header);

        assertEquals(208, res.getMinutesUntilReady());
    }
    @Test
    public void testBasicConnectionNoHeader() throws Exception {
        PizzaNoHeader port = getPortNoHeader();
        updateAddressPort(port, PORT);

        OrderPizzaType req = new OrderPizzaType();
        ToppingsListType t = new ToppingsListType();
        t.getTopping().add("NoHeader!");
        t.getTopping().add("test");
        req.setToppings(t);

        OrderPizzaResponseType res =  port.orderPizza(req);

        assertEquals(100, res.getMinutesUntilReady());
    }

    private Pizza getPort() {
        URL wsdl = getClass().getResource("/wsdl_systest/pizza_service.wsdl");
        assertNotNull("WSDL is null", wsdl);

        PizzaService service = new PizzaService(wsdl, serviceName);
        assertNotNull("Service is null ", service);

        return service.getPizzaPort();
    }
    
    private PizzaNoHeader getPortNoHeader() {
        URL wsdl = getClass().getResource("/wsdl_systest/pizza_service.wsdl");
        assertNotNull("WSDL is null", wsdl);

        PizzaService service = new PizzaService(wsdl, serviceName);
        assertNotNull("Service is null ", service);

        return service.getPort(PizzaNoHeader.class);
    }
    
    
    @WebService(targetNamespace = "http://cxf.apache.org/pizza", name = "Pizza")
    @XmlSeeAlso({ org.apache.cxf.pizza.types.ObjectFactory.class })
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public interface PizzaNoHeader {

        @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
        @WebResult(name = "OrderResponse",
                   targetNamespace = "http://cxf.apache.org/pizza/types",
                   partName = "body")
        @WebMethod(operationName = "OrderPizza")
        org.apache.cxf.pizza.types.OrderPizzaResponseType orderPizza(
            @WebParam(partName = "body", name = "OrderRequest",
                      targetNamespace = "http://cxf.apache.org/pizza/types")
            org.apache.cxf.pizza.types.OrderPizzaType body
        );
    }

}

