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
package org.apache.cxf.rs.security.saml.authorization;

import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.saml.SAMLUtils;
import org.apache.cxf.rs.security.saml.assertion.Claim;
import org.apache.cxf.rs.security.saml.assertion.Claims;
import org.apache.cxf.rs.security.saml.assertion.Subject;
import org.apache.cxf.security.SecurityContext;
import org.apache.ws.security.saml.ext.AssertionWrapper;

public class SecurityContextProviderImpl implements SecurityContextProvider {

    private static final String DEFAULT_NAME_ROLE_PROPERTY = "org.apache.cxf.saml.claims.role";
    private static final String DEFAULT_NAMEFORMAT_PROPERTY = "org.apache.cxf.saml.claims.format";
    
    public SecurityContext getSecurityContext(Message message,
            AssertionWrapper wrapper) {
        Claims claims = getClaims(wrapper);
        Subject subject = getSubject(message, wrapper, claims);
        
        String defaultName = (String)message.getContextualProperty(DEFAULT_NAME_ROLE_PROPERTY);
        String defaultNameFormat = (String)message.getContextualProperty(DEFAULT_NAMEFORMAT_PROPERTY);
        SecurityContext sc = new SAMLSecurityContext(new SubjectPrincipal(subject),
                claims,
                defaultName == null ? Claim.DEFAULT_ROLE_NAME : defaultName,
                defaultNameFormat == null ? Claim.DEFAULT_NAME_FORMAT : defaultNameFormat);
        return sc;
    }

    protected Claims getClaims(AssertionWrapper wrapper) {
        return SAMLUtils.getClaims(wrapper);
    }
    
    protected Subject getSubject(Message message, AssertionWrapper wrapper, Claims claims) {
        Subject subj = SAMLUtils.getSubject(message, wrapper);
        setSubjectPrincipalName(subj, claims);
        return subj;
    }
    
    protected void setSubjectPrincipalName(Subject sub, Claims claims) {
        // parse/decipher subject name id, or check attributes like 
        // givenName, email, firstName, etc
        
        // this can be overridden, but consider also introducing dedicated handlers 
    }
}
