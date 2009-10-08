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

package org.apache.cxf.bus.extension;

import org.junit.Assert;
import org.junit.Test;

public class ExtensionTest extends Assert {

    @Test
    public void testMutators() {
        Extension e = new Extension();
        
        String className = "org.apache.cxf.bindings.soap.SoapBinding";
        e.setClassname(className);
        assertEquals("Unexpected class name.", className, e.getClassname());
        assertNull("Unexpected interface name.", e.getInterfaceName());
        
        String interfaceName = "org.apache.cxf.bindings.Binding";
        e.setInterfaceName(interfaceName);
        assertEquals("Unexpected interface name.", interfaceName, e.getInterfaceName());
        
        assertTrue("Extension is deferred.", !e.isDeferred());
        e.setDeferred(true);
        assertTrue("Extension is not deferred.", e.isDeferred());
        
        assertEquals("Unexpected size of namespace list.", 0, e.getNamespaces().size());
    }
    
    @Test
    public void testLoad() throws ExtensionException {
        Extension e = new Extension();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        e.setClassname("no.such.Extension");        
        try {
            e.load(cl);                  
        } catch (ExtensionException ex) {
            assertTrue("ExtensionException does not wrap ClassNotFoundException",
                       ex.getCause() instanceof ClassNotFoundException);
        }

        e.setClassname("java.lang.System");
        try {
            e.load(cl);                  
        } catch (ExtensionException ex) {
            assertTrue("ExtensionException does not wrap IllegalAccessException",
                       ex.getCause() instanceof IllegalAccessException);
        } 
        e.setClassname(MyServiceConstructorThrowsException.class.getName());
        try {
            e.load(cl);                  
        } catch (ExtensionException ex) {
            assertTrue("ExtensionException does not wrap InstantiationException",
                       ex.getCause() instanceof InstantiationException);
        } 
        e.setClassname("java.lang.String");
        Object obj = e.load(cl);
        assertTrue("Object is not type String", obj instanceof String);        
    }
    
    @Test
    public void testLoadInterface() {
        Extension e = new Extension();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        e.setInterfaceName("no.such.Extension");        
        try {
            e.loadInterface(cl);                  
        } catch (ExtensionException ex) {
            assertTrue("ExtensionException does not wrap ClassNotFoundException",
                       ex.getCause() instanceof ClassNotFoundException);
        }
        
        e.setInterfaceName(Assert.class.getName());
        Class<?> cls = e.loadInterface(cl);
        assertTrue("Object is not type Class", cls instanceof Class); 
    }
    
    class MyServiceConstructorThrowsException {
        public MyServiceConstructorThrowsException() {
            throw new IllegalArgumentException();
        }
    }
    
}
