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

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Node;

import jakarta.annotation.Resource;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.handler.MessageContext;


//The following wsdl file is used.
//wsdlLocation = "/trunk/testutils/src/main/resources/wsdl/hello_world_rpc_lit.wsdl"
@WebServiceProvider(portName = "SoapPortProviderRPCLit5", serviceName = "SOAPServiceProviderRPCLit",
                  targetNamespace = "http://apache.org/hello_world_rpclit",
                  wsdlLocation = "/wsdl/hello_world_rpc_lit.wsdl")
@ServiceMode(value = Service.Mode.MESSAGE)
public class HWStreamSourceMessageProvider implements Provider<StreamSource> {

    private static QName sayHi = new QName("http://apache.org/hello_world_rpclit", "sayHi");
    private static QName greetMe = new QName("http://apache.org/hello_world_rpclit", "greetMe");
    @Resource
    WebServiceContext ctx;

    private InputStream sayHiInputStream;
    private InputStream greetMeInputStream;
    private MessageFactory factory;

    public HWStreamSourceMessageProvider() {

        try {
            factory = MessageFactory.newInstance();
            sayHiInputStream = getClass().getResourceAsStream("resources/sayHiRpcLiteralResp.xml");
            greetMeInputStream = getClass().getResourceAsStream("resources/GreetMeRpcLiteralResp.xml");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public StreamSource invoke(StreamSource request) {
        QName qn = (QName)ctx.getMessageContext().get(MessageContext.WSDL_OPERATION);
        if (qn == null) {
            throw new RuntimeException("No Operation Name");
        }

        StreamSource response = new StreamSource();
        try {
            SOAPMessage msg = factory.createMessage();
            msg.getSOAPPart().setContent(request);
            SOAPBody body = msg.getSOAPBody();
            Node n = body.getFirstChild();

            while (n.getNodeType() != Node.ELEMENT_NODE) {
                n = n.getNextSibling();
            }
            if (n.getLocalName().equals(sayHi.getLocalPart())) {
                response.setInputStream(sayHiInputStream);
            } else if (n.getLocalName().equals(greetMe.getLocalPart())) {
                response.setInputStream(greetMeInputStream);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return response;
    }

}
