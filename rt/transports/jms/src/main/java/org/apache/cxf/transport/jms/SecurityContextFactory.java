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
package org.apache.cxf.transport.jms;

import java.security.Principal;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.apache.cxf.security.SecurityContext;

public final class SecurityContextFactory {
    
    private SecurityContextFactory() {
    }

    /**
     * Extract the property JMSXUserID or JMS_TIBCO_SENDER from the jms message and
     * create a SecurityContext from it.
     * For more info see Jira Issue CXF-2055
     * {@link https://issues.apache.org/jira/browse/CXF-2055}
     *
     * @param message jms message to retrieve user information from
     * @return SecurityContext that contains the user of the producer of the message as the Principal
     * @throws JMSException if something goes wrong
     */
    public static SecurityContext buildSecurityContext(Message message,
                                                       JMSConfiguration config) throws JMSException {
        String tempUserName = message.getStringProperty("JMSXUserID");
        if (tempUserName == null && config.isJmsProviderTibcoEms()) {
            tempUserName = message.getStringProperty("JMS_TIBCO_SENDER");
        }
        if (tempUserName == null) {
            return null;
        }
        final String jmsUserName = tempUserName;
    
        final Principal principal = new Principal() {
            public String getName() {
                return jmsUserName;
            }
    
        };
    
        return new SecurityContext() {
    
            public Principal getUserPrincipal() {
                return principal;
            }
    
            public boolean isUserInRole(String role) {
                return false;
            }
    
        };
    }

}
