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

package org.apache.cxf.jibx;

import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.JiBXException;

public class JibxSchemaInitializer extends ServiceModelVisitor {
    private static final Logger LOG = LogUtils.getLogger(JibxSchemaInitializer.class);
    private SchemaCollection schemas;

    public JibxSchemaInitializer(ServiceInfo serviceInfo, SchemaCollection schemas,
                                 JibxDataBinding databinding) {
        super(serviceInfo);
        this.schemas = schemas;
    }

    @Override
    public void begin(MessagePartInfo part) {
        LOG.finest(part.getName().toString());
        // Check to see if the WSDL information has been filled in for us.
        if (part.getTypeQName() != null || part.getElementQName() != null) {
            checkForExistence(part);
            return;
        }

        Class<?> clazz = part.getTypeClass();
        if (clazz == null) {
            return;
        }

        boolean isFromWrapper = part.getMessageInfo().getOperation().isUnwrapped();
        if (isFromWrapper && clazz.isArray() && !Byte.TYPE.equals(clazz.getComponentType())) {
            clazz = clazz.getComponentType();
        }
        mapClass(part, clazz);
    }

    public void checkForExistence(MessagePartInfo part) {
        QName qn = part.getElementQName();
        if (qn != null) {
            XmlSchemaElement el = schemas.getElementByQName(qn);
            if (el == null) {
                Class<?> clazz = part.getTypeClass();
                if (clazz == null) {
                    return;
                }

                boolean isFromWrapper = part.getMessageInfo().getOperation().isUnwrapped();
                if (isFromWrapper && clazz.isArray() && !Byte.TYPE.equals(clazz.getComponentType())) {
                    clazz = clazz.getComponentType();
                }
                mapClass(part, clazz);
            }
        }
    }

    private void mapClass(MessagePartInfo part, Class<?> clazz) {
        if (JibxSimpleTypes.isSimpleType(clazz)) {
            QName schemaType = JibxSimpleTypes.schemaType(clazz);
            part.setTypeQName(schemaType);
            part.setXmlSchema(schemas.getTypeByQName(schemaType));
        } else {
            try {
                IBindingFactory factory = BindingDirectory.getFactory(clazz);
                String[][] abstractMappings = factory.getAbstractMappings();
                String stype = abstractMappings[0][0];
                QName qName = JibxSimpleTypes.toQname(stype);

                XmlSchema schema = schemas.getSchemaForElement(qName);
                if (schema != null) {
                    // needs to handle element is a Global element case
                    XmlSchemaElement element = schema.getElementByName(qName);
                    part.setXmlSchema(element);
                    part.setElementQName(qName);
                    part.setConcreteName(qName);
                    part.setElement(true);
                } else {
                    // TODO : throw an exception ?
                }

            } catch (JiBXException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
