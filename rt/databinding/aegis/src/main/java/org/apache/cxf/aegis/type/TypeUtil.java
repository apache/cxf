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
package org.apache.cxf.aegis.type;

import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.util.NamespaceHelper;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.common.xmlschema.XmlSchemaUtils;
import org.apache.ws.commons.schema.XmlSchema;


/**
 * Static methods/constants for Aegis.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public final class TypeUtil {
    public static final Logger LOG = LogUtils.getL7dLogger(TypeUtil.class);

    private TypeUtil() {
        //utility class
    }
    
    public static Type getReadType(XMLStreamReader xsr, AegisContext context, Type baseType) {

        if (!context.isReadXsiTypes()) {
            if (baseType == null) {
                LOG.warning("xsi:type reading disabled, and no type available for "  
                         + xsr.getName());
            }
            return baseType;
        }
        
        String overrideType = xsr.getAttributeValue(SOAPConstants.XSI_NS, "type");
        if (overrideType != null) {
            QName overrideName = NamespaceHelper.createQName(xsr.getNamespaceContext(), overrideType);

            if (baseType == null || !overrideName.equals(baseType.getSchemaType())) {
                Type improvedType = null;
                TypeMapping tm;
                if (baseType != null) {
                    tm = baseType.getTypeMapping();
                    improvedType = tm.getType(overrideName);
                }
                if (improvedType == null) {
                    improvedType = context.getRootType(overrideName);
                }
                if (improvedType != null) {
                    return improvedType;
                }
            }
        
            if (baseType != null) {
                LOG.finest("xsi:type=\"" + overrideName
                         + "\" was specified, but no corresponding Type was registered; defaulting to "
                         + baseType.getSchemaType());
                return baseType;
            } else {
                LOG.warning("xsi:type=\"" + overrideName
                         + "\" was specified, but no corresponding Type was registered; no default.");
                return null;
            }
        } else {
            if (baseType == null) {
                LOG.warning("xsi:type absent, and no type available for "  
                         + xsr.getName());
            }
            return baseType;
        }
    }

    /**
     * getReadType cannot just look up the xsi:type in the mapping. This function must be
     * called instead at the root where there is no initial mapping to start from, as from
     * a part or an element of some containing item.
     * @param xsr
     * @param context
     * @return
     */
    public static Type getReadTypeStandalone(XMLStreamReader xsr, AegisContext context, Type baseType) {
        
        if (baseType != null) {
            return getReadType(xsr, context, baseType);
        }

        if (!context.isReadXsiTypes()) {
            LOG.warning("xsi:type reading disabled, and no type available for "  
                     + xsr.getName());
            return null;
        }
        
        String typeNameString = xsr.getAttributeValue(SOAPConstants.XSI_NS, "type");
        if (typeNameString != null) {
            QName schemaTypeName = NamespaceHelper.createQName(xsr.getNamespaceContext(), 
                                                               typeNameString);
            TypeMapping tm;
            tm = context.getTypeMapping();
            Type type = tm.getType(schemaTypeName);
            
            if (type == null) {
                type = context.getRootType(schemaTypeName);
            }
            
            if (type != null) {
                return type;
            }
                    
            LOG.warning("xsi:type=\"" + schemaTypeName
                     + "\" was specified, but no corresponding Type was registered; no default.");
            return null;
        }
        LOG.warning("xsi:type was not specified for top-level element " + xsr.getName());
        return null;
    }
    
    public static Type getWriteType(AegisContext globalContext, Object value, Type type) {
        if (value != null && type != null && type.getTypeClass() != value.getClass()) {
            Type overrideType = globalContext.getRootType(value.getClass());
            if (overrideType != null) {
                return overrideType;
            }
        }
        return type;
    }

    public static Type getWriteTypeStandalone(AegisContext globalContext, Object value, Type type) {
        if (type != null) {
            return getWriteType(globalContext, value, type);
        }
        
        TypeMapping tm;
        tm = globalContext.getTypeMapping();
        // don't use this for null!
        type = tm.getType(value.getClass());

        return type;
    }
    
    
    public static void setAttributeAttributes(QName name, Type type, XmlSchema root) {
        String ns = type.getSchemaType().getNamespaceURI();
        XmlSchemaUtils.addImportIfNeeded(root, ns);
    }
}
