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
package org.apache.cxf.aegis.util.stax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.cxf.staxutils.AbstractDOMStreamReader;
import org.apache.cxf.staxutils.FastStack;
import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.EntityRef;
import org.jdom.Namespace;
import org.jdom.Text;

/**
 * Facade for DOMStreamReader using JDOM implmentation.
 * 
 * @author <a href="mailto:tsztelak@gmail.com">Tomasz Sztelak</a>
 */
public class JDOMStreamReader extends AbstractDOMStreamReader<Element, Integer> {

    private Content content;

    private FastStack<Map<String, Namespace>> namespaceStack
        = new FastStack<Map<String, Namespace>>();

    private List<Namespace> namespaces = new ArrayList<Namespace>();

    private Map<String, Namespace> prefix2decNs;

    private JDOMNamespaceContext namespaceContext;

    /**
     * @param element
     */
    public JDOMStreamReader(Element element) {
        super(new ElementFrame<Element, Integer>(element, null, -1));

        namespaceContext = new JDOMNamespaceContext();
        setupNamespaces(element);
    }

    /**
     * @param document
     */
    public JDOMStreamReader(Document document) {
        this(document.getRootElement());
    }

    public static String toStaxType(int jdom) {
        String val;
        switch (jdom) {
        case Attribute.CDATA_TYPE:
            val = "CDATA";
            break;
        case Attribute.ID_TYPE:
            val = "ID";
            break;
        case Attribute.IDREF_TYPE:
            val = "IDREF";
            break;
        case Attribute.IDREFS_TYPE:
            val = "IDREFS";
            break;
        case Attribute.ENTITY_TYPE:
            val = "ENTITY";
            break;
        case Attribute.ENTITIES_TYPE:
            val = "ENTITIES";
            break;
        case Attribute.ENUMERATED_TYPE:
            val = "ENUMERATED";
            break;
        case Attribute.NMTOKEN_TYPE:
            val = "NMTOKEN";
            break;
        case Attribute.NMTOKENS_TYPE:
            val = "NMTOKENS";
            break;
        case Attribute.NOTATION_TYPE:
            val = "NOTATION";
            break;
        default:
            val = null;
        }
        return val;
    }

    private void setupNamespaces(Element element) {
        namespaceContext.setElement(element);

        if (prefix2decNs != null) {
            namespaceStack.push(prefix2decNs);
        }

        prefix2decNs = new HashMap<String, Namespace>();
        namespaces.clear();

        for (Iterator itr = element.getAdditionalNamespaces().iterator(); itr.hasNext();) {
            declare((Namespace)itr.next());
        }

        Namespace ns = element.getNamespace();

        if (shouldDeclare(ns)) {
            declare(ns);
        }

        for (Iterator itr = element.getAttributes().iterator(); itr.hasNext();) {
            ns = ((Attribute)itr.next()).getNamespace();
            if (shouldDeclare(ns)) {
                declare(ns);
            }
        }
    }

    private void declare(Namespace ns) {
        prefix2decNs.put(ns.getPrefix(), ns);
        namespaces.add(ns);
    }

    private boolean shouldDeclare(Namespace ns) {
        if (ns == Namespace.XML_NAMESPACE) {
            return false;
        }

        if (ns == Namespace.NO_NAMESPACE && getDeclaredURI("") == null) {
            return false;
        }

        String decUri = getDeclaredURI(ns.getPrefix());

        return !(decUri != null && decUri.equals(ns.getURI()));
    }

    private String getDeclaredURI(String string) {
        for (int i = namespaceStack.size() - 1; i == 0; i--) {
            Map<String, Namespace> nmspaces = namespaceStack.get(i);

            Namespace dec = nmspaces.get(string);

            if (dec != null) {
                return dec.getURI();
            }
        }
        return null;
    }

    @Override
    protected void endElement() {
        if (namespaceStack.size() > 0) {
            prefix2decNs = namespaceStack.pop();
        }
    }

    public Element getCurrentElement() {
        return getCurrentFrame().getElement();
    }

    @Override
    protected ElementFrame<Element, Integer> getChildFrame() {
        int currentChild = getCurrentFrame().getCurrentChild();
        return new ElementFrame<Element, Integer>((Element)getCurrentElement().getContent(currentChild),
                                                  getCurrentFrame(),
                                                  -1);
    }

    @Override
    protected boolean hasMoreChildren() {
        int currentChild = getCurrentFrame().getCurrentChild();
        return currentChild < (getCurrentElement().getContentSize() - 1);
    }

    @Override
    protected int nextChild() {
        int currentChild = getCurrentFrame().getCurrentChild();
        currentChild++;
        getCurrentFrame().setCurrentChild(currentChild);
        this.content = getCurrentElement().getContent(currentChild);

        if (content instanceof Text) {
            return CHARACTERS;
        } else if (content instanceof Element) {
            setupNamespaces((Element)content);
            return START_ELEMENT;
        } else if (content instanceof CDATA) {
            return CHARACTERS;
        } else if (content instanceof Comment) {
            return CHARACTERS;
        } else if (content instanceof EntityRef) {
            return ENTITY_REFERENCE;
        }

        throw new IllegalStateException();
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return ((Text)content).getText();
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return getCurrentElement().getNamespace(prefix).getURI();
    }

    public String getAttributeValue(String ns, String local) {
        return getCurrentElement().getAttributeValue(local, Namespace.getNamespace(ns));
    }

    public int getAttributeCount() {
        return getCurrentElement().getAttributes().size();
    }

    Attribute getAttribute(int i) {
        return (Attribute)getCurrentElement().getAttributes().get(i);
    }

    public QName getAttributeName(int i) {
        Attribute at = getAttribute(i);

        return new QName(at.getNamespaceURI(), at.getName(), at.getNamespacePrefix());
    }

    public String getAttributeNamespace(int i) {
        return getAttribute(i).getNamespaceURI();
    }

    public String getAttributeLocalName(int i) {
        return getAttribute(i).getName();
    }

    public String getAttributePrefix(int i) {
        return getAttribute(i).getNamespacePrefix();
    }

    public String getAttributeType(int i) {
        return toStaxType(getAttribute(i).getAttributeType());
    }

    public String getAttributeValue(int i) {
        return getAttribute(i).getValue();
    }

    public boolean isAttributeSpecified(int i) {
        return getAttribute(i).getValue() != null;
    }

    public int getNamespaceCount() {
        return namespaces.size();
    }

    Namespace getNamespace(int i) {
        return namespaces.get(i);
    }

    public String getNamespacePrefix(int i) {
        return getNamespace(i).getPrefix();
    }

    public String getNamespaceURI(int i) {
        return getNamespace(i).getURI();
    }

    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    public String getText() {
        return content.getValue();
    }

    public char[] getTextCharacters() {
        return getText().toCharArray();
    }

    public int getTextStart() {
        return 0;
    }

    public int getTextLength() {
        return getText().length();
    }

    public String getEncoding() {
        return null;
    }

    public QName getName() {
        Element el = getCurrentElement();

        return new QName(el.getNamespaceURI(), el.getName(), el.getNamespacePrefix());
    }

    public String getLocalName() {
        return getCurrentElement().getName();
    }

    public String getNamespaceURI() {
        return getCurrentElement().getNamespaceURI();
    }

    public String getPrefix() {
        return getCurrentElement().getNamespacePrefix();
    }

    public String getPITarget() {
        throw new UnsupportedOperationException();
    }

    public String getPIData() {
        throw new UnsupportedOperationException();
    }
}
