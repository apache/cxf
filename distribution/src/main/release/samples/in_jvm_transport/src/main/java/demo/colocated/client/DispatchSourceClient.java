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

package demo.colocated.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.SOAPFaultException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import demo.colocated.server.Server;

public final class DispatchSourceClient {
    private static final String ADDRESS = "http://localhost:9000/SoapContext/GreeterPort";

    private static final String SERVICE_NS = "http://apache.org/hello_world_soap_http"; 
    private static final QName SERVICE_NAME = new QName(SERVICE_NS, "SOAPService");
    private static final QName PORT_NAME = new QName(SERVICE_NS, "SoapPort");
    private static final String PAYLOAD_NAMESPACE_URI = "http://apache.org/hello_world_soap_http/types";

    private static final String SAYHI_REQUEST_TEMPLATE 
        = "<ns1:sayHi xmlns:ns1=\"http://apache.org/hello_world_soap_http/types\" />";
    private static final String GREETME_REQUEST_TEMPLATE 
        = "<ns1:greetMe xmlns:ns1=\"http://apache.org/hello_world_soap_http/types\">"
            + "<ns1:requestType>%s</ns1:requestType></ns1:greetMe>";
    private static final String PINGME_REQUEST_TEMPLATE 
        = "<ns1:pingMe xmlns:ns1=\"http://apache.org/hello_world_soap_http/types\" />";

    private static final QName SAYHI_OPERATION_NAME = new QName(SERVICE_NS, "sayHi");
    private static final QName GREETME_OPERATION_NAME = new QName(SERVICE_NS, "greetMe");
    private static final QName PINGME_OPERATION_NAME = new QName(SERVICE_NS, "pingMe");
        


    private DispatchSourceClient() {
    }

    public static void main(String args[]) throws Exception {

        Server.main(new String[]{"inProcess"});
        
        Service service = Service.create(SERVICE_NAME);
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, ADDRESS);
        
        Dispatch<Source> dispatch = service.createDispatch(PORT_NAME, Source.class, Service.Mode.PAYLOAD);
        
        String resp;
        Source response;
        
        System.out.println("Invoking sayHi...");
        setOperation(dispatch, SAYHI_OPERATION_NAME);
        response = dispatch.invoke(encodeSource(SAYHI_REQUEST_TEMPLATE, null));
        resp = decodeSource(response, PAYLOAD_NAMESPACE_URI, "responseType");
        System.out.println("Server responded with: " + resp);
        System.out.println();

        System.out.println("Invoking greetMe...");
        setOperation(dispatch, GREETME_OPERATION_NAME);
        response = dispatch.invoke(encodeSource(GREETME_REQUEST_TEMPLATE, System.getProperty("user.name")));
        resp = decodeSource(response, PAYLOAD_NAMESPACE_URI, "responseType");
        System.out.println("Server responded with: " + resp);
        System.out.println();

        try {
            System.out.println("Invoking pingMe, expecting exception...");
            setOperation(dispatch, PINGME_OPERATION_NAME);
            response = dispatch.invoke(encodeSource(PINGME_REQUEST_TEMPLATE, null));
        } catch (SOAPFaultException ex) {
            System.out.println("Expected exception: SoapFault has occurred: " + ex.getMessage());
        }
        System.exit(0);
    }
    
    private static void setOperation(Dispatch<Source> dispatch, QName operationName) {
        dispatch.getRequestContext().put(MessageContext.WSDL_OPERATION, operationName);        
    }

    private static Source encodeSource(String template, String value) throws IOException {
        String payload = value == null ? template : String.format(template, value);
        Source source = new StreamSource(new ByteArrayInputStream(payload.getBytes("utf-8")));
        return source;
    }

    private static String decodeSource(Source source, String uri, String name) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformerFactory.newTransformer();
        ContentHandler handler = new ContentHandler(uri, name);
        transformer.transform(source, new SAXResult(handler));
        return handler.getValue();
    }
    
    static class ContentHandler extends DefaultHandler {
        StringBuffer buffer;
        String namespaceURI;
        String elementName;
        boolean recording;
        
        ContentHandler(String namespaceURI, String elementName) {
            this.namespaceURI = namespaceURI;
            this.elementName = elementName;
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, 
         *             java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
            if (namespaceURI.equals(uri) && elementName.equals(localName)) {
                recording = true;
            }
        }


        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, 
         *                            java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (namespaceURI.equals(uri) && elementName.equals(localName)) {
                recording = false;
            }
        }

        /* (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         */
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (recording) {
                if (buffer == null) {
                    buffer = new StringBuffer();
                }
                buffer.append(new String(ch, start, length));
            }
        }

        /**
         * @return
         */
        public String getValue() {
            return buffer == null ? null : buffer.toString();
        }
    }    
}
