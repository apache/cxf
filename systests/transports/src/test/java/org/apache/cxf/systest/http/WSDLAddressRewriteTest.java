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

import javax.xml.ws.Endpoint;

import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Test;

public class WSDLAddressRewriteTest extends AbstractBusClientServerTestBase {

    @Test
    public void testWithSameAddress() throws Exception {
        Endpoint endpoint = null;
        try {
            endpoint = publishEndpoint(false);
            String soapAddressLine = getSoapAddressLine("localhost");
            assertTrue(soapAddressLine.contains("address location=\"http://localhost"));
        } finally {
            if (endpoint != null) {
                endpoint.stop();
            }
        }
        try {
            endpoint = publishEndpoint(true);
            String soapAddressLine = getSoapAddressLine("localhost");
            assertTrue(soapAddressLine.contains("address location=\"http://localhost"));
        } finally {
            if (endpoint != null) {
                endpoint.stop();
            }
        }
    }

    @Test
    public void testWithEquivalentAddress() throws Exception {
        Endpoint endpoint = null;
        try {
            endpoint = publishEndpoint(false);
            String soapAddressLine = getSoapAddressLine("127.0.0.1");
            assertTrue(soapAddressLine.contains("address location=\"http://localhost"));
        } finally {
            if (endpoint != null) {
                endpoint.stop();
            }
        }
        //now test enabling the autoRewrite; this should be used when having multiple
        //addresses (belonging to different networks) for a single server instance
        try {
            endpoint = publishEndpoint(true);
            String soapAddressLine = getSoapAddressLine("127.0.0.1");
            assertTrue(soapAddressLine.contains("address location=\"http://127.0.0.1"));
        } finally {
            if (endpoint != null) {
                endpoint.stop();
            }
        }
    }

    private String getSoapAddressLine(String address) throws Exception {
        Socket s = new Socket(address, 9020);
        OutputStream os = s.getOutputStream();
        os.write("GET /SoapContext/GreeterPort?wsdl HTTP/1.1\r\n".getBytes());
        os.write(("Host:" + address + "\r\n\r\n").getBytes());
        os.flush();
        InputStream is = s.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while (true) {
            line = reader.readLine();
            if (line.contains("address location=\"")) {
                break;
            }
        }
        is.close();
        os.close();
        s.close();
        return line;
    }

    private Endpoint publishEndpoint(boolean autoRewriteSoapAddress) {
        Endpoint endpoint = Endpoint.publish("http://localhost:9020/SoapContext/GreeterPort",
                                             new GreeterImpl());
        EndpointInfo ei = ((EndpointImpl)endpoint).getServer().getEndpoint().getEndpointInfo();
        ei.setProperty("autoRewriteSoapAddress", autoRewriteSoapAddress);
        return endpoint;
    }
}
