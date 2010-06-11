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
package org.apache.cxf.systest.type_test.soap;


import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;


import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.type_test.TypeTestImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.type_test.doc.TypeTestPortType;
import org.apache.type_test.types1.FixedArray;

public class SOAPDocLitServerImpl extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(SOAPDocLitServerImpl.class);

    public void run()  {
        SpringBusFactory sf = new SpringBusFactory();
        BusFactory.setDefaultBus(null);
        BusFactory.setDefaultBus(
            sf.createBus("org/apache/cxf/systest/type_test/databinding-schema-validation.xml"));
        Object implementor = new SOAPTypeTestImpl();
        String address = "http://localhost:" + PORT + "/SOAPService/SOAPPort/";
        Endpoint.publish(address, implementor);              
    }

    public static void main(String args[]) {
        try {
            SOAPDocLitServerImpl s = new SOAPDocLitServerImpl();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally { 
            System.out.println("done!");
        }
    }
    
    @WebService(serviceName = "SOAPService", portName = "SOAPPort",
                endpointInterface = "org.apache.type_test.doc.TypeTestPortType",
                targetNamespace = "http://apache.org/type_test/doc",
                wsdlLocation = "testutils/type_test/type_test_doclit_soap.wsdl")
    public class SOAPTypeTestImpl extends TypeTestImpl implements TypeTestPortType {
        
        //override so we can test some bad validation things
        public FixedArray testFixedArray(
                                         FixedArray x,
                                         Holder<FixedArray> y,
                                         Holder<FixedArray> z) {
            z.value = new FixedArray();
            z.value.getItem().addAll(y.value.getItem());
            y.value = new FixedArray();
            y.value.getItem().addAll(x.getItem());
            if (x.getItem().get(0) == 24) {
                y.value.getItem().add(0);
                z.value.getItem().remove(0);
            }
            return x;
        }
    }
}
