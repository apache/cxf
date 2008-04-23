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

import javax.xml.namespace.QName;

/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @since Feb 18, 2004
 */
public interface TypeMapping {
    /**
     * Checks whether or not type mapping between specified XML type and Java
     * type is registered.
     * 
     * @param javaType Class of the Java type
     * @param xmlType Qualified name of the XML data type
     * @return boolean; <code>true</code> if type mapping between the
     *         specified XML type and Java type is registered; otherwise
     *         <code>false</code>
     */
    boolean isRegistered(Class javaType);

    boolean isRegistered(QName xmlType);

    void register(Class javaType, QName xmlType, Type type);

    void register(Type type);

    void removeType(Type type);

    Type getType(Class javaType);

    Type getType(QName xmlType);

    QName getTypeQName(Class clazz);

    TypeCreator getTypeCreator();
    
    /**
     * Each mapping has a URI that identifies it.
     * The mapping for a service uses the service URI.
     * XML files can choose to only contribute mappings that match.
     * @return the URI.
     */
    String getMappingIdentifierURI();
}
