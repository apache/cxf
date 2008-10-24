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

package org.apache.cxf.jaxrs.provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.fortest.AegisTestBean;
import org.junit.Assert;
import org.junit.Test;

public class AegisProviderTest extends Assert {
    
    private static final String SIMPLE_BEAN_XML 
        = "<ns1:AegisTestBean xmlns:ns1=\"http://fortest.jaxrs.cxf.apache.org\" "
            + "xmlns:ns2=\"http://www.w3.org/2001/XMLSchema-instance\" ns2:type=\"ns1:AegisTestBean\">"
            + "<ns1:boolValue>true</ns1:boolValue><ns1:strValue>hovercraft</ns1:strValue>"
            + "</ns1:AegisTestBean>";
    
    @Test
    public void testIsWriteable() {
        MessageBodyWriter<Object> p = new AegisElementProvider();
        assertTrue(p.isWriteable(AegisTestBean.class, null, null, null));
    }
    
    @Test
    public void testIsReadable() {
        MessageBodyReader<Object> p = new AegisElementProvider();
        assertTrue(p.isReadable(AegisTestBean.class, null, null, null));
    }
    
    
    @SuppressWarnings("unchecked")
    @Test
    public void testReadFrom() throws Exception {
        MessageBodyReader<Object> p = new AegisElementProvider();
        byte[] simpleBytes = SIMPLE_BEAN_XML.getBytes("utf-8");
        Object beanObject = p.readFrom((Class)AegisTestBean.class, null, null, 
                                          null, null, new ByteArrayInputStream(simpleBytes));
        AegisTestBean bean = (AegisTestBean) beanObject;
        assertEquals("hovercraft", bean.getStrValue());
        assertEquals(Boolean.TRUE, bean.getBoolValue());
    }
    
    @Test
    public void testWriteTo() throws Exception {
        MessageBodyWriter<Object> p = new AegisElementProvider();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        AegisTestBean bean = new AegisTestBean();
        bean.setBoolValue(Boolean.TRUE);
        bean.setStrValue("hovercraft");
        p.writeTo(bean, null, null, null, null, null, os);
        byte[] bytes = os.toByteArray();
        String xml = new String(bytes, "utf-8");
        assertEquals(SIMPLE_BEAN_XML, xml);
    }
}
