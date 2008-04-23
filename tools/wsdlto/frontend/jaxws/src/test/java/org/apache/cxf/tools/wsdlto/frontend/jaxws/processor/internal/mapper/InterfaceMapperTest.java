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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.mapper;

import javax.xml.namespace.QName;

import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.junit.Assert;
import org.junit.Test;

public class InterfaceMapperTest extends Assert {
    
    @Test
    public void testMap() throws Exception {
        InterfaceInfo interfaceInfo = new InterfaceInfo(new ServiceInfo(),
                                                        new QName("http://apache.org/hello_world_soap_http",
                                                                  "interfaceTest"));

        ToolContext context = new ToolContext();
        context.put(ToolConstants.CFG_WSDLURL, "http://localhost/?wsdl");
        
        JavaInterface intf = new InterfaceMapper(context).map(interfaceInfo);
        assertNotNull(intf);

        assertEquals("interfaceTest", intf.getWebServiceName());
        assertEquals("InterfaceTest", intf.getName());
        assertEquals("http://apache.org/hello_world_soap_http", intf.getNamespace());
        assertEquals("org.apache.hello_world_soap_http", intf.getPackageName());
        assertEquals("http://localhost/?wsdl", intf.getLocation());
    }
    
    @Test
    public void testMapWithUniqueWsdlLoc() throws Exception {
        InterfaceInfo interfaceInfo = new InterfaceInfo(new ServiceInfo(),
                                                        new QName("http://apache.org/hello_world_soap_http",
                                                                  "interfaceTest"));

        ToolContext context = new ToolContext();
        context.put(ToolConstants.CFG_WSDLURL, "http://localhost/?wsdl");
        context.put(ToolConstants.CFG_WSDLLOCATION, "/foo/blah.wsdl");
        
        JavaInterface intf = new InterfaceMapper(context).map(interfaceInfo);
        assertNotNull(intf);

        assertEquals("interfaceTest", intf.getWebServiceName());
        assertEquals("InterfaceTest", intf.getName());
        assertEquals("http://apache.org/hello_world_soap_http", intf.getNamespace());
        assertEquals("org.apache.hello_world_soap_http", intf.getPackageName());
        assertEquals("/foo/blah.wsdl", intf.getLocation());
    }

}
