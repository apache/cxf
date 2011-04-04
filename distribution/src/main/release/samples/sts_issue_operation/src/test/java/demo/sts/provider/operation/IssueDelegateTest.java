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

package demo.sts.provider.operation;



import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.UseKeyType;
import org.apache.cxf.ws.security.sts.provider.model.xmldsig.KeyInfoType;
import org.apache.cxf.ws.security.sts.provider.model.xmldsig.X509DataType;

import org.easymock.classextension.EasyMock;

import org.junit.Ignore;
import org.junit.Test;

import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.verify;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import demo.sts.provider.cert.CertificateVerifierConfig;
import demo.sts.provider.token.SAMLTokenIssueOperation;
import demo.sts.provider.token.Saml1TokenProvider;
import demo.sts.provider.token.Saml2TokenProvider;
import demo.sts.provider.token.TokenProvider;

public class IssueDelegateTest {

    private static final String CERT_DATA =
        "MIIEFjCCA3+gAwIBAgIJAJORWX2Xsa8DMA0GCSqGSIb3DQEBBQUAMIG5MQswCQYDVQQGEwJVUzERMA8G"
        + "A1UECBMITmV3IFlvcmsxFjAUBgNVBAcTDU5pYWdhcmEgRmFsbHMxLDAqBgNVBAoTI1NhbXBsZSBDbG"
        + "llbnQgLS0gTk9UIEZPUiBQUk9EVUNUSU9OMRYwFAYDVQQLEw1JVCBEZXBhcnRtZW50MRcwFQYDVQQD"
        + "Ew53d3cuY2xpZW50LmNvbTEgMB4GCSqGSIb3DQEJARYRY2xpZW50QGNsaWVudC5jb20wHhcNMTEwMj"
        + "A5MTgzMDI3WhcNMjEwMjA2MTgzMDI3WjCBuTELMAkGA1UEBhMCVVMxETAPBgNVBAgTCE5ldyBZb3Jr"
        + "MRYwFAYDVQQHEw1OaWFnYXJhIEZhbGxzMSwwKgYDVQQKEyNTYW1wbGUgQ2xpZW50IC0tIE5PVCBGT1"
        + "IgUFJPRFVDVElPTjEWMBQGA1UECxMNSVQgRGVwYXJ0bWVudDEXMBUGA1UEAxMOd3d3LmNsaWVudC5j"
        + "b20xIDAeBgkqhkiG9w0BCQEWEWNsaWVudEBjbGllbnQuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNAD"
        + "CBiQKBgQDauFNVqi4B2+u/PC9ktDkn82bglEQYcL4o5JRUhQVEhTK2iEloz1Rvo/qyfDhBPc1lzIUn"
        + "4ams+DKBSSjZMCgop3XbeCXzIVP784ruC8HF5QrYsXUQfTc7lzqafXZXH8Bk89gSScA1fFme6TpvYz"
        + "M0zjBETSXADtKOs9oKB2VOIwIDAQABo4IBIjCCAR4wHQYDVR0OBBYEFFIz+0BSZlLtXkA/udRjRgph"
        + "tREuMIHuBgNVHSMEgeYwgeOAFFIz+0BSZlLtXkA/udRjRgphtREuoYG/pIG8MIG5MQswCQYDVQQGEw"
        + "JVUzERMA8GA1UECBMITmV3IFlvcmsxFjAUBgNVBAcTDU5pYWdhcmEgRmFsbHMxLDAqBgNVBAoTI1Nh"
        + "bXBsZSBDbGllbnQgLS0gTk9UIEZPUiBQUk9EVUNUSU9OMRYwFAYDVQQLEw1JVCBEZXBhcnRtZW50MR"
        + "cwFQYDVQQDEw53d3cuY2xpZW50LmNvbTEgMB4GCSqGSIb3DQEJARYRY2xpZW50QGNsaWVudC5jb22C"
        + "CQCTkVl9l7GvAzAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBQUAA4GBAEjEr9QfaYsZf7ELnqB++O"
        + "kWcKxpMt1Yj/VOyL99AekkVTM+rRHCU9Bu+tncMNsfy8mIXUC1JqKQ+Cq5RlaDh/ujzt6i17G7uSGd"
        + "6U1U/DPZBqTm3Dxwl1cMAGU/CoAKTWE+o+fS4Q2xHv7L1KiXQQc9EWJ4C34Ik45fB6g3DiTj";

