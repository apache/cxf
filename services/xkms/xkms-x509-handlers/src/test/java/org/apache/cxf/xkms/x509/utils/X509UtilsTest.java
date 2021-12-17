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

package org.apache.cxf.xkms.x509.utils;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.List;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.xkms.exception.XKMSRequestException;
import org.apache.cxf.xkms.model.xkms.ValidateRequestType;
import org.apache.cxf.xkms.x509.validator.BasicValidationTest;
import org.apache.cxf.xkms.x509.validator.ValidateRequestParser;

import org.junit.Assert;
import org.junit.Test;

public class X509UtilsTest extends BasicValidationTest {
    private static final String CERT_DN = "CN=www.anothersts.com, L=CGN, ST=NRW, C=DE, O=AnotherSTS";

    @Test
    @org.junit.Ignore
    public void extractValidatingCertsOK() throws JAXBException {
        InputStream is = this.getClass().getResourceAsStream("/validateRequestOK.xml");
        @SuppressWarnings("unchecked")
        JAXBElement<ValidateRequestType> request = (JAXBElement<ValidateRequestType>)unmarshaller.unmarshal(is);
        List<X509Certificate> certs = ValidateRequestParser.parse(request.getValue());
        Assert.assertEquals("Exactly one certificate should be found", 1, certs.size());
        Assert.assertEquals("Unexcpected certificate DN", CERT_DN, certs.get(0).getSubjectDN().getName());
    }

    @Test(expected = XKMSRequestException.class)
    public void extractValidatingCertsCorrupted() throws JAXBException {
        InputStream is = this.getClass().getResourceAsStream("/validateRequestCorrupted.xml");
        @SuppressWarnings("unchecked")
        JAXBElement<ValidateRequestType> request = (JAXBElement<ValidateRequestType>)unmarshaller.unmarshal(is);
        ValidateRequestParser.parse(request.getValue());
    }
}
