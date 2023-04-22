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

package org.apache.cxf.systest.cxf6319;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import jakarta.xml.ws.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test case for CXF-6319 - namespace declarations in body and envelope are not processed correctly
 * when there is a SOAPHandler.
 *
 * @author Tomas Hofman (thofman@redhat.com)
 */
public class Cxf6319TestCase extends AbstractBusClientServerTestBase {

    static final String PORT = TestUtil.getPortNumber(Cxf6319TestCase.class);

    @Test
    public void testDeclarationsInEnvelope() throws Exception {
        Endpoint ep = Endpoint.publish("http://localhost:" + PORT + "/SoapContext/SoapPort", new ServiceImpl());

        try {
            HttpURLConnection httpConnection =
                    getHttpConnection("http://localhost:" + PORT + "/SoapContext/SoapPort/echo");
            httpConnection.setDoOutput(true);

            InputStream reqin = getClass().getResourceAsStream("request.xml");
            assertNotNull("could not load test data", reqin);

            httpConnection.setRequestMethod("POST");
            httpConnection.addRequestProperty("Content-Type", "text/xml");
            OutputStream reqout = httpConnection.getOutputStream();
            IOUtils.copy(reqin, reqout);
            reqout.close();

            int responseCode = httpConnection.getResponseCode();
            InputStream errorStream = httpConnection.getErrorStream();
            String error = null;
            if (errorStream != null) {
                error = IOUtils.readStringFromStream(errorStream);
            }
            assertEquals(error, 200, responseCode);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ep.stop();
        }
    }

}
