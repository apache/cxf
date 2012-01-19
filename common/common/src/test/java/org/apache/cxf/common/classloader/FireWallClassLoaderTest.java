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

package org.apache.cxf.common.classloader;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;


public class FireWallClassLoaderTest extends Assert {

    public FireWallClassLoaderTest() {
    }
    
    @Test
    public void testJavaLangStringAlt() throws Exception {
        ClassLoader c = new FireWallClassLoader(ClassLoader.getSystemClassLoader(), new String[] {"java.*"});
        Class c1 = c.loadClass("java.lang.String");
        assertNotNull("Should have returned a class here", c1);
    }
    
    @Test
    public void testJavaLangStringBlock() throws Exception {
        ClassLoader c = new FireWallClassLoader(ClassLoader.getSystemClassLoader(), 
                                                new String[] {}, 
                                                new String[] {"java.lang.String"});
        try {
            c.loadClass("java.lang.String");
            fail("Expected ClassNotFoundException");
        } catch (ClassNotFoundException ex) {
            assertNotNull("Exception message must not be null.", ex.getMessage());
            assertTrue("not found class must be part of the message. ",
                ex.getMessage().indexOf("java.lang.String") > -1);
        }
    }

    // Check that an internal JDK class can load a class with a prefix that
    // would have
    // been blocked by the firewall
    @Test
    public void testJDKInternalClass() throws Exception {
        // Just create a temp file we can play with
        File tmpFile = File.createTempFile("FireWall", "Test");
        OutputStream os = new FileOutputStream(tmpFile);
        os.write("This is a test".getBytes());
        os.close();
        tmpFile.deleteOnExit();
        String urlString = tmpFile.toURI().toURL().toString();

        ClassLoader c = new FireWallClassLoader(getClass().getClassLoader(), new String[] {"java."});
        Class<?> urlClass = c.loadClass("java.net.URL");
        Constructor<?> urlConstr = urlClass.getConstructor(new Class[] {String.class});
        Object url = urlConstr.newInstance(new Object[] {urlString});
        Method meth = url.getClass().getMethod("openConnection", new Class[] {});
        Object urlConn = meth.invoke(url, new Object[] {});

        // Make sure that the internal (sun) class used by the URL connection
        // cannot be found directly through the firewall
        try {
            c.loadClass(urlConn.getClass().getName());
        } catch (ClassNotFoundException cfne) {
            return;
        }
        fail("Should not have found the " + urlConn.getClass().getName() + " class");
    }
   
    @Test
    public void testSecurityException() {
        try {
            new FireWallClassLoader(ClassLoader.getSystemClassLoader(), new String[] {"hi.there"});
        } catch (SecurityException se) {
            return;
        }
        fail("Constructing a FireWallClassLoader that does not pass through java." 
             + " should cause a SecurityException.");
    }

}
