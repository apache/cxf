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
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.util.NamespaceHelper;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.common.util.SOAPConstants;
import org.jdom.Attribute;
import org.jdom.Element;

public class MapType extends Type {
    private Type keyType;
    private Type valueType;
    private QName keyName;
    private QName valueName;
    private QName entryName;

    public MapType(QName schemaType, Type keyType, Type valueType) {
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
            Type kType = getKeyType();
            Type vType = getValueType();

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

            Type kType = getKeyType();
            Type vType = getValueType();

            for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
                Map.Entry entry = (Map.Entry)itr.next();

                writeEntry(writer, context, kType, vType, entry);
            }
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument.", e);
        }
    }

    private void writeEntry(MessageWriter writer, Context context,
                            Type kType, Type vType,
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
    public void writeSchema(Element root) {
        Element complex = new Element("complexType", SOAPConstants.XSD_PREFIX, SOAPConstants.XSD);
        complex.setAttribute(new Attribute("name", getSchemaType().getLocalPart()));
        root.addContent(complex);

        Element seq = new Element("sequence", SOAPConstants.XSD_PREFIX, SOAPConstants.XSD);
        complex.addContent(seq);

        Type kType = getKeyType();
        Type vType = getValueType();

        Element element = new Element("element", SOAPConstants.XSD_PREFIX, SOAPConstants.XSD);
        seq.addContent(element);

        element.setAttribute(new Attribute("name", getEntryName().getLocalPart()));
        element.setAttribute(new Attribute("minOccurs", "0"));
        element.setAttribute(new Attribute("maxOccurs", "unbounded"));

        Element evComplex = new Element("complexType", SOAPConstants.XSD_PREFIX, SOAPConstants.XSD);
        element.addContent(evComplex);

        Element evseq = new Element("sequence", SOAPConstants.XSD_PREFIX, SOAPConstants.XSD);
        evComplex.addContent(evseq);

        createElement(root, evseq, getKeyName(), kType);
        createElement(root, evseq, getValueName(), vType);
    }

    /**
     * Creates a element in a sequence for the key type and the value type.
     */
    private void createElement(Element root, Element seq, QName name, Type type) {
        Element element = new Element("element", SOAPConstants.XSD_PREFIX, SOAPConstants.XSD);
        seq.addContent(element);

        String prefix = NamespaceHelper.getUniquePrefix((Element)root, type.getSchemaType()
            .getNamespaceURI());

        element.setAttribute(new Attribute("name", name.getLocalPart()));
        element.setAttribute(TypeUtil.createTypeAttribute(prefix, type, root));

        element.setAttribute(new Attribute("minOccurs", "0"));
        element.setAttribute(new Attribute("maxOccurs", "1"));
    }

    @Override
    public Set<Type> getDependencies() {
        Set<Type> deps = new HashSet<Type>();
        deps.add(getKeyType());
        deps.add(getValueType());
        return deps;
    }

    public Type getKeyType() {
        return keyType;
    }

    public Type getValueType() {
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
