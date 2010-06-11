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

package org.apache.cxf.systest.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class WSDLQueryTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BareServer.PORT;
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BareServer.class));
    }
    
    @Test
    public void testEmptyHostHeader() throws Exception {
        sendQuery("", "HTTP/1.1 200 OK");
    }

    @Test
    public void testCorrectHostHeader() throws Exception {
        sendQuery("localhost:" + PORT, "HTTP/1.1 200 OK");
    }

    @Test
    public void testCorrectHostNoPortHeader() throws Exception {
        sendQuery("localhost", "HTTP/1.1 200 OK");
    }

    @Test
    public void testBogusHostHeader() throws Exception {
        sendQuery("foobar:" + PORT, "HTTP/1.1 200 OK");
    }

    @Test
    public void testBogusHostBogusPortHeader() throws Exception {
        sendQuery("foobar:666", "HTTP/1.1 200 OK");
    }

    @Test
    public void testWithBogusHostNoPortHeader() throws Exception {
        sendQuery("foobar", "HTTP/1.1 200 OK");
    }

    private void sendQuery(String hostHeader, String expectedResponseLine)
        throws Exception {
        Socket s = new Socket("localhost", Integer.parseInt(PORT));
        OutputStream os = s.getOutputStream();
        os.write("GET /SoapContext/GreeterPort?wsdl HTTP/1.1\r\n".getBytes());
        os.write(("Host:" + hostHeader + "\r\n\r\n").getBytes());
        os.flush();
        InputStream is = s.getInputStream();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine();
        assertEquals("unexpected response", expectedResponseLine, line);
        is.close();
        os.close();
        s.close();
    }
}
