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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.aegis.xml.stax.ElementReader;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.XmlSchemaConstants;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;

/**
 * An ArrayType.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class ArrayType extends AegisType {
    private static final Logger LOG = LogUtils.getL7dLogger(ArrayType.class);

    private QName componentName;
    private long minOccurs;
    private long maxOccurs = Long.MAX_VALUE;

    public ArrayType() {
    }
    
    public Object readObject(MessageReader reader, QName flatElementName, Context context) 
        throws DatabindingException {
        try {
            Collection values = readCollection(reader, flatElementName, context);
            return makeArray(getComponentType().getTypeClass(), values);
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument.", e);
        }
    }

    /*
     * This version is not called for the flat case. 
     */
    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        return readObject(reader, null, context);
    }

    protected Collection<Object> createCollection() {
        return new ArrayList<Object>();
    }

    /**
     * Read the elements of an array or array-like item.
     * @param reader reader to read from.
     * @param flatElementName if flat, the elements we are looking for. When we see
     * something else. we stop.
     * @param context context.
     * @return a collection of the objects.
     * @throws DatabindingException
     */
    protected Collection readCollection(MessageReader reader, QName flatElementName,
                                        Context context) throws DatabindingException {
        Collection<Object> values = createCollection();

        /**
         * If we are 'flat' (writeOuter is false), then we aren't reading children. We're reading starting
         * from where we are.
         */

        if (isFlat()) {
            // the reader does some really confusing things.
            XMLStreamReader xmlReader = reader.getXMLStreamReader();
            while (xmlReader.getName().equals(flatElementName)) {
                AegisType compType = TypeUtil.getReadType(reader.getXMLStreamReader(),
                                                     context.getGlobalContext(), getComponentType());
                // gosh, what about message readers of some other type?
                ElementReader thisItemReader = new ElementReader(xmlReader);
                collectOneItem(context, values, thisItemReader, compType);
            }
        } else {
            while (reader.hasMoreElementReaders()) {
                MessageReader creader = reader.getNextElementReader();
                AegisType compType = TypeUtil.getReadType(creader.getXMLStreamReader(),
                                                     context.getGlobalContext(), getComponentType());
                collectOneItem(context, values, creader, compType);
            }
        }

        // check min occurs
        if (values.size() < minOccurs) {
            throw new DatabindingException("The number of elements in " + getSchemaType()
                                           + " does not meet the minimum of " + minOccurs);
        }
        return values;
    }

    private void collectOneItem(Context context, Collection<Object> values, MessageReader creader,
                                AegisType compType) {
        if (creader.isXsiNil()) {
            values.add(null);
            creader.readToEnd();
        } else {
            values.add(compType.readObject(creader, context));
        }

        // check max occurs
        int size = values.size();
        if (size > maxOccurs) {
            throw new DatabindingException("The number of elements in " + getSchemaType()
                                           + " exceeds the maximum of " + maxOccurs);
        }
    }

    @SuppressWarnings("unchecked")
    protected Object makeArray(Class arrayType, Collection values) {
        int i;
        int n;
        Object array = null;
        if (Integer.TYPE.equals(arrayType)) {
            Object[] objects = values.toArray();
            array = Array.newInstance(Integer.TYPE, objects.length);
            for (i = 0, n = objects.length; i < n; i++) {
                Array.set(array, i, objects[i]);
            }
        } else if (Long.TYPE.equals(arrayType)) {
            Object[] objects = values.toArray();
            array = Array.newInstance(Long.TYPE, objects.length);
            for (i = 0, n = objects.length; i < n; i++) {
                Array.set(array, i, objects[i]);
            }
        } else if (Short.TYPE.equals(arrayType)) {
            Object[] objects = values.toArray();
            array = Array.newInstance(Short.TYPE, objects.length);
            for (i = 0, n = objects.length; i < n; i++) {
                Array.set(array, i, objects[i]);
            }
        } else if (Double.TYPE.equals(arrayType)) {
            Object[] objects = values.toArray();
            array = Array.newInstance(Double.TYPE, objects.length);
            for (i = 0, n = objects.length; i < n; i++) {
                Array.set(array, i, objects[i]);
            }
        } else if (Float.TYPE.equals(arrayType)) {
            Object[] objects = values.toArray();
            array = Array.newInstance(Float.TYPE, objects.length);
            for (i = 0, n = objects.length; i < n; i++) {
                Array.set(array, i, objects[i]);
            }
        } else if (Byte.TYPE.equals(arrayType)) {
            Object[] objects = values.toArray();
            array = Array.newInstance(Byte.TYPE, objects.length);
            for (i = 0, n = objects.length; i < n; i++) {
                Array.set(array, i, objects[i]);
            }
        } else if (Boolean.TYPE.equals(arrayType)) {
            Object[] objects = values.toArray();
            array = Array.newInstance(Boolean.TYPE, objects.length);
            for (i = 0, n = objects.length; i < n; i++) {
                Array.set(array, i, objects[i]);
            }
        } else if (Character.TYPE.equals(arrayType)) {
            Object[] objects = values.toArray();
            array = Array.newInstance(Character.TYPE, objects.length);
            for (i = 0, n = objects.length; i < n; i++) {
                Array.set(array, i, objects[i]);
            }
        }
        return array == null ? values.toArray((Object[])Array.newInstance(getComponentType().getTypeClass(),
                                                                          values.size())) : array;
    }
    
    

    @Override
    public void writeObject(Object values, MessageWriter writer, 
                            Context context) throws DatabindingException {
        writeObject(values, writer, context, null);
    }
    
    
    /**
     * Write an array type, using the desired element name in the flattened case.
     * @param values values to write.
     * @param writer writer to sent it to.
     * @param context the aegis context.
     * @param flatElementName name to use for the element if flat.
     * @throws DatabindingException
     */
    public void writeObject(Object values, MessageWriter writer, 
                            Context context, QName flatElementName) throws DatabindingException {
        boolean forceXsiWrite = false;
        if (values == null) {
            return;
        }

        AegisType type = getComponentType();
        if (type == null) {
            throw new DatabindingException("Couldn't find type for array.");
        }
        if (XmlSchemaConstants.ANY_TYPE_QNAME.equals(type.getSchemaType())) {
            forceXsiWrite = true;
        }

        String ns = null;
        if (type.isAbstract()) {
            ns = getSchemaType().getNamespaceURI();
        } else {
            ns = type.getSchemaType().getNamespaceURI();
        }

        /* 
         * This is not the right name in the 'flat' case. In the flat case,
         * we need the element name that would have been attached
         * one level out.
         */
        String name;
        
        if (isFlat()) {
            name = flatElementName.getLocalPart();
            ns = flatElementName.getNamespaceURI(); // override the namespace.
        } else {
            name = type.getSchemaType().getLocalPart();
        }

        Class arrayType = type.getTypeClass();

        boolean oldXsiWrite = context.getGlobalContext().isWriteXsiTypes();
        try {
            if (forceXsiWrite) {
                context.getGlobalContext().setWriteXsiTypes(true);
            }

            int i;
            int n;
            if (Object.class.isAssignableFrom(arrayType)) {
                Object[] objects = (Object[])values;
                for (i = 0, n = objects.length; i < n; i++) {
                    writeValue(objects[i], writer, context, type, name, ns);
                }
            } else if (Integer.TYPE.equals(arrayType)) {
                int[] objects = (int[])values;
                for (i = 0, n = objects.length; i < n; i++) {
                    writeValue(new Integer(objects[i]), writer, context, type, name, ns);
                }
            } else if (Long.TYPE.equals(arrayType)) {
                long[] objects = (long[])values;
                for (i = 0, n = objects.length; i < n; i++) {
                    writeValue(new Long(objects[i]), writer, context, type, name, ns);
                }
            } else if (Short.TYPE.equals(arrayType)) {
                short[] objects = (short[])values;
                for (i = 0, n = objects.length; i < n; i++) {
                    writeValue(new Short(objects[i]), writer, context, type, name, ns);
                }
            } else if (Double.TYPE.equals(arrayType)) {
                double[] objects = (double[])values;
                for (i = 0, n = objects.length; i < n; i++) {
                    writeValue(new Double(objects[i]), writer, context, type, name, ns);
                }
            } else if (Float.TYPE.equals(arrayType)) {
                float[] objects = (float[])values;
                for (i = 0, n = objects.length; i < n; i++) {
                    writeValue(new Float(objects[i]), writer, context, type, name, ns);
                }
            } else if (Byte.TYPE.equals(arrayType)) {
                byte[] objects = (byte[])values;
                for (i = 0, n = objects.length; i < n; i++) {
                    writeValue(new Byte(objects[i]), writer, context, type, name, ns);
                }
            } else if (Boolean.TYPE.equals(arrayType)) {
                boolean[] objects = (boolean[])values;
                for (i = 0, n = objects.length; i < n; i++) {
                    writeValue(Boolean.valueOf(objects[i]), writer, context, type, name, ns);
                }
            } else if (Character.TYPE.equals(arrayType)) {
                char[] objects = (char[])values;
                for (i = 0, n = objects.length; i < n; i++) {
                    writeValue(new Character(objects[i]), writer, context, type, name, ns);
                }
            }
        } finally {
            context.getGlobalContext().setWriteXsiTypes(oldXsiWrite);
        }
    }

    protected void writeValue(Object value, MessageWriter writer, Context context, 
                              AegisType type, String name,
                              String ns) throws DatabindingException {
        type = TypeUtil.getWriteType(context.getGlobalContext(), value, type);
        MessageWriter cwriter;
        if (!type.isFlatArray()) {
            cwriter = writer.getElementWriter(name, ns);
        } else {
            cwriter = writer;
        }

        if (value == null && type.isNillable()) {
            cwriter.writeXsiNil();
        } else {
            type.writeObject(value, cwriter, context);
        }

        if (!type.isFlatArray()) {
            cwriter.close();
        }
    }

    @Override
    public void writeSchema(XmlSchema root) {

        if (isFlat()) {
            return; // there is no extra level of type.
        }
        if (hasDefinedArray(root)) {
            return;
        }

        XmlSchemaComplexType complex = new XmlSchemaComplexType(root);
        complex.setName(getSchemaType().getLocalPart());
        root.addType(complex);
        root.getItems().add(complex);

        XmlSchemaSequence seq = new XmlSchemaSequence();
        complex.setParticle(seq);

        AegisType componentType = getComponentType();
        XmlSchemaElement element = new XmlSchemaElement();
        element.setName(componentType.getSchemaType().getLocalPart());
        element.setSchemaTypeName(componentType.getSchemaType());

        seq.getItems().add(element);

        if (componentType.isNillable()) {
            element.setNillable(true);
        }

        element.setMinOccurs(getMinOccurs());
        element.setMaxOccurs(getMaxOccurs());

    }

    /**
     * Since both an Array and a List can have the same type definition, double check that there isn't already
     * a defined type already.
     * 
     * @param root
     * @return
     */
    private boolean hasDefinedArray(XmlSchema root) {
        return root.getTypeByName(getSchemaType().getLocalPart()) != null;
    }

    /**
     * We need to write a complex type schema for Beans, so return true.
     * 
     * @see org.apache.cxf.aegis.type.AegisType#isComplex()
     */
    @Override
    public boolean isComplex() {
        return true;
    }

    public QName getComponentName() {
        return componentName;
    }

    public void setComponentName(QName componentName) {
        this.componentName = componentName;
    }

    /**
     * @see org.apache.cxf.aegis.type.AegisType#getDependencies()
     */
    @Override
    public Set<AegisType> getDependencies() {
        Set<AegisType> deps = new HashSet<AegisType>();

        deps.add(getComponentType());

        return deps;
    }

    /**
     * Get the <code>AegisType</code> of the elements in the array.
     * 
     * @return
     */
    public AegisType getComponentType() {
        Class compType = getTypeClass().getComponentType();

        AegisType type;

        if (componentName == null) {
            type = getTypeMapping().getType(compType);
        } else {
            type = getTypeMapping().getType(componentName);

            // We couldn't find the type the user specified. One is created
            // below instead.
            if (type == null) {
                LOG.finest("Couldn't find array component type " + componentName + ". Creating one instead.");
            }
        }

        if (type == null) {
            type = getTypeMapping().getTypeCreator().createType(compType);
            getTypeMapping().register(type);
        }

        return type;
    }

    public long getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(long maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public long getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(long minOccurs) {
        this.minOccurs = minOccurs;
    }

    public boolean isFlat() {
        return isFlatArray();
    }

    public void setFlat(boolean flat) {
        setWriteOuter(!flat);
        setFlatArray(flat);
    }

    @Override
    public boolean hasMaxOccurs() {
        return true;
    }

    @Override
    public boolean hasMinOccurs() {
        return true;
    }
}
