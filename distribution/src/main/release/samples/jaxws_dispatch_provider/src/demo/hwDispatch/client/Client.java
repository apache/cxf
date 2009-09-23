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

package demo.hwDispatch.client;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service.Mode;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.hello_world_soap_http.SOAPService1;
import org.apache.hello_world_soap_http.SOAPService2;
import org.apache.hello_world_soap_http.SOAPService3;

public final class Client {

    private Client() {
    }

    public static void main(String args[]) throws Exception {

        if (args.length == 0) {
            System.out.println("please specify wsdl");
            System.exit(1);
        }

        URL wsdlURL;
        File wsdlFile = new File(args[0]);
        if (wsdlFile.exists()) {
            wsdlURL = wsdlFile.toURL();
        } else {
            wsdlURL = new URL(args[0]);
        }

        MessageFactory factory = MessageFactory.newInstance();
        System.out.println(wsdlURL + "\n\n");

        QName serviceName1 = new QName("http://apache.org/hello_world_soap_http", "SOAPService1");
        QName portName1 = new QName("http://apache.org/hello_world_soap_http", "SoapPort1");

        SOAPService1 service1 = new SOAPService1(wsdlURL, serviceName1);
        InputStream is1 =  Client.class.getResourceAsStream("GreetMeDocLiteralReq1.xml");
        if (is1 == null) {
            System.err.println("Failed to create input stream from file "
                               + "GreetMeDocLiteralReq1.xml, please check");
            System.exit(-1);
        }
        SOAPMessage soapReq1 = factory.createMessage(null, is1);

        Dispatch<SOAPMessage> dispSOAPMsg = service1.createDispatch(portName1,
                                                                    SOAPMessage.class, Mode.MESSAGE);

        System.out.println("Invoking server through Dispatch interface using SOAPMessage");
        SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq1);
        System.out.println("Response from server: " + soapResp.getSOAPBody().getTextContent());

        QName serviceName2 = new QName("http://apache.org/hello_world_soap_http", "SOAPService2");
        QName portName2 = new QName("http://apache.org/hello_world_soap_http", "SoapPort2");

        SOAPService2 service2 = new SOAPService2(wsdlURL, serviceName2);
        InputStream is2 =  Client.class.getResourceAsStream("GreetMeDocLiteralReq2.xml");
        if (is2 == null) {
            System.err.println("Failed to create input stream from file "
                               + "GreetMeDocLiteralReq2.xml, please check");
            System.exit(-1);
        }
        SOAPMessage soapReq2 = factory.createMessage(null, is2);
        DOMSource domReqMessage = new DOMSource(soapReq2.getSOAPPart());

        Dispatch<DOMSource> dispDOMSrcMessage = service2.createDispatch(portName2,
                                                                        DOMSource.class, Mode.MESSAGE);
        System.out.println("Invoking server through Dispatch interface using DOMSource in MESSAGE Mode");
        DOMSource domRespMessage = dispDOMSrcMessage.invoke(domReqMessage);
        System.out.println("Response from server: "
                           + domRespMessage.getNode().getLastChild().getTextContent());


        QName serviceName3 = new QName("http://apache.org/hello_world_soap_http", "SOAPService3");
        QName portName3 = new QName("http://apache.org/hello_world_soap_http", "SoapPort3");

        SOAPService3 service3 = new SOAPService3(wsdlURL, serviceName3);
        InputStream is3 =  Client.class.getResourceAsStream("GreetMeDocLiteralReq3.xml");
        if (is3 == null) {
            System.err.println("Failed to create input stream from file "
                               + "GreetMeDocLiteralReq3.xml, please check");
            System.exit(-1);
        }

        SOAPMessage soapReq3 = MessageFactory.newInstance().createMessage(null, is3);
        DOMSource domReqPayload = new DOMSource(soapReq3.getSOAPBody().extractContentAsDocument());

        Dispatch<DOMSource> dispDOMSrcPayload = service3.createDispatch(portName3,
                                                                        DOMSource.class, Mode.PAYLOAD);
        System.out.println("Invoking server through Dispatch interface using DOMSource in PAYLOAD Mode");
        DOMSource domRespPayload = dispDOMSrcPayload.invoke(domReqPayload);

        System.out.println("Response from server: "
                           + fetchElementByName(domRespPayload.getNode(), "greetMeResponse").getTextContent());

        System.exit(0);
    }

    private static Element fetchElementByName(Node parent, String name) {
        if (parent instanceof Element && ((Element)parent).getLocalName().equals(name)) {
            return (Element)parent;
        }
        Element ret = null;        
        Node node = parent.getFirstChild();
        while (node != null) {
            if (node instanceof Element && ((Element)node).getLocalName().equals(name)) {
                ret = (Element)node;
                break;
            }
            node = node.getNextSibling();
        }
        return ret;
    }
}
