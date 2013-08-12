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

public class MessagePartInfoTest extends Assert {
    
        
    private MessagePartInfo messagePartInfo;
        
    @Before
    public void setUp() throws Exception {
        
        MessageInfo msg = new MessageInfo(null,
                                          MessageInfo.Type.INPUT,
                                          new QName("http://apache.org/hello_world_soap_http/types",
                                                    "testMessage"));
        messagePartInfo = new MessagePartInfo(new QName(
            "http://apache.org/hello_world_soap_http", "testMessagePart"), msg);
        messagePartInfo.setElement(true);
    }
    
    @Test
    public void testName() throws Exception {
        assertEquals(messagePartInfo.getName().getLocalPart(), "testMessagePart");
        assertEquals(messagePartInfo.getName().getNamespaceURI()
                     , "http://apache.org/hello_world_soap_http");
        messagePartInfo.setName(new QName(
            "http://apache.org/hello_world_soap_http1", "testMessagePart1"));
        assertEquals(messagePartInfo.getName().getLocalPart(), "testMessagePart1");
        assertEquals(messagePartInfo.getName().getNamespaceURI()
                     , "http://apache.org/hello_world_soap_http1");
        
    }

    @Test
    public void testElement() {
        messagePartInfo.setElementQName(new QName("http://apache.org/hello_world_soap_http/types",
                                                  "testElement"));
        assertTrue(messagePartInfo.isElement());
        assertEquals(messagePartInfo.getElementQName().getLocalPart(), "testElement");
        assertEquals(messagePartInfo.getElementQName().getNamespaceURI(),
                     "http://apache.org/hello_world_soap_http/types");
        assertNull(messagePartInfo.getTypeQName());
    }
    
    @Test
    public void testType() {
        messagePartInfo.setTypeQName(new QName(
            "http://apache.org/hello_world_soap_http/types", "testType"));
        assertNull(messagePartInfo.getElementQName());
        assertFalse(messagePartInfo.isElement());
        assertEquals(messagePartInfo.getTypeQName().getLocalPart(), "testType");
        assertEquals(messagePartInfo.getTypeQName().getNamespaceURI(),
                     "http://apache.org/hello_world_soap_http/types");
    }
}
