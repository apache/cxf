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

import java.lang.reflect.Type;
import java.util.List;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.constants.Constants;

public class EnumType extends AegisType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        String value = reader.getValue();
        return matchValue(value);
    }

    @Override
    public void writeObject(Object object, MessageWriter writer, Context context) {
        // match the reader.
        writer.writeValue(getValue(object));
    }

    @Override
    public void setTypeClass(Type typeClass) {
        if (!(typeClass instanceof Class)) {
            throw new DatabindingException("Aegis cannot map generic Enums.");
        }

        Class<?> plainClass = (Class<?>)typeClass;
        if (!plainClass.isEnum()) {
            throw new DatabindingException("EnumType must map an enum.");
        }

        super.setTypeClass(typeClass);
    }

    @Override
    public void writeSchema(XmlSchema root) {

        XmlSchemaSimpleType simple = new XmlSchemaSimpleType(root, true);
        simple.setName(getSchemaType().getLocalPart());
        XmlSchemaSimpleTypeRestriction restriction = new XmlSchemaSimpleTypeRestriction();
        restriction.setBaseTypeName(Constants.XSD_STRING);
        simple.setContent(restriction);

        Object[] constants = getTypeClass().getEnumConstants();

        List<XmlSchemaFacet> facets = restriction.getFacets();
        for (Object constant : constants) {
            XmlSchemaEnumerationFacet f = new XmlSchemaEnumerationFacet();
            f.setValue(getValue(constant));
            facets.add(f);
        }
    }

    @SuppressWarnings("unchecked")
    private Enum<?> matchValue(String value) {
        if (value == null) {
            return null;
        }
        @SuppressWarnings("rawtypes")
        Class<? extends Enum> enumClass = (Class<? extends Enum>)getTypeClass();
        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
            if (value.equals(AnnotationReader.getEnumValue(enumConstant))) {
                return enumConstant;
            }
        }
        return Enum.valueOf(enumClass, value.trim());
    }


    private Object getValue(Object constant) {
        if (!(constant instanceof Enum<?>)) {
            return null;
        }
        Enum<?> enumConstant = (Enum<?>)constant;
        String annotatedValue = AnnotationReader.getEnumValue(enumConstant);
        if (annotatedValue != null) {
            return annotatedValue;
        }
        return enumConstant.name();
    }

    @Override
    public boolean isComplex() {
        return true;
    }
}
