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

package org.apache.cxf.xmlbeans;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.Schema;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;


public class DataReaderImpl implements DataReader<XMLStreamReader> {
    private static final Logger LOG = LogUtils.getLogger(XmlBeansDataBinding.class);
    private boolean validate;
    
    public DataReaderImpl() {
    }

    public Object read(XMLStreamReader input) {
        return read(null, input);
    }

    public Object read(MessagePartInfo part, XMLStreamReader reader) {
        Class<?> typeClass = part.getTypeClass();
        boolean unwrap = false;
        if (!XmlObject.class.isAssignableFrom(typeClass)) {
            typeClass = (Class<?>)part.getProperty(XmlAnySimpleType.class.getName());
            unwrap = true;
        }
        boolean isOutClass = false;
        Class<?> encClass = typeClass.getEnclosingClass();
        if (encClass != null) {
            typeClass = encClass;
            isOutClass = true;
        }
        Class<?> cls[] = typeClass.getDeclaredClasses();
        Object obj = null;
        for (Class<?> c : cls) {
            if ("Factory".equals(c.getSimpleName())) {
                try {
                    
                    SchemaType st = (SchemaType)part.getProperty(SchemaType.class.getName());
                    XmlOptions options = new XmlOptions();
                    if (validate) {
                        options.setValidateOnSet();
                    }
                    if (st != null && !st.isDocumentType() && !isOutClass) {
                        options.setLoadReplaceDocumentElement(null);
                    }
                    Method meth = c.getMethod("parse", XMLStreamReader.class, XmlOptions.class);
                    obj = meth.invoke(null, reader, options);                    
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new Fault(new Message("UNMARSHAL_ERROR", LOG, part.getTypeClass()), e);
                }
            }
        }
        if (unwrap && obj != null) {
            try {
                Class<?> tc = part.getTypeClass(); 
                String methName;
                if (tc.equals(Integer.TYPE) || tc.equals(Integer.class)) {
                    methName = "getIntValue";
                } else if (tc.equals(byte[].class)) {
                    methName = "byteArrayValue";
                } else {
                    String tp = tc.getSimpleName();
                    tp = Character.toUpperCase(tp.charAt(0)) + tp.substring(1);
                    methName = "get" + tp + "Value";
                }
                Method m = obj.getClass().getMethod(methName);
                obj = m.invoke(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (isOutClass) {
            for (Method m : encClass.getDeclaredMethods()) {
                if (m.getName().startsWith("get")
                    && m.getParameterTypes().length == 0
                    && m.getReturnType().equals(part.getTypeClass())) {
                    try {
                        obj = m.invoke(obj);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (reader.getEventType() == XMLStreamReader.END_ELEMENT) {
            try {
                reader.next();
            } catch (XMLStreamException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return obj;
    }

    public Object read(QName name, XMLStreamReader input, Class type) {        
        return null;
    }

    
    public void setAttachments(Collection<Attachment> attachments) {
    }

    public void setProperty(String prop, Object value) {
    }

    public void setSchema(Schema s) {
        validate = s != null;
    }

    public void setSchema(XmlSchemaCollection validationSchemas) {
        validate = validationSchemas != null;
    }

}
