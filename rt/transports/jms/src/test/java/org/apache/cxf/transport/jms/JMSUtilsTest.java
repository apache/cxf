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

package org.apache.cxf.transport.jms;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.naming.Context;

import org.junit.Assert;
import org.junit.Test;

public class JMSUtilsTest extends Assert {

    @Test
    public void testpopulateIncomingContextNonNull() throws Exception {
        AddressType addrType =  new AddressType();
        
        JMSNamingPropertyType prop = new JMSNamingPropertyType();
        prop.setName(Context.APPLET);
        prop.setValue("testValue");
        addrType.getJMSNamingProperty().add(prop);      
        
        JMSNamingPropertyType prop2 = new JMSNamingPropertyType();
        prop2.setName(Context.BATCHSIZE);
        prop2.setValue("12");
        addrType.getJMSNamingProperty().add(prop2);
        
        Properties env = JMSOldConfigHolder.getInitialContextEnv(addrType);
        assertTrue("Environment should not be empty", env.size() > 0);
        assertTrue("Environemnt should contain NamingBatchSize property", env.get(Context.BATCHSIZE) != null);
    }
    
    @Test
    public void testGetEncoding() throws IOException {                
        assertEquals("Get the wrong encoding", JMSUtils.getEncoding("text/xml; charset=utf-8"), "UTF-8");
        assertEquals("Get the wrong encoding", JMSUtils.getEncoding("text/xml"), "UTF-8");
        assertEquals("Get the wrong encoding", JMSUtils.getEncoding("text/xml; charset=GBK"), "GBK");
        try {
            JMSUtils.getEncoding("text/xml; charset=asci");
            fail("Expect the exception here");
        } catch (Exception ex) {
            assertTrue("we should get the UnsupportedEncodingException here",
                       ex instanceof UnsupportedEncodingException);
        }
        
        
        
    }

}
