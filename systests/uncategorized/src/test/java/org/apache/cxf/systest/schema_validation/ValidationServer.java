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

package org.apache.cxf.systest.schema_validation;


import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceProvider;
import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.ext.logging.Logging;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.schema_validation.types.ComplexStruct;

public class ValidationServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(ValidationServer.class);

    List<Endpoint> eps = new LinkedList<>();

    public ValidationServer() {
    }

    protected void run() {
        Object implementor = new SchemaValidationImpl();
        String address = "http://localhost:" + PORT + "/SoapContext";
        eps.add(Endpoint.publish(address + "/SoapPort", implementor));
        eps.add(Endpoint.publish(address + "/SoapPortValidate", new ValidatingSchemaValidationImpl()));
        eps.add(Endpoint.publish(address + "/PProvider", new PayloadProvider()));
        eps.add(Endpoint.publish(address + "/MProvider", new MessageProvider()));
        eps.add(Endpoint.publish(address + "/SoapPortMethodValidate", new ValidatingSchemaValidationMethodImpl()));
    }

    public void tearDown() throws Exception {
        while (!eps.isEmpty()) {
            eps.remove(0).stop();
        }
    }

    @WebService(serviceName = "SchemaValidationService",
        portName = "SoapPort",
        endpointInterface = "org.apache.schema_validation.SchemaValidation",
        targetNamespace = "http://apache.org/schema_validation",
        wsdlLocation = "classpath:/wsdl/schema_validation.wsdl")
    @SchemaValidation
    static class ValidatingSchemaValidationImpl extends SchemaValidationImpl {

    }

    @WebService(serviceName = "SchemaValidationService",
        portName = "SoapPort",
        endpointInterface = "org.apache.schema_validation.SchemaValidation",
        targetNamespace = "http://apache.org/schema_validation",
        wsdlLocation = "classpath:/wsdl/schema_validation.wsdl")
    static class ValidatingSchemaValidationMethodImpl extends SchemaValidationImpl {
        @SchemaValidation
        public boolean setComplexStruct(ComplexStruct in) {
            return true;
        }
    }

    private static String getResponse(String v) {
        if ("9999999999".equals(v)) {
            return "<soap:Fault xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<faultcode>soap:Server</faultcode>"
                + "<faultstring>Fault</faultstring>"
                + "<detail>"
                + "<SomeFault xmlns=\"http://apache.org/schema_validation/types\">"
                + "    <errorCode>1234</errorCode></SomeFault>"
                + "</detail>"
                + "</soap:Fault>";
        } else if ("8888888888".equals(v)) {
            return "<soap:Fault xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<faultcode>soap:Server</faultcode>"
                + "<faultstring>Fault</faultstring>"
                + "<detail>"
                + "<SomeFault xmlns=\"http://apache.org/schema_validation/types\">"
                + "    <errorCode>1</errorCode></SomeFault>"
                + "</detail>"
                + "</soap:Fault>";
        }
        return "";
    }

    @WebServiceProvider(wsdlLocation = "classpath:/wsdl/schema_validation.wsdl",
        serviceName = "SchemaValidationService",
        portName = "SoapPort",
        targetNamespace = "http://apache.org/schema_validation")
    @ServiceMode(Service.Mode.PAYLOAD)
    @SchemaValidation
    @Logging
    static class PayloadProvider implements Provider<Source> {
        @Override
        public Source invoke(Source request) {
            try {
                Document doc = StaxUtils.read(request);
                String name = doc.getDocumentElement().getLocalName();
                if ("SomeRequest".equals(name)) {
                    String v = DOMUtils.getFirstElement(doc.getDocumentElement()).getTextContent();
                    return new StreamSource(new StringReader(getResponse(v)));
                }
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @WebServiceProvider(wsdlLocation = "classpath:/wsdl/schema_validation.wsdl",
        serviceName = "SchemaValidationService",
        portName = "SoapPort",
        targetNamespace = "http://apache.org/schema_validation")
    @ServiceMode(Service.Mode.MESSAGE)
    @SchemaValidation
    static class MessageProvider implements Provider<Source> {
        @Override
        public Source invoke(Source request) {
            try {
                Document doc = StaxUtils.read(request);
                Element el = DOMUtils.getFirstElement(doc.getDocumentElement());
                while (!"Body".equals(el.getLocalName())) {
                    el = DOMUtils.getNextElement(el);
                }
                el = DOMUtils.getFirstElement(el);
                String name = el.getLocalName();

                if ("SomeRequest".equals(name)) {
                    String v = DOMUtils.getFirstElement(el).getTextContent();
                    return new StreamSource(
                        new StringReader("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                            + "<soap:Body>" + getResponse(v) + "</soap:Body></soap:Envelope>"));
                }
            } catch (XMLStreamException ex) {
                ex.printStackTrace();
            }

            return null;
        }
    }

    public static void main(String[] args) {
        try {
            ValidationServer s = new ValidationServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
