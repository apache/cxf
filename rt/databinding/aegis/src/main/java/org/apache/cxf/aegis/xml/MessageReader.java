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
import javax.xml.stream.XMLStreamReader;

/**
 * A MessageReader. You must call getNextChildReader() until
 * hasMoreChildReaders() returns false.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public interface MessageReader {
    
    String getValue();

    boolean isXsiNil();

    int getValueAsInt();

    long getValueAsLong();

    double getValueAsDouble();

    float getValueAsFloat();

    boolean getValueAsBoolean();

    char getValueAsCharacter();

    MessageReader getAttributeReader(QName qName);

    boolean hasMoreAttributeReaders();

    MessageReader getNextAttributeReader();

    boolean hasMoreElementReaders();

    MessageReader getNextElementReader();

    QName getName();

    /**
     * Get the local name of the element this reader represents.
     * 
     * @return Local Name
     */
    String getLocalName();

    /**
     * @return Namespace
     */
    String getNamespace();

    String getNamespaceForPrefix(String prefix);

    XMLStreamReader getXMLStreamReader();

    void readToEnd();
}
