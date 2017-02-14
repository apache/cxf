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

package org.apache.cxf.systest.ws.saml.client;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;

/**
 * A CallbackHandler instance that is used by the STS to mock up a SAML Attribute Assertion. This
 * particular CallbackHandler creates the SAML Assertion by delegating it to the standard
 * SamlCallbackHandler, and then just sets it on the SAMLCallback as a DOM Element. Essentially,
 * this is a test that it's possible to set a DOM Element on the SAMLCallback and have it included
 * in the request.
 */
public class SamlElementCallbackHandler implements CallbackHandler {
    private boolean saml2 = true;

    public SamlElementCallbackHandler() {
        //
    }

    public SamlElementCallbackHandler(boolean saml2) {
        this.saml2 = saml2;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                Element assertionElement;
                try {
                    Document doc = DOMUtils.createDocument();
                    assertionElement = getSAMLAssertion(doc);
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
                callback.setAssertionElement(assertionElement);
            }
        }
    }

    /**
     * Mock up a SAML Assertion by using another SAMLCallbackHandler
     * @throws Exception
     */
    private Element getSAMLAssertion(Document doc) throws Exception {
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(new SamlCallbackHandler(saml2), samlCallback);
        SamlAssertionWrapper assertionWrapper = new SamlAssertionWrapper(samlCallback);

        return assertionWrapper.toDOM(doc);
    }


}
