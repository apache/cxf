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

package org.apache.cxf.continuations;

import org.apache.cxf.common.i18n.UncheckedException;
import org.junit.Assert;
import org.junit.Test;


public class SuspendedInvocationExceptionTest extends Assert {
    
    @Test
    public void testValidRuntimeException() {
        
        Throwable t = new UncheckedException(new Throwable());
        SuspendedInvocationException ex = new SuspendedInvocationException(t);
        
        assertSame(t, ex.getRuntimeException());
        assertSame(t, ex.getCause());
        
    }
    
    @Test
    public void testNoRuntimeException() {
        
        SuspendedInvocationException ex = new SuspendedInvocationException(
                                              new Throwable());
        
        assertNull(ex.getRuntimeException());
    }
    
}
