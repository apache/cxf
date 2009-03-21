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
package org.apache.cxf.aegis.type.java5;

import javax.xml.XMLConstants;

import org.apache.cxf.aegis.type.basic.StringType;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;

public class CustomStringType extends StringType {

    @Override
    public void writeSchema(XmlSchema root) {
        // this mapping gets used with xs:string, and we might get called.
        if (root.getTargetNamespace().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            return;
        }
        XmlSchemaSimpleType type = new XmlSchemaSimpleType(root);
        type.setName(getSchemaType().getLocalPart());
        root.getItems().add(type);
        root.addType(type);
        XmlSchemaSimpleContentExtension ext = new XmlSchemaSimpleContentExtension();
        ext.setBaseTypeName(XmlSchemaConstants.STRING_QNAME);
        XmlSchemaSimpleTypeRestriction content = new XmlSchemaSimpleTypeRestriction();
        content.setBaseTypeName(XmlSchemaConstants.STRING_QNAME);
        type.setContent(content);
    }

}
