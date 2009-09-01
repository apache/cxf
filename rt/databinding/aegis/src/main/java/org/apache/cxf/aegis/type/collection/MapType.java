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
package org.apache.cxf.aegis.type.collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;

public class MapType extends AegisType {
    private AegisType keyType;
    private AegisType valueType;
    private QName keyName;
    private QName valueName;
    private QName entryName;

    public MapType(QName schemaType, AegisType keyType, AegisType valueType) {
        super();

        this.keyType = keyType;
        this.valueType = valueType;

        setSchemaType(schemaType);
        keyName = new QName(schemaType.getNamespaceURI(), "key");
        valueName = new QName(schemaType.getNamespaceURI(), "value");
        entryName = new QName(schemaType.getNamespaceURI(), "entry");
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        Map<Object, Object> map = instantiateMap();
        try {
            AegisType kType = getKeyType();
            AegisType vType = getValueType();

            while (reader.hasMoreElementReaders()) {
                MessageReader entryReader = reader.getNextElementReader();

                if (entryReader.getName().equals(getEntryName())) {
                    Object key = null;
                    Object value = null;

                    while (entryReader.hasMoreElementReaders()) {
                                                
                        MessageReader evReader = entryReader.getNextElementReader();

                        if (evReader.getName().equals(getKeyName())) {
                            key = kType.readObject(evReader, context);
                        } else if (evReader.getName().equals(getValueName())) {
                            value = vType.readObject(evReader, context);
                        } else {
                            readToEnd(evReader);
                        }
                    }

                    map.put(key, value);
                } else {
                    readToEnd(entryReader);
                }
            }

            return map;
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument.", e);
        }
    }

    private void readToEnd(MessageReader childReader) {
        while (childReader.hasMoreElementReaders()) {
            readToEnd(childReader.getNextElementReader());
        }
    }

    /**
     * Creates a map instance. If the type class is a <code>Map</code> or
     * extends the <code>Map</code> interface a <code>HashMap</code> is
     * created. Otherwise the map classs (i.e. LinkedHashMap) is instantiated
     * using the default constructor.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    protected Map<Object, Object> instantiateMap() {
        Map<Object, Object> map = null;

        if (getTypeClass().equals(Map.class)) {
            map = new HashMap<Object, Object>();
        } else if (getTypeClass().equals(Hashtable.class)) {
            map = new Hashtable<Object, Object>();
        } else if (getTypeClass().isInterface()) {
            map = new HashMap<Object, Object>();
        } else {
            try {
                map = (Map<Object, Object>)getTypeClass().newInstance();
            } catch (Exception e) {
                throw new DatabindingException("Could not create map implementation: "
                                               + getTypeClass().getName(), e);
            }
        }

        return map;
    }

    @Override
    public void writeObject(Object object,
                            MessageWriter writer,
                            Context context) throws DatabindingException {
        if (object == null) {
            return;
        }

        try {
            Map map = (Map)object;

            AegisType kType = getKeyType();
            AegisType vType = getValueType();

            for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
                Map.Entry entry = (Map.Entry)itr.next();

                writeEntry(writer, context, kType, vType, entry);
            }
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument.", e);
        }
    }

    private void writeEntry(MessageWriter writer, Context context,
                            AegisType kType, AegisType vType,
                            Map.Entry entry) throws DatabindingException {
        kType = TypeUtil.getWriteType(context.getGlobalContext(), entry.getKey(), kType);
        vType = TypeUtil.getWriteType(context.getGlobalContext(), entry.getValue(), vType);

        MessageWriter entryWriter = writer.getElementWriter(getEntryName());

        MessageWriter keyWriter = entryWriter.getElementWriter(getKeyName());
        kType.writeObject(entry.getKey(), keyWriter, context);
        keyWriter.close();

        MessageWriter valueWriter = entryWriter.getElementWriter(getValueName());
        vType.writeObject(entry.getValue(), valueWriter, context);
        valueWriter.close();

        entryWriter.close();
    }

    @Override
    public void writeSchema(XmlSchema root) {
        XmlSchemaComplexType complex = new XmlSchemaComplexType(root);
        complex.setName(getSchemaType().getLocalPart());
        root.addType(complex);
        root.getItems().add(complex);
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        complex.setParticle(sequence);

        AegisType kType = getKeyType();
        AegisType vType = getValueType();
        
        XmlSchemaElement element = new XmlSchemaElement();
        sequence.getItems().add(element);
        element.setName(getEntryName().getLocalPart());
        element.setMinOccurs(0);
        element.setMaxOccurs(Long.MAX_VALUE);
        
        XmlSchemaComplexType evType = new XmlSchemaComplexType(root);
        element.setType(evType);
        
        XmlSchemaSequence evSequence = new XmlSchemaSequence();
        evType.setParticle(evSequence);

        createElement(evSequence, getKeyName(), kType);
        createElement(evSequence, getValueName(), vType);
    }

    /**
     * Creates a element in a sequence for the key type and the value type.
     */
    private void createElement(XmlSchemaSequence seq, QName name, AegisType type) {
        XmlSchemaElement element = new XmlSchemaElement();
        seq.getItems().add(element);
        element.setName(name.getLocalPart());
        element.setSchemaTypeName(type.getSchemaType());
        element.setMinOccurs(0);
        element.setMaxOccurs(1);
    }

    @Override
    public Set<AegisType> getDependencies() {
        Set<AegisType> deps = new HashSet<AegisType>();
        deps.add(getKeyType());
        deps.add(getValueType());
        return deps;
    }

    public AegisType getKeyType() {
        return keyType;
    }

    public AegisType getValueType() {
        return valueType;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    public QName getKeyName() {
        return keyName;
    }

    public void setKeyName(QName keyName) {
        this.keyName = keyName;
    }

    public QName getValueName() {
        return valueName;
    }

    public void setValueName(QName valueName) {
        this.valueName = valueName;
    }

    public QName getEntryName() {
        return entryName;
    }

    public void setEntryName(QName entryName) {
        this.entryName = entryName;
    }
}
