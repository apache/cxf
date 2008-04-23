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

package org.apache.cxf.xjc.ts;

import java.lang.reflect.Method;


import org.apache.cxf.configuration.foo.Foo;
import org.apache.cxf.configuration.foo.TpAddress;
import org.apache.cxf.configuration.foo.TpAddressPresentation;
import org.junit.Assert;
import org.junit.Test;



public class ToStringTest extends Assert {

    @Test
    public void testFooToStringOverride() throws Exception {
        
        Foo foo = new org.apache.cxf.configuration.foo.ObjectFactory().createFoo();

        Method method = foo.getClass().getMethod("toString");
        assertEquals("toString is overridden", foo.getClass(),
                     method.getDeclaringClass());
        
        String fooS = foo.toString();
        assertTrue("contains null", fooS.indexOf("null") != -1);
    }    

    
    @Test
    public void testAddressToStringOverride() throws Exception {
        
        TpAddress foo = new org.apache.cxf.configuration.foo.ObjectFactory().createTpAddress();

        Method method = foo.getClass().getMethod("toString");
        assertEquals("toString is overridden", foo.getClass(),
                     method.getDeclaringClass());
     
        TpAddressPresentation value = TpAddressPresentation.P_ADDRESS_PRESENTATION_ALLOWED;
        foo.setPresentation(value);
        String fooS = foo.toString();
        assertTrue("contains null", fooS.indexOf("null") != -1);
        assertTrue("contains P_ADDRESS_PRESENTATION_ALLOWED", 
                   fooS.indexOf("P_ADDRESS_PRESENTATION_ALLOWED") != -1);
        

    }    

}
