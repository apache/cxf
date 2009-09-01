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

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.basic.ObjectType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;

/**
 * TrailingBlocks reads and writes the extra objects referenced but not written in the main message parts.
 * These objects are commonly refered to as serialization (SOAP spec) roots and trailing blocks (JaxRpc spec).
 * This class uses ObjectType to perform the actual reading and writting, so each block will (and must)
 * contain an xsi type element.
 * <p/>
 * Typically, all message parts are read or written using the SoapRefType and before closing the SOAP body
 * element the trailing blocks are read or written using this class.
 */
public class TrailingBlocks {
    /**
     * The ObjectType used to read and write the trailing block instances.
     */
    private ObjectType objectType;

    public TrailingBlocks() {
        // we only work with mapped types
        objectType = new ObjectType();
        objectType.setReadToDocument(false);
        objectType.setSerializedWhenUnknown(false);
    }

    public TrailingBlocks(TypeMapping typeMapping) {
        this();
        objectType.setTypeMapping(typeMapping);
    }

    public TrailingBlocks(ObjectType objectType) {
        this.objectType = objectType;
    }

    /**
     * Gets the ObjectType used to read and write the trailing block instances.
     *
     * @return the ObjectType used to read and write the trailing block instances.
     */
    public ObjectType getObjectType() {
        return objectType;
    }

    /**
     * Sets the ObjectType used to read and write the trailing block instances.
     *
     * @param objectType the ObjectType used to read and write the trailing block instances.
     */
    public void setObjectType(ObjectType objectType) {
        this.objectType = objectType;
    }

    /**
     * Reads all remailing elements in the reader and registers them with the SoapRefRegistry in the context.
     *
     * @param reader the stream to read
     * @param context the unmarshal context
     * @return a list containing the object instances read
     * @throws DatabindingException if a trailing block element does not contain a soap id attribute
     */
    public List<Object> readBlocks(MessageReader reader, Context context) throws DatabindingException {
        List<Object> blocks = new ArrayList<Object>();

        // read extra serialization roots
        while (reader.hasMoreElementReaders()) {
            MessageReader creader = reader.getNextElementReader();

            // read the instance id
            String id = SoapEncodingUtil.readId(creader);
            if (id == null) {
                throw new DatabindingException(
                        "Trailing block does not contain a SOAP id attribute " + creader.getName());
            }

            // read the instance
            Object instance = objectType.readObject(creader, context);
            blocks.add(instance);

            // register the instance 
            SoapRefRegistry.get(context).addInstance(id, instance);

            // close the element reader
            creader.readToEnd();
        }
        return blocks;
    }

    /**
     * Writes all of the unmarshalled objects in the MarshalRegistry.
     *
     * @param writer the stream to write the objects
     * @param context the marshal context
     * @return a list containing the object instances written
     */
    public List<Object> writeBlocks(MessageWriter writer, Context context) {
        List<Object> blocks = new ArrayList<Object>();

        for (Object instance : MarshalRegistry.get(context)) {
            // determine instance type
            AegisType type = objectType.determineType(context, instance.getClass());
            if (type == null) {
                TypeMapping tm = context.getTypeMapping();
                if (tm == null) {
                    tm = objectType.getTypeMapping();
                }

                type = tm.getTypeCreator().createType(instance.getClass());
                tm.register(type);
            }

            // create an new element for the instance
            MessageWriter cwriter = writer.getElementWriter(type.getSchemaType());

            // write the id attribute
            String id = MarshalRegistry.get(context).getInstanceId(instance);
            SoapEncodingUtil.writeId(cwriter, id);

            // write the instance
            objectType.writeObject(instance, cwriter, context);
            blocks.add(instance);

            // close the element
            cwriter.close();
        }

        return blocks;
    }

}
