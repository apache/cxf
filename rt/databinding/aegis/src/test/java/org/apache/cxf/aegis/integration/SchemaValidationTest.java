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

package org.apache.cxf.aegis.integration;

import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.services.ArrayService;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class SchemaValidationTest extends AbstractAegisTest {

    private ArrayService arrayService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        setEnableJDOM(true);
        arrayService = new ArrayService();
        Server server = createService(ArrayService.class, 
                                      arrayService, "Array", new QName("urn:Array", "Array"));
        server.getEndpoint().getService().put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.TRUE); 
    }
    
    @Test
    public void testInvalidArray() throws Exception {
        Node r = invoke("Array", "/org/apache/cxf/aegis/integration/invalidArrayMessage.xml");
        assertNotNull(r);
        StringWriter out = new StringWriter();
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out);
        StaxUtils.writeNode(r, writer, true);
        writer.flush();
        String m = out.toString();
        assertTrue(m.contains("Fault"));
    }
}
