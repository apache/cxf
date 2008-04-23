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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.extensions.ExtensionDeserializer;


/**
 * Workaround a bug in XmlSchema (WSCOMMONS-261). Remove this when there is a version of XmlSchema
 * with the fix.
 * 
 * In XmlSchema, the default deserializer will only allow a single extension per element.  The 
 * last one wipes out the earlier recorded extensions.
 */
@SuppressWarnings("unchecked")
class FixedExtensionDeserializer implements ExtensionDeserializer {

    public void deserialize(XmlSchemaObject schemaObject, QName name, Node node) {

        // we just attach the raw node either to the meta map of
        // elements or the attributes
        Map metaInfoMap = schemaObject.getMetaInfoMap();
        if (metaInfoMap == null) {
            metaInfoMap = new HashMap();
        }

        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
            if (name.getNamespaceURI().equals(Constants.XMLNS_ATTRIBUTE_NS_URI)) {
                return;
            }
            Map attribMap; 
            if (metaInfoMap.containsKey(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES)) {
                attribMap  = (Map)metaInfoMap.get(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES);
            } else {
                attribMap = new HashMap();
                metaInfoMap.put(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES, attribMap);
            }
            attribMap.put(name, node);
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            Map elementMap;
            if (metaInfoMap.containsKey(Constants.MetaDataConstants.EXTERNAL_ELEMENTS)) {
                elementMap  = (Map)metaInfoMap.get(Constants.MetaDataConstants.EXTERNAL_ELEMENTS);
            } else {
                elementMap = new HashMap();
                metaInfoMap.put(Constants.MetaDataConstants.EXTERNAL_ELEMENTS, elementMap);
            }
            elementMap.put(name, node);
        }

        //subsequent processing takes place only if this map is not empty
        if (!metaInfoMap.isEmpty() && schemaObject.getMetaInfoMap() == null) {
            schemaObject.setMetaInfoMap(metaInfoMap);
        }
    }
}
