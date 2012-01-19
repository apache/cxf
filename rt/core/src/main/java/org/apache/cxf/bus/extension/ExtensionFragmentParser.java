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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.staxutils.StaxUtils;

public class ExtensionFragmentParser {

    private static final String EXTENSION_ELEM_NAME = "extension";
    private static final String NAMESPACE_ELEM_NAME = "namespace";
    private static final String CLASS_ATTR_NAME = "class";
    private static final String INTERFACE_ATTR_NAME = "interface";
    private static final String DEFERRED_ATTR_NAME = "deferred";
    
    public List<Extension> getExtensionsFromXML(InputStream is) {
        Document document = null;
        try {
            document = StaxUtils.read(is);
        } catch (XMLStreamException ex) {
            throw new ExtensionException(ex);
        }
        
        return deserialiseExtensions(document);
    }
    
    /**
     * Reads extension definitions from a Text file and instantiates them
     * The text file has the following syntax
     * classname:interfacename:deferred(true|false)
     * 
     * @param is stream to read the extension from
     * @return list of Extensions
     * @throws IOException
     */
    public List<Extension> getExtensionsFromText(InputStream is) throws IOException {
        List<Extension> extensions = new ArrayList<Extension>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line = reader.readLine();
        while (line != null) {
            final Extension extension = getExtensionFromTextLine(line);
            if (extension != null) {
                extensions.add(extension);
            }
            line = reader.readLine();
        }
        return extensions;
    }

    private Extension getExtensionFromTextLine(String line) {
        line = line.trim();
        if (line.length() == 0 || line.charAt(0) == '#') {
            return null;
        }
        final Extension ext = new Extension();
        String[] parts = line.split(":");
        ext.setClassname(parts[0]);
        if (ext.getClassname() == null) {
            return null;
        }
        if (parts.length >= 2) {
            String interfaceName = parts[1];
            if (interfaceName != null && "".equals(interfaceName)) {
                interfaceName = null;
            }
            ext.setInterfaceName(interfaceName);
        }
        if (parts.length >= 3) {
            ext.setDeferred(Boolean.parseBoolean(parts[2]));
        }
        return ext;
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
