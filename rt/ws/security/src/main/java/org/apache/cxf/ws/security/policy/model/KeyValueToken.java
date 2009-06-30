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

public class KeyValueToken extends Token {
    boolean forceRsaKeyValue;
    public KeyValueToken(SPConstants version) {
        super(version);
    }


    public QName getName() {
        return SP12Constants.INSTANCE.getKeyValueToken();
    }

    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        QName name = constants.getUserNameToken();
        String localname = name.getLocalPart();
        String namespaceURI = name.getNamespaceURI();

        String prefix = writer.getPrefix(namespaceURI);
        if (prefix == null) {
            prefix = name.getPrefix();
            writer.setPrefix(prefix, namespaceURI);
        }

        // <sp:KeyValueToken
        writer.writeStartElement(prefix, localname, namespaceURI);

        writer.writeNamespace(prefix, namespaceURI);

        String inclusion;

        inclusion = constants.getAttributeValueFromInclusion(getInclusion());

        if (inclusion != null) {
            writer.writeAttribute(prefix, namespaceURI, SPConstants.ATTR_INCLUDE_TOKEN, inclusion);
        }


        if (forceRsaKeyValue) {
            String pPrefix = writer.getPrefix(SPConstants.POLICY.getNamespaceURI());
            if (pPrefix == null) {
                writer.setPrefix(SPConstants.POLICY.getPrefix(), SPConstants.POLICY.getNamespaceURI());
            }

            // <wsp:Policy>
            writer.writeStartElement(pPrefix, SPConstants.POLICY.getLocalPart(), SPConstants.POLICY
                .getNamespaceURI());
            
            writer.writeEmptyElement(prefix, "RsaKeyValue", namespaceURI);

            // </wsp:Policy>
            writer.writeEndElement();

        }
        writer.writeEndElement();
        // </sp:KeyValueToken>

    }


    public void setForceRsaKeyValue(boolean b) {
        forceRsaKeyValue = b;
    }
    public boolean isForceRsaKeyValue() {
        return forceRsaKeyValue;
    }
}
