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

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;

public class EnumType extends Type {
    @SuppressWarnings("unchecked")
    @Override
    public Object readObject(MessageReader reader, Context context) {
        String value = reader.getValue();

        return Enum.valueOf(getTypeClass(), value.trim());
    }

    @Override
    public void writeObject(Object object, MessageWriter writer, Context context) {
        // match the reader. 
        writer.writeValue(((Enum)object).name());
    }

    @Override
    public void setTypeClass(Class typeClass) {
        if (!typeClass.isEnum()) {
            throw new DatabindingException("Type class must be an enum.");
        }

        super.setTypeClass(typeClass);
    }

    @Override
    public void writeSchema(XmlSchema root) {
        
        XmlSchemaSimpleType simple = new XmlSchemaSimpleType(root);
        simple.setName(getSchemaType().getLocalPart());
        root.addType(simple);
        root.getItems().add(simple);
        XmlSchemaSimpleTypeRestriction restriction = new XmlSchemaSimpleTypeRestriction();
        restriction.setBaseTypeName(XmlSchemaConstants.STRING_QNAME);
        simple.setContent(restriction);

        Object[] constants = getTypeClass().getEnumConstants();

        XmlSchemaObjectCollection facets = restriction.getFacets();
        for (Object constant : constants) {
            XmlSchemaEnumerationFacet f = new XmlSchemaEnumerationFacet();
            f.setValue(((Enum)constant).name());
            facets.add(f);
        }
    }

    @Override
    public boolean isComplex() {
        return true;
    }
}
