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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClassLoaderUtilsTest {

    private static void setTCCL(ClassLoader loader) {
        Thread.currentThread().setContextClassLoader(loader);
    }

    /**
     * This test confirms that the expected thread context classloader
     * is returned from the getContextClassLoader method.
     */
    @Test
    public void getContextClassLoader() throws MalformedURLException {
        final ClassLoader nullLoader = null;
        final ClassLoader jvmAppLoader = ClassLoader.getSystemClassLoader();
        final ClassLoader jvmExtLoader = jvmAppLoader.getParent();
        final ClassLoader testClassLoader = ClassLoaderUtilsTest.class.getClassLoader();
        final ClassLoader clildLoader = new URLClassLoader(new URL[]{new URL("file:/.")});
        final ClassLoader previousTCCL = Thread.currentThread().getContextClassLoader();

        try {
            // TCCL = null
            setTCCL(nullLoader);
            assertEquals("TCCL == null; wrong loader returned; expected JVM App loader", 
                         jvmAppLoader, ClassLoaderUtils.getContextClassLoader());

            // TCCL = JVM App CL
            setTCCL(jvmAppLoader);
            assertEquals("TCCL == JVM App loader; wrong loader returned; expected JVM App loader",
                         jvmAppLoader, ClassLoaderUtils.getContextClassLoader());

            // TCCL = JVM Ext CL
            setTCCL(jvmExtLoader);
            assertEquals("TCCL == JVM Ext loader; wrong loader returned; expected JVM Ext loader",
                         jvmExtLoader, ClassLoaderUtils.getContextClassLoader());

            // TCCL = This test class loader (which is likely also the JVM App CL)
            setTCCL(testClassLoader);
            assertEquals("TCCL == this test laoder; wrong loader returned; expected JVM App loader",
                         testClassLoader, ClassLoaderUtils.getContextClassLoader());

            // TCCL = a random child classloader
            setTCCL(clildLoader);
            assertEquals("TCCL == random child loader, wrong loader returned; expected child of test class loader",
                         clildLoader, ClassLoaderUtils.getContextClassLoader());

        } finally {
            // reset the TCCL for other tests
            setTCCL(previousTCCL);
        }
    }
}