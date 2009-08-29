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

package org.apache.cxf.systest.jaxrs.security;

import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

public abstract class AbstractSpringSecurityTest extends AbstractBusClientServerTestBase {

    private String getStringFromInputStream(InputStream in) throws Exception {        
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(in, bos);
        in.close();
        bos.close();
        //System.out.println(bos.getOut().toString());        
        return bos.getOut().toString();        
    }
    
    private String base64Encode(String value) {
        return Base64Utility.encode(value.getBytes());
    }
    
    protected void getBook(String endpointAddress, String user, String password, 
                         int expectedStatus) 
        throws Exception {
        
        GetMethod get = new GetMethod(endpointAddress);
        get.setRequestHeader("Accept", "application/xml");
        get.setRequestHeader("Authorization", 
                             "Basic " + base64Encode(user + ":" + password));
        HttpClient httpClient = new HttpClient();
        try {
            int result = httpClient.executeMethod(get);
            assertEquals(expectedStatus, result);
            if (expectedStatus == 200) {
                String content = getStringFromInputStream(get.getResponseBodyAsStream());
                String resource = "/org/apache/cxf/systest/jaxrs/resources/expected_get_book123.txt";
                InputStream expected = getClass().getResourceAsStream(resource);
                assertEquals("Expected value is wrong", 
                             getStringFromInputStream(expected), content);
            }
        } finally {
            get.releaseConnection();
        }
        
    }
   
}
