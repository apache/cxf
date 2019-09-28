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

import org.apache.cxf.systest.type_test.TypeTestImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.type_test.rpc.TypeTestPortType;

public class SOAPRpcLitServerImpl extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(SOAPRpcLitServerImpl.class);

    public void run()  {
        Object implementor = new SOAPTypeTestImpl();
        String address = "http://localhost:" + PORT + "/SOAPService/SOAPPort/";
        Endpoint.publish(address, implementor);
    }

    public static void main(String[] args) {
        try {
            SOAPRpcLitServerImpl s = new SOAPRpcLitServerImpl();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

    @WebService(serviceName = "SOAPService",
                portName = "SOAPPort",
                endpointInterface = "org.apache.type_test.rpc.TypeTestPortType",
                targetNamespace = "http://apache.org/type_test/rpc",
                wsdlLocation = "wsdl/type_test/type_test_rpclit_soap.wsdl")
    class SOAPTypeTestImpl extends TypeTestImpl implements TypeTestPortType {
    }
}
