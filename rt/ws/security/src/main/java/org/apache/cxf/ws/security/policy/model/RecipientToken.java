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
package org.apache.cxf.ws.security.policy.model;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;

public class RecipientToken extends TokenWrapper {

    public RecipientToken(SPConstants version) {
        super(version);
    }

    /**
     * @return Returns the receipientToken.
     */
    public Token getRecipientToken() {
        return getToken();
    }

    /**
     * @param receipientToken The receipientToken to set.
     */
    public void setRecipientToken(Token recipientToken) {
        setToken(recipientToken);
    }


    public QName getRealName() {
        return constants.getRecipientToken();
    }
    public QName getName() {
        return SP12Constants.INSTANCE.getRecipientToken();
    }
    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        String localName = getRealName().getLocalPart();
        String namespaceURI = getRealName().getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);

        if (prefix == null) {
            prefix = getRealName().getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:RecipientToken>
        writer.writeStartElement(prefix, localName, namespaceURI);

        String pPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
        if (pPrefix == null) {
            pPrefix = SPConstants.POLICY.getPrefix();
            writer.setPrefix(pPrefix, SPConstants.POLICY.getNamespaceURI());
        }

        // <wsp:Policy>
        writer.writeStartElement(pPrefix, SPConstants.POLICY.getLocalPart(), SPConstants.POLICY
            .getNamespaceURI());

        Token token = getRecipientToken();
        if (token == null) {
            throw new RuntimeException("RecipientToken doesn't contain any token assertions");
        }
        token.serialize(writer);

        // </wsp:Policy>
        writer.writeEndElement();

        // </sp:RecipientToken>
        writer.writeEndElement();
    }
}
