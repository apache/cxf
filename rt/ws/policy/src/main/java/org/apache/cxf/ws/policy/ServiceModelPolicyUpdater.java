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
/////////////////////////////////////////////////////////////////////////
//
// Copyright University of Southampton IT Innovation Centre, 2009
//
// Copyright in this library belongs to the University of Southampton
// University Road, Highfield, Southampton, UK, SO17 1BJ
//
// This software may not be used, sold, licensed, transferred, copied
// or reproduced in whole or in part in any manner or form or in or
// on any media by any person other than in accordance with the terms
// of the License Agreement supplied with the software, or otherwise
// without the prior written consent of the copyright owners.
//
// This software is distributed WITHOUT ANY WARRANTY, without even the
// implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
// PURPOSE, except where stated in the License Agreement supplied with
// the software.
//
//  Created By :            Dominic Harries
//  Created Date :          2009-03-31
//  Created for Project :   BEinGRID
//
/////////////////////////////////////////////////////////////////////////


package org.apache.cxf.ws.policy;

import java.util.Collection;

import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.DescriptionInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.Extensible;
import org.apache.cxf.ws.policy.attachment.external.PolicyAttachment;
import org.apache.neethi.Constants;
import org.apache.neethi.Policy;

public class ServiceModelPolicyUpdater {
    private EndpointInfo ei;

    public ServiceModelPolicyUpdater(EndpointInfo ei) {
        this.ei = ei;
    }

    public void addPolicyAttachments(Collection<PolicyAttachment> attachments) {
        for (PolicyAttachment pa : attachments) {
            boolean policyUsed = false;

            for (BindingOperationInfo boi : ei.getBinding().getOperations()) {
                BindingMessageInfo inputMessage = boi.getInput();
                BindingMessageInfo outputMessage = boi.getOutput();

                if (pa.appliesTo(boi)) {
                    // Add wsp:PolicyReference to wsdl:binding/wsdl:operation
                    addPolicyRef(boi, pa.getPolicy());
                    // Add it to wsdl:portType/wsdl:operation too
                    // FIXME - since the appliesTo is for BindingOperationInfo, I think its dodgy
                    // that the policy ref should also be associated with the port type
                    addPolicyRef(ei.getInterface().getOperation(boi.getName()), pa.getPolicy());
                    policyUsed = true;
                } else if (pa.appliesTo(inputMessage)) {
                    addPolicyRef(inputMessage, pa.getPolicy());
                    policyUsed = true;
                } else if (pa.appliesTo(outputMessage)) {
                    addPolicyRef(outputMessage, pa.getPolicy());
                    policyUsed = true;
                }
            }

            // Add wsp:Policy to top-level wsdl:definitions
            if (policyUsed) {
                addPolicy(pa);
            }
        }
    }

    private void addPolicyRef(Extensible ext, Policy p) {
        Document doc = DOMUtils.getEmptyDocument();
        Element el = doc.createElementNS(p.getNamespace(), Constants.ELEM_POLICY_REF);
        el.setPrefix(Constants.ATTR_WSP);
        el.setAttribute(Constants.ATTR_URI, "#" + p.getId());

        UnknownExtensibilityElement uee = new UnknownExtensibilityElement();
        uee.setElementType(new QName(p.getNamespace(), Constants.ELEM_POLICY_REF));
        uee.setElement(el);
        uee.setRequired(true);

        ext.addExtensor(uee);
    }

    private void addPolicy(PolicyAttachment pa) {
        // TODO - do I need to defensively copy this?
        Element policyEl = pa.getElement();

        UnknownExtensibilityElement uee = new UnknownExtensibilityElement();
        uee.setRequired(true);
        uee.setElementType(DOMUtils.getElementQName(policyEl));
        uee.setElement(policyEl);

        if (ei.getService().getDescription() == null) {
            DescriptionInfo description = new DescriptionInfo();
            description.setName(ei.getService().getName());
            if (!StringUtils.isEmpty(ei.getAddress())) {
                description.setBaseURI(ei.getAddress() + "?wsdl");
            }

            ei.getService().setDescription(description);
        }
        ei.getService().getDescription().addExtensor(uee);
    }
}
