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

package org.apache.cxf.systest.source;

import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.hello_world_soap_http_source.source.GreetMeFault;
import org.apache.hello_world_soap_http_source.source.Greeter;
import org.apache.hello_world_soap_http_source.source.PingMeFault;

@jakarta.jws.WebService(portName = "SoapPort", serviceName = "SOAPService",
                      targetNamespace = "http://apache.org/hello_world_soap_http_source/source",
                      endpointInterface = "org.apache.hello_world_soap_http_source.source.Greeter")
public class GreeterImpl implements Greeter {

    public void greetMeOneWay(DOMSource in) {

    }

    public DOMSource sayHi(DOMSource in) {
        Document doc = DOMUtils.newDocument();
        Element el = doc.createElementNS("http://apache.org/hello_world_soap_http_source/source/types",
            "ns1:sayHiResponse");
        Element el2 = doc.createElementNS("http://apache.org/hello_world_soap_http_source/source/types",
            "ns1:responseType");
        el2.appendChild(doc.createTextNode("Bonjour"));
        el.appendChild(el2);
        doc.appendChild(el);
        return new DOMSource(doc);
    }
    private Element getElement(Node nd) {
        if (nd instanceof Document) {
            return ((Document)nd).getDocumentElement();
        }
        return (Element)nd;
    }

    public DOMSource greetMe(DOMSource in) throws GreetMeFault {
        Element eval = getElement(in.getNode());
        eval = DOMUtils.getFirstElement(eval);
        String val = DOMUtils.getContent(eval);
        if ("fault".equals(val)) {
            Document doc = DOMUtils.newDocument();
            Element el = doc.createElementNS("http://apache.org/hello_world_soap_http_source/"
                                             + "source/types",
                    "ns1:greetMeFaultDetail");
            el.appendChild(doc.createTextNode("Some fault detail"));
            doc.appendChild(el);

            throw new GreetMeFault("Fault String", new DOMSource(doc));
        }

        Document doc = DOMUtils.newDocument();
        Element el = doc.createElementNS("http://apache.org/hello_world_soap_http_source/source/types",
            "ns1:greetMeResponse");
        Element el2 = doc.createElementNS("http://apache.org/hello_world_soap_http_source/source/types",
            "ns1:responseType");
        el2.appendChild(doc.createTextNode("Hello " + val));
        el.appendChild(el2);

        doc.appendChild(el);

        return new DOMSource(doc);
    }

    public void pingMe() throws PingMeFault {

    }


}
