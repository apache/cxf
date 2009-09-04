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
 * Abstraction for the map between Java types (represented as 
 * {@link java.lang.reflect.Type} and Aegis types.
 */
public interface TypeMapping {
    /**
     * Returns a flag indicating if this type mapping has a mapping for a particular Java class.
     * @param javaType the class.
     * @return <code>true</code> if there is a mapping for the type.
     */
    boolean isRegistered(Type javaType);

    /**
     * Returns a flag indicating if this type mapping has a mapping for a particular
     * XML Schema QName.
     * @param xmlType the QName.
     * @return <code>true</code> if there is a mapping for the type.
     */
    boolean isRegistered(QName xmlType);

    /**
     * Register a type, manually specifying the java class, the schema type,
     * and the Aegis type object that provides serialization, deserialization,
     * and schema.
     * @param javaType Java class.
     * @param xmlType XML Schema type QName.
     * @param type Aegis type object.
     */
    void register(Type javaType, QName xmlType, AegisType type);

    /**
     * Register a type that self-describes the schema type and the Java class.
     * @param type Aegis type object that 
     */
    void register(AegisType type);

    void removeType(AegisType type);

    AegisType getType(Type javaType);

    AegisType getType(QName xmlType);

    QName getTypeQName(Type clazz);

    TypeCreator getTypeCreator();
    
    /**
     * Each mapping has a URI that identifies it.
     * The mapping for a service uses the service URI.
     * XML files can choose to only contribute mappings that match.
     * @return the URI.
     */
    String getMappingIdentifierURI();
    /**
     * This exists only to deal with an initialization order problem.
     * @param uri
     */
    void setMappingIdentifierURI(String uri);
}
