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

package org.apache.cxf.ws.transfer.shared.handlers;

import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.shared.TransferConstants;

/**
 *
 * @author erich
 */
public class ReferenceParameterParsingHandler implements SOAPHandler<SOAPMessageContext> {

    private final String refElementName;
    
    private final String refAttrName;
    
    private final String refElementNamespace;
    
    public ReferenceParameterParsingHandler() {
        this.refElementName = TransferConstants.REF_PARAMS_EL_NAME;
        this.refAttrName = TransferConstants.REF_PARAMS_ATTR_NAME;
        this.refElementNamespace = TransferConstants.REF_PARAMS_EL_NAMESPACE;
    }
    
    public ReferenceParameterParsingHandler(String refElementName, String refAttrName, String refElementNamespace) {
        this.refElementName = refElementName;
        this.refAttrName = refAttrName;
        this.refElementNamespace = refElementNamespace;
    }
    
    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        // we are interest only in inbound message
        if ((Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            return true;
        }
        try {
            ReferenceParametersType refParams = new ReferenceParametersType();
            NodeList children = context.getMessage().getSOAPHeader().getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node childNode = children.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element childEl = (Element) childNode;
                    if (refElementName.equals(childEl.getLocalName())
                            && refElementNamespace.equals(childEl.getNamespaceURI())) {
                        NodeList refElementChildren = childEl.getChildNodes();
                        for (int j = 0; j < refElementChildren.getLength(); j++) {
                            refParams.getAny().add(refElementChildren.item(j));
                        }
                    } else if (childEl.hasAttributeNS(refElementNamespace, refAttrName)
                            && "true".equals(childEl.getAttributeNS(refElementNamespace, refAttrName))) {
                        refParams.getAny().add(childEl);
                    }
                }
            }
            context.put(TransferConstants.REF_PARAMS_CONTEXT_KEY, refParams);
            return true;
        } catch (SOAPException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean handleFault(SOAPMessageContext c) {
        return true;
    }

    @Override
    public void close(MessageContext mc) {
        
    }
    
}
