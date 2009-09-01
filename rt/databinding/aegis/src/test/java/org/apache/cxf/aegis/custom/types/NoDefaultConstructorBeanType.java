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
import org.apache.cxf.aegis.custom.service.NoDefaultConstructorBean;
import org.apache.cxf.aegis.custom.service.NoDefaultConstructorBeanImpl;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.xml.MessageReader;

public class NoDefaultConstructorBeanType extends BeanType {

    public static final Class<?> TYPE_CLASS = NoDefaultConstructorBean.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    public NoDefaultConstructorBeanType() {
        super();
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        BeanTypeInfo inf = getTypeInfo();

        try {
            String id = null;
            String name = null;

            // Read child elements
            while (reader.hasMoreElementReaders()) {
                MessageReader childReader = reader.getNextElementReader();
                if (childReader.isXsiNil()) {
                    childReader.readToEnd();
                    continue;
                }
                QName qName = childReader.getName();
                AegisType defaultType = inf.getType(qName);
                AegisType type = TypeUtil.getReadType(childReader.getXMLStreamReader(),
                                                 context.getGlobalContext(), defaultType);
                if (type != null) {
                    String value = (String)type.readObject(childReader, context);
                    if ("id".equals(qName.getLocalPart())) {
                        id = value;
                    } else if ("name".equals(qName.getLocalPart())) {
                        name = value;
                    }
                } else {
                    childReader.readToEnd();
                }
            }

            return new NoDefaultConstructorBeanImpl(id, name);
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }

    @Override
    public Set<AegisType> getDependencies() {
        AegisType stringType = getTypeMapping().getType(String.class);
        return Collections.singleton(stringType);
    }
}
