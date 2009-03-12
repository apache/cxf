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
package org.apache.cxf.aegis.custom.types;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.custom.service.NoDefaultConstructorBeanKey;
import org.apache.cxf.aegis.custom.service.NoDefaultConstructorBeanKeyImpl;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.xml.MessageReader;

public class NoDefaultConstructorBeanKeyType extends BeanType {

    public static final Class<?> TYPE_CLASS = NoDefaultConstructorBeanKey.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    public NoDefaultConstructorBeanKeyType() {
        super();
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        BeanTypeInfo inf = getTypeInfo();

        try {
            String key = null;

            // Read child elements
            while (reader.hasMoreElementReaders()) {
                MessageReader childReader = reader.getNextElementReader();
                if (childReader.isXsiNil()) {
                    childReader.readToEnd();
                    continue;
                }
                QName name = childReader.getName();
                Type defaultType = inf.getType(name);
                Type type = TypeUtil.getReadType(childReader.getXMLStreamReader(),
                                                 context.getGlobalContext(), defaultType);
                if (type != null) {
                    String value = (String)type.readObject(childReader, context);
                    if ("key".equals(name.getLocalPart())) {
                        key = value;
                    }
                } else {
                    childReader.readToEnd();
                }
            }

            return new NoDefaultConstructorBeanKeyImpl(key);
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }

    @Override
    public Set<Type> getDependencies() {
        // The string type is provided by aegis, so it should always be there.
        Type stringType = getTypeMapping().getType(String.class);
        return Collections.singleton(stringType);
    }
}
