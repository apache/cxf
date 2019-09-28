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
package org.apache.cxf.binding.corba;

import java.util.List;



import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.omg.CORBA.ORB;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;


public class CorbaBindingTest {

    private ORB orb;

    @Before
    public void setUp() throws Exception {
        java.util.Properties props = System.getProperties();


        props.put("yoko.orb.id", "CXF-CORBA-Server-Binding");
        orb = ORB.init(new String[0], props);
    }

    @After
    public void tearDown() {
        if (orb != null) {
            try {
                orb.destroy();
            } catch (Exception ex) {
                // Do nothing.  Throw an Exception?
            }
        }
    }

    @Test
    public void testCorbaBinding() {
        CorbaBinding binding = new CorbaBinding();
        List<Interceptor<? extends Message>> in = binding.getInInterceptors();
        assertNotNull(in);
        List<Interceptor<? extends Message>> out = binding.getOutInterceptors();
        assertNotNull(out);
        List<Interceptor<? extends Message>> infault = binding.getInFaultInterceptors();
        assertNotNull(infault);
        List<Interceptor<? extends Message>> outfault = binding.getFaultOutInterceptors();
        assertNotNull(outfault);
        Message message = binding.createMessage();
        message.put(ORB.class, orb);
        assertNotNull(message);
        ORB corbaORB = message.get(ORB.class);
        assertNotNull(corbaORB);
        MessageImpl mesage = new MessageImpl();
        mesage.put(ORB.class, orb);
        Message msg = binding.createMessage(mesage);
        assertNotNull(msg);
        ORB corbaOrb = msg.get(ORB.class);
        assertNotNull(corbaOrb);
        /*List<Interceptor> infault = binding.getInFaultInterceptors();
        assertEquals(1, infault.size());
        List<Interceptor> outfault = binding.getOutFaultInterceptors();
        assertEquals(1, fault.size());*/
    }

}
