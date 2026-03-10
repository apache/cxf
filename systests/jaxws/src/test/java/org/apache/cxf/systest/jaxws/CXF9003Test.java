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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CXF9003Test extends AbstractClientServerTestBase {
    static final String PORT = allocatePort(Server9003.class);

    @WebService(targetNamespace = "urn:hello")
    interface Hello1Service {
        @WebMethod
        String hello(String name);
    }

    @WebService(targetNamespace = "urn:hello")
    public static class Hello1ServiceImpl implements Hello1Service {
        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }

    @WebService(targetNamespace = "urn:hello")
    interface Hello2Service {
        @WebMethod
        String hello(Name name);
    }

    @WebService(targetNamespace = "urn:hello")
    public static class Hello2ServiceImpl implements Hello2Service {
        @Override
        public String hello(Name person) {
            return "Hello " + person.getName();
        }
    }
    public static class Name {
        private String name;

        public Name() {

        }
        public Name(String name) {
            super();
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class Capture implements GeneratedClassClassLoaderCapture {

        private final Map<String, byte[]> sources = new HashMap<>();

        public void capture(String className, byte[] bytes) {
            if (sources.containsKey(className)) {
                throw new IllegalStateException("Class " + className + " defined twice");
            }
            sources.put(className, bytes);
        }
    }


    public static class Server9003 extends AbstractBusTestServerBase {
        protected void run() {
            Object implementor = new Hello1ServiceImpl();
            String address = "http://localhost:" + PORT + "/hello1/service";
            Endpoint.publish(address, implementor);

            Object proxyImpl = new Hello2ServiceImpl();
            String address2 = "http://localhost:" + PORT + "/hello2/service";
            Endpoint.publish(address2, proxyImpl);
        }

        public static void main(String[] args) {
            try {
                Server9003 s = new Server9003();
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
        Bus bus = BusFactory.getDefaultBus();
        Capture c = new Capture();
        bus.setExtension(c, GeneratedClassClassLoaderCapture.class);
        assertTrue("server did not launch correctly", launchServer(Server9003.class, true));
    }

    @Test
    public void generatedNamingClash() throws Exception {

        QName serviceName1 = new QName("urn:hello", "Hello1ServiceImplService");
        URL wsdlURL1 = new URL("http://localhost:" + PORT + "/hello1/service?wsdl");
        Service service1 = Service.create(wsdlURL1, serviceName1);
        Hello1Service helloService1 = service1.getPort(Hello1Service.class);

        QName serviceName2 = new QName("urn:hello", "Hello2ServiceImplService");
        URL wsdlURL2 = new URL("http://localhost:" + PORT + "/hello2/service?wsdl");
        Service service2 = Service.create(wsdlURL2, serviceName2);
        Hello2Service helloService2 = service2.getPort(Hello2Service.class);

        assertEquals(helloService1.hello("Dolly"), "Hello Dolly");
        assertEquals(helloService2.hello(new Name("Joe")), "Hello Joe");
    }

}
