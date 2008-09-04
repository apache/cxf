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

import java.util.List;

import javax.wsdl.extensions.UnknownExtensibilityElement;

import org.apache.cxf.service.model.DescriptionInfo;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.neethi.Policy;

/**
 * 
 */
public class LocalServiceModelReferenceResolver implements ReferenceResolver {

    private DescriptionInfo descriptionInfo;
    private PolicyBuilder builder;
    
    public LocalServiceModelReferenceResolver(DescriptionInfo d, PolicyBuilder b) {
        descriptionInfo = d;
        builder = b;
    }
    
    public Policy resolveReference(String uri) {
        List<UnknownExtensibilityElement> extensions = 
            descriptionInfo.getExtensors(UnknownExtensibilityElement.class);
        if (extensions != null) {
            for (UnknownExtensibilityElement e : extensions) {
                if (PolicyConstants.isPolicyElem(e.getElementType())
                    && uri.equals(e.getElement().getAttributeNS(PolicyConstants.WSU_NAMESPACE_URI,
                                                                PolicyConstants.WSU_ID_ATTR_NAME))) {
                    return builder.getPolicy(e.getElement());
                }
            }
        }
        return null;
    }

}
