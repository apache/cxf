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
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;

import org.apache.cxf.aegis.util.NamespaceHelper;
import org.jdom.Element;
import org.jdom.Namespace;

public class JDOMNamespaceContext implements NamespaceContext {
    private Element element;

    public String getNamespaceURI(String prefix) {
        Namespace namespace = element.getNamespace(prefix);
        if (namespace == null) {
            return null;
        }

        return namespace.getURI();
    }

    public String getPrefix(String uri) {
        return NamespaceHelper.getPrefix(element, uri);
    }

    public Iterator<String> getPrefixes(String uri) {
        List<String> prefixes = new ArrayList<String>();
        NamespaceHelper.getPrefixes(element, uri, prefixes);
        return prefixes.iterator();
    }

    public Element getElement() {
        return element;
    }

    public void setElement(Element element) {
        this.element = element;
    }
}
