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
package org.apache.cxf.binding.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;


/**
 * @author <a href=""mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public final class IriDecoderHelper {
    public static final ResourceBundle BUNDLE = BundleUtils.getBundle(IriDecoderHelper.class);

    private IriDecoderHelper() {

    }

    public static List<Param> decodeIri(String uri, String loc) {
        List<Param> values = new ArrayList<Param>();
        String path = getUriPath(uri);
        String locPath = getUriPath(loc);
        int idx2 = 0;
        char c;
        for (int idx1 = 0; idx1 < locPath.length(); idx1++) {
            c = locPath.charAt(idx1);
            if (c == '{') {
                if (locPath.charAt(idx1 + 1) == '{') {
                    // double curly brace
                    expect(path, idx2++, '{');
                } else {
                    int locEnd = locPath.indexOf('}', idx1);
                    String name = locPath.substring(idx1 + 1, locEnd);
                    idx1 = locEnd;
                    String endFragment = getEndFragment(locEnd + 1, locPath);

                    int end = findPartEnd(path, idx2, endFragment);
                    String value = path.substring(idx2, end);
                    idx2 = end;
                    values.add(new Param(name, value));
                }
            } else {
                expect(path, idx2++, c);
            }
        }
        if (idx2 < path.length()) {
            c = path.charAt(idx2++);
            if (c == '?') {
                int end = path.indexOf('#', idx2);
                if (end < 0) {
                    end = path.length();
                }
                addParams(path, idx2, end, values);
            }
        }
        return values;
    }

    private static String getEndFragment(int i, String locPath) {
        int end = locPath.indexOf('{', i);

        if (end == -1) {
            end = locPath.length();
        } else if (locPath.charAt(end + 1) == '{') {
            return getEndFragment(end + 1, locPath);
        }

        return locPath.substring(i, end);
    }

    public static void addParams(String input, int start, int stop, List<Param> params) {
        while (start < stop) {
            int eq = input.indexOf('=', start);
            int se = input.indexOf('&', eq);
            if (se < 0) {
                se = stop;
            }
            params.add(new Param(input.substring(start, eq), input.substring(eq + 1, se)));
            start = se + 1;
        }
    }

    /**
     * @param endFragment
     *
     */
    public static int findPartEnd(String path, int c, String endFragment) {
        int end = path.length();
        int i = end;

        if (!"".equals(endFragment)) {
            i = path.indexOf(endFragment, c);
            if (i >= c && i < end) {
                end = i;
            }
        }

        i =  path.indexOf('?', c);
        if (i >= c && i < end) {
            end = i;
        }

        return end;
    }

    /**
     * Check that the next character is the one expected or throw an exception
     */
    public static void expect(String path, int index, char c) {
        if (path.charAt(index) != c) {
            throw new IllegalStateException("Unexpected character '" + c + "' at index " + index);
        }
    }

    /**
     * Get the path of a given uri, removing the scheme and authority parts
     */
    public static String getUriPath(String uri) {
        int idx = uri.indexOf("://");
        int idx2 = uri.indexOf('/', idx + 3);
        return uri.substring(idx2 + 1);
    }

    public static String combine(String location, String httpLocation) {
        if (httpLocation == null) {
            return location;
        }
        if (httpLocation.indexOf("://") != -1) {
            return httpLocation;
        }
        if (location.endsWith("/")) {
            return location + httpLocation;
        } else {
            return location + "/" + httpLocation;
        }
    }

    private static XmlSchemaType findSchemaType(Collection<SchemaInfo> schemas,
                                         QName name) {
        for (SchemaInfo inf : schemas) {
            if (inf.getNamespaceURI().equals(name.getNamespaceURI())) {
                return inf.getSchema().getTypeByName(name);
            }
        }
        return null;
    }
    
    private static boolean findSchemaUnQualified(Collection<SchemaInfo> schemas, QName name) {
        for (SchemaInfo inf : schemas) {
            if (inf.getNamespaceURI().equals(name.getNamespaceURI())) {
                return inf.getSchema().getElementFormDefault().getValue().equals(XmlSchemaForm.UNQUALIFIED);
            }
        }
        //Unqualified by default
        return true;
    }

    /**
     * Create a dom document conformant with the given schema element with the
     * input parameters.
     * 
     * @param element
     * @param params
     * @return
     */
    public static Document buildDocument(XmlSchemaAnnotated schemaAnnotation,
                                         Collection<SchemaInfo> schemas,
                                         List<Param> params) {

        XmlSchemaElement element = null;
        QName qname = null;
        boolean unQualified = false;
        
        XmlSchemaComplexType cplxType = null;
        if (schemaAnnotation instanceof XmlSchemaElement) {
            element = (XmlSchemaElement)schemaAnnotation;
            qname = element.getQName();
            if (element.getSchemaType() instanceof XmlSchemaSimpleType) {
                throw new Fault(new Message("SIMPLE_TYPE", BUNDLE));
            }
            
            cplxType = (XmlSchemaComplexType)element.getSchemaType();
            unQualified = findSchemaUnQualified(schemas, element.getSchemaTypeName());
            if (cplxType == null) {
                cplxType = (XmlSchemaComplexType)findSchemaType(schemas, element.getSchemaTypeName());
            }
        } else if (schemaAnnotation instanceof XmlSchemaComplexType) {
            cplxType = (XmlSchemaComplexType)schemaAnnotation;
            qname = cplxType.getQName();
        } else if (schemaAnnotation instanceof XmlSchemaSimpleType) {
            throw new Fault(new Message("SIMPLE_TYPE", BUNDLE));
        }

        Document doc = DOMUtils.createDocument();


        XmlSchemaSequence seq = (XmlSchemaSequence)cplxType.getParticle();
        Element e = doc.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        e.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, qname.getNamespaceURI());
        doc.appendChild(e);

        if (seq == null || seq.getItems() == null) {
            return doc;
        }

        for (int i = 0; i < seq.getItems().getCount(); i++) {
            XmlSchemaElement elChild = (XmlSchemaElement)seq.getItems().getItem(i);
            Param param = null;
            for (Param p : params) {
                if (p.getName().equals(elChild.getQName().getLocalPart())) {
                    param = p;
                    break;
                }
            }
            Element ec = null;
            if (unQualified) {
                ec = doc.createElement(elChild.getQName().getLocalPart());
            } else {
                ec = doc.createElementNS(elChild.getQName().getNamespaceURI(), elChild.getQName()
                    .getLocalPart());
                if (!elChild.getQName().getNamespaceURI().equals(qname.getNamespaceURI())) {
                    ec.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, elChild.getQName().getNamespaceURI());
                }
            }
            
            if (param != null) {
                params.remove(param);
                ec.appendChild(doc.createTextNode(param.getValue()));
            }
            e.appendChild(ec);
        }
        return doc;
    }

    public static Document interopolateParams(Document doc,
                                              XmlSchemaAnnotated schemaAnnotation,
                                              Collection<SchemaInfo> schemas,
                                              List<Param> params) {
        XmlSchemaElement element = null;
        QName qname = null;
        XmlSchemaComplexType cplxType = null;
        if (schemaAnnotation instanceof XmlSchemaElement) {
            element = (XmlSchemaElement)schemaAnnotation;
            qname = element.getQName();
            cplxType = (XmlSchemaComplexType)element.getSchemaType();
            if (cplxType == null) {
                cplxType = (XmlSchemaComplexType)findSchemaType(schemas, element.getSchemaTypeName());
            }
        }
        if (schemaAnnotation instanceof XmlSchemaComplexType) {
            cplxType = (XmlSchemaComplexType)schemaAnnotation;
            qname = cplxType.getQName();
        }
        XmlSchemaSequence seq = (XmlSchemaSequence)cplxType.getParticle();
        Element root = doc.getDocumentElement();
        if (root == null) {
            root = doc.createElementNS(qname.getNamespaceURI(),
                                    qname.getLocalPart());
            root.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, qname.getNamespaceURI());
            doc.appendChild(root);
        }

        for (int i = 0; i < seq.getItems().getCount(); i++) {
            XmlSchemaElement elChild = (XmlSchemaElement)seq.getItems().getItem(i);
            Param param = null;
            for (Param p : params) {
                if (p.getName().equals(elChild.getQName().getLocalPart())) {
                    param = p;
                    break;
                }
            }
            if (param == null) {
                continue;
            }

            Element ec = getElement(root, elChild.getQName());
            if (ec == null) {
                ec = doc.createElementNS(elChild.getQName().getNamespaceURI(), elChild.getQName()
                                         .getLocalPart());
                if (!elChild.getQName().getNamespaceURI().equals(qname.getNamespaceURI())) {
                    ec.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, elChild.getQName().getNamespaceURI());
                }

                // insert the element at the appropriate position
                Element insertBeforeEl = getIndexedElement(root, i);
                if (insertBeforeEl != null) {
                    root.insertBefore(ec, insertBeforeEl);
                } else {
                    root.appendChild(ec);
                }
            } else {
                Node node = ec.getFirstChild();
                while (node != null) {
                    Node next = node.getNextSibling();
                    ec.removeChild(node);
                    node = next;
                }
            }

            if (param != null) {
                params.remove(param);
                ec.appendChild(doc.createTextNode(param.getValue()));
            }
        }
        return doc;
    }

    private static Element getIndexedElement(Element e, int i) {
        Element elem = DOMUtils.getFirstElement(e);
        int elNum = 0;
        while (elem != null) {
            if (i == elNum) {
                return elem;
            }
            elNum++;
            elem = DOMUtils.getNextElement(elem);
        }
        return null;
    }

    private static Element getElement(Element element, QName name) {
        Element elem = DOMUtils.getFirstElement(element);
        while (elem != null) {
            if (elem.getLocalName().equals(name.getLocalPart())
                && elem.getNamespaceURI().equals(name.getNamespaceURI())) {
                return elem;
            }
            elem = DOMUtils.getNextElement(elem);         
        }
        return null;
    }

    public static List<Param> decode(String uri, String loc, InputStream is) {
        List<Param> params = IriDecoderHelper.decodeIri(uri, loc);
        if (is != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                IOUtils.copy(is, baos);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            IriDecoderHelper.addParams(baos.toString(), 0, baos.size(), params);
        }
        return params;
    }

    /**
     * Simple holder class for a name/value pair.
     */
    public static class Param {

        private final String name;
        private final String value;

        public Param(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "[" + name + "=" + value + "]";
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Param other = (Param)obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            return true;
        }
    }

}
