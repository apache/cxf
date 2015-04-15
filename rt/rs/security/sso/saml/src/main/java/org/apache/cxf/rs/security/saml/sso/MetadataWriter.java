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

package org.apache.cxf.rs.security.saml.sso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.security.Key;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;

import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.utils.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataWriter {
    
    private static final Logger LOG = LoggerFactory.getLogger(MetadataWriter.class);
    
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();
    private static final DocumentBuilderFactory DOC_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
    private static final XMLSignatureFactory XML_SIGNATURE_FACTORY = XMLSignatureFactory.getInstance("DOM");
    
    static {
        DOC_BUILDER_FACTORY.setNamespaceAware(true);
    }

    //CHECKSTYLE:OFF
    public Document getMetaData(
        String serviceURL,
        String logoutURL,
        Key signingKey,
        X509Certificate signingCert,
        boolean wantRequestsSigned
    ) throws Exception {

        ByteArrayOutputStream bout = new ByteArrayOutputStream(4096);
        Writer streamWriter = new OutputStreamWriter(bout, "UTF-8");
        XMLStreamWriter writer = XML_OUTPUT_FACTORY.createXMLStreamWriter(streamWriter);

        writer.writeStartDocument("UTF-8", "1.0");

        String referenceID = IDGenerator.generateID("_");
        writer.writeStartElement("md", "EntityDescriptor", SSOConstants.SAML2_METADATA_NS);
        writer.writeAttribute("ID", referenceID);

        writer.writeAttribute("entityID", serviceURL);

        writer.writeNamespace("md", SSOConstants.SAML2_METADATA_NS);
        writer.writeNamespace("wsa", SSOConstants.WS_ADDRESSING_NS);
        writer.writeNamespace("xsi", SSOConstants.SCHEMA_INSTANCE_NS);

        writeSAMLMetadata(writer, serviceURL, logoutURL, signingCert, wantRequestsSigned);

        writer.writeEndElement(); // EntityDescriptor

        writer.writeEndDocument();

        streamWriter.flush();
        bout.flush();

        if (LOG.isDebugEnabled()) {
            String out = new String(bout.toByteArray());
            LOG.debug("***************** unsigned ****************");
            LOG.debug(out);
            LOG.debug("***************** unsigned ****************");
        }

        InputStream is = new ByteArrayInputStream(bout.toByteArray());

        if (signingKey != null) {
            return signMetaInfo(signingCert, signingKey, is, referenceID);
        }
        return DOC_BUILDER_FACTORY.newDocumentBuilder().parse(is);
    }
    
    private void writeSAMLMetadata(
        XMLStreamWriter writer, 
        String serviceURL,
        String logoutURL,
        X509Certificate signingCert,
        boolean wantRequestsSigned
    ) throws XMLStreamException, MalformedURLException, CertificateEncodingException {
        
        writer.writeStartElement("md", "SPSSODescriptor", SSOConstants.SAML2_METADATA_NS);
        writer.writeAttribute("AuthnRequestsSigned", Boolean.toString(wantRequestsSigned));
        writer.writeAttribute("WantAssertionsSigned", "true");
        writer.writeAttribute("protocolSupportEnumeration", "urn:oasis:names:tc:SAML:2.0:protocol");
        
        if (logoutURL != null) {
            writer.writeStartElement("md", "SingleLogoutService", SSOConstants.SAML2_METADATA_NS);
            
            writer.writeAttribute("Location", logoutURL);
            
            writer.writeAttribute("Binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
            writer.writeEndElement(); // SingleLogoutService
        }
        
        writer.writeStartElement("md", "AssertionConsumerService", SSOConstants.SAML2_METADATA_NS);
        writer.writeAttribute("Location", serviceURL);
        writer.writeAttribute("index", "0");
        writer.writeAttribute("isDefault", "true");
        writer.writeAttribute("Binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        writer.writeEndElement(); // AssertionConsumerService
        
        writer.writeStartElement("md", "AssertionConsumerService", SSOConstants.SAML2_METADATA_NS);
        writer.writeAttribute("Location", serviceURL);
        writer.writeAttribute("index", "1");
        writer.writeAttribute("isDefault", "true");
        writer.writeAttribute("Binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-REDIRECT");
        writer.writeEndElement(); // AssertionConsumerService
        
        /*
        if (protocol.getClaimTypesRequested() != null && !protocol.getClaimTypesRequested().isEmpty()) {
            writer.writeStartElement("md", "AttributeConsumingService", SSOConstants.SAML2_METADATA_NS);
            writer.writeAttribute("index", "0");
            
            writer.writeStartElement("md", "ServiceName", SSOConstants.SAML2_METADATA_NS);
            writer.writeAttribute("xml:lang", "en");
            writer.writeCharacters(config.getName());
            writer.writeEndElement(); // ServiceName
            
            for (Claim claim : protocol.getClaimTypesRequested()) {
                writer.writeStartElement("md", "RequestedAttribute", SSOConstants.SAML2_METADATA_NS);
                writer.writeAttribute("isRequired", Boolean.toString(claim.isOptional()));
                writer.writeAttribute("Name", claim.getType());
                writer.writeAttribute("NameFormat", 
                                      "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified");
                writer.writeEndElement(); // RequestedAttribute
            }
            
            writer.writeEndElement(); // AttributeConsumingService
        }
        */
        
        if (signingCert != null) {
            writer.writeStartElement("md", "KeyDescriptor", SSOConstants.SAML2_METADATA_NS);
            writer.writeAttribute("use", "signing");
            
            writer.writeStartElement("ds", "KeyInfo", "http://www.w3.org/2000/09/xmldsig#");
            writer.writeNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");
            writer.writeStartElement("ds", "X509Data", "http://www.w3.org/2000/09/xmldsig#");
            writer.writeStartElement("ds", "X509Certificate", "http://www.w3.org/2000/09/xmldsig#");

            // Write the Base-64 encoded certificate
            byte data[] = signingCert.getEncoded();
            String encodedCertificate = Base64.encode(data);
            writer.writeCharacters(encodedCertificate);
            
            writer.writeEndElement(); // X509Certificate
            writer.writeEndElement(); // X509Data
            writer.writeEndElement(); // KeyInfo
            writer.writeEndElement(); // KeyDescriptor
        }
        
        writer.writeEndElement(); // SPSSODescriptor
    }

    private static Document signMetaInfo(X509Certificate signingCert, Key signingKey, 
                                         InputStream metaInfo, String referenceID
    ) throws Exception {
        String signatureMethod = null;
        if ("SHA1withDSA".equals(signingCert.getSigAlgName())) {
            signatureMethod = SignatureMethod.DSA_SHA1;
        } else if ("SHA1withRSA".equals(signingCert.getSigAlgName())) {
            signatureMethod = SignatureMethod.RSA_SHA1;
        } else if ("SHA256withRSA".equals(signingCert.getSigAlgName())) {
            signatureMethod = SignatureMethod.RSA_SHA1;
        } else {
            LOG.error("Unsupported signature method: " + signingCert.getSigAlgName());
            throw new RuntimeException("Unsupported signature method: " + signingCert.getSigAlgName());
        }

        List<Transform> transformList = new ArrayList<Transform>();
        transformList.add(XML_SIGNATURE_FACTORY.newTransform(Transform.ENVELOPED, (TransformParameterSpec)null));
        transformList.add(XML_SIGNATURE_FACTORY.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE,
                                                                          (C14NMethodParameterSpec)null));

        // Create a Reference to the enveloped document (in this case,
        // you are signing the whole document, so a URI of "" signifies
        // that, and also specify the SHA1 digest algorithm and
        // the ENVELOPED Transform.
        Reference ref = 
            XML_SIGNATURE_FACTORY.newReference("#" + referenceID,
                                               XML_SIGNATURE_FACTORY.newDigestMethod(DigestMethod.SHA1, null),
                                               transformList,
                                               null, null);

        // Create the SignedInfo.
        SignedInfo si = 
            XML_SIGNATURE_FACTORY.newSignedInfo(
                XML_SIGNATURE_FACTORY.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, 
                                                                (C14NMethodParameterSpec)null),
                                                                XML_SIGNATURE_FACTORY.newSignatureMethod(signatureMethod, null), 
                                                                Collections.singletonList(ref));

        // Create the KeyInfo containing the X509Data.
        KeyInfoFactory kif = XML_SIGNATURE_FACTORY.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<Object>();
        x509Content.add(signingCert.getSubjectX500Principal().getName());
        x509Content.add(signingCert);
        X509Data xd = kif.newX509Data(x509Content);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

        // Instantiate the document to be signed.
        Document doc = DOC_BUILDER_FACTORY.newDocumentBuilder().parse(metaInfo);

        // Create a DOMSignContext and specify the RSA PrivateKey and
        // location of the resulting XMLSignature's parent element.
        //DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), doc.getDocumentElement());
        DOMSignContext dsc = new DOMSignContext(signingKey, doc.getDocumentElement());
        dsc.setIdAttributeNS(doc.getDocumentElement(), null, "ID");
        dsc.setNextSibling(doc.getDocumentElement().getFirstChild());

        // Create the XMLSignature, but don't sign it yet.
        XMLSignature signature = XML_SIGNATURE_FACTORY.newXMLSignature(si, ki);

        // Marshal, generate, and sign the enveloped signature.
        signature.sign(dsc);

        // Output the resulting document.
        return doc;
    }
    
}
