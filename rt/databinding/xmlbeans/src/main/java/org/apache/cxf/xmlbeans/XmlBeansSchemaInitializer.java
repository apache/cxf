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


import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.InputSource;


import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.resolver.URIResolver;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.schema.BuiltinSchemaTypeSystem;

/**
 * Walks the service model and sets up the element/type names.
 */
class XmlBeansSchemaInitializer extends ServiceModelVisitor {


    private static final Logger LOG = LogUtils.getLogger(XmlBeansSchemaInitializer.class);
    private static final Map<Class<?>, Class<? extends XmlAnySimpleType>> CLASS_MAP 
        = new HashMap<Class<?>, Class<? extends XmlAnySimpleType>>();
    private SchemaCollection schemas;
    private XmlBeansDataBinding dataBinding;
    private Map<String, XmlSchema> schemaMap 
        = new HashMap<String, XmlSchema>();
    
    static {
        CLASS_MAP.put(String.class, org.apache.xmlbeans.XmlString.class);
        CLASS_MAP.put(Integer.class, org.apache.xmlbeans.XmlInt.class);
        CLASS_MAP.put(Integer.TYPE, org.apache.xmlbeans.XmlInt.class);
        CLASS_MAP.put(Short.class, org.apache.xmlbeans.XmlShort.class);
        CLASS_MAP.put(Short.TYPE, org.apache.xmlbeans.XmlShort.class);
        CLASS_MAP.put(Byte.class, org.apache.xmlbeans.XmlByte.class);
        CLASS_MAP.put(Byte.TYPE, org.apache.xmlbeans.XmlByte.class);
        CLASS_MAP.put(Float.class, org.apache.xmlbeans.XmlFloat.class);
        CLASS_MAP.put(Float.TYPE, org.apache.xmlbeans.XmlFloat.class);
        CLASS_MAP.put(Double.class, org.apache.xmlbeans.XmlDouble.class);
        CLASS_MAP.put(Double.TYPE, org.apache.xmlbeans.XmlDouble.class);
        CLASS_MAP.put(Long.class, org.apache.xmlbeans.XmlLong.class);
        CLASS_MAP.put(Long.TYPE, org.apache.xmlbeans.XmlLong.class);
        CLASS_MAP.put(Boolean.class, org.apache.xmlbeans.XmlBoolean.class);
        CLASS_MAP.put(Boolean.TYPE, org.apache.xmlbeans.XmlBoolean.class);
        CLASS_MAP.put(BigDecimal.class, org.apache.xmlbeans.XmlDecimal.class);
        CLASS_MAP.put(BigInteger.class, org.apache.xmlbeans.XmlInteger.class);
        CLASS_MAP.put(Date.class, org.apache.xmlbeans.XmlDate.class);
        CLASS_MAP.put(Calendar.class, org.apache.xmlbeans.XmlDate.class);
        CLASS_MAP.put(byte[].class, org.apache.xmlbeans.XmlBase64Binary.class);
    }
    
    public XmlBeansSchemaInitializer(ServiceInfo serviceInfo,
                                     SchemaCollection col,
                                     XmlBeansDataBinding db) {
        super(serviceInfo);
        schemas = col;
        dataBinding = db;
    }
    
    public class XMLSchemaResolver implements URIResolver {
        final SchemaTypeSystem sts;
        public XMLSchemaResolver(SchemaTypeSystem sts) {
            this.sts = sts;
        }
        
        public InputSource resolveEntity(String targetNamespace, String schemaLocation, String baseUri) {
            InputStream ins = sts.getSourceAsStream(schemaLocation);
            if (ins != null) {
                return new InputSource(ins);
            }
            return null;
        }
    }

    XmlSchema getSchema(SchemaTypeSystem sts, String file) {
        if (schemaMap.containsKey(file)) {
            return schemaMap.get(file);
        }
        InputStream ins = sts.getSourceAsStream(file);
        try {
            //temporary marker to make sure recursive imports don't blow up
            schemaMap.put(file, null);

            Document doc = XMLUtils.parse(ins);
            Element elem = DOMUtils.getFirstElement(doc.getDocumentElement());
            while (elem != null) {
                if (elem.getLocalName().equals("import")) {
                    String loc = elem.getAttribute("schemaLocation");
                    if (!StringUtils.isEmpty(loc)) {
                        getSchema(sts, loc);
                    }
                }                 
                elem = DOMUtils.getNextElement(elem);
            }
                
            XmlSchema schema = dataBinding.addSchemaDocument(serviceInfo,
                                                             schemas, 
                                                             doc, 
                                                             file);
            schemaMap.put(file, schema);
            return schema;
        } catch (Exception e) {
            throw new RuntimeException("Failed to find schema for: " + file, e);
        }
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
    private void mapClass(MessagePartInfo part, Class clazz) {
        
        if (!XmlObject.class.isAssignableFrom(clazz)) {
            
            Class<? extends XmlAnySimpleType> type = CLASS_MAP.get(clazz);
            if (type == null) {
                LOG.log(Level.SEVERE, clazz.getName() + " was not found in class map");
                return;
            }
            SchemaTypeSystem sts = BuiltinSchemaTypeSystem.get();
            SchemaType st2 = sts.typeForClassname(type.getName());

            part.setProperty(SchemaType.class.getName(), st2);
            part.setProperty(XmlAnySimpleType.class.getName(), type);
            part.setTypeQName(st2.getName());
            XmlSchemaType xmlSchema = schemas.getTypeByQName(st2.getName());
            part.setXmlSchema(xmlSchema);
            return;
        }
        
        try {
            Field field = clazz.getField("type");
            SchemaType st = (SchemaType)field.get(null);
            part.setProperty(SchemaType.class.getName(), st);
            
            SchemaTypeSystem sts = st.getTypeSystem();
            schemas.getXmlSchemaCollection().setSchemaResolver(new XMLSchemaResolver(sts));

            XmlSchema schema = getSchema(sts, st.getSourceName());

            if (st.isDocumentType()) {
                XmlSchemaElement sct = schema.getElementByName(st.getDocumentElementName());
                part.setXmlSchema(sct);
                part.setElement(true);
                part.setElementQName(st.getDocumentElementName());
                part.setConcreteName(st.getDocumentElementName());
            } else if (st.getComponentType() == SchemaType.ELEMENT) {
                XmlSchemaElement sct = schema.getElementByName(st.getName());
                part.setXmlSchema(sct);
                part.setElement(true);
            } else {
                XmlSchemaType sct = schema.getTypeByName(st.getName());
                part.setTypeQName(st.getName());
                part.setXmlSchema(sct);
                part.setElement(false);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }        
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
    
}
