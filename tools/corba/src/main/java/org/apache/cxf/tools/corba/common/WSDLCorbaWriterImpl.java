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
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;

 /*
  * This class is extending the wsdl4j RI class to print out the 
  * extensibility elements at the top of a generated wsdl file.
  * 
  */
public class WSDLCorbaWriterImpl implements WSDLWriter {

    public static final int DEFAULT_INDENT_LEVEL = 2;
    
    final WSDLWriter wrapped;
    
    public WSDLCorbaWriterImpl(WSDLWriter orig) {
        wrapped = orig;
    }


    public void setFeature(String name, boolean value) throws IllegalArgumentException {
        wrapped.setFeature(name, value);
    }


    public boolean getFeature(String name) throws IllegalArgumentException {
        return wrapped.getFeature(name);
    }


    public Document getDocument(Definition wsdlDef) throws WSDLException {
        try {
            fixTypes(wsdlDef);
        } catch (Exception ex) {
            throw new WSDLException(WSDLException.PARSER_ERROR, ex.getMessage(), ex);
        }
        Document doc = wrapped.getDocument(wsdlDef);
        Element imp = null;
        Element child = DOMUtils.getFirstElement(doc.getDocumentElement());
        //move extensability things to the top
        while (child != null) {
            if (child.getNamespaceURI().equals(doc.getDocumentElement().getNamespaceURI())) {
                //wsdl node
                if (imp == null) {
                    imp = child;
                }
            } else if (imp != null) {
                doc.getDocumentElement().removeChild(child);
                doc.getDocumentElement().insertBefore(child, imp);
            }
            child = DOMUtils.getNextElement(child);
        }
        
        return doc;
    }


    private void fixTypes(Definition wsdlDef) throws ParserConfigurationException {
        Types t = wsdlDef.getTypes();
        if (t == null) {
            return;
        }
        List<ExtensibilityElement> l = CastUtils.cast(t.getExtensibilityElements());
        if (l == null) {
            return;
        }
        
        for (ExtensibilityElement e : l) {
            if (e instanceof Schema) {
                Schema sc = (Schema)e;
                String pfx = wsdlDef.getPrefix(sc.getElementType().getNamespaceURI());
                if (StringUtils.isEmpty(pfx)) {
                    pfx = "xsd";
                    String ns = wsdlDef.getNamespace(pfx);
                    int count = 1;
                    while (!StringUtils.isEmpty(ns)) {
                        pfx = "xsd" + count++;
                        ns = wsdlDef.getNamespace(pfx);
                    }
                    wsdlDef.addNamespace(pfx, sc.getElementType().getNamespaceURI());
                }
                if (sc.getElement() == null) {
                    fixSchema(sc, pfx);
                }
            }
        }        
    }


    private void fixSchema(Schema sc, String pfx) throws ParserConfigurationException {
        Document doc = XMLUtils.newDocument();
        Element el = doc.createElementNS(sc.getElementType().getNamespaceURI(),
                            pfx + ":" + sc.getElementType().getLocalPart());
        sc.setElement(el);
        Map<String, List<String>> mp = CastUtils.cast(sc.getImports());
        for (Map.Entry<String, List<String>> ent : mp.entrySet()) {
            Element imp = doc.createElementNS(sc.getElementType().getNamespaceURI(),
                                              pfx + ":import");
            el.appendChild(imp);
            imp.setAttribute("namespace", ent.getKey());
        }
    }


    public void writeWSDL(Definition wsdlDef, Writer sink) throws WSDLException {
        XMLUtils.writeTo(getDocument(wsdlDef), sink, 2);
    }    
    public void writeWSDL(Definition wsdlDef, OutputStream sink) throws WSDLException {
        XMLUtils.writeTo(getDocument(wsdlDef), sink, 2);
    }        
    
}
