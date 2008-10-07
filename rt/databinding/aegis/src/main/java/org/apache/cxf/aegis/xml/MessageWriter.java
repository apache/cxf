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
package org.apache.cxf.aegis.xml;

import javax.xml.namespace.QName;

/**
 * Writes messages to an output stream.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public interface MessageWriter {
    void writeValue(Object value);

    void writeValueAsInt(Integer i);

    void writeValueAsCharacter(Character char1);

    void writeValueAsDouble(Double double1);

    void writeValueAsLong(Long l);

    void writeValueAsFloat(Float f);

    void writeValueAsShort(Short short1);
    
    void writeValueAsByte(Byte b);

    void writeValueAsBoolean(boolean b);

    MessageWriter getAttributeWriter(String name);

    MessageWriter getAttributeWriter(String name, String namespace);

    MessageWriter getAttributeWriter(QName qname);

    MessageWriter getElementWriter(String name);

    MessageWriter getElementWriter(String name, String namespace);

    MessageWriter getElementWriter(QName qname);

    String getPrefixForNamespace(String namespace);

    /**
     * Get a prefix for a namespace. After calling this, the prefix returned is
     * registered with the namespace. <p/> This method will make an attempt to
     * use the hint prefix if possible. If the namespace is already registered
     * or the hint is already registered with a different namespace then the
     * behavior will be the same as the non-hint version.
     * 
     * @param namespace the namespace to retrieve the prefix for
     * @param hint the hint for the prefix.
     * @return the prefix associated with the namespace
     */
    String getPrefixForNamespace(String namespace, String hint);

    /**
     * Tells the MessageWriter that writing operations are completed so it can
     * write the end element.
     */
    void close();

    /**
     * As per <a href="http://www.w3.org/TR/xmlschema-1/#xsi_type">2.6.1</a> in
     * XML Schema Part 1: "An element information item in an instance may,
     * however, explicitly assert its type using the attribute
     * <code>xsi:type</code>."
     * 
     * @param type the QName of the type being referenced.
     */
    void writeXsiType(QName qn);

    void writeXsiNil();
}
