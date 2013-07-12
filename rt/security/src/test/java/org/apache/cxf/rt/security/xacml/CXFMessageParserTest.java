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
package org.apache.cxf.rt.security.xacml;

import java.util.List;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Assert;
import org.junit.Test;

public class CXFMessageParserTest {
    
    @Test
    public void testSOAPResource() throws Exception {
        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, operation);
        CXFMessageParser messageParser = new CXFMessageParser(msg);
        assertSingleElement(operation, messageParser.getResources(true));
    }
    
    @Test
    public void testSOAPResourceWithRequestURI() throws Exception {
        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, operation);
        msg.put(Message.REQUEST_URI, "/doubleIt");
        CXFMessageParser messageParser = new CXFMessageParser(msg);
        Assert.assertEquals(2, messageParser.getResources(false).size());
    }
        
    @Test
    public void testRelativeRestResource() throws Exception {
        String operation = "user/list.json";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.REQUEST_URI, operation);
        
        CXFMessageParser messageParser = new CXFMessageParser(msg);
        assertSingleElement(operation, messageParser.getResources(false));
    }
    
    @Test
    public void testAbsoluteRestResource() throws Exception {
        String operation = "https://localhost:8080/user/list.json";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.REQUEST_URL, operation);
        
        CXFMessageParser messageParser = new CXFMessageParser(msg);
        assertSingleElement(operation, messageParser.getResources(true));
    }
    
    public void assertSingleElement(String content, List<String> strings) {
        Assert.assertEquals(1, strings.size());
        Assert.assertEquals(content, strings.get(0));
    }
}
