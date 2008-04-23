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

package org.apache.cxf.common.i18n;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.junit.Assert;
import org.junit.Test;


public class MessageTest extends Assert {
    private static final Logger LOG = LogUtils.getL7dLogger(MessageTest.class);
    
    @Test
    public void testMessageWithLoggerBundle() throws Exception {
        Message msg = new Message("SUB1_EXC", LOG, new Object[] {1});
        assertSame("unexpected resource bundle",
                   LOG.getResourceBundle(),
                   msg.bundle);
        assertEquals("unexpected message string", 
                     "subbed in 1 only", 
                     msg.toString()); 
    }

    @Test
    public void testMessageWithExplicitBundle() throws Exception {
        ResourceBundle bundle = BundleUtils.getBundle(getClass());
        Message msg = new Message("SUB2_EXC", bundle, new Object[] {3, 4});
        assertSame("unexpected resource bundle", bundle, msg.bundle);
        assertEquals("unexpected message string", 
                     "subbed in 4 & 3",
                     msg.toString()); 
    }
    
    @Test
    public void testExceptionIO() throws java.lang.Exception {
        ResourceBundle bundle = BundleUtils.getBundle(getClass());
        UncheckedException ex = new UncheckedException(new Message("SUB2_EXC",
                                                                   bundle,
                                                                   new Object[] {3, 4}));
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(ex);
        
        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        Object o = in.readObject();
        assertTrue(o instanceof UncheckedException);
        UncheckedException ex2 = (UncheckedException)o;
        assertEquals("subbed in 4 & 3", ex2.getMessage());
        
    }
}
