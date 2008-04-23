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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;



//The following wsdl file is used.
//wsdlLocation = "/trunk/testutils/src/main/resources/wsdl/hello_world_rpc_lit.wsdl"
@WebServiceProvider(portName = "SoapPortProviderRPCLit7", serviceName = "SOAPServiceProviderRPCLit",
                targetNamespace = "http://apache.org/hello_world_rpclit",
wsdlLocation = "/wsdl/hello_world_rpc_lit.wsdl")
@ServiceMode(value = Service.Mode.PAYLOAD)
public class HWStreamSourcePayloadProvider implements Provider<StreamSource> {
    
    private static QName sayHi = new QName("http://apache.org/hello_world_rpclit", "sayHi");
    private static QName greetMe = new QName("http://apache.org/hello_world_rpclit", "greetMe");
    private InputStream sayHiInputStream;
    private InputStream greetMeInputStream;
    private MessageFactory factory;

    public HWStreamSourcePayloadProvider() {

        try {
            factory = MessageFactory.newInstance();
            InputStream is = getClass().getResourceAsStream("resources/sayHiRpcLiteralResp.xml");
            Document sayHiDocument = factory.createMessage(null, is).getSOAPBody().extractContentAsDocument();
            sayHiInputStream = getSOAPBodyStream(sayHiDocument);
            
            InputStream is2 = getClass().getResourceAsStream("resources/GreetMeRpcLiteralResp.xml");
            Document greetMeDocument = 
                factory.createMessage(null, is2).getSOAPBody().extractContentAsDocument();
            greetMeInputStream = getSOAPBodyStream(greetMeDocument);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public StreamSource invoke(StreamSource request) {
        StreamSource response = new StreamSource();
        try {
            DOMResult domResult = new DOMResult();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(request, domResult);
            Node n = domResult.getNode().getFirstChild();

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
    
    private InputStream getSOAPBodyStream(Document doc) throws Exception {
        // Try to get the DOMImplementation from the doc before
        // defaulting to the sun implementation class (which uses
        // sun internal xerces classes not found in the ibm jdk).
        DOMImplementationLS impl = null;
        DOMImplementation docImpl = doc.getImplementation();
        if (docImpl != null && docImpl.hasFeature("LS", "3.0")) {
            impl = (DOMImplementationLS)docImpl.getFeature("LS", "3.0");
        } else {
            System.setProperty(DOMImplementationRegistry.PROPERTY,
                "com.sun.org.apache.xerces.internal.dom.DOMImplementationSourceImpl");
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            impl = (DOMImplementationLS)registry.getDOMImplementation("LS");
        }
        LSOutput output = impl.createLSOutput();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        output.setByteStream(byteArrayOutputStream);
        LSSerializer writer = impl.createLSSerializer();
        writer.write(doc, output);
        byte[] buf = byteArrayOutputStream.toByteArray();
        return new ByteArrayInputStream(buf);
    }

    
}