    RequestSecurityTokenType requestMock = createMock(RequestSecurityTokenType.class);

    private String storePath = "/stsstore.jks";
    private String storePwd = "stsspass";
    private String keySignAlias = "mystskey";
    private String keySignPwd = "stskpass";

    @Test
    public void testIssueDelegateNullParameter() {
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();

        try {
            id.issue(null, null);
            fail("NullPointerException should be thrown");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testIssueDelegate() {
        
        WebServiceContext context = EasyMock.createNiceMock(WebServiceContext.class);
        MessageContext ctx2 = EasyMock.createNiceMock(MessageContext.class);
        EasyMock.expect(context.getMessageContext()).andReturn(ctx2).anyTimes();
        UsernameToken token = new UsernameToken("joe", null, null, false, null, null);
        EasyMock.expect(ctx2.get(SecurityToken.class.getName())).andReturn(token).anyTimes();

        
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();
        CertificateVerifierConfig certificateVerifierConfig = new CertificateVerifierConfig();
        certificateVerifierConfig.setTrustCertAliases(Arrays.asList("cacert"));
        certificateVerifierConfig.setKeySignAlias(keySignAlias);
        certificateVerifierConfig.setKeySignPwd(keySignPwd);
        certificateVerifierConfig.setStorePath(storePath);
        certificateVerifierConfig.setStorePwd(storePwd);
        id.setCertificateVerifierConfig(certificateVerifierConfig);

        JAXBElement<String> tokenType = new JAXBElement<String>(
                new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512",
                        "TokenType"), String.class,
                "urn:oasis:names:tc:SAML:1.0:assertion");

        EasyMock.expect(requestMock.getAny()).andStubReturn(
                Arrays.asList((Object) tokenType));
        
        
        EasyMock.replay(requestMock, context, ctx2);

        TokenProvider tp1 = new Saml1TokenProvider();
        TokenProvider tp2 = new Saml2TokenProvider();
        id.setTokenProviders(Arrays.asList(tp1, tp2));

        id.issue(requestMock, context);

        verify(requestMock);
    }

    @Test
    public void testIssueDelegateWrongSignKey() {
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();
        CertificateVerifierConfig certificateVerifierConfig = new CertificateVerifierConfig();
        certificateVerifierConfig.setTrustCertAliases(Arrays.asList("cacert"));
        certificateVerifierConfig.setKeySignAlias(keySignAlias);
        certificateVerifierConfig.setKeySignPwd("xxx");
        certificateVerifierConfig.setStorePath(storePath);
        certificateVerifierConfig.setStorePwd(storePwd);
        id.setCertificateVerifierConfig(certificateVerifierConfig);

        JAXBElement<String> tokenType = new JAXBElement<String>(new QName(
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
                "TokenType"), String.class,
                "urn:oasis:names:tc:SAML:1.0:assertion");

        EasyMock.expect(requestMock.getAny()).andStubReturn(
                Arrays.asList((Object) tokenType));

        EasyMock.replay(requestMock);

        TokenProvider tp1 = new Saml1TokenProvider();
        TokenProvider tp2 = new Saml2TokenProvider();
        id.setTokenProviders(Arrays.asList(tp1, tp2));

        try {
            id.issue(requestMock, null);
            fail("STSException should be thrown");
        } catch (STSException e) {
            // expected 
        } finally {
            verify(requestMock);
        }
    }

    @Test
    public void testIssueDelegateWrongSignAlias() {
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();
        CertificateVerifierConfig certificateVerifierConfig = new CertificateVerifierConfig();
        certificateVerifierConfig.setTrustCertAliases(Arrays.asList("cacert"));
        certificateVerifierConfig.setKeySignAlias("xxx");
        certificateVerifierConfig.setKeySignPwd(keySignPwd);
        certificateVerifierConfig.setStorePath(storePath);
        certificateVerifierConfig.setStorePwd(storePwd);
        id.setCertificateVerifierConfig(certificateVerifierConfig);

        JAXBElement<String> tokenType = new JAXBElement<String>(new QName(
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
                "TokenType"), String.class,
                "urn:oasis:names:tc:SAML:1.0:assertion");

        EasyMock.expect(requestMock.getAny()).andStubReturn(
                Arrays.asList((Object) tokenType));

        EasyMock.replay(requestMock);

        TokenProvider tp1 = new Saml1TokenProvider();
        TokenProvider tp2 = new Saml2TokenProvider();
        id.setTokenProviders(Arrays.asList(tp1, tp2));

        try {
            id.issue(requestMock, null);

            fail("STSException should be thrown");
        } catch (STSException e) {
            // expected 
        } finally {
            verify(requestMock);
        }
    }

    @Test
    public void testIssueDelegateUsernameNull() {
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();
        assertNotNull(id);

        EasyMock.expect(requestMock.getAny()).andStubReturn(Arrays.asList());
        EasyMock.replay(requestMock);

        TokenProvider tp1 = new Saml1TokenProvider();
        TokenProvider tp2 = new Saml2TokenProvider();
        id.setTokenProviders(Arrays.asList(tp1, tp2));

        try {
            id.issue(requestMock, null);
            fail("STSException should be thrown");
        } catch (STSException e) {
            //expected
        }
        verify(requestMock);
    }

    @Ignore
    @Test
    public void testIssueDelegateWithCert() throws Exception {
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();
        assertNotNull(id);
        CertificateVerifierConfig certificateVerifierConfig = new CertificateVerifierConfig();
        certificateVerifierConfig.setTrustCertAliases(Arrays.asList("cacert"));
        certificateVerifierConfig.setKeySignAlias(keySignAlias);
        certificateVerifierConfig.setKeySignPwd(keySignPwd);
        certificateVerifierConfig.setStorePath(storePath);
        certificateVerifierConfig.setStorePwd(storePwd);
        id.setCertificateVerifierConfig(certificateVerifierConfig);
        JAXBElement<byte[]> jX509Certificate = new JAXBElement<byte[]>(
                QName.valueOf("X509Certificate"), byte[].class,
                Base64Utility.decode(CERT_DATA));

        X509DataType x509DataType = new X509DataType();
        x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(
                jX509Certificate);
        JAXBElement<X509DataType> jX509DataType = new JAXBElement<X509DataType>(
                QName.valueOf("X509Data"), X509DataType.class, x509DataType);

        KeyInfoType keyInfoType = new KeyInfoType();
        keyInfoType.getContent().add(jX509DataType);
        JAXBElement<KeyInfoType> jKeyInfoType = new JAXBElement<KeyInfoType>(
                QName.valueOf("KeyInfo"), KeyInfoType.class, keyInfoType);

        UseKeyType useKeyType = new UseKeyType();
        useKeyType.setAny(jKeyInfoType);
        JAXBElement<UseKeyType> jUseKeyType = new JAXBElement<UseKeyType>(
                QName.valueOf("UseKey"), UseKeyType.class, useKeyType);

        JAXBElement<String> tokenType = new JAXBElement<String>(
                new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512",
                        "TokenType"), String.class,
                "urn:oasis:names:tc:SAML:1.0:assertion");

        EasyMock.expect(requestMock.getAny()).andStubReturn(
                Arrays.asList((Object) jUseKeyType, (Object) tokenType));
        EasyMock.replay(requestMock);

        TokenProvider tp1 = new Saml1TokenProvider();
        TokenProvider tp2 = new Saml2TokenProvider();
        id.setTokenProviders(Arrays.asList(tp1, tp2));

        id.issue(requestMock, null);

        verify(requestMock);
    }

