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



package org.apache.cxf.systest.outofband.header;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
//import org.apache.cxf.BusFactory;
//import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(Server.class);

    Bus bus;

    protected void run()  {
        System.setProperty("org.apache.cxf.bus.factory", "org.apache.cxf.bus.CXFBusFactory");
        System.setProperty("cxf.config.file", "org/apache/cxf/systest/outofband/header/cxf.xml");
        
//        // Create bus
//        SpringBusFactory bf = new SpringBusFactory();
//        bus = bf.createBus(OOBHeaderTest.CONFIG_FILE);
//        BusFactory.setDefaultBus(bus);
//        OOBHeaderTest.registerOutOfBandHeaders(bus);
        
        // Register expected Headers (namespace, element and class type mapping)
        
        Object implementor = new OOBHdrServiceImpl();
        Endpoint ep = Endpoint.create(implementor);
        Map<String, Object> props = new HashMap<String, Object>(2);
        props.put(Endpoint.WSDL_SERVICE, new QName("http://apache.org/hello_world_doc_lit_bare", 
                                                   "SOAPService"));
        props.put(Endpoint.WSDL_PORT, new QName("http://apache.org/hello_world_doc_lit_bare", "SoapPort"));
        ep.setProperties(props);
        ep.publish("http://localhost:" + PORT + "/SOAPDocLitBareService/SoapPort");
        
        ep = Endpoint.create(implementor);
        props = new HashMap<String, Object>(2);
        props.put(Endpoint.WSDL_SERVICE, new QName("http://apache.org/hello_world_doc_lit_bare", 
                                                   "SOAPService"));
        props.put(Endpoint.WSDL_PORT, new QName("http://apache.org/hello_world_doc_lit_bare", "SoapPort"));
        props.put("endpoint-processes-headers", "");
        ep.setProperties(props);
        ep.publish("http://localhost:" + PORT + "/SOAPDocLitBareService/SoapPortNoHeader");
        
        ep = Endpoint.create(implementor);
        props = new HashMap<String, Object>(2);
        props.put(Endpoint.WSDL_SERVICE, new QName("http://apache.org/hello_world_doc_lit_bare", 
                                                   "SOAPService"));
        props.put(Endpoint.WSDL_PORT, new QName("http://apache.org/hello_world_doc_lit_bare", "SoapPort"));
        props.put("endpoint-processes-headers", "{http://cxf.apache.org/outofband/Header}outofbandHeader");
        ep.setProperties(props);
        ep.publish("http://localhost:" + PORT + "/SOAPDocLitBareService/SoapPortHeader");

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
