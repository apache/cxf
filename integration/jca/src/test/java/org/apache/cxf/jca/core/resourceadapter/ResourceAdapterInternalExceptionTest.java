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
package org.apache.cxf.jca.core.resourceadapter;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ResourceAdapterInternalExceptionTest extends Assert {
    private static final Logger EXCEPTION_LOGGER = 
        LogUtils.getLogger(ResourceAdapterInternalException.class);
    private Level logLevel;  

    @Before    
    public void setUp() throws Exception { 
        logLevel = EXCEPTION_LOGGER.getLevel();
        EXCEPTION_LOGGER.setLevel(Level.SEVERE);
    } 
    
    @After
    public void tearDown() throws Exception {
        EXCEPTION_LOGGER.setLevel(logLevel);
    }
    
    @Test
    public void testMessage() {
        final String msg = "msg1";
        msg.intern();

        Exception e = new ResourceAdapterInternalException(msg);
        assertTrue(e.toString().indexOf(msg) != -1);
        assertTrue(e.toString().indexOf("reason") == -1);
        assertEquals(e.getMessage(), msg);
    }

    @Test
    public void testMessageWithNullTx() {
        final String msg = "msg1";
        msg.intern();

        javax.resource.spi.ResourceAdapterInternalException e = new ResourceAdapterInternalException(msg,
                                                                                                     null);
        assertTrue(e.toString().indexOf(msg) != -1);
        assertTrue(e.toString().indexOf("reason") == -1);
        assertEquals(e.getMessage(), msg);
        assertNull(e.getCause());        
    }

    @Test
    public void testMessageWithEx() throws Exception {
        final String msg = "msg";
        final String causeMsg = "cause";

        Exception cause = new RuntimeException(causeMsg);
        javax.resource.spi.ResourceAdapterInternalException e = new ResourceAdapterInternalException(msg,
                                                                                                     cause);
        assertTrue(e.toString().indexOf(msg) != -1);
        assertTrue(e.toString().indexOf("reason") != -1);
        assertTrue(e.toString().indexOf(causeMsg) != -1);

        assertEquals(e.getCause(), cause);
    }

    @Test
    public void testMessageWithThrowable() throws Exception {
        final String msg = "msg";
        final String causeMsg = "cause";

        Throwable cause = new Throwable(causeMsg);
        javax.resource.spi.ResourceAdapterInternalException e = new ResourceAdapterInternalException(msg,
                                                                                                     cause);
        assertTrue(e.toString().indexOf(msg) != -1);
        assertTrue(e.toString().indexOf("reason") != -1);
        assertTrue(e.toString().indexOf(causeMsg) != -1);
        assertEquals(e.getCause(), cause);       

    }

    @Test
    public void testMessageWithIteEx() throws Exception {
        final String msg = "msg";
        final String causeMsg = "cause";

        Exception cause = new RuntimeException(causeMsg);
        javax.resource.spi.ResourceAdapterInternalException re = 
            new ResourceAdapterInternalException(
                msg, 
                new java.lang.reflect.InvocationTargetException(cause));
        
        assertTrue(re.toString().indexOf(msg) != -1);
        assertTrue(re.toString().indexOf("reason") != -1);
        assertTrue(re.toString().indexOf(causeMsg) != -1);

        assertEquals(re.getCause(), cause);
    }

    @Test
    public void testMessageWithIteErroriNotThrow() throws Exception {
        final String msg = "msg";
        final String causeMsg = "cause";

        java.lang.Throwable cause = new java.lang.UnknownError(causeMsg);
        ResourceAdapterInternalException re = 
            new ResourceAdapterInternalException(
                msg,
                new java.lang.reflect.InvocationTargetException(cause));
        assertEquals(re.getCause(), cause);
    }

    
    @Test
    public void testGetLinkedExceptionReturnNullIfNoCause() throws Exception {
        ResourceAdapterInternalException re = new ResourceAdapterInternalException("ex");
        assertNull("getLinkedException return null", re.getLinkedException());
    }

    @Test
    public void testGetLinkedExceptionReturnNullIfCauseIsError() throws Exception {
        java.lang.Throwable cause = new java.lang.UnknownError("error");
        ResourceAdapterInternalException re = new ResourceAdapterInternalException("ex", cause);
        assertNull("getLinkedException return null", re.getLinkedException());
    }

    @Test
    public void testGetLinkedExceptionReturnNotNullIfCauseIsException() throws Exception {
        java.lang.Throwable cause = new RuntimeException("runtime exception");
        ResourceAdapterInternalException re = new ResourceAdapterInternalException("ex", cause);
        assertEquals("get same exception", cause, re.getLinkedException());
    }
    
}
