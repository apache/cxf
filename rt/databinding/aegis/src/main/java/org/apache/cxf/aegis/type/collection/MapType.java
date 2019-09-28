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
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

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

            while (reader.hasMoreElementReaders()) {
                MessageReader entryReader = reader.getNextElementReader();

                if (entryReader.getName().equals(getEntryName())) {
                    Object key = null;
                    Object value = null;

                    while (entryReader.hasMoreElementReaders()) {

                        MessageReader evReader = entryReader.getNextElementReader();

                        if (evReader.getName().equals(getKeyName())) {
                            AegisType kType = TypeUtil.getReadType(evReader.getXMLStreamReader(),
                                                              context.getGlobalContext(),
                                                              getKeyType());
                            key = kType.readObject(evReader, context);
                        } else if (evReader.getName().equals(getValueName())) {
                            AegisType vType = TypeUtil.getReadType(evReader.getXMLStreamReader(),
                                                              context.getGlobalContext(),
                                                              getValueType());
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

        Class<?> cls = getTypeClass();
        if (cls.equals(Map.class)) {
            map = new HashMap<>();
        } else if (cls.equals(Hashtable.class)) { //NOPMD
            map = new Hashtable<>();
        } else if (cls.equals(ConcurrentMap.class)) {
            map = new ConcurrentHashMap<>();
        } else if (cls.equals(ConcurrentNavigableMap.class)) {
            map = new ConcurrentSkipListMap<>();
        } else if (cls.equals(SortedMap.class) || cls.equals(NavigableMap.class)) {
            map = new TreeMap<>();
        } else if (cls.isInterface()) {
            map = new HashMap<>();
        } else {
            try {
                map = (Map<Object, Object>)cls.newInstance();
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
            Map<?, ?> map = (Map<?, ?>)object;

            AegisType kType = getKeyType();
            AegisType vType = getValueType();

            for (Iterator<?> itr = map.entrySet().iterator(); itr.hasNext();) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>)itr.next();

                writeEntry(writer, context, kType, vType, entry);
            }
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument.", e);
        }
    }

    private void writeEntry(MessageWriter writer, Context context,
                            AegisType kType, AegisType vType,
                            Map.Entry<?, ?> entry) throws DatabindingException {
        kType = TypeUtil.getWriteType(context.getGlobalContext(), entry.getKey(), kType);
        vType = TypeUtil.getWriteType(context.getGlobalContext(), entry.getValue(), vType);

        MessageWriter entryWriter = writer.getElementWriter(getEntryName());

        MessageWriter keyWriter = entryWriter.getElementWriter(getKeyName());
        kType.writeObject(entry.getKey(), keyWriter, context);
        keyWriter.close();
        if (entry.getValue() != null) {
            MessageWriter valueWriter = entryWriter.getElementWriter(getValueName());
            vType.writeObject(entry.getValue(), valueWriter, context);
            valueWriter.close();
        }

        entryWriter.close();
    }

    @Override
    public void writeSchema(XmlSchema root) {
        XmlSchemaComplexType complex = new XmlSchemaComplexType(root, true);
        complex.setName(getSchemaType().getLocalPart());
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        complex.setParticle(sequence);

        AegisType kType = getKeyType();
        AegisType vType = getValueType();

        XmlSchemaElement element = new XmlSchemaElement(root, false);
        sequence.getItems().add(element);
        element.setName(getEntryName().getLocalPart());
        element.setMinOccurs(0);
        element.setMaxOccurs(Long.MAX_VALUE);

        XmlSchemaComplexType evType = new XmlSchemaComplexType(root, false);
        element.setType(evType);

        XmlSchemaSequence evSequence = new XmlSchemaSequence();
        evType.setParticle(evSequence);

        createElement(root, evSequence, getKeyName(), kType, false);
        createElement(root, evSequence, getValueName(), vType, true);
    }

    /**
     * Creates a element in a sequence for the key type and the value type.
     */
    private void createElement(XmlSchema schema, XmlSchemaSequence seq, QName name,
                               AegisType type, boolean optional) {
        XmlSchemaElement element = new XmlSchemaElement(schema, false);
        seq.getItems().add(element);
        element.setName(name.getLocalPart());
        element.setSchemaTypeName(type.getSchemaType());
        if (optional) {
            element.setMinOccurs(0);
        } else {
            element.setMinOccurs(1);
        }
        element.setMaxOccurs(1);
    }

    @Override
    public Set<AegisType> getDependencies() {
        Set<AegisType> deps = new HashSet<>();
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
