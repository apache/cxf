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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import net.oauth.OAuth;
import net.oauth.OAuthMessage;

public final class OAuthTestUtils {

    public static final String CALLBACK = "http://www.example.com/callback";
    public static final String APPLICATION_NAME = "Test Oauth 1.0 application";
    public static final String CLIENT_ID = "12345678";
    public static final String CLIENT_SECRET = "secret";
    

    private OAuthTestUtils() {
    }


    public static String readBody(OAuthMessage msg) throws IOException {
        StringBuilder body = new StringBuilder();
        try (InputStream responseBody = msg.getBodyAsStream()) {
            if (responseBody != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(responseBody))) {
                    String buf;
                    while ((buf = br.readLine()) != null) {
                        body.append(buf);
                    }
                }
            }
        }

        return body.toString().trim();
    }

    public static OAuth.Parameter findOAuthParameter(List<OAuth.Parameter> list, String key) {
        for (OAuth.Parameter parameter : list) {
            if (key.equals(parameter.getKey())) {
                return parameter;
            }
        }
        return null;
    }

    public static List<OAuth.Parameter> getResponseParams(OAuthMessage message) throws IOException {
        String body = OAuthTestUtils.readBody(message);
        return OAuth.decodeForm(body);
    }
}
