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

package org.apache.cxf.helpers;

import org.junit.Assert;
import org.junit.Test;

public class NameSpaceTest extends Assert {
    
    private final String myURL1 = "http://test.apache.org/testurl1";
    private final String myURL2 = "http://test.apache.org/testurl2";
    private final String myCustomURL = "http://test.apache.org/custom-prefix-url";
    private final String myOwnPrefix = "myown-prefix";
    

    @Test
    public void testNSStackOperations() throws Exception {
        NSStack  nsStackObj = new NSStack();
        
        nsStackObj.push();
        
        nsStackObj.add(myURL1);
        nsStackObj.add(myOwnPrefix, myCustomURL);
        nsStackObj.add(myURL2);
        
        assertEquals(myURL1, nsStackObj.getURI("ns1"));
        assertEquals(myCustomURL, nsStackObj.getURI(myOwnPrefix));
        assertEquals(myURL2, nsStackObj.getURI("ns2"));
        assertNull(nsStackObj.getURI("non-existent-prefix"));
        
        assertEquals("ns2", nsStackObj.getPrefix(myURL2));
        assertEquals(myOwnPrefix, nsStackObj.getPrefix(myCustomURL));
        assertEquals("ns1", nsStackObj.getPrefix(myURL1));
        assertNull(nsStackObj.getPrefix("non-existent-prefix"));
        
        nsStackObj.pop();
        assertNull(nsStackObj.getPrefix("non-existent-prefix"));
        assertNull(nsStackObj.getPrefix(myCustomURL));
    }
    
    @Test
    public void testNSDeclOperaions() throws Exception {
        NSDecl nsDecl1 = new NSDecl(myOwnPrefix, myCustomURL);
        NSDecl nsDecl2 = new NSDecl("ns2", myURL2);
        NSDecl nsDecl3 = new NSDecl(myOwnPrefix, myCustomURL);
        
        assertFalse(nsDecl2.equals(nsDecl1));
        assertTrue(nsDecl3.equals(nsDecl1));
        
    }
}
