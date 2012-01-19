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

import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.ws.commons.schema.XmlSchema;

/**
 * SoapRefType reads and writes SoapRef objects.
 * <p/>
 * When reading, this class checks for a SOAP ref attribute, and if present creates SoapRef and registers it
 * with the SoapRefRegistry.  If the SOAP ref attribute is not present, this class delegates to the baseType
 * specified in the constructor.  Regardless of the element containing SOAP ref or not, a SoapRef object is
 * returned. In the case of an inline object the SoapRef will contain a null value.
 * <p/>
 * When writing, the class always writes an element containing a SOAP ref attribute.  The actual object
 * instance is registered with the MarshalRegistry, and is written at the end of the message body by the
 * TrailingBlocks class.
 */
public class SoapRefType extends AegisType {
    private final AegisType baseType;

    public SoapRefType(AegisType baseType) {
        if (baseType == null) {
            throw new NullPointerException("baseType is null");
        }
        this.baseType = baseType;
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        SoapRef soapRef = new SoapRef();

        // if we have a ref, register our soap ref with context
        String ref = SoapEncodingUtil.readRef(reader);
        if (ref != null) {
            SoapRefRegistry.get(context).addRef(ref, soapRef);
            reader.readToEnd();
            return soapRef;
        }

        Object object = baseType.readObject(reader, context);
        soapRef.set(object);
        return soapRef;
    }

    public void writeObject(Object object,
            MessageWriter writer,
            Context context) throws DatabindingException {

        // write the ref id
        String refId = MarshalRegistry.get(context).getInstanceId(object);
        SoapEncodingUtil.writeRef(writer, refId);
    }

    @Override
    public void writeSchema(XmlSchema schema) {
        baseType.writeSchema(schema);
    }

    public TypeMapping getTypeMapping() {
        return baseType.getTypeMapping();
    }

    public void setTypeMapping(TypeMapping typeMapping) {
        baseType.setTypeMapping(typeMapping);
    }

    public Class getTypeClass() {
        return baseType.getTypeClass();
    }

    public void setTypeClass(Class typeClass) {
        baseType.setTypeClass(typeClass);
    }

    public boolean isComplex() {
        return baseType.isComplex();
    }

    public boolean isAbstract() {
        return baseType.isAbstract();
    }

    public void setAbstract(boolean ab) {
        baseType.setAbstract(ab);
    }

    public boolean isNillable() {
        return baseType.isNillable();
    }

    public void setNillable(boolean nillable) {
        baseType.setNillable(nillable);
    }

    public Set<AegisType> getDependencies() {
        return baseType.getDependencies();
    }

    public QName getSchemaType() {
        return baseType.getSchemaType();
    }

    public void setSchemaType(QName name) {
        baseType.setSchemaType(name);
    }

    public boolean isWriteOuter() {
        return baseType.isWriteOuter();
    }

    public void setWriteOuter(boolean writeOuter) {
        baseType.setWriteOuter(writeOuter);
    }
}
