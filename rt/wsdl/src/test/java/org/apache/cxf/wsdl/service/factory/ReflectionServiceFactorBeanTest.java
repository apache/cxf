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

package org.apache.cxf.wsdl.service.factory;

import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.service.factory.FactoryBeanListenerManager;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.wsdl.WSDLManager;

import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ReflectionServiceFactorBeanTest {
    @Test
    public void testEmptyWsdlAndNoServiceClass() throws Exception {
        final String dummyWsdl = "target/dummy.wsdl";
        ReflectionServiceFactoryBean bean = new ReflectionServiceFactoryBean();
        Bus bus = mock(Bus.class);

        WSDLManager wsdlmanager = mock(WSDLManager.class);
        when(bus.getExtension(WSDLManager.class)).thenReturn(wsdlmanager);
        when(wsdlmanager.getDefinition(dummyWsdl))
            .thenThrow(new WSDLException("PARSER_ERROR", "Problem parsing '" + dummyWsdl + "'."));
        when(bus.getExtension(FactoryBeanListenerManager.class)).thenReturn(null);

        bean.setWsdlURL(dummyWsdl);
        bean.setServiceName(new QName("http://cxf.apache.org/hello_world_soap_http", "GreeterService"));
        bean.setBus(bus);

        try {
            bean.create();
            fail("no valid wsdl nor service class specified");
        } catch (ServiceConstructionException e) {
            // ignore
        }
    }
}