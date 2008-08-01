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
package org.apache.cxf.binding.corba;

import java.net.URL;
import javax.xml.namespace.QName;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.wsdl11.WSDLServiceFactory;

public class TestUtils { 
    
    protected static Bus bus;
    protected CorbaBindingFactory factory;
    protected EndpointInfo endpointInfo;
    
    public TestUtils() {     
        bus = BusFactory.getDefaultBus();
        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);
        try {
            factory = (CorbaBindingFactory)bfm.getBindingFactory(
                       "http://cxf.apache.org/bindings/corba");
            bfm.registerBindingFactory(CorbaConstants.NU_WSDL_CORBA, factory);
        } catch (BusException ex) {
            ex.printStackTrace();            
        }
    }        
    
    public EndpointInfo setupServiceInfo(String ns, String wsdl,
                                         String serviceName, String portName) throws Exception {      
        URL wsdlUrl = getClass().getResource(wsdl);
        WSDLServiceFactory f = new WSDLServiceFactory(bus, wsdlUrl,
                                                      new QName(ns, serviceName));

        Service service = f.create();
        return service.getEndpointInfo(new QName(ns, portName));
    }                  

    public CorbaDestination getExceptionTypesTestDestination() throws Exception {
        endpointInfo = setupServiceInfo("http://schemas.apache.org/idl/except",
                                                     "/wsdl_corbabinding/exceptions.wsdl", 
                                                     "ExceptionTestCORBAService",
                                                     "ExceptionTestCORBAPort");
        CorbaBindingFactory corbaBF = (CorbaBindingFactory)factory;
        return (CorbaDestination)corbaBF.getDestination(endpointInfo);        
    }
    
    public CorbaDestination getComplexTypesTestDestination() throws Exception {
        endpointInfo = setupServiceInfo("http://cxf.apache.org/bindings/corba/ComplexTypes",
                                                     "/wsdl_corbabinding/ComplexTypes.wsdl",
                                                     "ComplexTypesCORBAService",
                                                     "ComplexTypesCORBAPort");
        CorbaBindingFactory corbaBF = (CorbaBindingFactory)factory;        
        return (CorbaDestination)corbaBF.getDestination(endpointInfo);
    }
    public CorbaDestination getComplexTypesTestDestinationYoko() throws Exception {
        endpointInfo = setupServiceInfo("http://cxf.apache.org/bindings/corba/ComplexTypes",
                                                     "/wsdl_corbabinding/ComplexTypesYoko.wsdl",
                                                     "ComplexTypesCORBAService",
                                                     "ComplexTypesCORBAPort");
        CorbaBindingFactory corbaBF = (CorbaBindingFactory)factory;        
        return (CorbaDestination)corbaBF.getDestination(endpointInfo);
    }

}
