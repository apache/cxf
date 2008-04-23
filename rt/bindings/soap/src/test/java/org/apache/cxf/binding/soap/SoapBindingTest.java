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

package org.apache.cxf.binding.soap;


import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.Assert;
import org.junit.Test;

public class SoapBindingTest extends Assert {
    
    @Test
    public void testCreateMessage() throws Exception {
        Message message = new MessageImpl();
        SoapBinding sb = new SoapBinding(null);
        message = sb.createMessage(message);
        assertNotNull(message);
        assertTrue(message instanceof SoapMessage);
        SoapMessage soapMessage = (SoapMessage) message;
        assertEquals(Soap11.getInstance(), soapMessage.getVersion());

        assertEquals("text/xml", soapMessage.get(Message.CONTENT_TYPE));
        
        soapMessage.remove(Message.CONTENT_TYPE);
        
        sb.setSoapVersion(Soap12.getInstance());
        soapMessage = (SoapMessage) sb.createMessage(soapMessage);
        assertEquals(Soap12.getInstance(), soapMessage.getVersion());     
        assertEquals("application/soap+xml", soapMessage.get(Message.CONTENT_TYPE));
    }

}
