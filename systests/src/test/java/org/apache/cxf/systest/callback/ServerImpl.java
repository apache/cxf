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


package org.apache.cxf.systest.callback;

import java.net.URL;

import javax.annotation.Resource;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.callback.CallbackPortType;
import org.apache.callback.ServerPortType;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLManagerImpl;

@javax.jws.WebService(serviceName = "SOAPService", 
                      portName = "SOAPPort",
                      targetNamespace = "http://apache.org/callback",
                      endpointInterface = "org.apache.callback.ServerPortType",
                      wsdlLocation = "testutils/basic_callback_test.wsdl") 
                      
                  
public class ServerImpl implements ServerPortType  {

    @Resource
    Bus bus;
    
    public String foo(String s) {
        return s;
    }
    
    public String registerCallback(W3CEndpointReference w3cRef) {
        try {

            WSDLManager manager = new WSDLManagerImpl();

        
            EndpointReferenceType callback = VersionTransformer.convertToInternal(w3cRef);
        
            QName interfaceName = EndpointReferenceUtils.getInterfaceName(callback, bus);
            String wsdlLocation = EndpointReferenceUtils.getWSDLLocation(callback);
            QName serviceName = EndpointReferenceUtils.getServiceName(callback, bus);

            
            String portString = EndpointReferenceUtils.getPortName(callback);
            
            QName portName = new QName(serviceName.getNamespaceURI(), portString);
            
            StringBuffer seiName = new StringBuffer();
            seiName.append(JAXBUtils.namespaceURIToPackage(interfaceName.getNamespaceURI()));
            seiName.append(".");
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
            
            CallbackPortType port =  (CallbackPortType)service.getPort(portName, sei);

            port.serverSayHi("Sean");

            
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        
        return "registerCallback called";     
    }

    
        
}    