    @Test
    public void testIssueDelegateWithCertWithWrongStorePass() throws Exception {
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();

        CertificateVerifierConfig certificateVerifierConfig = new CertificateVerifierConfig();
        certificateVerifierConfig.setTrustCertAliases(Arrays.asList("cacert"));
        certificateVerifierConfig.setKeySignAlias(keySignAlias);
        certificateVerifierConfig.setKeySignPwd(keySignPwd);
        certificateVerifierConfig.setStorePath(storePath);
        certificateVerifierConfig.setStorePwd("xxx");
        id.setCertificateVerifierConfig(certificateVerifierConfig);
        JAXBElement<byte[]> jX509Certificate = new JAXBElement<byte[]>(
                QName.valueOf("X509Certificate"), byte[].class,
                Base64Utility.decode(CERT_DATA));

        X509DataType x509DataType = new X509DataType();
        x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(
                jX509Certificate);
        JAXBElement<X509DataType> jX509DataType = new JAXBElement<X509DataType>(
                QName.valueOf("X509Data"), X509DataType.class, x509DataType);

        KeyInfoType keyInfoType = new KeyInfoType();
        keyInfoType.getContent().add(jX509DataType);
        JAXBElement<KeyInfoType> jKeyInfoType = new JAXBElement<KeyInfoType>(
                QName.valueOf("KeyInfo"), KeyInfoType.class, keyInfoType);

        UseKeyType useKeyType = new UseKeyType();
        useKeyType.setAny(jKeyInfoType);
        JAXBElement<UseKeyType> jUseKeyType = new JAXBElement<UseKeyType>(
                QName.valueOf("UseKey"), UseKeyType.class, useKeyType);

        JAXBElement<String> tokenType = new JAXBElement<String>(new QName(
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
                "TokenType"), String.class,
                "urn:oasis:names:tc:SAML:1.0:assertion");

        EasyMock.expect(requestMock.getAny()).andStubReturn(
                Arrays.asList((Object) jUseKeyType, (Object) tokenType));
        EasyMock.replay(requestMock);

        TokenProvider tp1 = new Saml1TokenProvider();
        TokenProvider tp2 = new Saml2TokenProvider();
        id.setTokenProviders(Arrays.asList(tp1, tp2));

        try {
            id.issue(requestMock, null);
            fail("STSException should be thrown");
        } catch (STSException e) {
            // expected
        } finally {
            verify(requestMock);
        }
    }

