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
package org.apache.cxf.aegis.type.encoded;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.ws.commons.schema.XmlSchema;

import static org.apache.cxf.aegis.type.encoded.SoapEncodingUtil.readAttributeValue;

public class SoapArrayType extends Type {
    private static final Logger LOG = LogUtils.getL7dLogger(SoapArrayType.class);
    private static final String SOAP_ENCODING_NS_1_1 = Soap11.getInstance().getSoapEncodingStyle();
    private static final QName SOAP_ARRAY_POSITION = new QName(SOAP_ENCODING_NS_1_1, "position");

    private QName componentName;

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        try {
            // get the encoded array type info
            TypeMapping tm = context.getTypeMapping();
            if (tm == null) {
                tm = getTypeMapping();
            }
            ArrayTypeInfo arrayTypeInfo = new ArrayTypeInfo(reader, tm);

            // verify arrayType dimensions are the same as this type class's array dimensions
            if (getDimensions() != arrayTypeInfo.getTotalDimensions()) {
                throw new DatabindingException("In " + getSchemaType() + " expected array with "
                        + getDimensions() + " dimensions, but arrayType has "
                        + arrayTypeInfo.getTotalDimensions() + " dimensions: "
                        + arrayTypeInfo.toString());
            }

            // calculate max size
            int maxSize = 1;
            for (int dimension : arrayTypeInfo.getDimensions()) {
                maxSize *= dimension;
            }

            // verify offset doesn't exceed maximum size
            if (arrayTypeInfo.getOffset() >= maxSize) {
                throw new DatabindingException("The array offset " + arrayTypeInfo.getOffset() + " in "
                        + getSchemaType() + " exceeds the expecte size of " + maxSize);
            }

            // read the values
            List<Object> values = readCollection(reader,
                    context,
                    arrayTypeInfo,
                    maxSize - arrayTypeInfo.getOffset());

            // if it is a partially transmitted array offset the array values
            if (arrayTypeInfo.getOffset() > 0) {
                List<Object> list = new ArrayList<Object>(values.size() + arrayTypeInfo.getOffset());
                list.addAll(Collections.nCopies(arrayTypeInfo.getOffset(), null));
                list.addAll(values);
                values = list;
            }

            // check bounds
            if (values.size() > maxSize) {
                throw new DatabindingException("The number of elements " + values.size() + " in "
                        + getSchemaType() + " exceeds the expecte size of " + maxSize);
            }
            if (values.size() < maxSize) {
                values.addAll(Collections.nCopies(maxSize - values.size(), null));
                // todo is this an error?
                // throw new DatabindingException("The number of elements in " + getSchemaType() +
                //         " is less then the expected size of " + expectedSize);
            }
            if (values.size() != maxSize) {
                throw new IllegalStateException("Internal error: Expected values collection to contain "
                        + maxSize + " elements but it contains " + values.size() + " elements");
            }

            // create the array
            return makeArray(values, arrayTypeInfo.getDimensions(), getTypeClass().getComponentType());
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument.", e);
        }
    }

    protected List<Object> readCollection(MessageReader reader,
            Context context,
            ArrayTypeInfo arrayTypeInfo,
            int maxSize) throws DatabindingException {

        List<Object> values = new ArrayList<Object>();

        Boolean sparse = null;
        while (reader.hasMoreElementReaders()) {
            MessageReader creader = reader.getNextElementReader();

            // if the first element contains a position attribute, this is a sparse array
            // and all subsequent elements must contain the position attribute
            String position = readAttributeValue(creader, SOAP_ARRAY_POSITION);
            if (sparse == null) {
                sparse = position != null;
            }

            // nested element names can specify a type
            Type compType = getTypeMapping().getType(creader.getName());
            if (compType == null) {
                // use the type declared in the arrayType attribute
                compType = arrayTypeInfo.getType();
            }
            // check for an xsi:type override
            compType = TypeUtil.getReadType(creader.getXMLStreamReader(), 
                                            context.getGlobalContext(), compType);

            // wrap type with soap ref to handle hrefs
            compType = new SoapRefType(compType);

            // read the value
            Object value;
            if (creader.isXsiNil()) {
                value = null;
                creader.readToEnd();
            } else {
                value = compType.readObject(creader, context);
            }

            // add the value
            if (!sparse) {
                if (values.size() + 1 > maxSize) {
                    throw new DatabindingException("The number of elements in " + getSchemaType()
                            + " exceeds the maximum size of " + maxSize);
                }
                values.add(value);
            } else {
                int valuesPosition = readValuesPosition(position, arrayTypeInfo.getDimensions());
                if (valuesPosition > maxSize) {
                    throw new DatabindingException("Array position " + valuesPosition + " in "
                            + getSchemaType() + " exceeds the maximum size of " + maxSize);
                }
                if (values.size() <= valuesPosition) {
                    values.addAll(Collections.nCopies(valuesPosition - values.size() + 1, null));
                }
                Object oldValue = values.set(valuesPosition, value);
                if (oldValue != null) {
                    throw new DatabindingException("Array position " + valuesPosition + " in "
                            + getSchemaType() + " is already assigned to value " + oldValue);
                }
            }
        }

        return values;
    }

    private int readValuesPosition(String positionString, List<Integer> dimensions) {
        if (positionString == null) {
            throw new DatabindingException("Sparse array entry does not contain a position attribute");
        }

        try {
            // position = "[" , length , { "," , lenght } , "]" ;
            List<String> tokens = CastUtils.cast(Collections.list(new StringTokenizer(positionString,
                    "[],",
                    true)));
            if (tokens.size() == 2 + dimensions.size() + dimensions.size() - 1 && tokens.get(0).equals("[")
                    && tokens.get(tokens.size() - 1).equals("]")) {

                // strip off leading [ and trailing ]
                tokens = tokens.subList(1, tokens.size() - 1);

                // return the product of the values
                int[] index = new int[dimensions.size()];
                for (int i = 0; i < index.length; i++) {
                    int tokenId = i * 2;

                    index[i] = Integer.parseInt(tokens.get(tokenId));

                    if (tokenId + 1 < tokens.size() && !tokens.get(tokenId + 1).equals(",")) {
                        throw new IllegalStateException(
                                "Expected a comma but got " + tokens.get(tokenId + 1));
                    }
                }

                // determine the real position withing the flattened square array
                int valuePosition = 0;
                int multiplier = 1;
                for (int i = index.length - 1; i >= 0; i--) {
                    int position = index[i];
                    valuePosition += position * multiplier;
                    multiplier *= dimensions.get(i);
                }

                return valuePosition;
            }
        } catch (Exception ignored) {
            // exception is thrown below
        }

        // failed print the expected format
        StringBuilder expectedFormat = new StringBuilder();
        expectedFormat.append("[x");
        for (int i = 1; i < dimensions.size(); i++) {
            expectedFormat.append(",x");
        }
        expectedFormat.append("]");
        throw new DatabindingException("Expected sparse array position value in format " + expectedFormat
                + ", but was " + positionString);
    }

    protected Object makeArray(List values, List<Integer> dimensions, Class componentType) {

        // if this is an array of arrays, recurse into this function
        // for each nested array
        if (componentType.isArray() && dimensions.size() > 1) {
            // square array
            int chunkSize = 1;
            for (Integer dimension : dimensions.subList(1, dimensions.size())) {
                chunkSize *= dimension;
            }
            Object[] array = (Object[]) Array.newInstance(componentType, dimensions.get(0));
            for (int i = 0; i < array.length; i++) {
                List chunk = values.subList(i * chunkSize, (i + 1) * chunkSize);
                Object value = makeArray(chunk,
                        dimensions.subList(1, dimensions.size()),
                        componentType.getComponentType());
                Array.set(array, i, value);
            }
            return array;
        }

        // build the array
        Object array = Array.newInstance(componentType, dimensions.get(0));
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (value != null) {
                SoapRef soapRef = (SoapRef) value;
                soapRef.setAction(new SetArrayAction(array, i));
            }
        }
        return array;
    }

    @Override
    public void writeObject(Object values,
            MessageWriter writer,
            Context context) throws DatabindingException {
        
        if (values == null) {
            return;
        }

        // ComponentType
        Type type = getComponentType();
        if (type == null) {
            throw new DatabindingException("Couldn't find component type for array.");
        }

        // Root component's schema type
        QName rootType = getRootType();
        String prefix = writer.getPrefixForNamespace(rootType.getNamespaceURI(), rootType.getPrefix());
        if (prefix == null) {
            prefix = "";
        }
        rootType = new QName(rootType.getNamespaceURI(), rootType.getLocalPart(), prefix);


        // write the soap arrayType attribute
        ArrayTypeInfo arrayTypeInfo = new ArrayTypeInfo(rootType,
                getDimensions() - 1,
                Array.getLength(values));
        // ensure that the writer writes out this prefix...
        writer.getPrefixForNamespace(arrayTypeInfo.getTypeName().getNamespaceURI(), 
                                     arrayTypeInfo.getTypeName().getPrefix());
        arrayTypeInfo.writeAttribute(writer);

        // write each element
        for (int i = 0; i < Array.getLength(values); i++) {
            writeValue(Array.get(values, i), writer, context, type);
        }
    }

    protected void writeValue(Object value,
            MessageWriter writer,
            Context context,
            Type type) throws DatabindingException {

        type = TypeUtil.getWriteType(context.getGlobalContext(), value, type);

        MessageWriter cwriter = writer.getElementWriter(type.getSchemaType().getLocalPart(), "");

        if (value == null && type.isNillable()) {
            // null
            cwriter.writeXsiNil();
        } else if (type instanceof BeanType || type instanceof SoapArrayType) {
            // write refs to complex type
            String refId = MarshalRegistry.get(context).getInstanceId(value);
            SoapEncodingUtil.writeRef(cwriter, refId);
        } else {
            // write simple types inline
            type.writeObject(value, cwriter, context);
        }

        cwriter.close();
    }

    /**
     * Throws UnsupportedOperationException
     */
    @Override
    public void writeSchema(XmlSchema root) {
        throw new UnsupportedOperationException();
    }

    /**
     * We need to write a complex type schema for Beans, so return true.
     *
     * @see org.apache.cxf.aegis.type.Type#isComplex()
     */
    @Override
    public boolean isComplex() {
        return true;
    }

    /**
     * Gets the QName of the component type of this array.
     * @return the QName of the component type of this array
     */
    public QName getComponentName() {
        return componentName;
    }

    /**
     * Sets the QName of the component type of this array.
     * @param componentName the QName of the component type of this array
     */
    public void setComponentName(QName componentName) {
        this.componentName = componentName;
    }

    @Override
    public Set<Type> getDependencies() {
        Set<Type> deps = new HashSet<Type>();

        deps.add(getComponentType());

        return deps;
    }

    /**
     * Get the <code>Type</code> of the elements in the array.  This is only used for writing an array.
     * When reading the type is solely determined by the required arrayType soap attribute.
     */
    public Type getComponentType() {
        Class compType = getTypeClass().getComponentType();

        Type type;
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

    /**
     * Gets the QName of the root component type of this array.  This will be a non-array type such as
     * a simple xsd type.
     * @return the QName of the root component type of this array
     */
    protected QName getRootType() {
        Type componentType = getComponentType();
        if (componentType instanceof SoapArrayType) {
            SoapArrayType arrayType = (SoapArrayType) componentType;
            return arrayType.getRootType();
        }
        return componentType.getSchemaType();
    }

    /**
     * Gets the number of array dimensions in the class for this type.
     * @return the number of array dimensions
     */
    private int getDimensions() {
        int dimensions = 0;
        for (Class type = getTypeClass(); type.isArray(); type = type.getComponentType()) {
            dimensions++;
        }
        return dimensions;
    }

    /**
     * Sets an array entry when the soap ref is resolved
     */
    private static class SetArrayAction implements SoapRef.Action {
        private final Object array;
        private final int index;

        public SetArrayAction(Object array, int index) {
            this.array = array;
            this.index = index;
        }

        public void onSet(SoapRef ref) {
            Array.set(array, index, ref.get());
        }
    }
}
