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
package org.apache.cxf.service.factory;

import java.util.HashMap;
import java.util.Map;

import javax.wsdl.extensions.soap.SOAPAddress;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.hello_world.types.GreetMe;
import org.apache.hello_world_soap_http.types.GreetMeOneWay;
import org.junit.Test;

public class ClientFactoryBeanTest extends AbstractSimpleFrontendTest {

    @Test
    public void testClientFactoryBean() throws Exception {
        
        ClientFactoryBean cfBean = new ClientFactoryBean();
        cfBean.setAddress("http://localhost/Hello");
        cfBean.setBus(getBus());
        cfBean.setServiceClass(HelloService.class);
        
        Client client = cfBean.create();
        assertNotNull(client);
        
        Service service = client.getEndpoint().getService();
        Map<QName, Endpoint> eps = service.getEndpoints();
        assertEquals(1, eps.size());
        
        Endpoint ep = eps.values().iterator().next();
        EndpointInfo endpointInfo = ep.getEndpointInfo();
        
        SOAPAddress soapAddress = endpointInfo.getExtensor(SOAPAddress.class);
        assertNotNull(soapAddress);
        
        BindingInfo b = endpointInfo.getService().getBindings().iterator().next();
        
        assertTrue(b instanceof SoapBindingInfo);
        
        SoapBindingInfo sb = (SoapBindingInfo) b;
        assertEquals("HelloServiceSoapBinding", b.getName().getLocalPart());
        assertEquals("document", sb.getStyle());
        
        assertEquals(4, b.getOperations().size());
        
        BindingOperationInfo bop = b.getOperations().iterator().next();
        SoapOperationInfo sop = bop.getExtensor(SoapOperationInfo.class);
        assertNotNull(sop);
        assertEquals("", sop.getAction());
        assertEquals("document", sop.getStyle());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testJaxbExtraClass() throws Exception {
        
        ClientFactoryBean cfBean = new ClientFactoryBean();
        cfBean.setAddress("http://localhost/Hello");
        cfBean.setBus(getBus());
        cfBean.setServiceClass(HelloService.class);
        Map props = cfBean.getProperties();
        if (props == null) {
            props = new HashMap<String, Object>();
        }
        props.put("jaxb.additionalContextClasses", 
                  new Class[] {GreetMe.class, GreetMeOneWay.class});
        cfBean.setProperties(props);
        Client client = cfBean.create();
        assertNotNull(client);
        Class[] extraClass = ((JAXBDataBinding)cfBean.getServiceFactory().getDataBinding()).getExtraClass();
        assertEquals(extraClass.length, 2);
        assertEquals(extraClass[0], GreetMe.class);
        assertEquals(extraClass[1], GreetMeOneWay.class);
    }
}