    @Test
    public void testIssueDelegateWithCertWithoutTokenProvidersAndTokenType() throws Exception {
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();

        CertificateVerifierConfig certificateVerifierConfig = new CertificateVerifierConfig();
        certificateVerifierConfig.setTrustCertAliases(Arrays.asList("cacert"));
        certificateVerifierConfig.setKeySignAlias(keySignAlias);
        certificateVerifierConfig.setKeySignPwd(keySignPwd);
        certificateVerifierConfig.setStorePath(storePath);
        certificateVerifierConfig.setStorePwd(storePwd);
        id.setCertificateVerifierConfig(certificateVerifierConfig);
        JAXBElement<byte[]> jX509Certificate = new JAXBElement<byte[]>(
                QName.valueOf("X509Certificate"), byte[].class,
                Base64Utility.decode(CERT_DATA));

        X509DataType x509DataType = new X509DataType();
        x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(
                jX509Certificate);
        JAXBElement<X509DataType> jX509DataType = new JAXBElement<X509DataType>(
                QName.valueOf("X509Data"), X509DataType.class, x509DataType);

        KeyInfoType keyInfoType = new KeyInfoType();
        keyInfoType.getContent().add(jX509DataType);
        JAXBElement<KeyInfoType> jKeyInfoType = new JAXBElement<KeyInfoType>(
                QName.valueOf("KeyInfo"), KeyInfoType.class, keyInfoType);

        UseKeyType useKeyType = new UseKeyType();
        useKeyType.setAny(jKeyInfoType);
        JAXBElement<UseKeyType> jUseKeyType = new JAXBElement<UseKeyType>(
                QName.valueOf("UseKey"), UseKeyType.class, useKeyType);

        EasyMock.expect(requestMock.getAny()).andStubReturn(
                Arrays.asList((Object) jUseKeyType));
        EasyMock.replay(requestMock);


        List<TokenProvider> tps = Collections.emptyList();
        id.setTokenProviders(tps);

        try {
            id.issue(requestMock, null);
            fail("STSException should be thrown");
        } catch (STSException e) {
            //expected
        } finally {
            verify(requestMock);
        }
    }

    @Test
    public void testIssueDelegateWithoutCertAndUserToken() throws CertificateException {
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();

        JAXBElement<String> tokenType = new JAXBElement<String>(new QName(
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
                "TokenType"), String.class,
                "urn:oasis:names:tc:SAML:1.0:assertion");

        EasyMock.expect(requestMock.getAny()).andStubReturn(
                Arrays.asList((Object) tokenType));
        EasyMock.replay(requestMock);

        TokenProvider tp1 = new Saml1TokenProvider();
        TokenProvider tp2 = new Saml2TokenProvider();
        id.setTokenProviders(Arrays.asList(tp1, tp2));

        try {
            id.issue(requestMock, null);

            fail("STSException should be thrown");
        } catch (STSException e) {
            // expected
        } finally {
            verify(requestMock);
        }
    }

