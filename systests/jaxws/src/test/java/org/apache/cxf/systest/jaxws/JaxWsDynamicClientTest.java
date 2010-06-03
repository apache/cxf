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

package org.apache.cxf.systest.jaxws;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.no_body_parts.types.Operation1;
import org.apache.cxf.no_body_parts.types.Operation1Response;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.BeforeClass;
import org.junit.Test;

public class JaxWsDynamicClientTest extends AbstractBusClientServerTestBase {
    static final String PORT = TestUtil.getPortNumber(ServerNoBodyParts.class);

    private String md5(byte[] bytes) {
        MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        algorithm.reset();
        algorithm.update(bytes);
        byte messageDigest[] = algorithm.digest();

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        }
        return hexString.toString();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(ServerNoBodyParts.class, true));
    }
    
    @Test
    public void testInvocation() throws Exception {
        JaxWsDynamicClientFactory dcf = 
            JaxWsDynamicClientFactory.newInstance();
        URL wsdlURL = new URL("http://localhost:" + PORT + "/NoBodyParts/NoBodyPartsService?wsdl");
        Client client = dcf.createClient(wsdlURL);
        byte[] bucketOfBytes = 
            IOUtils.readBytesFromStream(getClass().getResourceAsStream("/wsdl/no_body_parts.wsdl"));
        Operation1 parameters = new Operation1();
        parameters.setOptionString("opt-ion");
        parameters.setTargetType("tar-get");
        Object[] rparts = client.invoke("operation1", parameters, bucketOfBytes);
        Operation1Response r = (Operation1Response)rparts[0];
        assertEquals(md5(bucketOfBytes), r.getStatus());
        
        ClientCallback callback = new ClientCallback();
        client.invoke(callback, "operation1", parameters, bucketOfBytes);
        rparts = callback.get();
        r = (Operation1Response)rparts[0];
        assertEquals(md5(bucketOfBytes), r.getStatus());
    }
    
}
