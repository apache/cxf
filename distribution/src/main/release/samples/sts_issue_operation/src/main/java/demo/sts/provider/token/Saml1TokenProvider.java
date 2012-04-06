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

package demo.sts.provider.token;

import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.apache.cxf.common.logging.LogUtils;
import org.joda.time.DateTime;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.security.x509.X509KeyInfoGeneratorFactory;
import org.opensaml.xml.signature.KeyInfo;


public class Saml1TokenProvider implements TokenProvider {

    private static final Logger LOG = LogUtils.getL7dLogger(Saml1TokenProvider.class);
    private static final String RESPONSE_TOKENTYPE_SAML1 
        = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
    
    public String getResponseTokentype() {
        return RESPONSE_TOKENTYPE_SAML1;
    }

    public String getTokenType() {
        return SAMLConstants.SAML1_NS;
    }

    public Element createToken(X509Certificate certificate) {
        try {
            org.opensaml.saml1.core.Subject subject = createSubjectSAML1(certificate);
            org.opensaml.saml1.core.Assertion samlAssertion = createAuthnAssertionSAML1(subject);
            return SamlUtils.toDom(samlAssertion).getDocumentElement();
        } catch (Exception e) {
            throw new TokenException("Can't serialize SAML assertion", e);
        }
    }

    public Element createToken(String username) {
        try {
            org.opensaml.saml1.core.Subject subject = createSubjectSAML1(username);
            org.opensaml.saml1.core.Assertion samlAssertion = createAuthnAssertionSAML1(subject);
            return SamlUtils.toDom(samlAssertion).getDocumentElement();
        } catch (Exception e) {
            throw new TokenException("Can't serialize SAML assertion", e);
        }
    }

    public String getTokenId(Element token) {
        return token
                .getAttribute(org.opensaml.saml1.core.Assertion.ID_ATTRIB_NAME);
    }

    private org.opensaml.saml1.core.Subject createSubjectSAML1(String username) {
        org.opensaml.saml1.core.NameIdentifier nameID = 
            (new org.opensaml.saml1.core.impl.NameIdentifierBuilder())
                .buildObject();
        nameID.setNameIdentifier(username);
        String format = "urn:oasis:names:tc:SAML:1.1:nameid-format:transient";

        if (format != null) {
            nameID.setFormat(format);
        }

        org.opensaml.saml1.core.Subject subject = (new org.opensaml.saml1.core.impl.SubjectBuilder())
                .buildObject();
        subject.setNameIdentifier(nameID);

        String confirmationString = "urn:oasis:names:tc:SAML:1.0:cm:bearer";

        if (confirmationString != null) {

            org.opensaml.saml1.core.ConfirmationMethod confirmationMethod = 
                (new org.opensaml.saml1.core.impl.ConfirmationMethodBuilder())
                    .buildObject();
            confirmationMethod.setConfirmationMethod(confirmationString);

            org.opensaml.saml1.core.SubjectConfirmation confirmation = 
                (new org.opensaml.saml1.core.impl.SubjectConfirmationBuilder())
                    .buildObject();
            confirmation.getConfirmationMethods().add(confirmationMethod);

            subject.setSubjectConfirmation(confirmation);
        }
        return subject;
    }

    private org.opensaml.saml1.core.Subject createSubjectSAML1(
            X509Certificate certificate) throws Exception {
        DefaultBootstrap.bootstrap();
        org.opensaml.saml1.core.NameIdentifier nameID = 
            (new org.opensaml.saml1.core.impl.NameIdentifierBuilder())
                .buildObject();
        nameID.setNameIdentifier(certificate.getSubjectDN().getName());
        nameID.setFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName");
        org.opensaml.saml1.core.Subject subject = (new org.opensaml.saml1.core.impl.SubjectBuilder())
                .buildObject();
        subject.setNameIdentifier(nameID);
        org.opensaml.saml1.core.ConfirmationMethod confirmationMethod = 
            (new org.opensaml.saml1.core.impl.ConfirmationMethodBuilder())
                .buildObject();
        confirmationMethod
                .setConfirmationMethod("Urn:oasis:names:tc:SAML:1.0:cm:holder-of-key");
        org.opensaml.saml1.core.SubjectConfirmation confirmation = 
            (new org.opensaml.saml1.core.impl.SubjectConfirmationBuilder())
                .buildObject();
        confirmation.getConfirmationMethods().add(confirmationMethod);
        BasicX509Credential keyInfoCredential = new BasicX509Credential();
        keyInfoCredential.setEntityCertificate(certificate);
        X509KeyInfoGeneratorFactory kiFactory = new X509KeyInfoGeneratorFactory();
        kiFactory.setEmitPublicKeyValue(true);
        KeyInfo keyInfo = kiFactory.newInstance().generate(keyInfoCredential);
        confirmation.setKeyInfo(keyInfo);
        subject.setSubjectConfirmation(confirmation);
        return subject;
    }

    private org.opensaml.saml1.core.Assertion createAuthnAssertionSAML1(
            org.opensaml.saml1.core.Subject subject) {
        org.opensaml.saml1.core.AuthenticationStatement authnStatement = 
            (new org.opensaml.saml1.core.impl.AuthenticationStatementBuilder())
                .buildObject();
        authnStatement.setSubject(subject);
        // authnStatement.setAuthenticationMethod(strAuthMethod);

        DateTime now = new DateTime();

        authnStatement.setAuthenticationInstant(now);

        org.opensaml.saml1.core.Conditions conditions = (new org.opensaml.saml1.core.impl.ConditionsBuilder())
                .buildObject();
        conditions.setNotBefore(now.minusMillis(3600000));
        conditions.setNotOnOrAfter(now.plusMillis(3600000));

        String issuerURL = "http://www.sopera.de/SAML1";

        org.opensaml.saml1.core.Assertion assertion = (new org.opensaml.saml1.core.impl.AssertionBuilder())
                .buildObject();
        try {
            SecureRandomIdentifierGenerator generator = new SecureRandomIdentifierGenerator();
            assertion.setID(generator.generateIdentifier());
        } catch (NoSuchAlgorithmException e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }

        assertion.setIssuer(issuerURL);
        assertion.setIssueInstant(now);
        assertion.setVersion(SAMLVersion.VERSION_11);

        assertion.getAuthenticationStatements().add(authnStatement);
        // assertion.getAttributeStatements().add(attrStatement);
        assertion.setConditions(conditions);

        return assertion;
    }

}
