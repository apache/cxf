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

package org.apache.cxf.resource;

import java.io.InputStream;
import java.net.URL;

import org.apache.cxf.helpers.IOUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class URIResolverTest {

    private URIResolver uriResolver;

    private URL resourceURL = getClass().getResource("resources/helloworld.bpr");

    private Throwable checkingThreadThrowable;      // assumes single-thread test execution

    @Test
    public void testJARProtocol() throws Exception {
        uriResolver = new URIResolver();

        byte[] barray = new byte[]{0};
        byte[] barray2 = new byte[]{1};
        String uriStr = "jar:" + resourceURL.toString() + "!/wsdl/hello_world.wsdl";

        // Check standard Java API's work with "jar:"
        URL jarURL = new URL(uriStr);
        InputStream is = jarURL.openStream();
        assertNotNull(is);
        barray = new byte[is.available()];
        is.read(barray);
        is.close();

        uriResolver.resolve("baseUriStr", uriStr, null);

        InputStream is2 = uriResolver.getInputStream();
        assertNotNull(is2);
        barray2 = new byte[is2.available()];
        is2.read(barray2);
        is2.close();
        assertEquals(IOUtils.newStringFromBytes(barray), IOUtils.newStringFromBytes(barray2));

        resolveWithCheckingClassloader(uriResolver, "baseUriStr", uriStr, null);
        resolveWithCheckingClassloaderInConstructor("baseUriStr", uriStr, null);
    }

    @Test
    public void testJARResolver() throws Exception {
        uriResolver = new URIResolver();

        String uriStr = "jar:" + resourceURL.toString() + "!/wsdl/hello_world.wsdl";

        URL jarURL = new URL(uriStr);
        InputStream is = jarURL.openStream();
        assertNotNull(is);

        String uriStr2 = "jar:" + resourceURL.toString() + "!/wsdl/hello_world_2.wsdl";

        URL jarURL2 = new URL(uriStr2);
        InputStream is2 = jarURL2.openStream();
        assertNotNull(is2);

        uriResolver.resolve(uriStr, "hello_world_2.wsdl", null);

        InputStream is3 = uriResolver.getInputStream();
        assertNotNull(is3);

        resolveWithCheckingClassloader(uriResolver, uriStr, "hello_world_2.wsdl", null);
        resolveWithCheckingClassloaderInConstructor(uriStr, "hello_world_2.wsdl", null);
    }

    @Test
    public void testJARResolveBaseAndAbsolute() throws Exception {
        uriResolver = new URIResolver();

        String baseUriStr = "jar:" + resourceURL.toString() + "!/wsdl/hello_world.wsdl";

        URL jarURL = new URL(baseUriStr);
        InputStream is = jarURL.openStream();
        assertNotNull(is);

        String uriStr = "jar:" + resourceURL.toString() + "!/wsdl/hello_world_2.wsdl";

        URL jarURL2 = new URL(uriStr);
        InputStream is2 = jarURL2.openStream();
        assertNotNull(is2);

        resolveWithCheckingClassloader(uriResolver, baseUriStr, uriStr, null);
        resolveWithCheckingClassloaderInConstructor(baseUriStr, uriStr, null);
    }

    @Test
    public void testResolveRelativeFile() throws Exception {
        URIResolver wsdlResolver = new URIResolver();

        // resolve the wsdl
        wsdlResolver.resolve(null, "wsdl/foo.wsdl", this.getClass());
        assertTrue(wsdlResolver.isResolved());

        // get the base uri from the resolved wsdl location
        String baseUri = wsdlResolver.getURI().toString();

        // resolve the schema using relative location
        String schemaLocation = "../schemas/configuration/bar.xsd";
        URIResolver xsdResolver = new URIResolver();
        xsdResolver.resolve(baseUri, schemaLocation, this.getClass());
        assertNotNull(xsdResolver.getInputStream());
        resolveWithCheckingClassloader(xsdResolver, baseUri, schemaLocation, this.getClass());
        resolveWithCheckingClassloaderInConstructor(baseUri, schemaLocation, this.getClass());

        // resolve the schema using relative location with base uri fragment
        xsdResolver = new URIResolver();
        xsdResolver.resolve(baseUri + "#type2", schemaLocation, this.getClass());
        assertNotNull(xsdResolver.getInputStream());
        resolveWithCheckingClassloader(xsdResolver, baseUri + "#type2", schemaLocation, this.getClass());
        resolveWithCheckingClassloaderInConstructor(baseUri + "#type2", schemaLocation, this.getClass());
    }

    @Test
    public void testResolvePathWithSpace() throws Exception {
        URIResolver wsdlResolver = new URIResolver();

        // resolve the wsdl
        wsdlResolver.resolve(null, "wsdl/foo.wsdl", this.getClass());
        assertTrue(wsdlResolver.isResolved());

        // get the base uri from the resolved wsdl location
        String baseUri = wsdlResolver.getURI().toString();

        // resolve the schema using relative location
        String schemaLocation = "../schemas/configuration/folder with spaces/bar.xsd";
        URIResolver xsdResolver = new URIResolver();
        xsdResolver.resolve(baseUri, schemaLocation, this.getClass());
        assertNotNull(xsdResolver.getInputStream());
        resolveWithCheckingClassloader(xsdResolver, baseUri, schemaLocation, this.getClass());
        resolveWithCheckingClassloaderInConstructor(baseUri, schemaLocation, this.getClass());

        // resolve the schema using relative location with base uri fragment
        xsdResolver = new URIResolver();
        xsdResolver.resolve(baseUri + "#type2", schemaLocation, this.getClass());
        assertNotNull(xsdResolver.getInputStream());
        resolveWithCheckingClassloader(xsdResolver, baseUri + "#type2", schemaLocation, this.getClass());
        resolveWithCheckingClassloaderInConstructor(baseUri + "#type2", schemaLocation, this.getClass());
    }

    @Test
    public void testBasePathWithSpace() throws Exception {
        URIResolver wsdlResolver = new URIResolver();
        // resolve the wsdl
        wsdlResolver.resolve(null, "wsdl/folder with spaces/foo.wsdl", this.getClass());
        assertTrue(wsdlResolver.isResolved());
        resolveWithCheckingClassloader(wsdlResolver, null, "wsdl/folder with spaces/foo.wsdl", this.getClass());
        resolveWithCheckingClassloaderInConstructor(null, "wsdl/folder with spaces/foo.wsdl", this.getClass());
    }

    @Test
    public void testBasePathWithEncodedSpace() throws Exception {
        URIResolver wsdlResolver = new URIResolver();
        // resolve the wsdl
        wsdlResolver.resolve(null, "wsdl/folder%20with%20spaces/foo.wsdl", this.getClass());
        assertTrue(wsdlResolver.isResolved());
        resolveWithCheckingClassloader(wsdlResolver, null, "wsdl/folder%20with%20spaces/foo.wsdl", this.getClass());
        resolveWithCheckingClassloaderInConstructor(null, "wsdl/folder%20with%20spaces/foo.wsdl", this.getClass());
    }

    private void resolveWithCheckingClassloader(URIResolver resolver, String baseUriStr, String uriStr,
            Class<?> callingCls) throws InterruptedException {
        checkingThreadThrowable = null;
        Runnable operation = () -> {
            try {
                resolver.resolve(baseUriStr, uriStr, callingCls);
            } catch (Throwable t) {
                checkingThreadThrowable = t;
            }
            assertNotNull(resolver.getInputStream());
        };
        Thread thread = new Thread(operation);
        thread.setContextClassLoader(new CheckingClassLoader(this.getClass().getClassLoader()));
        thread.start();
        thread.join(10000);
        assertFalse("resolve operation did not finish in time", thread.isAlive());
        assertNull(checkingThreadThrowable);
    }

    private void resolveWithCheckingClassloaderInConstructor(String baseUriStr, String uriStr, Class<?> callingCls)
            throws InterruptedException {
        checkingThreadThrowable = null;
        Runnable operation = () -> {
            URIResolver resolver = null;
            try {
                resolver = new URIResolver(baseUriStr, uriStr, callingCls);
            } catch (Throwable t) {
                checkingThreadThrowable = t;
            }
            if (resolver != null) {
                assertNotNull(resolver.getInputStream());
            }
        };
        Thread thread = new Thread(operation);
        thread.setContextClassLoader(new CheckingClassLoader(this.getClass().getClassLoader()));
        thread.start();
        thread.join(10000);
        assertFalse("resolve operation did not finish in time", thread.isAlive());
        assertNull(checkingThreadThrowable);
    }

    static class CheckingClassLoader extends ClassLoader {
        CheckingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected URL findResource(String name) {
            if (name != null && name.startsWith("jar:file:")) {
                throw new AssertionError("Suspicious resource name: " + name);
            }
            return super.findResource(name);
        }
    }
}
