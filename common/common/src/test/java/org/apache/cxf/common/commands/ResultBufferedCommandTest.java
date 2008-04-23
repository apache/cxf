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

package org.apache.cxf.common.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

public class ResultBufferedCommandTest extends Assert {
    
    private static final String OUT = "Hello World!";
    private static final String ERR = "Please contact your administrator.";
    

    @Test
    public void testStreamsEmpty() throws Exception {
        URL url = TestCommand.class.getResource("TestCommand.class");
        File file = new File(url.toURI());
        file = file.getParentFile();
        file = new File(file, "../../../../..");
        String[] cmd = new String[] {
            JavaHelper.getJavaCommand(),
            "-classpath",
            file.getCanonicalPath(),
            "org.apache.cxf.common.commands.TestCommand",
        };
        ResultBufferedCommand rbc = new ResultBufferedCommand(cmd);
        assertEquals(0, rbc.execute());
        BufferedReader br = rbc.getBufferedOutputReader();
        assertNotNull(br);
        assertNull(br.readLine());
        br.close();
        br = rbc.getBufferedErrorReader();      
        assertNotNull(br);
        assertNull(br.readLine());
        br.close();
        InputStream is = rbc.getOutput();
        assertEquals(0, is.available());
        is.close();
        is = rbc.getError();
        assertEquals(0, is.available());
        is.close();
    }
    
    @Test
    public void testStreamsNotEmpty() throws Exception {
        URL url = TestCommand.class.getResource("TestCommand.class");
        File file = new File(url.toURI());
        file = file.getParentFile();
        file = new File(file, "../../../../..");
        String[] cmd = new String[] {
            JavaHelper.getJavaCommand(),
            "-classpath",
            file.getCanonicalPath(),
            "org.apache.cxf.common.commands.TestCommand",
            "-out",
            OUT,
            "-err",
            ERR,
            "-result",
            "2",          
        };
        ResultBufferedCommand rbc = new ResultBufferedCommand();
        rbc.setArgs(cmd);
        assertEquals(2, rbc.execute());
        BufferedReader br = rbc.getBufferedOutputReader();
        assertNotNull(br);
        String line = br.readLine();
        assertEquals(OUT, line);
        assertNull(br.readLine());
        br.close();
        br = rbc.getBufferedErrorReader();
        assertNotNull(br);
        line = br.readLine();
        assertEquals(ERR, line);
        assertNull(br.readLine());
        br.close();
        InputStream is = rbc.getOutput();
        assertTrue(is.available() > 0);
        is.close();
        is = rbc.getError();
        assertTrue(is.available() > 0);
        is.close();
    }

}
