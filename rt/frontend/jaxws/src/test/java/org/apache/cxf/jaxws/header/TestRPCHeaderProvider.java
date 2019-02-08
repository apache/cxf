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


package org.apache.cxf.jaxws.header;

import java.io.InputStream;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider(serviceName = "SOAPRPCHeaderService",
                    portName = "SoapRPCHeaderPort",
                    targetNamespace = "http://apache.org/header_test/rpc",
                    wsdlLocation = "testutils/soapheader_rpc.wsdl")
@ServiceMode(value = Service.Mode.MESSAGE)
public class TestRPCHeaderProvider implements Provider<SOAPMessage> {

    private SOAPMessage helloResponse;

    public TestRPCHeaderProvider() {

        try {
            MessageFactory factory = MessageFactory.newInstance();
            InputStream is = getClass().getClassLoader()
                .getResourceAsStream("./soapheader_rpc_provider/sayHelloResponseMsg.xml");
            helloResponse = factory.createMessage(null, is);
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public SOAPMessage invoke(SOAPMessage request) {
        return helloResponse;
    }
}
