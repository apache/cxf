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

import javax.annotation.Resource;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.DOMUtils;

//The following wsdl file is used.
//wsdlLocation = "/trunk/testutils/src/main/resources/wsdl/hello_world_rpc_lit.wsdl"
@WebServiceProvider(portName = "XMLProviderPort",
        serviceName = "XMLService",
        targetNamespace = "http://apache.org/hello_world_xml_http/wrapped",
        wsdlLocation = "/wsdl/hello_world_xml_wrapped.wsdl")
@ServiceMode(value = Service.Mode.PAYLOAD)
@javax.xml.ws.BindingType(value = "http://cxf.apache.org/bindings/xformat")
public class HWDOMSourcePayloadXMLBindingProvider implements
        Provider<DOMSource> {
    @Resource 
    WebServiceContext ctx;
    
    public HWDOMSourcePayloadXMLBindingProvider() {
    }

    public DOMSource invoke(DOMSource request) {
        if (request != null) {
            QName qn = (QName)ctx.getMessageContext().get(MessageContext.WSDL_OPERATION);
            if (qn == null) {
                throw new RuntimeException("No Operation Name");
            }
        }
        
        DocumentBuilderFactory factory;
        DocumentBuilder builder;
        Document document = null;
        DOMSource response = null;

        try {
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
            InputStream greetMeResponse = getClass().getResourceAsStream(
                    "resources/XML_GreetMeDocLiteralResp.xml");

            document = builder.parse(greetMeResponse);
            DOMUtils.writeXml(document, System.out);
            response = new DOMSource(document.getDocumentElement());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}
