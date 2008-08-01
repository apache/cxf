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

package org.apache.cxf.binding.corba.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.types.CorbaHandlerUtils;
import org.apache.cxf.binding.corba.types.CorbaObjectHandler;
import org.apache.cxf.binding.corba.types.CorbaTypeListener;
import org.apache.cxf.binding.corba.wsdl.ArgType;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;

public class CorbaStreamWriter implements XMLStreamWriter {

    protected String defaultNS = "";
    protected CorbaTypeListener[] listeners;
    protected ServiceInfo serviceInfo;
    protected CorbaTypeMap typeMap;
    protected ORB orb;
    protected CorbaTypeListener currentTypeListener;
    protected CorbaNamespaceContext ctx = new CorbaNamespaceContext();

    private List<ArgType> params;
    private int paramCounter;

    private Stack<QName> elements = new Stack<QName>();
    private QName paramElement;
    private QName wrapElementName;    

    private boolean skipWrap;
    

    public CorbaStreamWriter(ORB orbRef,
                             CorbaTypeMap map,
                             ServiceInfo sinfo) {
        orb = orbRef;
        typeMap = map;
        serviceInfo = sinfo;
    }

    public CorbaStreamWriter(ORB orbRef,
                             List<ArgType> paramTypes,
                             CorbaTypeMap map,
                             ServiceInfo sinfo,
                             boolean wrap) {
        this(orbRef, map, sinfo);
        params = paramTypes;
        skipWrap = wrap;
        listeners = new CorbaTypeListener[paramTypes.size()];
    }

