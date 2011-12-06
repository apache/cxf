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

package org.apache.cxf.tools.corba.common;

import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.xml.WSDLWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;


 /*
  * This class is extending the wsdl4j RI class to print out the 
  * extensibility elements of the schema into a separate file.
  * 
  */
public class SchemaWriterImpl implements WSDLWriter {

    public static final int DEFAULT_INDENT_LEVEL = 0;

    
    public Element getElement(Definition wsdlDef) throws WSDLException {
        Types types = wsdlDef.getTypes();          
        if (types != null) {
            List<ExtensibilityElement> l = CastUtils.cast(types.getExtensibilityElements());
            if (l == null) {
                return null;
            }

            for (ExtensibilityElement e : l) {
                if (e instanceof Schema) {
                    Schema sc = (Schema)e;
                    return sc.getElement();
                }
            }
        }
        return null;
    }
    public Document getDocument(Definition wsdlDef) throws WSDLException {
        Element el = getElement(wsdlDef);
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.importNode(el, true));
        return doc;
    }



    public void setFeature(String name, boolean value) throws IllegalArgumentException {
    }

    public boolean getFeature(String name) throws IllegalArgumentException {
        return false;
    }

    public void writeWSDL(Definition wsdlDef, Writer sink) throws WSDLException {
        XMLUtils.writeTo(getDocument(wsdlDef), sink, 2);
    }    
    public void writeWSDL(Definition wsdlDef, OutputStream sink) throws WSDLException {
        XMLUtils.writeTo(getDocument(wsdlDef), sink, 2);
    }   
}
