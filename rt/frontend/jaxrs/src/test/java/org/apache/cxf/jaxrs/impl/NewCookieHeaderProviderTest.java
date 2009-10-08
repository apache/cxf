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

package org.apache.cxf.jaxrs.impl;

import javax.ws.rs.core.NewCookie;

import org.junit.Assert;
import org.junit.Test;

public class NewCookieHeaderProviderTest extends Assert {
    
        
    @Test
    public void testFromSimpleString() {
        NewCookie c = NewCookie.valueOf("foo=bar");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName()));
    }
    
        
    @Test
    public void testFromComplexString() {
        NewCookie c = NewCookie.valueOf(
                      "foo=bar;Comment=comment;Path=path;Max-Age=10;Domain=domain;Secure;Version=1");
        assertTrue("bar".equals(c.getValue())
                   && "foo".equals(c.getName())
                   && 1 == c.getVersion()
                   && "path".equals(c.getPath())
                   && "domain".equals(c.getDomain())
                   && "comment".equals(c.getComment())
                   && 10 == c.getMaxAge());
    }
    
    @Test
    public void testToString() {
        NewCookie c = new NewCookie("foo", "bar", "path", "domain", "comment", 2, true);
        assertEquals("foo=bar;Comment=comment;Domain=domain;Max-Age=2;Path=path;Secure;Version=1", 
                     c.toString());
               
    }
}
