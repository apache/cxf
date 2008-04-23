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
package org.apache.cxf.systest.type_test.xml;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.type_test.TypeTestImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.type_test.xml.TypeTestPortType;

public class XMLServerImpl extends AbstractBusTestServerBase {

    public void run()  {
        SpringBusFactory sf = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        BusFactory.setDefaultBus(
            sf.createBus("org/apache/cxf/systest/type_test/databinding-schema-validation.xml"));

        Object implementor = new XMLTypeTestImpl();
        String address = "http://localhost:9008/XMLService/XMLPort/";
        Endpoint.publish(address, implementor);
    }

    public static void main(String args[]) {
        try { 
            XMLServerImpl s = new XMLServerImpl();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally { 
            System.out.println("done!");
        }
    }
    
    @WebService(serviceName = "XMLService", 
                portName = "XMLPort",
                endpointInterface = "org.apache.type_test.xml.TypeTestPortType",
                targetNamespace = "http://apache.org/type_test/xml",
                wsdlLocation = "testutils/type_test/type_test_xml.wsdl")
    @javax.xml.ws.BindingType(value = "http://cxf.apache.org/bindings/xmlformat")
    class XMLTypeTestImpl extends TypeTestImpl implements TypeTestPortType {
    }
}
