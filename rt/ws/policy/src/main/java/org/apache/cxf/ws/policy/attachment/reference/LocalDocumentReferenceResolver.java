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

package org.apache.cxf.ws.policy.attachment.reference;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.neethi.Policy;

/**
 * 
 */
public class LocalDocumentReferenceResolver implements ReferenceResolver {

    private Document document;
    private PolicyBuilder builder;
    
    public LocalDocumentReferenceResolver(Document di, PolicyBuilder b) {
        document = di;
        builder = b;
    }
    public Policy resolveReference(String uri) {
        return resolveReference(uri, document.getDocumentElement());
    }    
    public Policy resolveReference(String uri, Element el) {
        if (el == null) {
            return null;
        }
        if (uri.equals(el.getAttributeNS(PolicyConstants.WSU_NAMESPACE_URI,
                                         PolicyConstants.WSU_ID_ATTR_NAME))) {
            return builder.getPolicy(el);
        }
        Element el2 = DOMUtils.getFirstElement(el);
        while (el2 != null) {
            Policy p = resolveReference(uri, el2);
            if (p != null) {
                return p;
            }
            el2 = DOMUtils.getNextElement(el2);
        }
        return null;
    }

}
