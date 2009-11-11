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
package org.apache.cxf.aegis.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * Namespace utilities.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @author <a href="mailto:poutsma@mac.com">Arjen Poutsma</a>
 */
public final class NamespaceHelper {
    
    private NamespaceHelper() {
        //utility class
    }
    /**
     * Create a unique namespace uri/prefix combination.
     * 
     * @param nsUri
     * @return The namespace with the specified URI. If one doesn't exist, one
     *         is created.
     */
    public static String getUniquePrefix(Element element, String namespaceURI) {
        String prefix = getPrefix(element, namespaceURI);
        // it is OK to have both namespace URI and prefix be empty. 
        if (prefix == null) {
            if ("".equals(namespaceURI)) {
                return "";
            }
            prefix = DOMUtils.createNamespace(element, namespaceURI);
        }
        return prefix;
    }

    public static String getPrefix(Element element, String namespaceURI) {
        return DOMUtils.getPrefixRecursive(element, namespaceURI);
    }

    public static void getPrefixes(Element element, String namespaceURI, List<String> prefixes) {
        DOMUtils.getPrefixesRecursive(element, namespaceURI, prefixes);
    }

    /**
     * Create a unique namespace uri/prefix combination.
     * 
     * @param nsUri
     * @return The namespace with the specified URI. If one doesn't exist, one
     *         is created.
     * @throws XMLStreamException
     */
    public static String getUniquePrefix(XMLStreamWriter writer, String namespaceURI, boolean declare)
        throws XMLStreamException {
        return getUniquePrefix(writer, namespaceURI, null, declare);
    }
    
    
    /**
     * Make a unique prefix.
     * @param writer target writer.
     * @param namespaceURI namespace
     * @param preferred if there's a proposed prefix (e.g. xsi), here it is.
     * @param declare whether to declare to the stream.
     * @return the prefix.
     * @throws XMLStreamException
     */
    public static String getUniquePrefix(XMLStreamWriter writer, 
                                         String namespaceURI,
                                         String preferred,
                                         boolean declare)
        throws XMLStreamException {
        
        if (preferred != null) {
            String existing = writer.getNamespaceContext().getNamespaceURI(preferred);
            if (namespaceURI.equals(existing)) {
                return preferred;
            }
        }
        String prefix = preferred; 
        if (prefix == null) {
            prefix = StaxUtils.getUniquePrefix(writer);

            if (declare) {
                writer.setPrefix(prefix, namespaceURI);
                writer.writeNamespace(prefix, namespaceURI);
            }
        }

        return prefix;
    }

    /**
     * Generates the name of a XML namespace from a given class name and
     * protocol. The returned namespace will take the form
     * <code>protocol://domain</code>, where <code>protocol</code> is the
     * given protocol, and <code>domain</code> the inversed package name of
     * the given class name. <p/> For instance, if the given class name is
     * <code>org.codehaus.xfire.services.Echo</code>, and the protocol is
     * <code>http</code>, the resulting namespace would be
     * <code>http://services.xfire.codehaus.org</code>.
     * 
     * @param className the class name
     * @param protocol the protocol (eg. <code>http</code>)
     * @return the namespace
     */
    public static String makeNamespaceFromClassName(String className, String protocol) {
        int index = className.lastIndexOf(".");

        if (index == -1) {
            return protocol + "://" + "DefaultNamespace";
        }

        String packageName = className.substring(0, index);

        StringTokenizer st = new StringTokenizer(packageName, ".");
        String[] words = new String[st.countTokens()];

        for (int i = 0; i < words.length; ++i) {
            words[i] = st.nextToken();
        }

        StringBuffer sb = new StringBuffer(80);

        for (int i = words.length - 1; i >= 0; --i) {
            String word = words[i];

            // seperate with dot
            if (i != words.length - 1) {
                sb.append('.');
            }

            sb.append(word);
        }

        return protocol + "://" + sb.toString();
    }

