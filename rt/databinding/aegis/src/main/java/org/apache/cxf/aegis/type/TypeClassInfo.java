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

import java.lang.reflect.Type;

import javax.xml.namespace.QName;

/**
 * Object to carry information for an Aegis type, 
 * such as that from an XML mapping file.
 * 
 * Note that this class has a misleading name. It is used both for 
 * type information that corresponds to a type, and also for parameters 
 * of methods and elements of beans. When describing a top-level type,
 * minOccurs and maxOccurs are not meaningful. Aegis does not have a
 * very clear model of a 'type', in the sense of an AegisType object
 * corresponding to some particular XML Schema type, in isolation
 * from the mapping system. 
 * 
 * Historically, Aegis talked about Java types as Class. However, 
 * we want to be able to keep track, distinctly, of un-erased
 * generics. That requires java.lang.reflect.Type.
 * 
 *  Nillable is only used for parameters.
 * 
 *  It might be that the code could be deconfused by
 * using the nillable property in here for the non-parameters cases
 * that look at minOccurs and maxOccurs.
 * 
 * Historically, the code for dealing with nillable was very confused,
 * and so the support here is a bit ginger, until someone figures out how
 * to sort things out. Thus the three-valued support instead
 * of a plain boolean.
 */
public class TypeClassInfo {
    // The general reflection Type.
    private Type type;
    private Object[] annotations;

    // for collection types we pull out the parameters for convenience.
    private Type keyType;
    private Type valueType;

    // Preferred element name.
    private QName mappedName;
    // XML schema name for the type.
    private QName typeName;
    
    // a Class reference to the aegis aegisTypeClass, if the app has specified it
    // via XML or via an annotation.
    private Class<? extends AegisType> aegisTypeClass;

    private String description;
    private long minOccurs = -1;
    private long maxOccurs = -1;
    // Flat array.
    private boolean flat;
    private Boolean nillable;
    
    public boolean nonDefaultAttributes() {
        return minOccurs != -1 || maxOccurs != -1 || flat;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Object[] annotations) {
        this.annotations = annotations;
    }

    public Type getKeyType() {
        return keyType;
    }

    public void setKeyType(Type keyType) {
        this.keyType = keyType;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public QName getTypeName() {
        return typeName;
    }

    public void setTypeName(QName name) {
        this.typeName = name;
    }

    public Class<? extends AegisType> getAegisTypeClass() {
        return aegisTypeClass;
    }

    public void setAegisTypeClass(Class<? extends AegisType> aegisTypeClass) {
        this.aegisTypeClass = aegisTypeClass;
    }

    public QName getMappedName() {
        return mappedName;
    }

    public void setMappedName(QName mappedName) {
        this.mappedName = mappedName;
    }

    public long getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(long maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public long getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(long minOccurs) {
        this.minOccurs = minOccurs;
    }

    public boolean isFlat() {
        return flat;
    }

    public void setFlat(boolean flat) {
        this.flat = flat;
    }

    @Override
    public String toString() {
        return "TypeClassInfo " + getDescription();
    }

    public Type getValueType() {
        return valueType;
    }

    public void setValueType(Type valueType) {
        this.valueType = valueType;
    }

    public Boolean getNillable() {
        return nillable;
    }

    public void setNillable(Boolean nillable) {
        this.nillable = nillable;
    }
}