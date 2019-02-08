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
package org.apache.cxf.systest.handlers;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.test.AbstractCXFSpringTest;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SoapFaultHandlerTest extends AbstractCXFSpringTest {

    static String port = TestUtil.getPortNumber("SoapFaultHandler");
    static String addNumbersAddress = "http://localhost:" + port + "/SpringEndpoint";

    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:/org/apache/cxf/systest/handlers/soap_fault_beans.xml" };
    }

    @Test
    public void testFaultThrowingHandler() throws Exception {
        // set the post request using url connection
        URL postUrl = new URL(addNumbersAddress);
        HttpURLConnection connection = (HttpURLConnection) postUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type",
            "text/xml; charset=UTF-8");
        connection.connect();
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        InputStream is = this.getClass().getResourceAsStream("resources/GreetMeDocLiteralReq.xml");

        IOUtils.copyAndCloseInput(is, out);
        out.flush();
        out.close();
        InputStream response = getInputStream(connection);
        // get the response fault message
        String result = IOUtils.toString(response, StandardCharsets.UTF_8.name());
        // just make sure the custom namespace is working
        assertTrue("The custom namespace is not working.", result.indexOf("cxf:Provider") > 0);

    }

    protected InputStream getInputStream(HttpURLConnection connection) throws IOException {
        InputStream in = null;
        if (connection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            in = connection.getErrorStream();
            if (in == null) {
                try {
                    // just in case - but this will most likely cause an exception
                    in = connection.getInputStream();
                } catch (IOException ex) {
                    // ignore
                }
            }
        } else {
            in = connection.getInputStream();
        }
        return in;
    }



}