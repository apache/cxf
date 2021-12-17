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
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.annotation.Resource;
import jakarta.xml.soap.Detail;
import jakarta.xml.soap.DetailEntry;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.helpers.DOMUtils;


//The following wsdl file is used.
//wsdlLocation = "/trunk/testutils/src/main/resources/wsdl/hello_world_rpc_lit.wsdl"
@WebServiceProvider(portName = "SoapPortProviderRPCLit3", serviceName = "SOAPServiceProviderRPCLit",
                    targetNamespace = "http://apache.org/hello_world_rpclit",
                    wsdlLocation = "/wsdl/hello_world_rpc_lit.wsdl")
public class HWDOMSourcePayloadProvider implements Provider<DOMSource> {
    private static QName sayHi = new QName("http://apache.org/hello_world_rpclit", "sayHi");
    private static QName greetMe = new QName("http://apache.org/hello_world_rpclit", "greetMe");

    @Resource
    WebServiceContext ctx;

    private Document sayHiResponse;
    private Document greetMeResponse;
    private MessageFactory factory;


    public HWDOMSourcePayloadProvider() {
        try {
            factory = MessageFactory.newInstance();
            InputStream is = getClass().getResourceAsStream("resources/sayHiRpcLiteralResp.xml");
            sayHiResponse = factory.createMessage(null, is).getSOAPBody().extractContentAsDocument();
            is.close();
            is = getClass().getResourceAsStream("resources/GreetMeRpcLiteralResp.xml");
            greetMeResponse = factory.createMessage(null, is).getSOAPBody().extractContentAsDocument();
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public DOMSource invoke(DOMSource request) {
        QName qn = (QName)ctx.getMessageContext().get(MessageContext.WSDL_OPERATION);
        if (qn == null) {
            throw new RuntimeException("No Operation Name");
        }

        DOMSource response = new DOMSource();

        Node n = request.getNode();
        if (n instanceof Document) {
            n = ((Document)n).getDocumentElement();
        }
        if (n.getLocalName().equals(sayHi.getLocalPart())) {
            response.setNode(sayHiResponse);
        } else if (n.getLocalName().equals(greetMe.getLocalPart())) {
            Element el = DOMUtils.getFirstElement(n);
            String s = DOMUtils.getContent(el);
            if ("throwFault".equals(s.trim())) {
                try {
                    SOAPFactory f = SOAPFactory.newInstance();
                    SOAPFault soapFault = f.createFault();

                    soapFault.setFaultString("Test Fault String ****");

                    Detail detail = soapFault.addDetail();
                    detail = soapFault.getDetail();

                    QName qName = new QName("http://www.Hello.org/greeter", "TestFault", "ns");
                    DetailEntry de = detail.addDetailEntry(qName);

                    qName = new QName("http://www.Hello.org/greeter", "ErrorCode", "ns");
                    SOAPElement errorElement = de.addChildElement(qName);
                    errorElement.setTextContent("errorcode");

                    throw new SOAPFaultException(soapFault);
                } catch (SOAPException e) {
                    e.printStackTrace();
                }
            }

            response.setNode(greetMeResponse);
        }
        return response;
    }

}
