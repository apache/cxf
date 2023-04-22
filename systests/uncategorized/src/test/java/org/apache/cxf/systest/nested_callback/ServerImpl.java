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


package org.apache.cxf.systest.nested_callback;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.annotation.Resource;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.apache.cxf.Bus;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLManagerImpl;
import org.apache.nested_callback.CallbackPortType;
import org.apache.nested_callback.NestedCallback;
import org.apache.nested_callback.ServerPortType;

@jakarta.jws.WebService(serviceName = "SOAPService",
                      portName = "SOAPPort",
                      targetNamespace = "http://apache.org/nested_callback",
                      endpointInterface = "org.apache.nested_callback.ServerPortType",
                      wsdlLocation = "wsdl/nested_callback.wsdl")


public class ServerImpl implements ServerPortType  {
    @Resource
    Bus bus;

    public String foo(String s) {
        return s;
    }

    public String registerCallback(NestedCallback callbackObject) {
        try {
            W3CEndpointReference w3cEpr = callbackObject.getCallback();

            EndpointReferenceType callback = ProviderImpl.convertToInternal(w3cEpr);

            WSDLManager manager = new WSDLManagerImpl();

            QName interfaceName = EndpointReferenceUtils.getInterfaceName(callback, bus);
            String wsdlLocation = EndpointReferenceUtils.getWSDLLocation(callback);
            QName serviceName = EndpointReferenceUtils.getServiceName(callback, bus);
            String address = EndpointReferenceUtils.getAddress(callback);

            String portString = EndpointReferenceUtils.getPortName(callback);

            QName portName = new QName(serviceName.getNamespaceURI(), portString);

            StringBuilder seiName = new StringBuilder();
            seiName.append(JAXBUtils.namespaceURIToPackage(interfaceName.getNamespaceURI()));
            seiName.append('.');
            seiName.append(JAXBUtils.nameToIdentifier(interfaceName.getLocalPart(),
                                                      JAXBUtils.IdentifierType.INTERFACE));

            Class<?> sei = null;
            try {
                sei = Class.forName(seiName.toString(),
                                    true, manager.getClass().getClassLoader());
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }

            URL wsdlURL = new URL(wsdlLocation);
            Service service = Service.create(wsdlURL, serviceName);
            CallbackPortType port = (CallbackPortType)service.getPort(portName, sei);
            ((BindingProvider)port).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);

            port.serverSayHi("Sean");


        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return "registerCallback called";
    }



}
