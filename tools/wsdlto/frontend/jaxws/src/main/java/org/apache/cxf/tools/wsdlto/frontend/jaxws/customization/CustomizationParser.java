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
package org.apache.cxf.tools.wsdlto.frontend.jaxws.customization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.URIParserUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.ProcessorUtil;

public final class CustomizationParser {
    // For WSDL1.1
    private static final Logger LOG = LogUtils.getL7dLogger(CustomizationParser.class);

    private ToolContext env;
    // map for jaxws binding and wsdl element
    private final Map<Element, Element> jaxwsBindingsMap = new HashMap<Element, Element>();
    private final List<InputSource> jaxbBindings = new ArrayList<InputSource>();
    private final Map<String, Element> customizedElements = new HashMap<String, Element>();

    private Element handlerChains;
    private Element wsdlNode;
    private String wsdlURL;

    private CustomNodeSelector nodeSelector = new CustomNodeSelector();

    public CustomizationParser() {
        jaxwsBindingsMap.clear();
        jaxbBindings.clear();
    }

    public Element getHandlerChains() {
        return this.handlerChains;
    }

    public void parse(ToolContext pe) {
        this.env = pe;
        String[] bindingFiles;
        try {
            wsdlURL = URIParserUtil.getAbsoluteURI((String)env.get(ToolConstants.CFG_WSDLURL));

            wsdlNode = getTargetNode(this.wsdlURL);

            if (wsdlNode == null && env.get(ToolConstants.CFG_CATALOG) != null) {
                wsdlNode = resolveNodeByCatalog(wsdlURL);
            }
            
            if (wsdlNode == null) {
                throw new ToolException(new Message("MISSING_WSDL", LOG, wsdlURL));
            }
            customizedElements.put(wsdlURL.toString(), wsdlNode);
            bindingFiles = (String[])env.get(ToolConstants.CFG_BINDING);
            if (bindingFiles == null) {
                return;
            }
        } catch (ClassCastException e) {
            bindingFiles = new String[1];
            bindingFiles[0] = (String)env.get(ToolConstants.CFG_BINDING);
        }

        for (int i = 0; i < bindingFiles.length; i++) {
            try {
                addBinding(bindingFiles[i]);
            } catch (XMLStreamException xse) {
                Message msg = new Message("STAX_PARSER_ERROR", LOG);
                throw new ToolException(msg, xse);
            }
        }

        for (Element element : jaxwsBindingsMap.keySet()) {
            nodeSelector.addNamespaces(element);
            Element oldTargetNode = jaxwsBindingsMap.get(element);
            Element targetNode = oldTargetNode;
            internalizeBinding(element, targetNode, "");
            String uri = element.getAttribute("wsdlLocation");
            customizedElements.put(uri, targetNode);
            updateJaxwsBindingMapValue(targetNode);  
        }
        
        buildHandlerChains();
    }

    public Element getTargetNode(String uri) {
        if (uri.equals(wsdlURL) && wsdlNode != null) {
            return wsdlNode;
        }
        Document doc = null;
        InputStream ins = null;

        try {
            URIResolver resolver = new URIResolver(uri);
            ins = resolver.getInputStream();
        } catch (IOException e1) {
            return null;
        }

        if (ins == null) {
            return null;
        }
        
        try {
            doc = DOMUtils.readXml(ins);
            doc.setDocumentURI(uri);
        } catch (Exception e) {
            Message msg = new Message("CAN_NOT_READ_AS_ELEMENT", LOG, new Object[] {uri});
            throw new ToolException(msg, e);
        }

        if (doc != null) {
            return doc.getDocumentElement();
        }
        return null;
    }

    private void updateJaxwsBindingMapValue(Element value) {
        String baseURI = value.getBaseURI();        
        for (Element ele : jaxwsBindingsMap.keySet()) {
            String uri = jaxwsBindingsMap.get(ele).getBaseURI();
            if (uri != null && uri.equals(baseURI)) {
                jaxwsBindingsMap.put(ele, value);
            }
        }      
    }
    
    private void buildHandlerChains() {

        for (Element jaxwsBinding : jaxwsBindingsMap.keySet()) {            
            List<Element> elemList = 
                DOMUtils.findAllElementsByTagNameNS(jaxwsBinding, 
                                                    ToolConstants.HANDLER_CHAINS_URI, 
                                                    ToolConstants.HANDLER_CHAINS);         
            if (elemList.size() == 0) {
                continue;
            }         
            // take the first one, anyway its 1 handler-config per customization
            this.handlerChains = elemList.get(0);
 
            return;
        }

    }

