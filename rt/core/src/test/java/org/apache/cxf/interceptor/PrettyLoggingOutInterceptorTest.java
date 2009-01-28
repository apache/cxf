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

package org.apache.cxf.interceptor;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.apache.cxf.io.CachedOutputStream;

import org.junit.Assert;
import org.junit.Test;

public class PrettyLoggingOutInterceptorTest extends Assert {
    
    @Test
    public void testFormatting() throws Exception { 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(baos);
        
        PrettyLoggingOutInterceptor p = new PrettyLoggingOutInterceptor(pw);
        PrettyLoggingOutInterceptor.LoggingCallback l = p.new LoggingCallback();
        CachedOutputStream cos = new CachedOutputStream();

        String s = "<today><is><the><twenty> <second> <of> <january> <two> <thousand> <and> <nine></nine> " 
            + "</and></thousand></two></january></of></second></twenty></the></is></today>";
        cos.getOut().write(s.getBytes());
        l.onClose(cos); 
        String str = baos.toString();
        //format has changed
        assertFalse(str.matches(s));
        assertTrue(str.contains("<today>"));

    }
}
