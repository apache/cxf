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

package org.apache.cxf.common.xmlschema;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaEnumerationFacet;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;

/**
 * Some functions that avoid problems with Commons XML Schema.  
 */
public final class XmlSchemaTools {
    private static final Logger LOG = LogUtils.getL7dLogger(XmlSchemaTools.class);
    
    private XmlSchemaTools() {
    }
    
    private static void setNameFromQName(XmlSchemaElement element, QName name) {
        if (name == null) {
            element.setName(null);
        } else {
            element.setName(name.getLocalPart());
        }
    }
    
    /**
     * Wrapper around XmlSchemaElement.setQName that checks for inconsistency with 
     * refName.
     * @param element
     * @param name
     */
    public static void setElementQName(XmlSchemaElement element, QName name) {
        if (name != null && element.getRefName() != null && !element.getRefName().equals(name)) {
            LOG.severe("Attempt to set the QName of an element with a reference name");
            throw new 
                XmlSchemaInvalidOperation("Attempt to set the QName of an element "
                                          + "with a reference name.");
        }
        element.setQName(name);
        // in CXF, we want them to be consistent.
        setNameFromQName(element, name);
    }

    /**
     * Wrapper around XmlSchemaElement.setName that checks for inconsistency with 
     * refName.
     * @param element
     * @param name
     */
    public static void setElementName(XmlSchemaElement element, String name) {
        if (name != null 
            && element.getRefName() != null 
            && !element.getRefName().getLocalPart().equals(name)
            && (element.getQName() == null || element.getQName().getLocalPart().equals(name))) {
            LOG.severe("Attempt to set the name of an element with a reference name.");
            throw new 
                XmlSchemaInvalidOperation("Attempt to set the name of an element "
                                          + "with a reference name.");
        }
        element.setName(name);
    }

    /**
     * Wrapper around XmlSchemaElement.setRefName that checks for inconsistency with 
     * name and QName.
     * @param element
     * @param name
     */
    public static void setElementRefName(XmlSchemaElement element, QName name) {
        if (name != null
            && ((element.getQName() != null && !element.getQName().equals(name)) 
            || (element.getName() != null && !element.getName().equals(name.getLocalPart())))) {
            LOG.severe("Attempt to set the refName of an element with a name or QName");
            throw new 
                XmlSchemaInvalidOperation("Attempt to set the refName of an element "
                                          + "with a name or QName.");
        }
        element.setRefName(name);
        // cxf conventionally keeps something in the name slot.
        setNameFromQName(element, name);
    }
    
    /**
     * Return true if a simple type is a straightforward XML Schema representation of an enumeration.
     * If we discover schemas that are 'enum-like' with more complex structures, we might
     * make this deal with them.
     * @param type Simple type, possible an enumeration.
     * @return true for an enumeration.
     */
    public static boolean isEumeration(XmlSchemaSimpleType type) {
        XmlSchemaSimpleTypeContent content = type.getContent();
        if (!(content instanceof XmlSchemaSimpleTypeRestriction)) {
            return false;
        }
        XmlSchemaSimpleTypeRestriction restriction = (XmlSchemaSimpleTypeRestriction) content;
        XmlSchemaObjectCollection facets = restriction.getFacets();
        for (int x = 0; x < facets.getCount(); x++) {
            XmlSchemaFacet facet = (XmlSchemaFacet) facets.getItem(x);
            if (!(facet instanceof XmlSchemaEnumerationFacet)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Retrieve the string values for an enumeration.
     * @param type
     * @return
     */
    public static List<String> enumeratorValues(XmlSchemaSimpleType type) {
        XmlSchemaSimpleTypeContent content = type.getContent();
        XmlSchemaSimpleTypeRestriction restriction = (XmlSchemaSimpleTypeRestriction) content;
        XmlSchemaObjectCollection facets = restriction.getFacets();
        List<String> values = new ArrayList<String>(); 
        for (int x = 0; x < facets.getCount(); x++) {
            XmlSchemaFacet facet = (XmlSchemaFacet) facets.getItem(x);
            XmlSchemaEnumerationFacet enumFacet = (XmlSchemaEnumerationFacet) facet;
            values.add(enumFacet.getValue().toString());
        }
        return values;
    }
}
