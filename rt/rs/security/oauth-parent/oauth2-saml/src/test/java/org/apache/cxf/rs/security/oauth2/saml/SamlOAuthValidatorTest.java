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
package org.apache.cxf.rs.security.oauth2.saml;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import jakarta.ws.rs.NotAuthorizedException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.AudienceRestrictionBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.SubjectConfirmationDataBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.dom.engine.WSSConfig;

import org.junit.Test;

public class SamlOAuthValidatorTest {

    static {
        WSSConfig.init();
    }

    @Test
    public void testValidateWithMatchingClientAddress() throws Exception {
        String tokenServiceAddress = "https://service.example.org/token";
        String clientAddress = "192.0.2.10";

        SamlOAuthValidator validator = new SamlOAuthValidator();
        validator.setAccessTokenServiceAddress(tokenServiceAddress);
        validator.setClientAddress(clientAddress);

        SamlAssertionWrapper wrapper = createAssertion(tokenServiceAddress, clientAddress);

        // This should pass for a valid assertion but currently fails because
        // setClientAddress incorrectly updates issuer instead of clientAddress.
        validator.validate(null, wrapper);
    }

    @Test(expected = NotAuthorizedException.class)
    public void testValidateWithMismatchingClientAddress() throws Exception {
        String tokenServiceAddress = "https://service.example.org/token";

        SamlOAuthValidator validator = new SamlOAuthValidator();
        validator.setAccessTokenServiceAddress(tokenServiceAddress);
        validator.setClientAddress("192.0.2.10");

        SamlAssertionWrapper wrapper = createAssertion(tokenServiceAddress, "192.0.2.20");
        validator.validate(null, wrapper);
    }

    private SamlAssertionWrapper createAssertion(String recipient, String clientAddress) throws Exception {
        SubjectConfirmationDataBean confirmationData = new SubjectConfirmationDataBean();
        confirmationData.setRecipient(recipient);
        confirmationData.setAddress(clientAddress);
        confirmationData.setNotAfter(Instant.now().plus(Duration.ofMinutes(5)));

        ConditionsBean conditions = new ConditionsBean();
        conditions.setNotBefore(Instant.now().minusSeconds(30));
        conditions.setNotAfter(Instant.now().plus(Duration.ofMinutes(5)));
        AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
        audienceRestriction.setAudienceURIs(Collections.singletonList(recipient));
        conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));

        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(new TestSaml2CallbackHandler(conditions, confirmationData), samlCallback);
        return new SamlAssertionWrapper(samlCallback);
    }

    private static class TestSaml2CallbackHandler implements CallbackHandler {
        private final ConditionsBean conditions;
        private final SubjectConfirmationDataBean confirmationData;

        TestSaml2CallbackHandler(ConditionsBean conditions, SubjectConfirmationDataBean confirmationData) {
            this.conditions = conditions;
            this.confirmationData = confirmationData;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (!(callback instanceof SAMLCallback)) {
                    throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
                }

                SAMLCallback samlCallback = (SAMLCallback)callback;
                samlCallback.setSamlVersion(Version.SAML_20);
                samlCallback.setIssuer("https://issuer.example.org");
                samlCallback.setConditions(conditions);

                SubjectBean subject = new SubjectBean(
                    "alice",
                    "service.example.org",
                    SAML2Constants.CONF_BEARER
                );
                subject.setSubjectConfirmationData(confirmationData);
                samlCallback.setSubject(subject);
            }
        }
    }
}