    @Test
    public void testIssueDelegateWithInvalidCert() throws CertificateException {
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();
        assertNotNull(id);

        // CertificateFactory certificateFactory =
        // CertificateFactory.getInstance("X.509");
        // X509Certificate x509Certificate = null;
        // try {
        // x509Certificate =
        // (X509Certificate)certificateFactory.generateCertificate(new
        // ByteArrayInputStream(Base64.decodeBase64(CERT_DATA.getBytes())));
        // } catch (CertificateException e) {
        // e.printStackTrace();
        // }
        // JAXBElement<X509Certificate> jX509Certificate = new
        // JAXBElement<X509Certificate>(QName.valueOf("X509Certificate"),
        // X509Certificate.class, x509Certificate);

        JAXBElement<byte[]> jX509Certificate = new JAXBElement<byte[]>(
                QName.valueOf("X509Certificate"), byte[].class,
                CERT_DATA.getBytes());

        X509DataType x509DataType = new X509DataType();
        x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(
                jX509Certificate);
        JAXBElement<X509DataType> jX509DataType = new JAXBElement<X509DataType>(
                QName.valueOf("X509Data"), X509DataType.class, x509DataType);

        KeyInfoType keyInfoType = new KeyInfoType();
        keyInfoType.getContent().add(jX509DataType);
        JAXBElement<KeyInfoType> jKeyInfoType = new JAXBElement<KeyInfoType>(
                QName.valueOf("KeyInfo"), KeyInfoType.class, keyInfoType);

        UseKeyType useKeyType = new UseKeyType();
        useKeyType.setAny(jKeyInfoType);
        JAXBElement<UseKeyType> jUseKeyType = new JAXBElement<UseKeyType>(
                QName.valueOf("UseKey"), UseKeyType.class, useKeyType);

        EasyMock.expect(requestMock.getAny()).andStubReturn(
                Arrays.asList((Object) jUseKeyType));
        EasyMock.replay(requestMock);


        TokenProvider tp1 = new Saml1TokenProvider();
        TokenProvider tp2 = new Saml2TokenProvider();
        id.setTokenProviders(Arrays.asList(tp1, tp2));

        try {
            id.issue(requestMock, null);
            fail("STSException should be thrown");
        } catch (STSException e) {
            //expected
        }

        verify(requestMock);
    }

    @Test
    public void testIssueDelegateWithInvalidCert2() throws Exception {
        SAMLTokenIssueOperation id = new SAMLTokenIssueOperation();
        assertNotNull(id);

        CertificateFactory certificateFactory = CertificateFactory
                .getInstance("X.509");
        X509Certificate x509Certificate = (X509Certificate) certificateFactory
                    .generateCertificate(new ByteArrayInputStream(Base64Utility
                            .decode(CERT_DATA)));
        JAXBElement<X509Certificate> jX509Certificate = new JAXBElement<X509Certificate>(
                QName.valueOf("X509Certificate"), X509Certificate.class,
                x509Certificate);

        // JAXBElement<byte[]> jX509Certificate = new
        // JAXBElement<byte[]>(QName.valueOf("X509Certificate"), byte[].class,
        // CERT_DATA.getBytes());

        X509DataType x509DataType = new X509DataType();
        x509DataType.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(
                jX509Certificate);
        JAXBElement<X509DataType> jX509DataType = new JAXBElement<X509DataType>(
                QName.valueOf("X509Data"), X509DataType.class, x509DataType);

        KeyInfoType keyInfoType = new KeyInfoType();
        keyInfoType.getContent().add(jX509DataType);
        JAXBElement<KeyInfoType> jKeyInfoType = new JAXBElement<KeyInfoType>(
                QName.valueOf("KeyInfo"), KeyInfoType.class, keyInfoType);

        UseKeyType useKeyType = new UseKeyType();
        useKeyType.setAny(jKeyInfoType);
        JAXBElement<UseKeyType> jUseKeyType = new JAXBElement<UseKeyType>(
                QName.valueOf("UseKey"), UseKeyType.class, useKeyType);

        EasyMock.expect(requestMock.getAny()).andStubReturn(
                Arrays.asList((Object) jUseKeyType));
        EasyMock.replay(requestMock);

        TokenProvider tp1 = new Saml1TokenProvider();
        TokenProvider tp2 = new Saml2TokenProvider();
        id.setTokenProviders(Arrays.asList(tp1, tp2));


        try {
            id.issue(requestMock, null);
            fail("CertificateException should be thrown");
        } catch (Exception e) {
            //expected
        }

        verify(requestMock);
    }
}
