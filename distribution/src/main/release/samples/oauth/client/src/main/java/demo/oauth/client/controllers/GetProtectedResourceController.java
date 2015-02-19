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
package demo.oauth.client.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.ParameterStyle;
import net.oauth.client.OAuthClient;
import net.oauth.client.OAuthResponseMessage;
import net.oauth.client.URLConnectionClient;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import demo.oauth.client.model.OAuthParams;

@Controller
public class GetProtectedResourceController {

    @RequestMapping("/getProtectedResource")
    protected ModelAndView handleRequest(@ModelAttribute("oAuthParams") OAuthParams oAuthParams,
                                         HttpServletRequest request)
        throws Exception {

        OAuthServiceProvider provider = new OAuthServiceProvider(
            oAuthParams.getTemporaryCredentialsEndpoint(),
            oAuthParams.getResourceOwnerAuthorizationEndpoint(), null);

        OAuthConsumer consumer = new OAuthConsumer(null, oAuthParams.getClientID(),
            oAuthParams.getClientSecret(),
            provider);
        OAuthAccessor accessor = new OAuthAccessor(consumer);
        accessor.requestToken = oAuthParams.getOauthToken();
        accessor.tokenSecret = oAuthParams.getOauthTokenSecret(); 

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(OAuth.OAUTH_SIGNATURE_METHOD, oAuthParams.getSignatureMethod());
        parameters.put(OAuth.OAUTH_NONCE, UUID.randomUUID().toString());
        parameters.put(OAuth.OAUTH_TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000));
        parameters.put(OAuth.OAUTH_TOKEN, oAuthParams.getOauthToken());
        parameters.put(OAuth.OAUTH_CONSUMER_KEY, oAuthParams.getClientID());

        OAuthMessage msg = null;
        String method = request.getParameter("op");


        if ("GET".equals(method)) {
            msg = accessor
                .newRequestMessage(OAuthMessage.GET, oAuthParams.getGetResourceURL(), parameters.entrySet());
        } else {
            msg = accessor
                .newRequestMessage(OAuthMessage.POST, oAuthParams.getPostResourceURL(),
                    parameters.entrySet());
        }


        OAuthClient client = new OAuthClient(new URLConnectionClient());

        msg = client.access(msg, ParameterStyle.QUERY_STRING);

        StringBuilder bodyBuffer = readBody(msg);

        oAuthParams.setResourceResponse(bodyBuffer.toString());
        String authHeader = msg.getHeader("WWW-Authenticate");
        String oauthHeader = msg.getHeader("OAuth");
        String header = "";

        if (authHeader != null) {
            header += "WWW-Authenticate:" + authHeader;
        }

        if (oauthHeader != null) {
            header += "OAuth:" + oauthHeader;
        }

        oAuthParams.setHeader(header);
        oAuthParams.setResponseCode(((OAuthResponseMessage)msg).getHttpResponse().getStatusCode());

        return new ModelAndView("accessToken");
    }

    private StringBuilder readBody(OAuthMessage msg) throws IOException {
        StringBuilder body = new StringBuilder();
        InputStream responseBody = null;
        BufferedReader br = null;
        try {
            responseBody = msg.getBodyAsStream();
            if (responseBody != null) {
                br = new BufferedReader(new InputStreamReader(responseBody));
                String buf;
                while ((buf = br.readLine()) != null) {
                    body.append(buf);
                }
            }
        } finally {
            if (br != null) {
                br.close();
            }
            if (responseBody != null) {
                responseBody.close();
            }
        }
        return body;
    }

}
