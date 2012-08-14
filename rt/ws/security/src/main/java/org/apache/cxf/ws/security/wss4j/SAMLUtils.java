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

package org.apache.cxf.ws.security.wss4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.opensaml.common.SAMLVersion;
import org.opensaml.xml.XMLObject;

/**
 * internal SAMLUtils to avoid direct reference to opensaml from WSS4J interceptors.
 */
final class SAMLUtils {
    private SAMLUtils() {
    }
    
    public static List<String> parseRolesInAssertion(Object assertion, String roleAttributeName) {
        if (((AssertionWrapper) assertion).getSamlVersion().equals(SAMLVersion.VERSION_20)) {
            return parseRolesInAssertion(((AssertionWrapper)assertion).getSaml2(), roleAttributeName);
        } else {
            return parseRolesInAssertion(((AssertionWrapper)assertion).getSaml1(), roleAttributeName);
        }
    }
    
    //
    // these methods are moved from previous WSS4JInInterceptor
    //
    private static List<String> parseRolesInAssertion(org.opensaml.saml1.core.Assertion assertion,
            String roleAttributeName) {
        List<org.opensaml.saml1.core.AttributeStatement> attributeStatements = 
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            return null;
        }
        List<String> roles = new ArrayList<String>();
        
        for (org.opensaml.saml1.core.AttributeStatement statement : attributeStatements) {
            
            List<org.opensaml.saml1.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml1.core.Attribute attribute : attributes) {
                
                if (attribute.getAttributeName().equals(roleAttributeName)) {
                    for (XMLObject attributeValue : attribute.getAttributeValues()) {
                        Element attributeValueElement = attributeValue.getDOM();
                        String value = attributeValueElement.getTextContent();
                        roles.add(value);                    
                    }
                    if (attribute.getAttributeValues().size() > 1) {
//                        Don't search for other attributes with the same name if                         
//                        <saml:Attribute xmlns:saml="urn:oasis:names:tc:SAML:1.0:assertion"
//                             AttributeNamespace="http://schemas.xmlsoap.org/claims" AttributeName="roles">
//                        <saml:AttributeValue>Value1</saml:AttributeValue>
//                        <saml:AttributeValue>Value2</saml:AttributeValue>
//                        </saml:Attribute>
                        break;
                    }
                }
                
            }
        }
        return Collections.unmodifiableList(roles);
    }
    

    private static List<String> parseRolesInAssertion(org.opensaml.saml2.core.Assertion assertion,
            String roleAttributeName) {
        List<org.opensaml.saml2.core.AttributeStatement> attributeStatements = 
            assertion.getAttributeStatements();
        if (attributeStatements == null || attributeStatements.isEmpty()) {
            return null;
        }
        List<String> roles = new ArrayList<String>();
        
        for (org.opensaml.saml2.core.AttributeStatement statement : attributeStatements) {
            
            List<org.opensaml.saml2.core.Attribute> attributes = statement.getAttributes();
            for (org.opensaml.saml2.core.Attribute attribute : attributes) {
                
                if (attribute.getName().equals(roleAttributeName)) {
                    for (XMLObject attributeValue : attribute.getAttributeValues()) {
                        Element attributeValueElement = attributeValue.getDOM();
                        String value = attributeValueElement.getTextContent();
                        roles.add(value);                    
                    }
                    if (attribute.getAttributeValues().size() > 1) {
//                        Don't search for other attributes with the same name if                         
//                        <saml:Attribute xmlns:saml="urn:oasis:names:tc:SAML:1.0:assertion"
//                             AttributeNamespace="http://schemas.xmlsoap.org/claims" AttributeName="roles">
//                        <saml:AttributeValue>Value1</saml:AttributeValue>
//                        <saml:AttributeValue>Value2</saml:AttributeValue>
//                        </saml:Attribute>
                        break;
                    }
                }
                
            }
        }
        return Collections.unmodifiableList(roles);
    }

}
