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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.util.StringTokenizer;


import org.apache.cxf.common.i18n.Message;
import org.junit.Assert;
import org.junit.Test;

public class ForkedCommandTest extends Assert {

    private static final String[] ENV_COMMAND;

    static {
        if (System.getProperty("os.name").startsWith("Windows")) {
            ENV_COMMAND = new String[] {"cmd", "/c", "set"};
        } else {
            ENV_COMMAND = new String[] {"env"};
        }
    }

    @Test
    public void testBasics() {
        ForkedCommand fc1 = new ForkedCommand();
        String cmdline1 = fc1.toString();
        assertEquals("", cmdline1);
        try {
            fc1.execute();
        } catch (ForkedCommandException ex) {
            assertEquals("NO_ARGUMENTS_EXC", ex.getCode());
        }
        String[] args = new String[] {"a", "b", "c d e"};
        ForkedCommand fc2 = new ForkedCommand(args);
        String cmdline2 = fc2.toString();
        assertTrue(cmdline2.startsWith("a"));
        assertTrue(cmdline2.endsWith("\""));
        fc1.setArgs(args);
        cmdline1 = fc1.toString();
        assertEquals(cmdline1, cmdline2);
        
        new ForkedCommandException(new NullPointerException());
        Message msg = org.easymock.classextension.EasyMock.createMock(Message.class);
        new ForkedCommandException(msg, new NullPointerException());
    }

    @Test
    public void testExecuteInDefaultEnvironment() {
        ByteArrayOutputStream bosOut = new ByteArrayOutputStream();
        ByteArrayOutputStream bosErr = new ByteArrayOutputStream();
        
        executeEnvCommand(null, bosOut, bosErr);
        
        String output = bosOut.toString();
        assertTrue(output.indexOf("AVAR") < 0 || output.indexOf("BVAR") < 0);      
    }
    
    @Test
    public void testExecuteInNonDefaultEnvironment() {
        ByteArrayOutputStream bosOut = new ByteArrayOutputStream();
        ByteArrayOutputStream bosErr = new ByteArrayOutputStream();
        String[] env = new String[3];
        env[0] = "BVAR=strange";
        if (System.getProperty("os.name").startsWith("Windows")) {
            env[1] = "AVAR=something %BVAR%";
            env[2] = "AVAR=something very %BVAR%";
        } else {
            env[1] = "AVAR=something $BVAR";
            env[2] = "AVAR=something very $BVAR";
        }
        
        
        executeEnvCommand(env, bosOut, bosErr);
        
        // test variables are overwritten but not replaced
        
        boolean found = false;
        String output = bosOut.toString();
        StringTokenizer st = new StringTokenizer(output, System.getProperty("line.separator"));
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (line.length() > 0) {
                if (System.getProperty("os.name").startsWith("Windows")) {
                    if ("AVAR=something very %BVAR%".equals(line)) {
                        found = true;
                        break;
                    }
                } else {
                    if ("AVAR=something very $BVAR".equals(line)
                        || "AVAR=something $BVAR".equals(line)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        assertTrue(found);
        
    }
    
    @Test
    public void testTimeout() throws Exception {
        URL url = TestCommand.class.getResource("TestCommand.class");
        File file = new File(url.toURI());
        file = file.getParentFile();
        file = new File(file, "../../../../..");
        String[] cmd = new String[] {
            JavaHelper.getJavaCommand(),
            "-classpath",
            file.getCanonicalPath(),
            "org.apache.cxf.common.commands.TestCommand",
            "-duration",
            "60000",
        };
        ForkedCommand fc = new ForkedCommand(cmd);
        try {
            fc.execute(1);
            fail("Expected ForkedCommandException not thrown.");
        } catch (ForkedCommandException ex) {
            assertEquals("TIMEOUT_EXC", ex.getCode());
        }
    }

    private void executeEnvCommand(String[] env, ByteArrayOutputStream bosOut, ByteArrayOutputStream bosErr) {

        ForkedCommand fc = new ForkedCommand(ENV_COMMAND);
        if (null != env) {
            fc.setEnvironment(env);
        }
        fc.joinErrOut(true);

        PrintStream pso = new PrintStream(bosOut);
        PrintStream pse = new PrintStream(bosErr);
        fc.setOutputStream(pso);
        fc.setErrorStream(pse);

        int result = fc.execute();
        assertEquals(0, result);
        
    }
    
    
    
    

}
