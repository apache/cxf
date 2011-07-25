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

package org.apache.cxf.systest.ws.kerberos.client;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.TokenElementCallback;


/**
 * A CallbackHandler instance that is used to mock up a Kerberos Token
 */
public class KerberosCallbackHandler implements CallbackHandler {
    
    private String valueType = WSConstants.WSS_GSS_KRB_V5_AP_REQ;
    private String token = "8721958125981";
    
    public KerberosCallbackHandler() {
        //
    }
    
    public void setValueType(String valueType) {
        this.valueType = valueType;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof TokenElementCallback) {
                TokenElementCallback callback = (TokenElementCallback) callbacks[i];
                
                // Mock up a BinarySecurityToken
                Document doc = DOMUtils.createDocument();
                BinarySecurity bst = new BinarySecurity(doc);
                bst.addWSSENamespace();
                bst.addWSUNamespace();
                bst.setID("BST-812847");
                bst.setValueType(valueType);
                bst.setEncodingType(BinarySecurity.BASE64_ENCODING);
                bst.setToken(token.getBytes());
                
                callback.setTokenElement(bst.getElement());
            }
        }
    }
    
}
