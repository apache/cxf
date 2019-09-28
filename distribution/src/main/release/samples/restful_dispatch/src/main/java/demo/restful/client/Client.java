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

package demo.restful.client;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPBinding;

import org.w3c.dom.Document;

import org.apache.cxf.staxutils.StaxUtils;

public final class Client {

    private Client() {
    }

    public static void main(String[] args) throws Exception {
        QName serviceName = new QName("http://apache.org/hello_world_xml_http/wrapped",
                                                "cutomerservice");
        QName portName = new QName("http://apache.org/hello_world_xml_http/wrapped",
                                             "RestProviderPort");
        String endpointAddress = "http://localhost:9000/customerservice/customer";

        // Sent HTTP GET request to query all customer info
        URL url = new URL(endpointAddress);
        System.out.println("Invoking server through HTTP GET to query all customer info");
        InputStream in = url.openStream();
        StreamSource source = new StreamSource(in);
        printSource(source);

        // Sent HTTP GET request to query customer info
        url = new URL(endpointAddress + "?id=1234");
        System.out.println("Invoking server through HTTP GET to query customer info");
        in = url.openStream();
        source = new StreamSource(in);
        printSource(source);

        Service service = Service.create(serviceName);
        service.addPort(portName, HTTPBinding.HTTP_BINDING,  endpointAddress);
        Dispatch<DOMSource> dispatcher = service.createDispatch(portName,
                                                                DOMSource.class, Service.Mode.PAYLOAD);
        Map<String, Object> requestContext = dispatcher.getRequestContext();

        Client client = new Client();
        InputStream is = client.getClass().getResourceAsStream("/CustomerJohnReq.xml");
        Document doc = StaxUtils.read(is);
        DOMSource reqMsg = new DOMSource(doc);

        requestContext.put(MessageContext.HTTP_REQUEST_METHOD, "POST");
        System.out.println("Invoking through HTTP POST to update customer using JAX-WS Dispatch");
        DOMSource result = dispatcher.invoke(reqMsg);
        printSource(result);

        System.out.println("Client Invoking succeeded!");
        System.exit(0);
    }

    private static void printSource(Source source) {
    	System.out.println("**** Response ******");
    	try {
			StaxUtils.copy(source, System.out);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
    	System.out.println();
    }

}
