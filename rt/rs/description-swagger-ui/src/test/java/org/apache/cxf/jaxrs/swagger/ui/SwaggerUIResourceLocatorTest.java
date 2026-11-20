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

package org.apache.cxf.jaxrs.swagger.ui;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SwaggerUIResourceLocatorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testPathTraversalIsRejected() throws Exception {
        SwaggerUiResourceLocator locator = createFileLocator();
        String[] paths = {
            "../secret.txt",
            "/../secret.txt",
            "css/../../secret.txt",
            "..",
            "%2e%2e/secret.txt",
            "%2E%2E%2Fsecret.txt",
            "%252e%252e/secret.txt",
            "..%5csecret.txt",
            "..\\secret.txt",
            "%zz/secret.txt"
        };
        for (String path : paths) {
            assertFalse("Must not resolve: " + path, locator.exists(path));
            try {
                locator.locate(path);
                fail("Expected MalformedURLException for: " + path);
            } catch (MalformedURLException ex) {
                // expected
            }
        }
    }

    @Test
    public void testResourcesInsideRootAreLocated() throws Exception {
        SwaggerUiResourceLocator locator = createFileLocator();
        assertTrue(locator.exists("index.html"));
        assertTrue(locator.exists("/index.html"));
        assertTrue(locator.exists(""));
        assertTrue(locator.exists("swagger-ui..min.js"));
        assertTrue(locator.exists("a+b.js"));
        assertEquals("index.html", new File(locator.locate("index.html").toURI()).getName());
    }

    private SwaggerUiResourceLocator createFileLocator() throws Exception {
        File base = tempFolder.getRoot();
        Files.write(new File(base, "secret.txt").toPath(), "secret".getBytes());
        File root = tempFolder.newFolder("swagger-ui");
        for (String name : new String[] {"index.html", "swagger-ui..min.js", "a+b.js"}) {
            Files.write(new File(root, name).toPath(), "ui".getBytes());
        }
        return new SwaggerUiResourceLocator(root.toURI().toString());
    }

    @Test(expected = MalformedURLException.class)
    public void testLocateWithBadCharactersInUrl() throws MalformedURLException {
        String url =
                "jar:file:/Volumes/bigdrive/test157/jetty/base/webapps/"
                + "Rhythmyx/WEB-INF/lib/swagger-ui-2.2.10-1.jar!/META-INF/resources/"
                + "webjars/swagger-ui/2.2.10-1/assets/by-path//Assets/uploads/"
                + "Screen Shot 2020-02-05 at 10.50.53 AM.png";

        SwaggerUiResourceLocator locator = new SwaggerUiResourceLocator("/");

        locator.locate(url);
        
    }
}
