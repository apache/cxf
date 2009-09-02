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

package org.apache.cxf.systest.provider;
import java.io.InputStream;

import javax.jws.HandlerChain;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

import org.w3c.dom.Node;

//The following wsdl file is used.
//wsdlLocation = "/trunk/testutils/src/main/resources/wsdl/hello_world_rpc_lit.wsdl"
@WebServiceProvider(portName = "SoapPortProviderRPCLit1", 
                    serviceName = "SOAPServiceProviderRPCLit",
                    targetNamespace = "http://apache.org/hello_world_rpclit",
                    wsdlLocation = "wsdl/hello_world_rpc_lit.wsdl")
@ServiceMode(value = Service.Mode.MESSAGE)      
@HandlerChain(file = "./handlers_invocation.xml", name = "TestHandlerChain")
public class HWSoapMessageProvider implements Provider<SOAPMessage> {

    private static QName sayHi = new QName("http://apache.org/hello_world_rpclit", "sayHi");
    private static QName greetMe = new QName("http://apache.org/hello_world_rpclit", "greetMe");
    private SOAPMessage sayHiResponse;
    private SOAPMessage greetMeResponse;
    
    public HWSoapMessageProvider() {
       
        try {
            MessageFactory factory = MessageFactory.newInstance();            
            InputStream is = getClass().getResourceAsStream("resources/sayHiRpcLiteralResp.xml");
            sayHiResponse =  factory.createMessage(null, is);
            is.close();
            is = getClass().getResourceAsStream("resources/GreetMeRpcLiteralResp.xml");
            greetMeResponse =  factory.createMessage(null, is);
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public SOAPMessage invoke(SOAPMessage request) {
        SOAPMessage response = null;        
        try {
            SOAPBody body = request.getSOAPBody();
            Node n = body.getFirstChild();

            while (n.getNodeType() != Node.ELEMENT_NODE) {
                n = n.getNextSibling();
            }
            if (n.getLocalName().equals(sayHi.getLocalPart())) {
                response = sayHiResponse;
            } else if (n.getLocalName().equals(greetMe.getLocalPart())) {
                response = greetMeResponse;
            } else {
                response = request;
                //response.writeTo(System.out);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return response;
    }
}
