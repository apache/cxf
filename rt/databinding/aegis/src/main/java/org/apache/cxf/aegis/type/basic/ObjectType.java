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
package org.apache.cxf.aegis.type.basic;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.ws.commons.schema.constants.Constants;

/**
 * AegisType for runtime inspection of types. Looks as the class to be written, and
 * looks to see if there is a type for that class. If there is, it writes out
 * the value and inserts a <em>xsi:type</em> attribute to signal what the type
 * of the value is. Can specify an optional set of dependent <code>AegisType</code>'s
 * in the constructor, in the case that the type is a custom type that may not
 * have its schema in the WSDL. Can specify whether or not unknown objects
 * should be serialized as a byte stream.
 *
 */
public class ObjectType extends AegisType {
    private static final QName XSI_TYPE = new QName(Constants.URI_2001_SCHEMA_XSI, "type");
    private static final QName XSI_NIL = new QName(Constants.URI_2001_SCHEMA_XSI, "nil");

    private Set<AegisType> dependencies;
    private boolean readToDocument;

    @SuppressWarnings("unchecked")
    public ObjectType() {
        this(Collections.EMPTY_SET);
        readToDocument = true;
    }

    public ObjectType(Set<AegisType> dependencies) {
        this(dependencies, false);
    }

    public ObjectType(Set<AegisType> dependencies, boolean serializeWhenUnknown) {
        this.dependencies = dependencies;
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        if (isNil(reader.getAttributeReader(XSI_NIL))) {
            while (reader.hasMoreElementReaders()) {
                reader.getNextElementReader();
            }

            return null;
        }

        MessageReader typeReader = reader.getAttributeReader(XSI_TYPE);

        if (null == typeReader && !readToDocument) {
            throw new DatabindingException("Missing 'xsi:type' attribute");
        }

        String typeName = null;
        if (typeReader != null) {
            typeName = typeReader.getValue();
        }

        if (null == typeName && !readToDocument) {
            throw new DatabindingException("Missing 'xsi:type' attribute value");
        }

        AegisType type = null;
        QName typeQName = null;

        if (typeName != null) {
            typeName = typeName.trim();
            typeQName = extractQName(reader, typeName);
        } else {
            typeQName = reader.getName();
        }

        TypeMapping tm = context.getTypeMapping();
        if (tm == null) {
            tm = getTypeMapping();
        }

        type = tm.getType(typeQName);

        if (type == null) {
            type = tm.getType(getSchemaType());
        }

        if (type == this) {
            throw new DatabindingException("Could not determine how to read type: " + typeQName);
        }

        if (type == null && readToDocument) {
            type = getTypeMapping().getType(Document.class);
        }

        if (null == type) {
            throw new DatabindingException("No mapped type for '" + typeName + "' (" + typeQName + ")");
        }

        return type.readObject(reader, context);
    }

    private QName extractQName(MessageReader reader, String typeName) {
        int colon = typeName.indexOf(':');

        if (-1 == colon) {
            return new QName(reader.getNamespace(), typeName);
        }
        return new QName(reader.getNamespaceForPrefix(typeName.substring(0, colon)), typeName
            .substring(colon + 1));
    }


    private boolean isNil(MessageReader reader) {
        return null != reader && "true".equals(reader.getValue() == null ? "" : reader.getValue());
    }

    @Override
    public void writeObject(Object object, MessageWriter writer, Context context)
        throws DatabindingException {
        if (null == object) {
            MessageWriter nilWriter = writer.getAttributeWriter(XSI_NIL);

            nilWriter.writeValue("true");

            nilWriter.close();
        } else {
            AegisType type = determineType(context, object.getClass());

            if (null == type) {
                TypeMapping tm = context.getTypeMapping();
                if (tm == null) {
                    tm = getTypeMapping();
                }

                type = tm.getTypeCreator().createType(object.getClass());
                tm.register(type);
            }

            writer.writeXsiType(type.getSchemaType());
            boolean nextIsBeanType = type instanceof BeanType;
            if (nextIsBeanType) {
                ((BeanType)type).writeObjectFromObjectType(object, writer, context, true);
            } else {
                type.writeObject(object, writer, context);
            }
        }
    }

    public AegisType determineType(Context context, Class<?> clazz) {
        TypeMapping tm = context.getTypeMapping();
        if (tm == null) {
            tm = getTypeMapping();
        }
        AegisType type = tm.getType(clazz);

        if (null != type) {
            return type;
        }

        Class<?>[] interfaces = clazz.getInterfaces();

        for (int i = 0; i < interfaces.length; i++) {
            Class<?> anInterface = interfaces[i];

            type = tm.getType(anInterface);

            if (null != type) {
                return type;
            }
        }

        Class<?> superclass = clazz.getSuperclass();

        if (null == superclass || Object.class.equals(superclass)) {
            return null;
        }

        return determineType(context, superclass);
    }

    public boolean isReadToDocument() {
        return readToDocument;
    }

    public void setReadToDocument(boolean readToDocument) {
        this.readToDocument = readToDocument;
    }

    public void setDependencies(Set<AegisType> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public Set<AegisType> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

}
