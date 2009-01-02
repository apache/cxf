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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeCreator;
import org.apache.cxf.aegis.type.TypeMapping;

public class BeanTypeInfo {
    private Map<QName, QName> mappedName2typeName = new HashMap<QName, QName>();
    private Map<QName, String> mappedName2pdName = new HashMap<QName, String>();
    private Map<QName, Type> mappedName2type = new HashMap<QName, Type>();
    private Class<?> beanClass;
    private List<QName> attributes = new ArrayList<QName>();
    private List<QName> elements = new ArrayList<QName>();
    private PropertyDescriptor[] descriptors;
    private TypeMapping typeMapping;
    private volatile boolean initialized;
    private String defaultNamespace;
    private int minOccurs;
    private boolean nillable = true;
    private boolean isExtension;
    private boolean qualifyAttributes;
    private boolean qualifyElements = true;

    /**
     * extensibleElements means adding xs:any to WSDL Complex Type Definition
     */
    private boolean extensibleElements = true;

    /**
     * extensibleAttributes means adding xs:anyAttribute to WSDL Complex Type
     * Definition
     */
    private boolean extensibleAttributes = true;

    public BeanTypeInfo(Class<?> typeClass, String defaultNamespace) {
        this.beanClass = typeClass;
        this.defaultNamespace = defaultNamespace;

        initializeProperties();
    }

