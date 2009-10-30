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


package org.apache.cxf.transport.https;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.helpers.DOMUtils;

public class CertConstraintsTest extends org.junit.Assert {

    @org.junit.Test
    public void
    testCertConstraints() throws Exception {
        final X509Certificate bethalCert = 
            loadCertificate("Bethal.jks", "JKS", "password", "bethal");
        final X509Certificate gordyCert = 
            loadCertificate("Gordy.jks", "JKS", "password", "gordy");
        
        CertConstraints tmp = null;
        //
        // bethal matches but gordy doesn't
        //
        tmp = loadCertConstraints("subject-CN-bethal");
        assertTrue(tmp.matches(bethalCert) && !tmp.matches(gordyCert));
        //
        // gordy matches but bethal doesn't
        //
        tmp = loadCertConstraints("subject-CN-gordy");
        assertTrue(!tmp.matches(bethalCert) && tmp.matches(gordyCert));
        
        //
        // both are under the ApacheTest organization
        //
        tmp = loadCertConstraints("subject-O-apache");
        assertTrue(tmp.matches(bethalCert) && tmp.matches(gordyCert));
        
        //
        // only bethal is both CN=Bethal and O=ApacheTest
        //
        tmp = loadCertConstraints("subject-CN-bethal-O-apache");
        assertTrue(tmp.matches(bethalCert) && !tmp.matches(gordyCert));
        
        //
        // neither are O=BadApacheTest
        //
        tmp = loadCertConstraints("subject-CN-bethal-O-badapache");
        assertTrue(!tmp.matches(bethalCert) && !tmp.matches(gordyCert));
        
        //
        // both satisfy either CN=Bethal or O=ApacheTest
        //
        tmp = loadCertConstraints("subject-CN-bethal-O-apache-ANY");
        assertTrue(tmp.matches(bethalCert) && tmp.matches(gordyCert));
        
        //
        // only Bethal has "Bethal" as an issuer
        //
        tmp = loadCertConstraints("issuer-CN-bethal-O-apache");
        assertTrue(tmp.matches(bethalCert) && !tmp.matches(gordyCert));
    }

    //
    // Private utilities
    //
    
    private static CertConstraints
    loadCertConstraints(
        final String id
    ) throws Exception {
        CertificateConstraintsType certsConstraintsType = 
            loadCertificateConstraintsType(id);
        return CertConstraintsJaxBUtils.createCertConstraints(certsConstraintsType);
    }
    
    private static CertificateConstraintsType
    loadCertificateConstraintsType(
        final String id
    ) throws Exception {
        return loadGeneratedType(
            CertificateConstraintsType.class, 
            "certConstraints", 
            "resources/cert-constraints.xml", 
            id
        );
    }
    
    private static X509Certificate
    loadCertificate(
        final String keystoreFilename,
        final String keystoreType,
        final String keystorePassword,
        final String id
    ) throws Exception {
        final KeyStore store = KeyStore.getInstance(keystoreType);
        FileInputStream fis = new FileInputStream(
                "src/test/java/org/apache/cxf/transport/https/resources/" + keystoreFilename);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        store.load(bin, keystorePassword.toCharArray());
        for (java.util.Enumeration<String> aliases = store.aliases(); aliases.hasMoreElements();) {
            final String alias = aliases.nextElement();
            if (id.equals(alias)) {
                return (X509Certificate) store.getCertificate(alias);
            }
        }
        assert false;
        throw new RuntimeException("error in test -- keystore " + id + " has no trusted certs");
    }
    
    private static <T> T
    loadGeneratedType(
        final Class<T> cls,
        final String elementName,
        final String name,
        final String id
    ) throws Exception {
        final org.w3c.dom.Document doc = loadDocument(name);
        final org.w3c.dom.Element testData = doc.getDocumentElement();
        final org.w3c.dom.NodeList data = testData.getElementsByTagName("datum");
        for (int i = 0;  i < data.getLength();  ++i) {
            final org.w3c.dom.Element datum = (org.w3c.dom.Element) data.item(i);
            if (datum.getAttribute("id").equals(id)) {
                final org.w3c.dom.NodeList elts = datum.getElementsByTagNameNS(
                    "http://cxf.apache.org/configuration/security", elementName
                );
                assert elts.getLength() == 1;
                return unmarshal(cls, (org.w3c.dom.Element) elts.item(0));
            }
        }
        throw new Exception("Bad test!  No test data with id " + id);
    }
    
    
    private static org.w3c.dom.Document
    loadDocument(
        final String name
    ) throws Exception {
        final java.io.InputStream inStream = 
            CertConstraintsTest.class.getResourceAsStream(name);
        return DOMUtils.readXml(inStream);
    }

    private static <T> T
    unmarshal(
        final Class<T> cls,
        final org.w3c.dom.Element elt
    ) throws JAXBException {
        final JAXBContext ctx = JAXBContext.newInstance(cls.getPackage().getName());
        final Unmarshaller unmarshaller = ctx.createUnmarshaller();
        final JAXBElement<T> jaxbElement = 
            unmarshaller.unmarshal(elt, cls);
        return jaxbElement.getValue();
    }
    
}
