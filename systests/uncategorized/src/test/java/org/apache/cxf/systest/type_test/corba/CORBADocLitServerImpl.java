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
package org.apache.cxf.systest.type_test.corba;


import javax.jws.WebService;
import javax.xml.ws.Endpoint;


import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.type_test.TypeTestImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.type_test.doc.TypeTestPortType;

public class CORBADocLitServerImpl extends AbstractBusTestServerBase {

    public void run()  {
        SpringBusFactory sf = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        BusFactory.setDefaultBus(
            sf.createBus("org/apache/cxf/systest/type_test/databinding-schema-validation.xml"));
        Object implementor = new CORBATypeTestImpl();
        String address = "file:./TypeTest.ref";
        Endpoint.publish(address, implementor);              
    }
    
    public static void main(String args[]) {
        try {
            CORBADocLitServerImpl s = new CORBADocLitServerImpl();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally { 
            System.out.println("done!");
        }
    }    

    @WebService(serviceName = "TypeTestCORBAService", portName = "TypeTestCORBAPort",
                endpointInterface = "org.apache.type_test.doc.TypeTestPortType",
                targetNamespace = "http://apache.org/type_test/doc",
                wsdlLocation = "classpath:/wsdl_systest/type_test_corba/type_test_corba-corba.wsdl")
    class CORBATypeTestImpl extends TypeTestImpl implements TypeTestPortType {
    }
}
