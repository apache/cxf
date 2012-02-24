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
package org.apache.cxf.systest.jaxrs.security.oauth;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.oauth.OAuth;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.ParameterStyle;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.eclipse.jetty.http.HttpHeaders;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TemporaryCredentialServiceTest extends AbstractBusClientServerTestBase {

    public static final String TEMPORARY_CREDENTIALS_URL = "/a/oauth/initiate";
    public static final String HOST = "http://localhost:";

    private static final Logger LOG = LogUtils.getL7dLogger(TemporaryCredentialServiceTest.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(OAuthServer.class, true));
    }
    
    @Ignore
    @Test
    public void testGetTemporaryCredentialsURIQuery() throws Exception {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(OAuth.OAUTH_CALLBACK, OAuthTestUtils.CALLBACK);
        
        //check all parameter transmissions
        for (ParameterStyle style : ParameterStyle.values()) {
            //for all signing methods
            for (String signMethod : OAuthTestUtils.SIGN_METHOD) {
                LOG.log(Level.INFO, "Preparing request with parameter style: {0} and signature method: {1}",
                    new String[] {style.toString(), signMethod});

                parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, signMethod);
                parameters.put(OAuth.OAUTH_NONCE, UUID.randomUUID().toString());
                parameters.put(OAuth.OAUTH_TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000));
                parameters.put(OAuth.OAUTH_CONSUMER_KEY, OAuthTestUtils.CLIENT_ID);
                OAuthMessage message = invokeRequestToken(parameters, style, OAuthServer.PORT);

                //test response ok
                boolean isFormEncoded = OAuth.isFormEncoded(message.getBodyType());
                Assert.assertTrue(isFormEncoded);

                List<OAuth.Parameter> responseParams = OAuthTestUtils.getResponseParams(message);

                String wwwHeader = message.getHeader(HttpHeaders.WWW_AUTHENTICATE);
                Assert.assertNull(wwwHeader);

                String callbacConf = OAuthTestUtils
                    .findOAuthParameter(responseParams, OAuth.OAUTH_CALLBACK_CONFIRMED)
                    .getValue();
                Assert.assertEquals("true", callbacConf);

                String oauthToken = OAuthTestUtils.findOAuthParameter(responseParams, OAuth.OAUTH_TOKEN)
                    .getKey();
                Assert.assertFalse(StringUtils.isEmpty(oauthToken));

                String tokenSecret = OAuthTestUtils
                    .findOAuthParameter(responseParams, OAuth.OAUTH_TOKEN_SECRET)
                    .getKey();
                Assert.assertFalse(StringUtils.isEmpty(tokenSecret));


                //test wrong client id
                parameters.put(OAuth.OAUTH_CONSUMER_KEY, "wrong");
                message = invokeRequestToken(parameters, style, OAuthServer.PORT);

                wwwHeader = message.getHeader(HttpHeaders.WWW_AUTHENTICATE);
                List<OAuth.Parameter> list = OAuthMessage.decodeAuthorization(wwwHeader);

                String oauthProblem = OAuthTestUtils.findOAuthParameter(list, "oauth_problem").getValue();
                Assert.assertEquals(OAuth.Problems.CONSUMER_KEY_UNKNOWN, oauthProblem);
            }
        }
    }

    protected OAuthMessage invokeRequestToken(Map<String, String> parameters, ParameterStyle style,
                                              int port)
        throws IOException, URISyntaxException, OAuthException {
        OAuthMessage message;
        String uri = HOST + port + TEMPORARY_CREDENTIALS_URL;
        message = OAuthTestUtils
            .access(uri, OAuthMessage.POST, parameters, style);
        return message;
    }

}
