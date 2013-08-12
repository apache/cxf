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

package org.apache.cxf.service.model;

import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FaultInfoTest extends Assert {
    
    private FaultInfo faultInfo;
    
    @Before
    public void setUp() throws Exception {
        faultInfo = new FaultInfo(new QName("urn:test:ns", "fault"), new QName(
             "http://apache.org/hello_world_soap_http", "faultMessage"), null);
    }
    
    @Test
    public void testName() throws Exception {
        assertEquals(faultInfo.getFaultName(), new QName("urn:test:ns", "fault"));
        assertEquals(faultInfo.getName().getLocalPart(), "faultMessage");
        assertEquals(faultInfo.getName().getNamespaceURI(),
                     "http://apache.org/hello_world_soap_http");
        
        faultInfo.setFaultName(new QName("urn:test:ns", "fault"));
        assertEquals(faultInfo.getFaultName(), new QName("urn:test:ns", "fault"));
    }
}
