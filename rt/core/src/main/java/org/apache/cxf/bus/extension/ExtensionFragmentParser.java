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

package org.apache.cxf.bus.extension;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.SAXException;

public class ExtensionFragmentParser {

    private static final String EXTENSION_ELEM_NAME = "extension";
    private static final String NAMESPACE_ELEM_NAME = "namespace";
    private static final String CLASS_ATTR_NAME = "class";
    private static final String INTERFACE_ATTR_NAME = "interface";
    private static final String DEFERRED_ATTR_NAME = "deferred";
    
    List<Extension> getExtensions(InputStream is) {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder parser = factory.newDocumentBuilder();
            document = parser.parse(is);
        } catch (ParserConfigurationException ex) {
            throw new ExtensionException(ex);
        } catch (SAXException ex) {
            throw new ExtensionException(ex);
        } catch (IOException ex) {
            throw new ExtensionException(ex);
        }
        
        return deserialiseExtensions(document);
    }
    
    
    List<Extension> deserialiseExtensions(Document document) {
        List<Extension> extensions = new ArrayList<Extension>();
        
        Element root = document.getDocumentElement();
        for (Node nd = root.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() 
                    && EXTENSION_ELEM_NAME.equals(nd.getLocalName())) {
                Extension e = new Extension();
                Element elem = (Element)nd;
                e.setClassname(elem.getAttribute(CLASS_ATTR_NAME));
                e.setInterfaceName(elem.getAttribute(INTERFACE_ATTR_NAME));
                String bval = elem.getAttribute(DEFERRED_ATTR_NAME).trim();
                e.setDeferred("1".equals(bval) || "true".equals(bval));
                
                deserialiseNamespaces(elem, e);
          
                extensions.add(e);
            }
        }
        return extensions;
    }
        
    void deserialiseNamespaces(Element extensionElem, Extension e) {
        for (Node nd  = extensionElem.getFirstChild(); nd != null; nd = nd.getNextSibling()) {
            if (Node.ELEMENT_NODE == nd.getNodeType() && NAMESPACE_ELEM_NAME.equals(nd.getLocalName())) {
                e.getNamespaces().add(nd.getTextContent());
            }
        }
    }   
}
