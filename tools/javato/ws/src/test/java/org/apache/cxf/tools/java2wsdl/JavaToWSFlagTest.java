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

package org.apache.cxf.tools.java2wsdl;

import org.apache.cxf.tools.common.CommandInterfaceUtils;
import org.apache.cxf.tools.common.ToolTestBase;
import org.apache.cxf.tools.java2ws.JavaToWS;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JavaToWSFlagTest extends ToolTestBase {

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testVersionOutput() throws Exception {
        String[] args = new String[] {"-v"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }
        assertNotNull(getStdOut());
    }

    @Test
    public void testHelpOutput() {
        String[] args = new String[] {"-help"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }
        assertNotNull(getStdOut());
    }

    @Test
    public void testNormalArgs() throws Exception {
        System.err.println(getLocation("test.wsdl"));
        String[] args = new String[] {"-o",
                                      getLocation("normal.wsdl"),
                                      "org.apache.hello_world_soap_http.Greeter"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }
        assertNotNull(getStdOut());
    }

    @Test
    public void testBadUsage() {
        String[] args = new String[] {"-ttt", "a.ww"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }
        assertNotNull(getStdOut());

    }

    @Test
    public void testValidArgs() {
        String[] args = new String[] {"a.ww"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }
        assertNotNull(getStdOut());
    }

    @Test
    public void testNoOutPutFile() throws Exception {
        String[] args = new String[] {"-o",
                                      getLocation("nooutput.wsdl"),
                                      "org.apache.hello_world_soap_http.Greeter"};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }
        assertNotNull(getStdOut());
    }

    @Test
    public void testNoArg() {
        String[] args = new String[] {};
        CommandInterfaceUtils.commandCommonMain();
        JavaToWS j2w = new JavaToWS(args);
        try {
            j2w.run();
        } catch (Throwable ex) {
            System.err.println("JavaToWS Error: " + ex.toString());
            System.err.println();
        }
        assertEquals(-1, getStdOut().indexOf("Caused by:"));
    }
}