    /**
     * Method makePackageName
     * 
     * @param namespace
     * @return
     */
    public static String makePackageName(String namespace) {

        String hostname = null;
        String path = "";

        // get the target namespace of the document
        try {
            URL u = new URL(namespace);

            hostname = u.getHost();
            path = u.getPath();
        } catch (MalformedURLException e) {
            if (namespace.indexOf(":") > -1) {
                hostname = namespace.substring(namespace.indexOf(":") + 1);

                if (hostname.indexOf("/") > -1) {
                    hostname = hostname.substring(0, hostname.indexOf("/"));
                }
            } else {
                hostname = namespace;
            }
        }

        // if we didn't file a hostname, bail
        if (hostname == null) {
            return null;
        }

        // convert illegal java identifier
        hostname = hostname.replace('-', '_');
        path = path.replace('-', '_');

        // chomp off last forward slash in path, if necessary
        if ((path.length() > 0) && (path.charAt(path.length() - 1) == '/')) {
            path = path.substring(0, path.length() - 1);
        }

        // tokenize the hostname and reverse it
        StringTokenizer st = new StringTokenizer(hostname, ".:");
        String[] words = new String[st.countTokens()];

        for (int i = 0; i < words.length; ++i) {
            words[i] = st.nextToken();
        }

        StringBuffer sb = new StringBuffer(namespace.length());

        for (int i = words.length - 1; i >= 0; --i) {
            addWordToPackageBuffer(sb, words[i], i == words.length - 1);
        }

        // tokenize the path
        StringTokenizer st2 = new StringTokenizer(path, "/");

        while (st2.hasMoreTokens()) {
            addWordToPackageBuffer(sb, st2.nextToken(), false);
        }

        return sb.toString();
    }

    /**
     * Massage <tt>word</tt> into a form suitable for use in a Java package
     * name. Append it to the target string buffer with a <tt>.</tt> delimiter
     * iff <tt>word</tt> is not the first word in the package name.
     * 
     * @param sb the buffer to append to
     * @param word the word to append
     * @param firstWord a flag indicating whether this is the first word
     */
    private static void addWordToPackageBuffer(StringBuffer sb, String word, boolean firstWord) {

        if (JavaUtils.isJavaKeyword(word)) {
            word = JavaUtils.makeNonJavaKeyword(word);
        }

        // separate with dot after the first word
        if (!firstWord) {
            sb.append('.');
        }

        // prefix digits with underscores
        if (Character.isDigit(word.charAt(0))) {
            sb.append('_');
        }

        // replace periods with underscores
        if (word.indexOf('.') != -1) {
            char[] buf = word.toCharArray();

            for (int i = 0; i < word.length(); i++) {
                if (buf[i] == '.') {
                    buf[i] = '_';
                }
            }

            word = new String(buf);
        }

        sb.append(word);
    }

    /**
     * Reads a QName from the element text. Reader must be positioned at the
     * start tag.
     * 
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    public static QName readQName(XMLStreamReader reader) throws XMLStreamException {
        String value = reader.getElementText();
        if (value == null) {
            return null;
        }

        int index = value.indexOf(":");

        if (index == -1) {
            return new QName(value);
        }

        String prefix = value.substring(0, index);
        String localName = value.substring(index + 1);
        String ns = reader.getNamespaceURI(prefix);

        if (ns == null || localName == null) {
            throw new DatabindingException("Invalid QName in mapping: " + value);
        }

        return new QName(ns, localName, prefix);
    }

    public static QName createQName(NamespaceContext nc, String value) {
        if (value == null) {
            return null;
        }
        int index = value.indexOf(':');
        if (index == -1) {
            return new QName(nc.getNamespaceURI(""), value, "");
        } else {
            String prefix = value.substring(0, index);
            String localName = value.substring(index + 1);
            String ns = nc.getNamespaceURI(prefix);
            return new QName(ns, localName, prefix);
        }
    }

    public static QName createQName(Element e, String value, String defaultNamespace) {
        if (value == null) {
            return null;
        }

        int index = value.indexOf(":");

        if (index == -1) {
            return new QName(defaultNamespace, value);
        }

        String prefix = value.substring(0, index);
        String localName = value.substring(index + 1);
        String jNS = DOMUtils.getNamespace(e, prefix);
        if (jNS == null) {
            throw new DatabindingException("No namespace was found for prefix: " + prefix);
        }
        
        if (jNS == null || localName == null) {
            throw new DatabindingException("Invalid QName in mapping: " + value);
        }

        return new QName(jNS, localName, prefix);
    }
}
