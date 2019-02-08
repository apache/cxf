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

package org.apache.cxf.wsdl11;

import java.io.PrintWriter;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.ExtensionSerializer;
import javax.wsdl.extensions.schema.Schema;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Node;

import org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * A custom Schema serializer because WSDL4J's is buggy.
 */
public class SchemaSerializer implements ExtensionSerializer {

    public void marshall(@SuppressWarnings("rawtypes") Class parentType,
                         QName elementType, ExtensibilityElement extension, PrintWriter pw,
                         Definition def, ExtensionRegistry extReg) throws WSDLException {
        try {
            writeXml(((Schema)extension).getElement(), pw);
        } catch (XMLStreamException e) {
            throw new WSDLException("", "Could not write schema.", e);
        }
    }

    private void writeXml(Node n, PrintWriter pw) throws XMLStreamException {
        XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(pw);
        writer = new PrettyPrintXMLStreamWriter(writer, 2);
        StaxUtils.copy(new DOMSource(n), writer);
        writer.close();
    }
}
