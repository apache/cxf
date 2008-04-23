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
package org.apache.cxf.jaxws;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.calculator.CalculatorPortType;
import org.junit.Test;

public class SOAPBindingTest extends AbstractJaxWsTest {

    private static final QName SERVICE_1 = 
        new QName("http://apache.org/cxf/calculator", "CalculatorService");

    private static final QName PORT_1 = 
        new QName("http://apache.org/cxf/calculator", "CalculatorPort");

    @Test
    public void testRoles() throws Exception {
        URL wsdl1 = getClass().getResource("/wsdl/calculator.wsdl");
        assertNotNull(wsdl1);
        
        ServiceImpl service = new ServiceImpl(getBus(), wsdl1, SERVICE_1, ServiceImpl.class);

        CalculatorPortType cal = (CalculatorPortType)service.getPort(PORT_1, CalculatorPortType.class);
        
        BindingProvider bindingProvider = (BindingProvider)cal;
        
        assertTrue(bindingProvider.getBinding() instanceof SOAPBinding);
        SOAPBinding binding = (SOAPBinding)bindingProvider.getBinding();
        
        assertNotNull(binding.getRoles());
        assertEquals(2, binding.getRoles().size());
        assertTrue(binding.getRoles().contains(Soap12.getInstance().getNextRole()));
        assertTrue(binding.getRoles().contains(Soap12.getInstance().getUltimateReceiverRole()));
        
        String myrole = "http://myrole";
        Set<String> roles = new HashSet<String>();
        roles.add(myrole);
        
        binding.setRoles(roles);
        
        assertNotNull(binding.getRoles());
        assertEquals(3, binding.getRoles().size());
        assertTrue(binding.getRoles().contains(myrole));
        assertTrue(binding.getRoles().contains(Soap12.getInstance().getNextRole()));
        assertTrue(binding.getRoles().contains(Soap12.getInstance().getUltimateReceiverRole()));
                
        roles.add(Soap12.getInstance().getNoneRole());
        
        try {        
            binding.setRoles(roles);
            fail("did not throw exception");
        } catch (WebServiceException e) {
            // that's expected with none role
        }                   
    }

    @Test
    public void testSAAJ() throws Exception {
        URL wsdl1 = getClass().getResource("/wsdl/calculator.wsdl");
        assertNotNull(wsdl1);
        
        ServiceImpl service = new ServiceImpl(getBus(), wsdl1, SERVICE_1, ServiceImpl.class);

        CalculatorPortType cal = (CalculatorPortType)service.getPort(PORT_1, CalculatorPortType.class);
        
        BindingProvider bindingProvider = (BindingProvider)cal;
        
        assertTrue(bindingProvider.getBinding() instanceof SOAPBinding);
        SOAPBinding binding = (SOAPBinding)bindingProvider.getBinding();
        
        assertNotNull(binding.getMessageFactory());
        
        assertNotNull(binding.getSOAPFactory());
    }
        
}
