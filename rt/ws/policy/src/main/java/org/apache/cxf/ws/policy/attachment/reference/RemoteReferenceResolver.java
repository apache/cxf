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

import org.xml.sax.InputSource;

import org.apache.cxf.resource.ExtendedURIResolver;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;

/**
 *
 */
public class RemoteReferenceResolver implements ReferenceResolver {

    private String baseURI;
    private PolicyBuilder builder;

    public RemoteReferenceResolver(String uri, PolicyBuilder b) {
        baseURI = uri;
        builder = b;
    }

    public Policy resolveReference(String uri) {
        int pos = uri.indexOf('#');
        String documentURI = pos == -1 ? uri : uri.substring(0, pos);
        ExtendedURIResolver resolver = new ExtendedURIResolver();
        InputSource is = resolver.resolve(documentURI, baseURI);
        if (null == is) {
            return null;
        }
        Document doc = null;
        try {
            doc = StaxUtils.read(is.getByteStream());
        } catch (Exception ex) {
            throw new PolicyException(ex);
        } finally {
            resolver.close();
        }
        if (pos == -1) {
            return builder.getPolicy(doc.getDocumentElement());
        }
        String id = uri.substring(pos + 1);
        for (Element elem : PolicyConstants
                .findAllPolicyElementsOfLocalName(doc,
                                                  Constants.ELEM_POLICY)) {

            if (id.equals(elem.getAttributeNS(PolicyConstants.WSU_NAMESPACE_URI,
                                              PolicyConstants.WSU_ID_ATTR_NAME))) {
                return builder.getPolicy(elem);
            }
        }

        return null;
    }
}
