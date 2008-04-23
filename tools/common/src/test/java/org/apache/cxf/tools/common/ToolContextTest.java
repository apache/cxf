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

package org.apache.cxf.tools.common;

import java.util.HashMap;

import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ToolContextTest extends Assert {

    ToolContext context = new ToolContext();

    @Before
    public void setUp() {        
        context.setParameters(new HashMap<String, Object>());
    }
    
    @Test
    public void testGetQName() throws Exception {
        assertNull(context.getQName(ToolConstants.CFG_SERVICENAME));
        
        context.put(ToolConstants.CFG_SERVICENAME, "SoapService");
        QName qname = context.getQName(ToolConstants.CFG_SERVICENAME);        
        assertEquals(new QName(null, "SoapService"), qname);
        
        qname = context.getQName(ToolConstants.CFG_SERVICENAME, "http://cxf.org");
        assertEquals(new QName("http://cxf.org", "SoapService"), qname);
        
        context.put(ToolConstants.CFG_SERVICENAME, "http://apache.org=SoapService");
        qname = context.getQName(ToolConstants.CFG_SERVICENAME);
        assertEquals(new QName("http://apache.org", "SoapService"), qname);
    }
}
