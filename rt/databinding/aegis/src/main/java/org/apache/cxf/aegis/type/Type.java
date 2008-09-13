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

import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.jdom.Element;

/**
 * A Type reads and writes XML fragments to create and write objects.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public abstract class Type {

    protected Class typeClass;

    private QName schemaType;

    private TypeMapping typeMapping;

    private boolean abstrct = true;

    private boolean nillable = true;

    private boolean writeOuter = true;

    public Type() {
    }

    /**
     * Read in the XML fragment and create an object.
     * 
     * @param reader
     * @param context
     * @return
     * @throws DatabindingException
     */
    public abstract Object readObject(MessageReader reader, Context context) throws DatabindingException;

    /**
     * Writes the object to the MessageWriter.
     * 
     * @param object
     * @param writer
     * @param context
     * @throws DatabindingException
     */
    public abstract void writeObject(Object object, MessageWriter writer, Context context)
        throws DatabindingException;

    /**
     * If this type should correspond to a global, named, schema type, here is where the
     * type object adds it to the schema.
     * @param root root of the XSD document.
     */
    public void writeSchema(Element root) {
    }
    
    /**
     * If the type object merely wants to contribute attributes to the 
     * xsd:element element, it can implement this. 
     * @param schemaElement
     */
    public void addToSchemaElement(Element schemaElement) {
    }

    /**
     * @return Returns the typeMapping.
     */
    public TypeMapping getTypeMapping() {
        return typeMapping;
    }

    /**
     * @param typeMapping The typeMapping to set.
     */
    public void setTypeMapping(TypeMapping typeMapping) {
        this.typeMapping = typeMapping;
    }

    /**
     * @return Returns the typeClass.
     */
    public Class getTypeClass() {
        return typeClass;
    }

    /**
     * @param typeClass The typeClass to set.
     */
    public void setTypeClass(Class typeClass) {
        this.typeClass = typeClass;

        if (typeClass.isPrimitive()) {
            setNillable(false);
        }
    }

    /**
     * @return True if a complex type schema must be written.
     */
    public boolean isComplex() {
        return false;
    }

    public boolean isAbstract() {
        return abstrct;
    }

    public void setAbstract(boolean ab) {
        this.abstrct = ab;
    }

    public boolean isNillable() {
        return nillable;
    }

    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    /**
     * Return a set of Type dependencies. Returns null if this type has no
     * dependencies.
     * 
     * @return Set of <code>Type</code> dependencies
     */
    public Set<Type> getDependencies() {
        return null;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Type) {
            Type type = (Type)obj;

            if (type.getSchemaType().equals(getSchemaType()) && type.getTypeClass().equals(getTypeClass())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hashcode = 0;

        if (getTypeClass() != null) {
            hashcode ^= getTypeClass().hashCode();
        }

        if (getSchemaType() != null) {
            hashcode ^= getSchemaType().hashCode();
        }

        return hashcode;
    }

    /**
     * @return Get the schema type.
     */
    public QName getSchemaType() {
        return schemaType;
    }

    /**
     * @param name The qName to set.
     */
    public void setSchemaType(QName name) {
        schemaType = name;
    }

    public boolean isWriteOuter() {
        return writeOuter;
    }

    public void setWriteOuter(boolean writeOuter) {
        this.writeOuter = writeOuter;
    }
    
    public boolean usesXmime() {
        return false;
    }
    
    /**
     * True if this type requires the import of the aegisTypes schema.
     * @return
     */
    public boolean usesUtilityTypes() {
        return false;
    }
    
    public boolean hasMinOccurs() {
        return false;
    }
    
    public boolean hasMaxOccurs() {
        return false;
    }
    
    public long getMinOccurs() {
        return 0; // not valid in general
    }
    
    public long getMaxOccurs() {
        return 0;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(getClass().getName());
        sb.append("[class=");
        Class c = getTypeClass();
        sb.append((c == null) ? "<null>" : c.getName());
        sb.append(",\nQName=");
        QName q = getSchemaType();
        sb.append((q == null) ? "<null>" : q.toString());
        sb.append("]");
        return sb.toString();
    }
}
