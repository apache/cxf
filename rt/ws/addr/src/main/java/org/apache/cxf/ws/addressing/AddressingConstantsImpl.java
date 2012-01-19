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

package org.apache.cxf.ws.addressing;


import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.util.PackageUtils;

/**
 * Encapsulation of version-specific WS-Addressing constants.
 */
public class AddressingConstantsImpl implements AddressingConstants {

    private static final ResourceBundle BUNDLE = 
        BundleUtils.getBundle(AddressingConstantsImpl.class);
    
    public AddressingConstantsImpl() {
    }

    /**
     * @return namespace defined by the normative WS-Addressing Core schema
     */
    public String getNamespaceURI() {
        return Names.WSA_NAMESPACE_NAME;
    }


    /**
     * @return namespace defined by the normative WS-Addressing WSDL bindings
     * schema
     */
    public String getWSDLNamespaceURI() {
        return Names.WSA_NAMESPACE_WSDL_NAME;
    }

    /**
     * @return QName of the WSDL extensiblity element
     */
    public QName getWSDLExtensibilityQName() {
        return Names.WSAW_USING_ADDRESSING_QNAME;
    }

    /**
     * @return QName of the wsaw:Action element
     */
    public QName getWSDLActionQName() {
        return Names.WSAW_ACTION_QNAME;
    }

    /**
     * @return Anonymous address URI
     */
    public String getAnonymousURI() {
        return Names.WSA_ANONYMOUS_ADDRESS;
    }
    
    /**
     * @return None address URI
     */
    public String getNoneURI() {
        return Names.WSA_NONE_ADDRESS;
    }

    /**
     * @return QName of the From addressing header
     */
    public QName getFromQName() {
        return Names.WSA_FROM_QNAME;
    }
    
    /**
     * @return QName of the To addressing header
     */    
    public QName getToQName() {
        return Names.WSA_TO_QNAME;
    }
    
    /**
     * @return QName of the ReplyTo addressing header
     */
    public QName getReplyToQName() {
        return Names.WSA_REPLYTO_QNAME;
    }
    
    /**
     * @return QName of the FaultTo addressing header
     */
    public QName getFaultToQName() {
        return Names.WSA_FAULTTO_QNAME;
    }

    /**
     * @return QName of the Action addressing header
     */
    public QName getActionQName() {
        return Names.WSA_ACTION_QNAME;
    }
    
    /**
     * @return QName of the MessageID addressing header
     */
    public QName getMessageIDQName() {
        return Names.WSA_MESSAGEID_QNAME;
    }
    
    /**
     * @return Default value for RelationshipType indicating a reply
     * to the related message
     */
    public String getRelationshipReply() {
        return Names.WSA_RELATIONSHIP_REPLY;
    }
    
    /**
     * @return QName of the RelatesTo addressing header
     */
    public QName getRelatesToQName() {
        return Names.WSA_RELATESTO_QNAME;
    }
    
    /**
     * @return QName of the Relationship addressing header
     */
    public QName getRelationshipTypeQName() {
        return Names.WSA_RELATIONSHIPTYPE_QNAME;
    }
    
    /**
     * @return QName of the Metadata
     */
    public QName getMetadataQName() {
        return Names.WSA_METADATA_QNAME;
    }
    
    /**
     * @return QName of the Address
     */
    public QName getAddressQName() {
        return Names.WSA_ADDRESS_QNAME;
    }
    
    /**
     * @return package name of the implementation
     */
    public String getPackageName() {
        return PackageUtils.getPackageName(AddressingConstantsImpl.class);
    }
    
    /**
     * @return QName of the reference parameter marker
     */
    public QName getIsReferenceParameterQName() {
        return Names.WSA_IS_REFERENCE_PARAMETER_QNAME;
    }
    
    /**
     * @return QName of the Invalid Message Addressing Property fault subcode
     */
    public QName getInvalidMapQName() {
        return Names.INVALID_MAP_QNAME;
    }
    
    /**
     * @return QName of the Message Addressing Property Required fault subcode
     */
    public QName getMapRequiredQName() {
        return Names.MAP_REQUIRED_QNAME;
    }
    
    /**
     * @return QName of the Destination Unreachable fault subcode
     */
    public QName getDestinationUnreachableQName() {
        return Names.DESTINATION_UNREACHABLE_QNAME;
    }
    
    /**
     * @return QName of the Action Not Supported fault subcode
     */
    public QName getActionNotSupportedQName() {
        return Names.ACTION_NOT_SUPPORTED_QNAME;
    }
    
    /**
     * @return QName of the Endpoint Unavailable fault subcode
     */
    public QName getEndpointUnavailableQName() {
        return Names.ENDPOINT_UNAVAILABLE_QNAME;
    }
    
    /**
     * @return Default Fault Action
     */
    public String getDefaultFaultAction() {
        return Names.WSA_DEFAULT_FAULT_ACTION;
    }
    
    /**
     * @return Action Not Supported text
     */
    public String getActionNotSupportedText() {
        return BUNDLE.getString("ACTION_NOT_SUPPORTED_MSG");
    }

    /**
     * @return Destination Unreachable text
     */
    public String getDestinationUnreachableText() {
        return BUNDLE.getString("DESTINATION_UNREACHABLE_MSG");
    }
    
    /**
     * @return Endpoint Unavailable text
     */
    public String getEndpointUnavailableText() {
        return BUNDLE.getString("ENDPOINT_UNAVAILABLE_MSG");
    }
    
    /**
     * @return Invalid Message Addressing Property text
     */    
    public String getInvalidMapText() {
        return BUNDLE.getString("INVALID_MAP_MSG");
    }
    
    /**
     * @return Message Addressing Property Required text
     */    
    public String getMapRequiredText() {
        return BUNDLE.getString("MAP_REQUIRED_MSG");
    }

    /**
     * @return Duplicate Message ID text
     */
    public String getDuplicateMessageIDText() {
        return BUNDLE.getString("DUPLICATE_MESSAGE_ID_MSG");
    }
}