    /**
     * Create a BeanTypeInfo class.
     * 
     * @param typeClass
     * @param defaultNamespace
     * @param initiallize If true attempt default property/xml mappings.
     */
    public BeanTypeInfo(Class<?> typeClass, String defaultNamespace, boolean initialize) {
        this.beanClass = typeClass;
        this.defaultNamespace = defaultNamespace;

        initializeProperties();
        initialized = !initialize;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public void initialize() {
        try {
            if (!initialized) {
                initializeSync();
            }
        } catch (Exception e) {
            if (e instanceof DatabindingException) {
                throw (DatabindingException)e;
            }
            throw new DatabindingException("Couldn't create TypeInfo.", e);
        }
    }

    private synchronized void initializeSync() {
        if (!initialized) {
            for (int i = 0; i < descriptors.length; i++) {
                // Don't map the property unless there is a read property
                if (isMapped(descriptors[i])) {
                    mapProperty(descriptors[i]);
                }
            }
            initialized = true;
        }
    }

    public boolean isMapped(PropertyDescriptor pd) {
        if (pd.getReadMethod() == null) {
            return false;
        }

        return true;
    }

    protected void mapProperty(PropertyDescriptor pd) {
        String name = pd.getName();

        if (isAttribute(pd)) {
            mapAttribute(name, createMappedName(pd, qualifyAttributes));
        } else if (isElement(pd)) {
            mapElement(name, createMappedName(pd, qualifyElements));
        }
    }

    protected PropertyDescriptor[] getPropertyDescriptors() {
        return descriptors;
    }

    protected PropertyDescriptor getPropertyDescriptor(String name) {
        for (int i = 0; i < descriptors.length; i++) {
            if (descriptors[i].getName().equals(name)) {
                return descriptors[i];
            }
        }

        return null;
    }

    /**
     * Get the type class for the field with the specified QName.
     */
    public Type getType(QName name) {
        // 1. Try a prexisting mapped type
        Type type = mappedName2type.get(name);

        // 2. Try to get the type by its name, if there is one
        if (type == null) {
            QName typeName = getMappedTypeName(name);
            if (typeName != null) {
                type = getTypeMapping().getType(typeName);

                if (type != null) {
                    mapType(name, type);
                }
            }
        }

        // 3. Create the type from the property descriptor and map it
        if (type == null) {
            PropertyDescriptor desc;
            try {
                desc = getPropertyDescriptorFromMappedName(name);
            } catch (Exception e) {
                if (e instanceof DatabindingException) {
                    throw (DatabindingException)e;
                }
                throw new DatabindingException("Couldn't get properties.", e);
            }

            if (desc == null) {
                return null;
            }

            try {
                TypeMapping tm = getTypeMapping();
                TypeCreator tc = tm.getTypeCreator();
                type = tc.createType(desc);
            } catch (DatabindingException e) {
                e.prepend("Couldn't create type for property " + desc.getName() + " on " + getTypeClass());

                throw e;
            }

            // second part is possible workaround for XFIRE-586
            if (registerType(desc)) {
                getTypeMapping().register(type);
            }

            mapType(name, type);
        }

        if (type == null) {
            throw new DatabindingException("Couldn't find type for property " + name);
        }

        return type;
    }

    protected boolean registerType(PropertyDescriptor desc) {
        return true;
    }

    public void mapType(QName name, Type type) {
        mappedName2type.put(name, type);
    }

    private QName getMappedTypeName(QName name) {
        return mappedName2typeName.get(name);
    }

    public TypeMapping getTypeMapping() {
        return typeMapping;
    }

    public void setTypeMapping(TypeMapping typeMapping) {
        this.typeMapping = typeMapping;
    }

    /**
     * Specifies the name of the property as it shows up in the xml schema. This
     * method just returns <code>propertyDescriptor.getName();</code>
     * 
     * @param desc
     * @return
     */
    protected QName createMappedName(PropertyDescriptor desc, boolean qualified) {
        if (qualified) {
            return new QName(getDefaultNamespace(), desc.getName());
        } else {
            return new QName(null, desc.getName());
        }
    }

    public void mapAttribute(String property, QName mappedName) {
        mappedName2pdName.put(mappedName, property);
        attributes.add(mappedName);
    }

    public void mapElement(String property, QName mappedName) {
        mappedName2pdName.put(mappedName, property);
        elements.add(mappedName);
    }

    /**
     * Specifies the SchemaType for a particular class.
     * 
     * @param mappedName
     * @param type
     */
    public void mapTypeName(QName mappedName, QName type) {
        mappedName2typeName.put(mappedName, type);
    }

    private void initializeProperties() {
        BeanInfo beanInfo = null;
        try {
            if (beanClass.isInterface() || beanClass.isPrimitive()) {
                descriptors = getInterfacePropertyDescriptors(beanClass);
            } else if (beanClass == Object.class || beanClass == Throwable.class) {
                // do nothing
            } else if (beanClass == Throwable.class) {
                // do nothing
            } else if (Throwable.class.isAssignableFrom(beanClass)) {
                beanInfo = Introspector.getBeanInfo(beanClass, Throwable.class);
            } else if (RuntimeException.class.isAssignableFrom(beanClass)) {
                beanInfo = Introspector.getBeanInfo(beanClass, RuntimeException.class);
            } else if (Throwable.class.isAssignableFrom(beanClass)) {
                beanInfo = Introspector.getBeanInfo(beanClass, Throwable.class);
            } else {
                beanInfo = Introspector.getBeanInfo(beanClass, Object.class);
            }
        } catch (IntrospectionException e) {
            throw new DatabindingException("Couldn't introspect interface.", e);
        }

        if (beanInfo != null) {
            descriptors = beanInfo.getPropertyDescriptors();
        }

        if (descriptors == null) {
            descriptors = new PropertyDescriptor[0];
        }
    }

    private PropertyDescriptor[] getInterfacePropertyDescriptors(Class<?> clazz) {
        List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();

        getInterfacePropertyDescriptors(clazz, pds, new HashSet<Class<?>>());

        return pds.toArray(new PropertyDescriptor[pds.size()]);
    }

    private void getInterfacePropertyDescriptors(Class<?> clazz, List<PropertyDescriptor> pds,
                                                 Set<Class<?>> classes) {
        if (classes.contains(clazz)) {
            return;
        }

        classes.add(clazz);

        try {
            Class[] interfaces = clazz.getInterfaces();

            /**
             * add base interface information
             */
            BeanInfo info = Introspector.getBeanInfo(clazz);
            for (int j = 0; j < info.getPropertyDescriptors().length; j++) {
                PropertyDescriptor pd = info.getPropertyDescriptors()[j];
                if (!containsPropertyName(pds, pd.getName())) {
                    pds.add(pd);
                }
            }

            /**
             * add extended interface information
             */
            for (int i = 0; i < interfaces.length; i++) {
                getInterfacePropertyDescriptors(interfaces[i], pds, classes);
            }
        } catch (IntrospectionException e) {
            // do nothing
        }
    }

    private boolean containsPropertyName(List<PropertyDescriptor> pds, String name) {
        for (Iterator<PropertyDescriptor> itr = pds.iterator(); itr.hasNext();) {
            PropertyDescriptor pd = itr.next();
            if (pd.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public PropertyDescriptor getPropertyDescriptorFromMappedName(QName name) {
        return getPropertyDescriptor(getPropertyNameFromMappedName(name));
    }

    protected boolean isAttribute(PropertyDescriptor desc) {
        return false;
    }

    protected boolean isElement(PropertyDescriptor desc) {
        return true;
    }

    protected boolean isSerializable(PropertyDescriptor desc) {
        return true;
    }

    protected Class<?> getTypeClass() {
        return beanClass;
    }

    /**
     * Nillable is only allowed if the actual property is Nullable
     * 
     * @param name
     * @return
     */
    public boolean isNillable(QName name) {
        Type type = getType(name);
        if (!type.isNillable()) {
            return false;
        }
        return nillable;
    }

    public int getMinOccurs(QName name) {
        return minOccurs;
    }
    
    public long getMinOccurs() {
        return minOccurs;
    }

    public void setDefaultMinOccurs(int m) {
        this.minOccurs = m;
    }

    public void setDefaultNillable(boolean n) {
        this.nillable = n;
    }

    private String getPropertyNameFromMappedName(QName name) {
        return mappedName2pdName.get(name);
    }

    public Iterator<QName> getAttributes() {
        return attributes.iterator();
    }

    public Iterator<QName> getElements() {
        return elements.iterator();
    }

    public boolean isExtensibleElements() {
        return extensibleElements;
    }

    public void setExtensibleElements(boolean futureProof) {
        this.extensibleElements = futureProof;
    }

    public boolean isExtensibleAttributes() {
        return extensibleAttributes;
    }

    public void setExtensibleAttributes(boolean extensibleAttributes) {
        this.extensibleAttributes = extensibleAttributes;
    }

    public void setExtension(boolean extension) {
        this.isExtension = extension;
    }

    public boolean isExtension() {
        return isExtension;
    }

    /** * @return Returns the qualifyAttributes.
     */
    public boolean isQualifyAttributes() {
        return qualifyAttributes;
    }

    /**
     * @param qualifyAttributes The qualifyAttributes to set.
     */
    public void setQualifyAttributes(boolean qualifyAttributes) {
        this.qualifyAttributes = qualifyAttributes;
    }

    /** * @return Returns the qualifyElements.
     */
    public boolean isQualifyElements() {
        return qualifyElements;
    }

    /**
     * @param qualifyElements The qualifyElements to set.
     */
    public void setQualifyElements(boolean qualifyElements) {
        this.qualifyElements = qualifyElements;
    }

}
