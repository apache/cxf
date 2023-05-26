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

package org.apache.cxf.systest.jaxws;

import java.util.ArrayList;
import java.util.List;

import jakarta.jws.WebService;
import jakarta.xml.ws.Response;
import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.RandomStrategy;
import org.apache.cxf.greeter_control.AbstractGreeterImpl;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.types.GreetMeResponse;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JaxwsAsyncFailOverTest  extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ServerNoBodyParts.class, 1);
    static final String PORT2 = allocatePort(ServerNoBodyParts.class, 2);

    public static class Server extends AbstractBusTestServerBase {

        protected void run()  {
            GreeterImpl implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
            jakarta.xml.ws.Endpoint.publish(address, implementor);
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

        @WebService(serviceName = "BasicGreeterService",
                    portName = "GreeterPort",
                    endpointInterface = "org.apache.cxf.greeter_control.Greeter",
                    targetNamespace = "http://cxf.apache.org/greeter_control",
                    wsdlLocation = "testutils/greeter_control.wsdl")
        public class GreeterImpl extends AbstractGreeterImpl {
        }
    }


    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @AfterClass
    public static void stopServers() throws Exception {
        stopAllServers();
    }

    @Test
    public void testUseFailOverOnClient() throws Exception {
        List<String> serviceList = new ArrayList<>();
        serviceList.add("http://localhost:" + PORT + "/SoapContext/GreeterPort");

        RandomStrategy strategy = new RandomStrategy();
        strategy.setAlternateAddresses(serviceList);

        FailoverFeature ff = new FailoverFeature();
        ff.setStrategy(strategy);

        // setup the feature by using JAXWS front-end API
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        // set a fake address to kick off the failover feature
        factory.setAddress("http://localhost:" + PORT2 + "/SoapContext/GreeterPort");
        factory.getFeatures().add(ff);
        factory.setServiceClass(Greeter.class);
        Greeter proxy = factory.create(Greeter.class);

        Response<GreetMeResponse>  response = proxy.greetMeAsync("cxf");
        int waitCount = 0;
        while (!response.isDone() && waitCount < 150) {
            Thread.sleep(100);
            waitCount++;
        }
        assertTrue("Response still not received.", response.isDone());
        //make sure we actually got a proper response and not an exception
        assertEquals("CXF", response.get().getResponseType());
    }

}
