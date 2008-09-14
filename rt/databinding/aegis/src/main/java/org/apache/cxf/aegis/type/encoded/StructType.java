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

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;

/**
 * StructType is a small extension of the BeanType which can properly read and write SOAP encoded structs. The
 * modifications are:
 * <ul>
 * <li>Nested elements MUST be unqualified</li>
 * <li>Nested elements MAY contain a SOAP ref attribute instead of an inline value</li>
 * <li>Struct MAY contain a SOAP id attribute</li>
 * </ul>
 * </p>
 * When writting, the class will always write the struct in the following canonical format:
 * <ul>
 * <li>Struct will contain a SOAP id</li>
 * <li>Nested structs will be written as SOAP references (with SoapRefType)</li>
 * </ul>
 */
public class StructType extends BeanType {
    public StructType() {
    }

    public StructType(BeanTypeInfo info) {
        super(info);
    }

    /**
     * Gets the BeanTypeInfo using an unqualified name.
     * @param name the unqualified name of the element
     * @return the BeanTypeInfo containing a property with the specified unqualified name
     */
    @Override
    protected BeanTypeInfo getBeanTypeInfoWithProperty(QName name) {
        // nested elements use unqualified names
        name = qualifyName(name);

        return super.getBeanTypeInfoWithProperty(name);
    }

    /**
     * Returns a SoapRefType wrapping the actual type.
     */
    @Override
    protected Type getElementType(QName name,
            BeanTypeInfo beanTypeInfo,
            MessageReader reader,
            Context context) {
        
        // nested elements use unqualified names
        name = qualifyName(name);

        Type type = super.getElementType(name, beanTypeInfo, reader, context);
        if (type != null) {
            type = new SoapRefType(type);
        }
        return type;
    }

    /**
     * Adds special handeling for SoapRefs
     */
    @Override
    protected void writeProperty(QName name,
            Object object,
            Object property,
            Class impl,
            BeanTypeInfo inf) throws DatabindingException {

        // nested elements use unqualified names
        name = qualifyName(name);

        if (property instanceof SoapRef) {
            SoapRef soapRef = (SoapRef) property;

            // register an action with the ref that will set the bean property
            // if the reference has already been resolved the action will be
            // invoked immedately
            soapRef.setAction(new WritePropertyAction(name, object, impl, inf));
        } else {
            // normal property
            super.writeProperty(name, object, property, impl, inf);
        }
    }

    /**
     * Writes a nested element with an unqualified name.
     */
    @Override
    protected void writeElement(QName name, Object value, Type type, MessageWriter writer, Context context) {
        // Nested elements are unqualified
        name = new QName("", name.getLocalPart());

        MessageWriter cwriter = writer.getElementWriter(name);

        if (type instanceof BeanType || type instanceof SoapArrayType) {
            String refId = MarshalRegistry.get(context).getInstanceId(value);
            SoapEncodingUtil.writeRef(cwriter, refId);
        } else {
            type.writeObject(value, cwriter, context);
        }

        cwriter.close();
    }

    /**
     * Gets the qualified name of a nested element.  Soap encoded structs contain unqualified elements so
     * the method searches for a property matching the local part of the unqualified name.
     */
    private QName qualifyName(QName name) {
        // is the name already qualified, we're done
        if (!"".equals(name.getNamespaceURI())) {
            return name;
        }

        // find the matching property and get it's name
        for (BeanType sooper = this; sooper != null; sooper = superBeanType(sooper)) {
            QName qualifiedName = new QName(sooper.getTypeInfo().getDefaultNamespace(), name.getLocalPart());

            if (sooper.getTypeInfo().getType(qualifiedName) != null) {
                return qualifiedName;
            }
        }

        return name;
    }
    
    private BeanType superBeanType(Type t) {
        if (t instanceof BeanType) {
            BeanType bt = (BeanType)t;
            Type supertype = bt.getSuperType();
            if (supertype instanceof BeanType) {
                return (BeanType)supertype;
            }
        }
        return null;
    }

    /**
     * When the SoapRef is resolved write the matching property on the target object.
     */
    private final class WritePropertyAction implements SoapRef.Action {
        private final QName name;
        private final Object targetObject;
        private final Class targetClass;
        private final BeanTypeInfo beanTypeInfo;

        private WritePropertyAction(QName name,
                Object targetObject,
                Class targetClass,
                BeanTypeInfo beanTypeInfo) {
            this.name = name;
            this.targetObject = targetObject;
            this.targetClass = targetClass;
            this.beanTypeInfo = beanTypeInfo;
        }

        public void onSet(SoapRef ref) {
            writeProperty(name, targetObject, ref.get(), targetClass, beanTypeInfo);
        }
    }
}
