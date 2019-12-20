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
package org.apache.cxf.systest.jaxrs.security.oauth2.common;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.wss4j.common.ext.WSPasswordCallback;

public class CallbackHandlerImpl implements CallbackHandler {

    private OAuthDataProvider dataProvider;

    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback) { // CXF
                WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
                if ("alice".equals(pc.getIdentifier())) {
                    pc.setPassword("security");
                    break;
                } else if ("bob".equals(pc.getIdentifier())) {
                    pc.setPassword("security");
                    break;
                } else if (pc.getIdentifier() != null
                    && pc.getIdentifier().startsWith("consumer-id")) {
                    pc.setPassword("this-is-a-secret");
                    break;
                } else if ("service".equals(pc.getIdentifier())) {
                    pc.setPassword("service-pass");
                    break;
                } else if (dataProvider != null) {
                    Client client = dataProvider.getClient(pc.getIdentifier());
                    pc.setPassword(client.getClientSecret());
                    break;
                }
            }
        }
    }

    public OAuthDataProvider getDataProvider() {
        return dataProvider;
    }

    public void setDataProvider(OAuthDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }
}