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
package org.apache.cxf.systest.ws.wssec10.server;

import javax.security.auth.Subject;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.security.SimpleGroup;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.UsernameTokenInterceptor;
import org.apache.wss4j.common.util.UsernameTokenUtil;
import org.apache.xml.security.utils.XMLUtils;

public class CustomUsernameTokenInterceptor extends UsernameTokenInterceptor {

    protected Subject createSubject(String name,
                                    String password,
                                    boolean isDigest,
                                    String nonce,
                                    String created) throws SecurityException {
        Subject subject = new Subject();

        // delegate to the external security system if possible

        // authenticate the user somehow
        subject.getPrincipals().add(new SimplePrincipal(name));

        // add roles this user is in
        String roleName = "Alice".equals(name) ? "developers" : "pms";
        try {
            String expectedPassword = "Alice".equals(name) ? "ecilA"
                : UsernameTokenUtil.doPasswordDigest(XMLUtils.decode(nonce), created, "invalid-password");
            if (!password.equals(expectedPassword)) {
                throw new SecurityException("Wrong Password");
            }
        } catch (org.apache.wss4j.common.ext.WSSecurityException ex) {
            throw new SecurityException("Wrong Password");
        }

        subject.getPrincipals().add(new SimpleGroup(roleName, name));
        subject.setReadOnly();
        return subject;
    }

    public void handleMessage(SoapMessage message) throws Fault {
        message.put(SecurityConstants.VALIDATE_TOKEN, Boolean.FALSE);
        super.handleMessage(message);
    }

    //  or, if needed

    // protected WSUsernameTokenPrincipal getPrincipal(Element tokenElement, SoapMessage message)
    //    throws WSSecurityException {
    //    return super.parseTokenAndCreatePrincipal(tokenElement);
    //}


}