    private Node[] getAnnotationNodes(final Node node) {
        Node[] nodes = new Node[2];

        Node annotationNode = node.getFirstChild();
        while (annotationNode != null) {
            if ("annotation".equals(annotationNode.getLocalName())
                && ToolConstants.SCHEMA_URI.equals(annotationNode.getNamespaceURI())) {
                break;
            }
            annotationNode = annotationNode.getNextSibling();
        }
        if (annotationNode == null) {
            annotationNode = node.getOwnerDocument().createElementNS(ToolConstants.SCHEMA_URI, "annotation");
        }

        nodes[0] = annotationNode;

        Node appinfoNode = annotationNode.getFirstChild();
        while (appinfoNode != null) {
            if ("appinfo".equals(appinfoNode.getLocalName())
                && ToolConstants.SCHEMA_URI.equals(appinfoNode.getNamespaceURI())) {
                break;
            }
            appinfoNode = appinfoNode.getNextSibling();
        }

        if (appinfoNode == null) {
            appinfoNode = node.getOwnerDocument().createElementNS(ToolConstants.SCHEMA_URI, "appinfo");
            annotationNode.appendChild(appinfoNode);
        }
        nodes[1] = appinfoNode;
        return nodes;
    }

    private void appendJaxbVersion(final Element schemaElement) {
        String jaxbPrefix = schemaElement.lookupPrefix(ToolConstants.NS_JAXB_BINDINGS);
        if (jaxbPrefix == null) {
            schemaElement.setAttribute("xmlns:jaxb", ToolConstants.NS_JAXB_BINDINGS);
            schemaElement.setAttribute("jaxb:version", "2.0");
        }
    }

    protected void copyAllJaxbDeclarations(final Node schemaNode, final Element jaxwsBindingNode) {
        appendJaxbVersion((Element)schemaNode);

        Node[] embededNodes = getAnnotationNodes(schemaNode);
        Node annotationNode = embededNodes[0];
        Node appinfoNode = embededNodes[1];
        
        for (Node childNode = jaxwsBindingNode.getFirstChild();
            childNode != null;
            childNode = childNode.getNextSibling()) {
            
            copyJaxbAttributes(childNode, (Element)schemaNode);
            
            if (!isJaxbBindings(childNode)) {
                continue;
            }
            
            Element childEl = (Element)childNode;
            if (isJaxbBindingsElement(childEl)) {
                
                NodeList nlist = nodeSelector.queryNodes(schemaNode, childEl.getAttribute("node"));
                for (int i = 0; i < nlist.getLength(); i++) {
                    Node node = nlist.item(i);
                    copyAllJaxbDeclarations(node, childEl);
                }              
                
            } else {
                Element cloneNode = (Element)ProcessorUtil.cloneNode(schemaNode.getOwnerDocument(), 
                                                                     childEl, true);
                
                NamedNodeMap atts = cloneNode.getAttributes();
                for (int x = 0; x < atts.getLength(); x++) {
                    Attr attr = (Attr)atts.item(x);
                    if (ToolConstants.NS_JAXB_BINDINGS.equals(attr.getNamespaceURI())) {
                        cloneNode.removeAttributeNode(attr);
                        atts = cloneNode.getAttributes();
                        x = -1;
                    }
                }
                appinfoNode.appendChild(cloneNode);
                childNode = childNode.getNextSibling();
            }
        }
        
        if (schemaNode.getFirstChild() != null) {
            schemaNode.insertBefore(annotationNode, schemaNode.getFirstChild());
        } else {
            schemaNode.appendChild(annotationNode);
        }
    }

