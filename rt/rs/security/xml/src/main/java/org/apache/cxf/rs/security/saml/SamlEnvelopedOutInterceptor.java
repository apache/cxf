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
package org.apache.cxf.rs.security.saml;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.xml.AbstractXmlSecOutInterceptor;
import org.apache.cxf.rs.security.xml.XmlEncOutInterceptor;
import org.apache.cxf.rs.security.xml.XmlSigOutInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;


public class SamlEnvelopedOutInterceptor extends AbstractXmlSecOutInterceptor {

    private static final String DEFAULT_ENV_PREFIX = "env";
    private static final QName DEFAULT_ENV_QNAME =
        new QName("http://org.apache.cxf/rs/env", "Envelope", DEFAULT_ENV_PREFIX);
    private QName envelopeQName = DEFAULT_ENV_QNAME;
    private boolean signLater;

    public SamlEnvelopedOutInterceptor() {
        // SAML assertions may contain enveloped XML signatures so
        // makes sense to avoid having them signed in the detached mode
        super.addAfter(XmlSigOutInterceptor.class.getName());

        super.addBefore(XmlEncOutInterceptor.class.getName());
    }

    public SamlEnvelopedOutInterceptor(boolean signLater) {
        if (signLater) {
            super.addBefore(XmlSigOutInterceptor.class.getName());
        } else {
            super.addAfter(XmlSigOutInterceptor.class.getName());
        }
        this.signLater = signLater;

        super.addBefore(XmlEncOutInterceptor.class.getName());
    }


    protected Document processDocument(Message message, Document doc)
        throws Exception {
        return createEnvelopedSamlToken(message, doc);
    }

    // enveloping & detached sigs will be supported too
    private Document createEnvelopedSamlToken(Message message, Document payloadDoc)
        throws Exception {

        Element docEl = payloadDoc.getDocumentElement();
        SamlAssertionWrapper assertion = SAMLUtils.createAssertion(message);

        QName rootName = DOMUtils.getElementQName(payloadDoc.getDocumentElement());
        if (rootName.equals(envelopeQName)) {
            docEl.appendChild(assertion.toDOM(payloadDoc));
            return payloadDoc;
        }

        Document newDoc = DOMUtils.createDocument();

        Element root =
            newDoc.createElementNS(envelopeQName.getNamespaceURI(),
                    envelopeQName.getPrefix() + ":" + envelopeQName.getLocalPart());
        newDoc.appendChild(root);

        Element assertionEl = assertion.toDOM(newDoc);
        root.appendChild(assertionEl);

        payloadDoc.removeChild(docEl);
        newDoc.adoptNode(docEl);
        root.appendChild(docEl);

        if (signLater) {
            // It appears adopting and removing nodes
            // leaves some stale refs/state with adopted nodes and thus the digest ends up
            // being wrong on the server side if XML sig is applied later in the enveloped mode
            // TODO: this is not critical now - but figure out if we can avoid copying
            // DOMs
            CachedOutputStream bos = new CachedOutputStream();
            StaxUtils.writeTo(newDoc, bos);
            return StaxUtils.read(bos.getInputStream());
        }
        return newDoc;
    }


    public void setEnvelopeName(String expandedName) {
        setEnvelopeQName(DOMUtils.convertStringToQName(expandedName, DEFAULT_ENV_PREFIX));
    }

    public void setEnvelopeQName(QName name) {
        if (name.getPrefix().length() == 0) {
            name = new QName(name.getNamespaceURI(), name.getLocalPart(), DEFAULT_ENV_PREFIX);
        }
        this.envelopeQName = name;
    }

}
