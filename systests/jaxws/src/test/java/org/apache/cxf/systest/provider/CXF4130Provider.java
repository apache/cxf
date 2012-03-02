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

import java.io.StringReader;

import javax.annotation.Resource;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Provider;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceProvider;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

@WebServiceProvider(serviceName = "InBandSoapHeaderService",
    targetNamespace = "http://cxf.apache.org/soapheader/inband", 
    portName = "InBandSoapHeaderSoapHttpPort", 
    wsdlLocation = "/wsdl_systest_jaxws/cxf4130.wsdl")
@ServiceMode(value = javax.xml.ws.Service.Mode.MESSAGE)
public class CXF4130Provider implements Provider<SOAPMessage> {

    @Resource
    protected WebServiceContext context;

    public SOAPMessage invoke(SOAPMessage request) {
        try {
            Document soapBodyDomDocument = request.getSOAPBody().extractContentAsDocument();
            Node node = soapBodyDomDocument.getDocumentElement();
            String requestMsgName = node.getLocalName();
            String responseText = null;

            if ("FooRequest".equals(requestMsgName)) {
                responseText = "<SOAP-ENV:Envelope "
                               + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                               + "<SOAP-ENV:Header>"
                               + "<FooResponseHeader xmlns:ns2=\"http://cxf.apache.org/soapheader/inband\">"
                               + "FooResponseHeader</FooResponseHeader>"
                               + "</SOAP-ENV:Header>"
                               + "<SOAP-ENV:Body>"
                               + "<ns2:FooResponse xmlns:ns2=\"http://cxf.apache.org/soapheader/inband\">"
                               + "<ns2:Return>Foo Response Body</ns2:Return>"
                               + "</ns2:FooResponse>"
                               + "</SOAP-ENV:Body>" 
                               + "</SOAP-ENV:Envelope>\n";

            } else {
                throw new WebServiceException("Error in InBand Provider JAX-WS service -- Unknown Request: "
                                              + requestMsgName);
            }

            // Create a SOAP request message
            MessageFactory soapmsgfactory = MessageFactory.newInstance();
            SOAPMessage responseMessage = soapmsgfactory.createMessage();
            StreamSource responseMessageSrc = null;

            responseMessageSrc = new StreamSource(new StringReader(responseText));
            responseMessage.getSOAPPart().setContent(responseMessageSrc);
            responseMessage.saveChanges();

            return responseMessage;

        } catch (Exception e) {
            throw new WebServiceException(e);
        }

    }

}
