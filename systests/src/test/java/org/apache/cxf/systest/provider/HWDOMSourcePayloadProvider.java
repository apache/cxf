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
import javax.xml.soap.Detail;
import javax.xml.soap.DetailEntry;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.DOMUtils;


//The following wsdl file is used.
//wsdlLocation = "/trunk/testutils/src/main/resources/wsdl/hello_world_rpc_lit.wsdl"
@WebServiceProvider(portName = "SoapPortProviderRPCLit3", serviceName = "SOAPServiceProviderRPCLit",
                      targetNamespace = "http://apache.org/hello_world_rpclit",
 wsdlLocation = "/wsdl/hello_world_rpc_lit.wsdl")
public class HWDOMSourcePayloadProvider implements Provider<DOMSource> {

    private static QName sayHi = new QName("http://apache.org/hello_world_rpclit", "sayHi");
    private static QName greetMe = new QName("http://apache.org/hello_world_rpclit", "greetMe");
    private Document sayHiResponse;
    private Document greetMeResponse;
    private MessageFactory factory;

    public HWDOMSourcePayloadProvider() {
        try {
            factory = MessageFactory.newInstance();
            InputStream is = getClass().getResourceAsStream("resources/sayHiRpcLiteralResp.xml");
            sayHiResponse =  factory.createMessage(null, is).getSOAPBody().extractContentAsDocument();
            is.close();
            is = getClass().getResourceAsStream("resources/GreetMeRpcLiteralResp.xml");
            greetMeResponse =  factory.createMessage(null, is).getSOAPBody().extractContentAsDocument();
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public DOMSource invoke(DOMSource request) {
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
            if (s.trim().equals("throwFault")) {
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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            response.setNode(greetMeResponse);
        }
        return response;
    }
/*    
    private Node getElementChildNode(SOAPBody body) {
        Node n = body.getFirstChild();

        while (n.getNodeType() != Node.ELEMENT_NODE) {
            n = n.getNextSibling();
        }
        
        return n;        
    }*/
}