    private void copyJaxbAttributes(Node childNode, Element schemaNode) {
        if (childNode instanceof Element) {
            Element el = (Element)childNode;
            if (el.getParentNode() != null) {
                copyJaxbAttributes(el.getParentNode(), schemaNode);
            }
            NamedNodeMap atts = el.getAttributes();
            for (int x = 0; x < atts.getLength(); x++) {
                Attr attr = (Attr)atts.item(x);
                if (ToolConstants.NS_JAXB_BINDINGS.equals(attr.getNamespaceURI())) { 
                    Attr attrnew = schemaNode.getOwnerDocument().createAttributeNS(attr.getNamespaceURI(), 
                                                                                attr.getName());
                    attrnew.setValue(attr.getValue());
                    schemaNode.setAttributeNodeNS(attrnew);
                    
                    if ("extensionBindingPrefixes".equals(attr.getLocalName())) {
                        String pfxs = attr.getValue();
                        while (pfxs.length() > 0) {
                            String pfx = pfxs;
                            int idx = pfx.indexOf(',');
                            if (idx != -1) {
                                pfxs = pfxs.substring(idx + 1);
                                pfx = pfx.substring(0, idx);
                            } else {
                                pfxs = "";
                            }
                            String ns = el.lookupNamespaceURI(pfx);
                            schemaNode.setAttribute("xmlns:" + pfx,
                                                    ns);
                        }
                    }
                }
            }
        }
        
    }

    protected void internalizeBinding(Element bindings, Element targetNode, String expression) {
        if (bindings.getAttributeNode("wsdlLocation") != null) {
            expression = "/";
        }

        if (isGlobaleBindings(bindings)) {
            nodeSelector.addNamespaces(wsdlNode);
            if (targetNode != wsdlNode) {
                nodeSelector.addNamespaces(targetNode);
            }
            
            copyBindingsToWsdl(targetNode, bindings, nodeSelector.getNamespaceContext());
        }

        if (isJAXWSBindings(bindings) && bindings.getAttributeNode("node") != null) {
            expression = expression + "/" + bindings.getAttribute("node");

            nodeSelector.addNamespaces(bindings);

            NodeList nodeList = nodeSelector.queryNodes(targetNode, expression);
            if (nodeList == null || nodeList.getLength() == 0) {
                throw new ToolException(new Message("NODE_NOT_EXISTS", LOG, new Object[] {expression}));
            }

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (hasJaxbBindingDeclaration(bindings)) {
                    copyAllJaxbDeclarations(node, bindings);
                } else {
                    copyBindingsToWsdl(node, bindings, nodeSelector.getNamespaceContext());
                }
            }
            
           
        }

