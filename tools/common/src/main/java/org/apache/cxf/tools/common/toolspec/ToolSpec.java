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

package org.apache.cxf.tools.common.toolspec;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.dom.ExtendedDocumentBuilder;

public class ToolSpec {

    private static final Logger LOG = LogUtils.getL7dLogger(ToolSpec.class);

    private final ExtendedDocumentBuilder builder = new ExtendedDocumentBuilder();
    private Document doc;
    private Tool handler;

    public ToolSpec() {
    }

    public ToolSpec(InputStream in) throws ToolException {
        this(in, true);
    }

    public ToolSpec(InputStream in, boolean validate) throws ToolException {
        if (in == null) {
            throw new NullPointerException("Cannot create a ToolSpec object from a null stream");
        }
        try {
            builder.setValidating(validate);
            this.doc = builder.parse(in);
        } catch (Exception ex) {
            Message message = new Message("FAIL_TO_PARSING_TOOLSPCE_STREAM", LOG);
            throw new ToolException(message, ex);
        }
    }

    public ToolSpec(Document d) {
        if (d == null) {
            throw new NullPointerException("Cannot create a ToolSpec object from "
                                           + "a null org.w3c.dom.Document");
        }
        this.doc = d;
    }

    public ExtendedDocumentBuilder getDocumentBuilder() {
        return builder;
    }

    public boolean isValidInputStream(String id) {
        Element streams = getStreams();

        if (streams == null) {
            return false;
        }
        
        List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(streams, 
                                                                     Tool.TOOL_SPEC_PUBLIC_ID, 
                                                                     "instream");
        for (Element elem : elemList) {
            if (elem.getAttribute("id").equals(id)) {
                return true;
            }
        }
        return false;
    }

    public Element getElementById(String id) {
        Element ele = doc.getElementById(id);
        if (ele != null) {
            return ele;
        }

        XPathUtils xpather = new XPathUtils(new HashMap<String, String>());
        NodeList nl = (NodeList) xpather.getValue("//*[@id='" + id + "']",
                                                  doc,
                                                  XPathConstants.NODESET);
        if (nl != null && nl.getLength() > 0) {
            return (Element)nl.item(0);
        }
        return null;
    }

    public boolean hasHandler() {
        return doc.getDocumentElement().hasAttribute("handler");
    }

    public Tool getHandler() throws ToolException {
        if (!hasHandler()) {
            return null;
        }

        if (handler == null) {
            String handlerClz = doc.getDocumentElement().getAttribute("handler");

            try {
                handler = (Tool)Class.forName(handlerClz).newInstance();
            } catch (Exception ex) {
                Message message = new Message("FAIL_TO_INSTANTIATE_HANDLER", LOG, handlerClz);
                throw new ToolException(message, ex);
            }
        }
        return handler;
    }

    public Tool getHandler(ClassLoader loader) throws ToolException {
        if (!hasHandler()) {
            return null;
        }

        if (handler == null) {
            String handlerClz = doc.getDocumentElement().getAttribute("handler");

            try {
                handler = (Tool)Class.forName(handlerClz, true, loader).newInstance();
            } catch (Exception ex) {
                Message message = new Message("FAIL_TO_INSTANTIATE_HANDLER", LOG, handlerClz);
                throw new ToolException(message, ex);
            }
        }
        return handler;
    }

    public Element getStreams() {        
        List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(), 
                                                                     Tool.TOOL_SPEC_PUBLIC_ID, 
                                                                     "streams");
        if (elemList.size() > 0) {
            return elemList.get(0);
        } else {
            return null;
        }
    }

    public List getInstreamIds() {
        List<Object> res = new ArrayList<Object>();
        Element streams = getStreams();

        if (streams != null) {
            List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(streams, 
                                                                         Tool.TOOL_SPEC_PUBLIC_ID, 
                                                                         "instream");
            for (Element elem : elemList) {
                res.add(elem.getAttribute("id"));
            }
        }
        return Collections.unmodifiableList(res);
    }

    public List getOutstreamIds() {
        List<Object> res = new ArrayList<Object>();
        Element streams = getStreams();

        if (streams != null) {
            List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(streams, 
                                                                         Tool.TOOL_SPEC_PUBLIC_ID, 
                                                                         "outstream");

            for (Element elem : elemList) {
                res.add(elem.getAttribute("id"));
            }
        }
        return Collections.unmodifiableList(res);
    }

    public Element getUsage() {
        return DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(), 
                                            Tool.TOOL_SPEC_PUBLIC_ID, 
                                            "usage").get(0);
    }

    public void transform(InputStream stylesheet, OutputStream out) throws TransformerException {
        Transformer trans = TransformerFactory.newInstance().newTransformer(new StreamSource(stylesheet));
        trans.transform(new DOMSource(doc), new StreamResult(out));
    }

    public Element getPipeline() {
        
        List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(doc.getDocumentElement(), 
                                            Tool.TOOL_SPEC_PUBLIC_ID, 
                                            "pipeline");
        if (elemList.size() > 0) {
            return elemList.get(0);
        } else {
            return null;
        }
    }

    public List<Element> getUsageForms() {  
        return DOMUtils.findAllElementsByTagNameNS(getUsage(), Tool.TOOL_SPEC_PUBLIC_ID, "form");   
    }

    /**
     * Arguments can have streamref attributes which associate them with a
     * stream. Tools usually request streams and rely on them being ready. If an
     * argument is given a streamref, then the container constructs a stream
     * from the argument value. This would usually be a simple FileInputStream
     * or FileOutputStream. The mechanics of this are left for the container to
     * sort out, but that is the reason why this getter method exists.
     */
    public String getStreamRefName(String streamId) {
        if (getUsage() != null) {            
            List<Element> elemList = DOMUtils.findAllElementsByTagNameNS(getUsage(), 
                                                                        Tool.TOOL_SPEC_PUBLIC_ID, 
                                                                        "associatedArgument");
            for (Element elem : elemList) {
                if (elem.getAttribute("streamref").equals(streamId)) {
                    return ((Element)elem.getParentNode()).getAttribute("id");
                }
            }
            
            elemList = DOMUtils.findAllElementsByTagNameNS(getUsage(), 
                                                           Tool.TOOL_SPEC_PUBLIC_ID, 
                                                           "argument");
            for (Element elem : elemList) {
                if (elem.getAttribute("streamref").equals(streamId)) {
                    return elem.getAttribute("id");
                }
            }
        }
        return null;
    }

    public String getParameterDefault(String name) {
        Element el = getElementById(name);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Element with id " + name + " is " + el);
        }
        if (el != null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("local name is " + el.getLocalName());
            }
            if ("argument".equals(el.getLocalName())) {
                if (el.hasAttribute("default")) {
                    return el.getAttribute("default");
                }
            } else if ("option".equals(el.getLocalName())) {              
                List<Element> elemList = 
                    DOMUtils.findAllElementsByTagNameNS(el, 
                                                        "http://cxf.apache.org/Xpipe/ToolSpecification", 
                                                        "associatedArgument");
                if (elemList.size() > 0) {
                    Element assArg = elemList.get(0);
                    if (assArg.hasAttribute("default")) {
                        return assArg.getAttribute("default");
                    }
                }
            }
        }
        return null;
    }

    public String getAnnotation() {
        String result = null;
        Element element = doc.getDocumentElement();
        
        Node node = element.getFirstChild();
        while (node != null) {
            if ((node.getNodeType() == Node.ELEMENT_NODE)
                && ("annotation".equals(node.getNodeName()))) {
                result = node.getNodeValue();
                break;
            }
            node = node.getNextSibling();
        }
        return result;
    }

}
