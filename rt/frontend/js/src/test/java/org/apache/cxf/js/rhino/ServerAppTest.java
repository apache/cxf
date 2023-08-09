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

package org.apache.cxf.js.rhino;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

public class ServerAppTest {

    private String epAddr = "http://cxf.apache.org/";

    private ProviderFactory phMock;
    private String emptyFile;

    @Before
    public void setUp() throws Exception {
        phMock = mock(ProviderFactory.class);
        doCallRealMethod().when(phMock).createAndPublish(nullable(File.class));
        emptyFile = getClass().getResource("empty/empty.js").toURI().getPath();
    }

    private ServerApp createServerApp() {
        return new ServerApp() {
                protected ProviderFactory createProviderFactory() {
                    return phMock;
                }
            };
    }

    @Test
    public void testNoArgs() {
        try {
            ServerApp app = createServerApp();
            String[] args = {};
            app.start(args);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertEquals("wrong exception message", ServerApp.NO_FILES_ERR, ex.getMessage());
        }
    }

    @Test
    public void testUknownOption() {
        try {
            ServerApp app = createServerApp();
            String[] args = {"-x"};
            app.start(args);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertTrue(ex.getMessage().startsWith(ServerApp.UNKNOWN_OPTION));
        }
    }

    @Test
    public void testMissingOptionA() {
        try {
            ServerApp app = createServerApp();
            String[] args = {"-a"};
            app.start(args);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertEquals("wrong exception message", ServerApp.WRONG_ADDR_ERR, ex.getMessage());
        }
    }

    @Test
    public void testBrokenOptionA() {
        try {
            ServerApp app = createServerApp();
            String[] args = {"-a", "not-a-url"};
            app.start(args);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertEquals("wrong exception message", ServerApp.WRONG_ADDR_ERR, ex.getMessage());
        }
    }

    @Test
    public void testMissingOptionB() {
        try {
            ServerApp app = createServerApp();
            String[] args = {"-b"};
            app.start(args);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertEquals("wrong exception message", ServerApp.WRONG_BASE_ERR, ex.getMessage());
        }
    }

    @Test
    public void testBrokenOptionB() {
        try {
            ServerApp app = createServerApp();
            String[] args = {"-b", "not-a-url"};
            app.start(args);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertEquals("wrong exception message", ServerApp.WRONG_BASE_ERR, ex.getMessage());
        }
    }

    @Test
    public void testFileOnly() throws Exception {
        phMock.createAndPublish(new File(emptyFile), null, false);
        ServerApp app = createServerApp();
        String[] args = {emptyFile};
        app.start(args);
    }

    @Test
    public void testOptionsAB() throws Exception {
        phMock.createAndPublish(new File(emptyFile), epAddr, true);
        ServerApp app = createServerApp();
        String[] args = {"-a", epAddr, "-b", epAddr, emptyFile};
        app.start(args);
    }

    @Test
    public void testOptionA() throws Exception {
        phMock.createAndPublish(new File(emptyFile), epAddr, false);
        ServerApp app = createServerApp();
        String[] args = {"-a", epAddr, emptyFile};
        app.start(args);
    }

    @Test
    public void testOptionAWithOptionV() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(bout);
        PrintStream orig = System.out;
        try {
            System.setOut(pout);
            phMock.createAndPublish(new File(emptyFile), epAddr, false);
            ServerApp app = createServerApp();
            String[] args = {"-a", epAddr, "-v", emptyFile};
            app.start(args);
            pout.flush();
            assertTrue(new String(bout.toByteArray()).contains("processing file"));
        } finally {
            System.setOut(orig);
        }
    }

    @Test
    public void testOptionB() throws Exception {
        phMock.createAndPublish(new File(emptyFile), epAddr, true);
        ServerApp app = createServerApp();
        String[] args = {"-b", epAddr, emptyFile};
        app.start(args);
    }

    @Test
    public void testOptionBWithOptionV() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(bout);
        PrintStream orig = System.out;
        try {
            System.setOut(pout);

            phMock.createAndPublish(new File(emptyFile), epAddr, true);
            ServerApp app = createServerApp();
            String[] args = {"-b", epAddr, "-v", emptyFile};
            app.start(args);
            assertTrue(new String(bout.toByteArray()).contains("processing file"));
        } finally {
            System.setOut(orig);
        }
    }

    @Test
    public void testDirectory() throws Exception {
        File f = new File(emptyFile);
        String dir = f.getParent();
        assertNotNull(dir);

        phMock.createAndPublish(new File(emptyFile), epAddr, true);
        String file = getClass().getResource("empty/empty2.jsx").toURI().getPath();
        phMock.createAndPublish(new File(file), epAddr, true);
        file = getClass().getResource("empty/empty3.js").toURI().getPath();
        phMock.createAndPublish(new File(file), epAddr, true);
        file = getClass().getResource("empty/empty4.jsx").toURI().getPath();
        phMock.createAndPublish(new File(file), epAddr, true);
        
        ServerApp app = createServerApp();
        String[] args = {"-b", epAddr, dir};
        app.start(args);
    }

}