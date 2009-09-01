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
package org.apache.cxf.aegis.type.basic;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.util.NamespaceHelper;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;

public class XMLBeanTypeInfo extends BeanTypeInfo {
    private static final Logger LOG = LogUtils.getL7dLogger(XMLBeanTypeInfo.class);
    private List mappings;

    /**
     * Map used for storing meta data about each property
     */
    private Map<QName, BeanTypePropertyInfo> name2PropertyInfo = new HashMap<QName, BeanTypePropertyInfo>();

    public XMLBeanTypeInfo(Class typeClass, List mappings, String defaultNS) {
        super(typeClass, defaultNS);

        this.mappings = mappings;
    }

    @Override
    protected boolean registerType(PropertyDescriptor desc) {
        Element e = getPropertyElement(desc.getName());
        if (e != null && DOMUtils.getAttributeValueEmptyNull(e, "type") != null) {
            return false;
        }

        return super.registerType(desc);
    }

    @Override
    protected void mapProperty(PropertyDescriptor pd) {
        Element e = getPropertyElement(pd.getName());
        String style = null;
        QName mappedName = null;

        if (e != null) {
            String ignore = DOMUtils.getAttributeValueEmptyNull(e, "ignore");
            if (ignore != null && ignore.equals("true")) {
                return;
            }

            LOG.finest("Found mapping for property " + pd.getName());

            style = DOMUtils.getAttributeValueEmptyNull(e, "style");
        }

        if (style == null) {
            style = "element";
        }
        
        boolean element = "element".equals(style);
        boolean qualify;
        if (element) {
            qualify = isQualifyElements();
        } else {
            qualify = isQualifyAttributes();
        }
        String namespace = null;
        if (qualify) {
            namespace = getDefaultNamespace();
        }
        
        if (e != null) {
            mappedName = NamespaceHelper.createQName(e, DOMUtils.getAttributeValueEmptyNull(e, "mappedName"),
                                                     namespace);
        }

        if (mappedName == null) {
            mappedName = createMappedName(pd, qualify);
        }

        if (e != null) {
            

            QName mappedType = NamespaceHelper.createQName(e, 
                                                           DOMUtils.getAttributeValueEmptyNull(e, "typeName"),
                                                           getDefaultNamespace());
            if (mappedType != null) {
                mapTypeName(mappedName, mappedType);
            } 
            
            /*
             * Whenever we create a type object, it has to have a schema type. If we created a custom type
             * object out of thin air here, we've may have a problem. If "typeName" was specified, then then
             * we know the mapping. But if mappedName was not specified, then the typeName will come from the
             * type mapping, so we have to ask it. And if some other type creator has something to say about
             * it, we'll get it wrong.
             */

            
            String explicitTypeName = DOMUtils.getAttributeValueEmptyNull(e, "type");
            if (explicitTypeName != null) {
                try {
                    Class<?> typeClass = 
                        ClassLoaderUtils.loadClass(explicitTypeName, XMLBeanTypeInfo.class);
                    AegisType customTypeObject = (AegisType) typeClass.newInstance();
                    mapType(mappedName, customTypeObject);
                    QName schemaType = mappedType;
                    if (schemaType == null) {
                        schemaType = getTypeMapping().getTypeQName(pd.getPropertyType());
                    }
                    customTypeObject.setSchemaType(schemaType);
                } catch (ClassNotFoundException e1) {
                    //
                } catch (InstantiationException e2) {
                    //
                } catch (IllegalAccessException e3) {
                    //
                }                
            }
            
            String nillableVal = DOMUtils.getAttributeValueEmptyNull(e, "nillable");
            if (nillableVal != null && nillableVal.length() > 0) {
                ensurePropertyInfo(mappedName).setNillable(Boolean.valueOf(nillableVal).booleanValue());
            }

            String minOccurs = DOMUtils.getAttributeValueEmptyNull(e, "minOccurs");
            if (minOccurs != null && minOccurs.length() > 0) {
                ensurePropertyInfo(mappedName).setMinOccurs(Integer.parseInt(minOccurs));
            }
            String maxOccurs = DOMUtils.getAttributeValueEmptyNull(e, "maxOccurs");
            if (maxOccurs != null && maxOccurs.length() > 0) {
                ensurePropertyInfo(mappedName).setMinOccurs(Integer.parseInt(maxOccurs));
            }
        }

        try {
            // logger.debug("Mapped " + pd.getName() + " as " + style + " with
            // name " + mappedName);
            if ("element".equals(style)) {
                mapElement(pd.getName(), mappedName);
            } else if ("attribute".equals(style)) {
                mapAttribute(pd.getName(), mappedName);
            } else {
                throw new DatabindingException("Invalid style: " + style);
            }
        } catch (DatabindingException ex) {
            ex.prepend("Couldn't create type for property " + pd.getName() + " on " + getTypeClass());

            throw ex;
        }
    }

    private Element getPropertyElement(String name2) {
        for (Iterator itr = mappings.iterator(); itr.hasNext();) {
            Element mapping2 = (Element)itr.next();
            List<Element> elements = DOMUtils.getChildrenWithName(mapping2, "", "property");
            for (int i = 0; i < elements.size(); i++) {
                Element e = (Element)elements.get(i);
                String name = DOMUtils.getAttributeValueEmptyNull(e, "name");

                if (name != null && name.equals(name2)) {
                    return e;
                }
            }
        }

        return null;
    }

    /**
     * Grab Nillable by looking in PropertyInfo map if no entry found, revert to
     * parent class
     */
    @Override
    public boolean isNillable(QName name) {
        BeanTypePropertyInfo info = getPropertyInfo(name);
        if (info != null) {
            return info.isNillable();
        }
        return super.isNillable(name);
    }

    /**
     * Return minOccurs if specified in the XML, otherwise from the defaults
     * in the base class.
     */
    @Override
    public int getMinOccurs(QName name) {
        BeanTypePropertyInfo info = getPropertyInfo(name);
        if (info != null) {
            return info.getMinOccurs();
        }
        return super.getMinOccurs(name);
    }

    /**
     * Return maxOccurs if specified in the XML, otherwise from the
     * default in the base class.
     */
    @Override
    public int getMaxOccurs(QName name) {
        BeanTypePropertyInfo info = getPropertyInfo(name);
        if (info != null) {
            return info.getMaxOccurs();
        }
        return super.getMaxOccurs(name);
    }

    /**
     * Grab the Property Info for the given property
     * 
     * @param name
     * @return the BeanTypePropertyInfo for the property or NULL if none found
     */
    private BeanTypePropertyInfo getPropertyInfo(QName name) {
        return name2PropertyInfo.get(name);
    }

    /**
     * Grab the Property Info for the given property but if not found create one
     * and add it to the map
     * 
     * @param name
     * @return the BeanTypePropertyInfo for the property
     */
    private BeanTypePropertyInfo ensurePropertyInfo(QName name) {
        BeanTypePropertyInfo result = getPropertyInfo(name);
        if (result == null) {
            result = new BeanTypePropertyInfo();
            name2PropertyInfo.put(name, result);
        }
        return result;
    }
}
