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

package org.apache.cxf.service;

import org.apache.cxf.common.xmlschema.InvalidXmlSchemaReferenceException;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;

/**
 * 
 */
public class ServiceModelSchemaValidator extends ServiceModelVisitor {
    
    private SchemaCollection schemaCollection;
    private StringBuilder complaints;

    public ServiceModelSchemaValidator(ServiceInfo serviceInfo) {
        super(serviceInfo);
        schemaCollection = serviceInfo.getXmlSchemaCollection();
        complaints = new StringBuilder();
    }
    
    public String getComplaints() {
        return complaints.toString();
    }

    @Override
    public void begin(MessagePartInfo part) {
        // the unwrapped parts build for wrapped operations don't have real elements.
        if (part.isElement() && !part.getMessageInfo().getOperation().isUnwrapped()) {
            try {
                schemaCollection.validateElementName(part.getName(), part.getElementQName());
            } catch (InvalidXmlSchemaReferenceException ixsre) {
                complaints.append(part.getName() + " part element name " + ixsre.getMessage() + "\n");
            }
        } else if (!part.getMessageInfo().getOperation().isUnwrapped()) {
            if (part.getTypeQName() == null) {
                complaints.append(part.getName() + " of message " 
                                  + part.getMessageInfo().getName() 
                                  + " part type QName null.\n");
            } else {
                try {
                    schemaCollection.validateTypeName(part.getName(), part.getTypeQName());
                } catch (InvalidXmlSchemaReferenceException ixsre) {
                    complaints.append(part.getName() + " part type name " + ixsre.getMessage() + "\n");
                }
            }
        }
    }
}
