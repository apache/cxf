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

package org.apache.cxf.databinding.source.mime;

import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.extensions.ExtensionSerializer;

public class MimeSerializer implements ExtensionSerializer {
    
    public void serialize(XmlSchemaObject schemaObject, Class classOfType, Node domNode) {
        Map metaInfoMap = schemaObject.getMetaInfoMap();
        MimeAttribute mimeType = (MimeAttribute)metaInfoMap.get(MimeAttribute.MIME_QNAME);

        Element elt = (Element)domNode;
        Attr att1 = elt.getOwnerDocument().createAttributeNS(MimeAttribute.MIME_QNAME.getNamespaceURI(),
                                                             MimeAttribute.MIME_QNAME.getLocalPart());
        att1.setValue(mimeType.getValue());
        elt.setAttributeNodeNS(att1);
    }

}
