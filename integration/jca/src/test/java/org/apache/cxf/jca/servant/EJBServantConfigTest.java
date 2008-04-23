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

package org.apache.cxf.jca.servant;

import javax.xml.namespace.QName;
import junit.framework.Assert;

import org.junit.Test;


public class EJBServantConfigTest extends Assert {
    
    @Test
    public void testNoWsdl() throws Exception {
        String value = "{http://apache.org/hello_world_soap_http}Greeter";
        QName result = new QName("http://apache.org/hello_world_soap_http", "Greeter");
        EJBServantConfig config = new EJBServantConfig(null, value);
        assertEquals(result, config.getServiceName());
        assertNull(config.getWsdlURL());
    }
    
    @Test
    public void testNoWsdlNoLocalPart() throws Exception {
        String value = "{http://apache.org/hello_world_soap_http}";
        QName result = new QName("http://apache.org/hello_world_soap_http", "");
        EJBServantConfig config = new EJBServantConfig(null, value);
        assertEquals(result, config.getServiceName());
        assertNull(config.getWsdlURL());
    }
    
    @Test
    public void testNoWsdlNoNamespace() throws Exception {
        String value = "Greeter";
        QName result = new QName("", "Greeter");
        EJBServantConfig config = new EJBServantConfig(null, value);
        assertEquals(result, config.getServiceName());
        assertNull(config.getWsdlURL());
    }
    
    @Test
    public void testAllNull() throws Exception {
        String value = "";
        EJBServantConfig config = new EJBServantConfig(null, value);
        assertNull(config.getServiceName());
        assertNull(config.getWsdlURL());
    }
    
    @Test
    public void testWithNullWsdl() throws Exception {
        String value = "@";
        EJBServantConfig config = new EJBServantConfig(null, value);
        assertNull(config.getServiceName());
        assertNull(config.getWsdlURL());
    }
    
    @Test
    public void testWithNullServiceName() throws Exception {
        String value = "@wsdl/hello_world.wsdl";
        EJBServantConfig config = new EJBServantConfig(null, value);
        assertNull(config.getServiceName());
        assertEquals("wsdl/hello_world.wsdl", config.getWsdlURL()); 
    }
    
    @Test
    public void testFullValue() throws Exception {
        String value = "{http://apache.org/hello_world_soap_http}SOAPService@file:/wsdl/hello_world.wsdl";
        QName result = new QName("http://apache.org/hello_world_soap_http", "SOAPService");
        EJBServantConfig config = new EJBServantConfig(null, value);
        assertEquals("file:/wsdl/hello_world.wsdl", config.getWsdlURL());
        assertEquals(result, config.getServiceName());
    }
    
    @Test
    public void testGetServiceClassName() throws Exception {
        String value = "{http://apache.org/hello_world_soap_http}Greeter@file:";
        EJBServantConfig config = new EJBServantConfig("GreeterBean", value);
        EJBEndpoint endpoint = new EJBEndpoint(config);
        assertEquals("org.apache.hello_world_soap_http.Greeter", endpoint.getServiceClassName());
    }

}