        Element[] children = getChildElements(bindings, ToolConstants.NS_JAXWS_BINDINGS);
        for (Element child : children) {
            internalizeBinding(child, targetNode, expression);
        }
    }

    private void copyBindingsToWsdl(Node node, Node bindings, MapNamespaceContext ctx) {
        if (bindings.getNamespaceURI().equals(ToolConstants.JAXWS_BINDINGS.getNamespaceURI())) {
            bindings.setPrefix("jaxws");
        }

        for (Map.Entry<String, String> ent : ctx.getUsedNamespaces().entrySet()) {
            if (node.lookupNamespaceURI(ent.getKey()) == null) {
                node.getOwnerDocument().getDocumentElement().setAttribute("xmlns:" + ent.getKey(),
                                                                          ent.getValue());
            }

        }

        
        
        Element element = DOMUtils.getFirstElement(bindings);
        while (element != null) {
            if (element.getNamespaceURI().equals(ToolConstants.JAXWS_BINDINGS.getNamespaceURI())) {
                element.setPrefix("jaxws");
            }
            element = DOMUtils.getNextElement(element);
        }
        
        

        Node cloneNode = ProcessorUtil.cloneNode(node.getOwnerDocument(), bindings, true);
        Node firstChild = DOMUtils.getChild(node, "jaxws:bindings");
        if (firstChild == null && cloneNode.getNodeName().indexOf("bindings") == -1) {
            wsdlNode.setAttribute("xmlns:jaxws", ToolConstants.JAXWS_BINDINGS.getNamespaceURI());
            Element jaxwsBindingElement = node.getOwnerDocument().createElement("jaxws:bindings");
            node.appendChild(jaxwsBindingElement);
            firstChild = jaxwsBindingElement;
        }

        if (firstChild == null && cloneNode.getNodeName().indexOf("bindings") > -1) {
            firstChild = node;
            if (wsdlNode.getAttributeNode("xmls:jaxws") == null) {
                wsdlNode.setAttribute("xmlns:jaxws", ToolConstants.JAXWS_BINDINGS.getNamespaceURI());
            }
        }

        Element cloneEle = (Element)cloneNode;
        cloneEle.removeAttribute("node");
        
        Element elem = DOMUtils.getFirstElement(cloneNode);
        while (elem != null) {
            Node attrNode = elem.getAttributeNode("node");
            if (attrNode != null) {
                cloneNode.removeChild(elem);
            }        
            elem = DOMUtils.getNextElement(elem);       
        }

        firstChild.appendChild(cloneNode);
    }

    private boolean isGlobaleBindings(Element binding) {

        boolean globleNode = binding.getNamespaceURI().equals(ToolConstants.NS_JAXWS_BINDINGS)
                             && binding.getLocalName().equals("package")
                             || binding.getLocalName().equals("enableAsyncMapping")
                             || binding.getLocalName().equals("enableAdditionalSOAPHeaderMapping")
                             || binding.getLocalName().equals("enableWrapperStyle")
                             || binding.getLocalName().equals("enableMIMEContent");
        Node parentNode = binding.getParentNode();
        if (parentNode instanceof Element) {
            Element ele = (Element)parentNode;
            if (ele.getAttributeNode("wsdlLocation") != null && globleNode) {
                return true;
            }

        }
        return false;

    }

    private Element[] getChildElements(Element parent, String nsUri) {
        List<Element> a = new ArrayList<Element>();
        for (Node item = parent.getFirstChild(); item != null; item = item.getNextSibling()) {
            if (!(item instanceof Element)) {
                continue;
            }
            if (nsUri.equals(item.getNamespaceURI())) {
                a.add((Element)item);
            }
        }
        return (Element[])a.toArray(new Element[a.size()]);
    }

    private void addBinding(String bindingFile) throws XMLStreamException {

        Element root = null;
        try {
            URIResolver resolver = new URIResolver(bindingFile);
            root = DOMUtils.readXml(resolver.getInputStream()).getDocumentElement();
        } catch (Exception e1) {
            Message msg = new Message("CAN_NOT_READ_AS_ELEMENT", LOG, new Object[] {bindingFile});
            throw new ToolException(msg, e1);
        }
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(root);
        StaxUtils.toNextTag(reader);
        if (isValidJaxwsBindingFile(bindingFile, reader)) {

            String wsdlLocation = root.getAttribute("wsdlLocation");
            Element targetNode = null;
            if (!StringUtils.isEmpty(wsdlLocation)) {
                String wsdlURI = getAbsoluteURI(wsdlLocation, bindingFile);
                targetNode = getTargetNode(wsdlURI);
                String resolvedLoc = wsdlURI;
                if (targetNode == null && env.get(ToolConstants.CFG_CATALOG) != null) {
                    resolvedLoc = resolveByCatalog(wsdlURI.toString());
                    targetNode = getTargetNode(resolvedLoc);
                }
                if (targetNode == null) {
                    Message msg = new Message("POINT_TO_WSDL_DOES_NOT_EXIST", 
                                              LOG, new Object[] {bindingFile, resolvedLoc});
                    throw new ToolException(msg);
                }

                root.setAttribute("wsdlLocation", wsdlURI);
            } else {
                targetNode = wsdlNode;

                root.setAttribute("wsdlLocation", wsdlURL);
            }
            jaxwsBindingsMap.put(root, targetNode);

        } else if (isValidJaxbBindingFile(reader)) {
            String schemaLocation = root.getAttribute("schemaLocation");
            String resolvedSchemaLocation = resolveByCatalog(schemaLocation);
            if (resolvedSchemaLocation != null) {
                InputSource tmpIns = null;
                try {
                    tmpIns = convertToTmpInputSource(root, resolvedSchemaLocation);
                } catch (Exception e1) {
                    Message msg = new Message("FAILED_TO_ADD_SCHEMALOCATION", LOG, bindingFile);
                    throw new ToolException(msg, e1);
                }
                jaxbBindings.add(tmpIns);
            } else {
                jaxbBindings.add(new InputSource(bindingFile));
            } 
        }
    }

    private String getAbsoluteURI(String  uri, String bindingFile) {
        URI locURI = null;
        try {
            locURI = new URI(uri);
        } catch (URISyntaxException e) {
            Message msg = new Message("BINDING_LOC_ERROR", 
                                      LOG, new Object[] {uri});
            throw new ToolException(msg);
        }

        if (!locURI.isAbsolute()) {
            try {
                String base = URIParserUtil.getAbsoluteURI(bindingFile);
                URI baseURI = new URI(base);
                locURI = baseURI.resolve(locURI);
            } catch (URISyntaxException e) {
                Message msg = new Message("NOT_URI", LOG, new Object[] {bindingFile});
                throw new ToolException(msg, e);
            }

        }
        return locURI.toString();
    }
    
    private Element resolveNodeByCatalog(String url) {
        String resolvedLocation = resolveByCatalog(url);
        return getTargetNode(resolvedLocation);
    }

    private String resolveByCatalog(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        Bus bus = (Bus)env.get(Bus.class);
        OASISCatalogManager catalogResolver = OASISCatalogManager.getCatalogManager(bus);
        if (catalogResolver == null) {
            return null;
        }
        String resolvedLocation;
        try {
            resolvedLocation = catalogResolver.resolveSystem(url);
            if (resolvedLocation == null) {
                resolvedLocation = catalogResolver.resolveURI(url);
            }
        } catch (Exception e1) {
            Message msg = new Message("FAILED_RESOLVE_CATALOG", LOG, url);
            throw new ToolException(msg, e1);
        }
        return resolvedLocation;
    }

    private InputSource convertToTmpInputSource(Element ele, String schemaLoc) throws Exception {
        InputSource result = null;
        ele.setAttribute("schemaLocation", schemaLoc);
        File tmpFile = FileUtils.createTempFile("jaxbbinding", ".xml");
        XMLUtils.writeTo(ele, new FileOutputStream(tmpFile));
        result = new InputSource(URIParserUtil.getAbsoluteURI(tmpFile.getAbsolutePath()));
        tmpFile.deleteOnExit();
        return result;
    }

    private boolean isValidJaxbBindingFile(XMLStreamReader reader) {
        if (ToolConstants.JAXB_BINDINGS.equals(reader.getName())) {

            return true;
        }
        return false;
    }

    private boolean isValidJaxwsBindingFile(String bindingLocation, XMLStreamReader reader) {
        if (ToolConstants.JAXWS_BINDINGS.equals(reader.getName())) {
            // Comment this check , by default wsdlLocation value will be the
            // user input wsdl url
            /*
             * String wsdlLocation = reader.getAttributeValue(null,
             * "wsdlLocation"); if (!StringUtils.isEmpty(wsdlLocation)) { return
             * true; }
             */
            return true;
        }
        return false;

    }

    protected void setWSDLNode(final Element node) {
        this.wsdlNode = node;
    }

    public Node getWSDLNode() {
        return this.wsdlNode;
    }

    private boolean isJAXWSBindings(Node bindings) {
        return ToolConstants.NS_JAXWS_BINDINGS.equals(bindings.getNamespaceURI())
               && "bindings".equals(bindings.getLocalName());
    }

    private boolean isJaxbBindings(Node bindings) {
        return ToolConstants.NS_JAXB_BINDINGS.equals(bindings.getNamespaceURI());
    }

    private boolean isJaxbBindingsElement(Node bindings) {
        return "bindings".equals(bindings.getLocalName());
    }

    protected boolean hasJaxbBindingDeclaration(Node bindings) {
        for (Node childNode = bindings.getFirstChild();
            childNode != null;
            childNode = childNode.getNextSibling()) {
            if (isJaxbBindings(childNode)) {
                return true;
            }
        }
        return false;
    }

    public Map<String, Element> getCustomizedWSDLElements() {
        return this.customizedElements;
    }

    public List<InputSource> getJaxbBindings() {
        return this.jaxbBindings;
    }

    public static JAXWSBinding mergeJawsBinding(JAXWSBinding binding1, JAXWSBinding binding2) {
        if (binding1 != null && binding2 != null) {
            if (binding2.isEnableAsyncMapping()) {
                binding1.setEnableAsyncMapping(true);
            }
            if (binding2.isEnableWrapperStyle()) {
                binding1.setEnableWrapperStyle(true);
            }
            if (binding2.isEnableMime()) {
                binding1.setEnableMime(true);
            }

            if (binding2.getJaxwsClass() != null) {
                binding1.setJaxwsClass(binding2.getJaxwsClass());
            }

            if (binding2.getJaxwsPara() != null) {
                binding1.setJaxwsPara(binding2.getJaxwsPara());
            }
            return binding1;
        }

        return binding1 == null ? binding2 : binding1;
    }

}
