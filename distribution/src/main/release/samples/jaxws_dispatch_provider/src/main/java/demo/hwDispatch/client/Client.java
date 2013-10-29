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

import org.example.newwsdlfile.NewWSDLFile_Service;

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
            wsdlURL = wsdlFile.toURI().toURL();
        } else {
            wsdlURL = new URL(args[0]);
        }

        MessageFactory factory = MessageFactory.newInstance();
        System.out.println(wsdlURL + "\n\n");

        QName serviceName1 = new QName("http://www.example.org/NewWSDLFile/", "NewWSDLFile");
        QName portName1 = new QName("http://www.example.org/NewWSDLFile/", "NewWSDLFileSOAP");

        NewWSDLFile_Service service1 = new NewWSDLFile_Service(wsdlURL, serviceName1);
        InputStream is1 =  Client.class.getResourceAsStream("/NoteDocLiteralReq.xml");
        if (is1 == null) {
            System.err.println("Failed to create input stream from file "
                               + "NoteDocLiteralReq.xml, please check");
            System.exit(-1);
        }
        SOAPMessage soapReq1 = factory.createMessage(null, is1);

        Dispatch<SOAPMessage> dispSOAPMsg = service1.createDispatch(portName1,
                                                                    SOAPMessage.class, Mode.MESSAGE);

        System.out.println("Invoking server through Dispatch interface using SOAPMessage");
        SOAPMessage soapResp = dispSOAPMsg.invoke(soapReq1);
        System.out.println("Response from server: " + soapResp.getSOAPBody().getTextContent());

	NewWSDLFile_Service service2 = new NewWSDLFile_Service(wsdlURL, serviceName1);
        InputStream is2 =  Client.class.getResourceAsStream("/NoteDocLiteralReq_doesnt_match_schema.xml");
        if (is2 == null) {
            System.err.println("Failed to create input stream from file "
                               + "NoteDocLiteralReq_doesnt_match_schema.xml, please check");
            System.exit(-1);
        }
        SOAPMessage soapReq2 = factory.createMessage(null, is2);

        Dispatch<SOAPMessage> dispSOAPMsg2 = service2.createDispatch(portName1,
                                                                    SOAPMessage.class, Mode.MESSAGE);

        System.out.println("Invoking server through Dispatch interface using SOAPMessage");
        SOAPMessage soapResp2 = dispSOAPMsg2.invoke(soapReq2);
        System.out.println("Response from server: " + soapResp2.getSOAPBody().getTextContent());

        System.exit(0);
    }

    private static Element fetchElementByName(Node parent, String name) {
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