    public CorbaObjectHandler[] getCorbaObjects() {
        //REVISIT, we can make the CorbaTypeListener & CorbaObjectHandler the same object.
        CorbaObjectHandler[] handlers = new CorbaObjectHandler[listeners.length];
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == null) {
                throw new CorbaBindingException("Uninitalized object for parameter "
                                                + params.get(i).getName());
            }
            handlers[i] = listeners[i].getCorbaObject();
        }
        return handlers;
    }

    public void writeStartElement(String namespaceURI, String localName)
        throws XMLStreamException {
        writeStartElement("", localName, namespaceURI);
    }

    public void writeEndElement() throws XMLStreamException {
        if (elements.empty()) {
            currentTypeListener = null;
        } else {
            QName name = elements.pop();
            if ((name.equals(paramElement)) || (name.equals(wrapElementName))) {
                currentTypeListener = null;
            } else {
                currentTypeListener.processEndElement(name);
            }
        }
        ctx.pop();
    }

    public void writeStartElement(java.lang.String localName)
        throws XMLStreamException {
        writeStartElement("", localName, defaultNS);
    }

    public void writeStartElement(java.lang.String prefix,
                                  java.lang.String localName,
                                  java.lang.String namespaceURI)
        throws XMLStreamException {
        ctx.push();
        if (prefix != null) {
            setPrefix(prefix, namespaceURI);
        }
        QName name = new QName(namespaceURI, localName, prefix);
        elements.push(name);
        if (!skipWrap) {
            if (currentTypeListener == null) {
                paramElement = name;
                setCurrentTypeListener(name);
            } else {
                currentTypeListener.processStartElement(name);
            }
        } else {
            wrapElementName = name;
            skipWrap = false;
        }
    }

    protected void setCurrentTypeListener(QName name) throws XMLStreamException {
        ArgType param = params.get(paramCounter);
        QName idlType = param.getIdltype();
        if (!skipWrap || (name.getLocalPart().equals(param.getName()))) {
            currentTypeListener = CorbaHandlerUtils.getTypeListener(name, idlType, typeMap, orb, serviceInfo);
            currentTypeListener.setNamespaceContext(ctx);
            listeners[paramCounter] = currentTypeListener;
            paramCounter++;
        } else {
            throw new XMLStreamException("Expected element not found: " + param.getName()
                                         + " (Found " + name.getLocalPart() + ")");
        }
    }

    public void writeEmptyElement(java.lang.String namespaceURI,
                                  java.lang.String localName)
        throws XMLStreamException {
        writeStartElement(namespaceURI, localName);
        writeEndElement();
    }

    public void writeEmptyElement(java.lang.String prefix,
                                  java.lang.String localName,
                                  java.lang.String namespaceURI)
        throws XMLStreamException {
        writeStartElement(namespaceURI, localName, prefix);
        writeEndElement();
    }

    public void writeEmptyElement(java.lang.String localName)
        throws XMLStreamException {
        writeStartElement(localName);
        writeEndElement();
    }
    
    public void writeEndDocument()
        throws XMLStreamException {
    }

    public void close()
        throws XMLStreamException {
    }

    public void flush()
        throws XMLStreamException {
    }

    public void writeAttribute(java.lang.String localName,
                               java.lang.String value)
        throws XMLStreamException {
        writeAttribute("", defaultNS, localName, value);
    }

    public void writeAttribute(java.lang.String prefix,
                               java.lang.String namespaceURI,
                               java.lang.String localName,
                               java.lang.String value)
        throws XMLStreamException {
        currentTypeListener.processWriteAttribute(prefix, namespaceURI, localName, value);
    }

    public void writeAttribute(java.lang.String namespaceURI,
                               java.lang.String localName,
                               java.lang.String value)
        throws XMLStreamException {
        writeAttribute("", namespaceURI, localName, value);
    }

    public void writeNamespace(java.lang.String prefix,
                               java.lang.String namespaceURI)
        throws XMLStreamException {
        if (currentTypeListener != null) {
            currentTypeListener.processWriteNamespace(prefix, namespaceURI);
        }
        setPrefix(prefix, namespaceURI);
    }

    public void writeDefaultNamespace(java.lang.String namespaceURI)
        throws XMLStreamException {
        defaultNS = namespaceURI;
    }

    public void writeComment(java.lang.String data)
        throws XMLStreamException {
    }

    public void writeProcessingInstruction(java.lang.String target)
        throws XMLStreamException {
    }

    public void writeProcessingInstruction(java.lang.String target,
                                           java.lang.String data)
        throws XMLStreamException {
    }

    public void writeCData(java.lang.String data)
        throws XMLStreamException {
        throw new XMLStreamException("Not yet implemented");
    }

    public void writeDTD(java.lang.String dtd)
        throws XMLStreamException {
    }

    public void writeEntityRef(java.lang.String name)
        throws XMLStreamException {
        throw new XMLStreamException("Not yet implemented");
    }

    public void writeStartDocument()
        throws XMLStreamException {
    }

    public void writeStartDocument(java.lang.String encoding,
                                   java.lang.String version)
        throws XMLStreamException {
    }

    public void writeStartDocument(java.lang.String version)
        throws XMLStreamException {
    }

    public void writeCharacters(java.lang.String text)
        throws XMLStreamException {
        currentTypeListener.processCharacters(text);
    }

    public void writeCharacters(char[] text,
                                int start,
                                int len)
        throws XMLStreamException {
        currentTypeListener.processCharacters(new String(text));
    }

    public java.lang.String getPrefix(java.lang.String uri)
        throws XMLStreamException {
        return ctx.getPrefix(uri);
    }

    public void setPrefix(java.lang.String prefix,
                          java.lang.String uri)
        throws XMLStreamException {
        ctx.setPrefix(prefix, uri);
    }

    public void setDefaultNamespace(java.lang.String uri)
        throws XMLStreamException {
        defaultNS = uri;
    }

    public void setNamespaceContext(NamespaceContext context)
        throws XMLStreamException {
        //ignore
    }

    public NamespaceContext getNamespaceContext() {
        return ctx;
    }

    public java.lang.Object getProperty(java.lang.String name)
        throws java.lang.IllegalArgumentException {
        return null;
    }
    
    
    
    public class CorbaNamespaceContext implements NamespaceContext {

        private Map<String, String> map;
        private CorbaNamespaceContext parent;

        public CorbaNamespaceContext() {
            this.map = new HashMap<String, String>();
        }

        private CorbaNamespaceContext(Map<String, String> map, CorbaNamespaceContext p) {
            this.map = map;
            this.parent = p;
        }
        
        public void push() {
            parent = new CorbaNamespaceContext(map, parent);
            map = new HashMap<String, String>();
        }
        public void pop() {
            if (parent != null) {
                map = parent.map;
                parent = parent.parent;
            }
        }
        
        public void setPrefix(String pfx, String ns) {
            map.put(pfx, ns);
        }
        public String getNamespaceURI(String prefix) {
            String answer = (String) map.get(prefix);
            if (answer == null && parent != null) {
                return parent.getNamespaceURI(prefix);
            }
            return answer;
        }

        public String getPrefix(String namespaceURI) {
            for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                if (namespaceURI.equals(entry.getValue())) {
                    return (String) entry.getKey();
                }
            }
            if (parent != null) {
                return parent.getPrefix(namespaceURI);
            }
            return null;
        }

        public Iterator getPrefixes(String namespaceURI) {
            Set<String> set = new HashSet<String>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (namespaceURI.equals(entry.getValue())) {
                    set.add(entry.getKey());
                }
            }
            if (parent != null) {
                Iterator iter = parent.getPrefixes(namespaceURI);
                while (iter.hasNext()) {
                    set.add((String)iter.next());
                }
            }
            return set.iterator();
        }
    }

    
}
