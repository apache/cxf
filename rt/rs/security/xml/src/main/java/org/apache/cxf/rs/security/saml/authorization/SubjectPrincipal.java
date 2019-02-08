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

import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.rs.security.saml.assertion.Subject;

public class SubjectPrincipal extends SimplePrincipal {
    private static final long serialVersionUID = 1L;

    private Subject subject;
    public SubjectPrincipal(String principalName, Subject subject) {
        super(principalName);
        this.subject = subject;
    }

    public Subject getSubject() {
        return subject;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof SubjectPrincipal)) {
            return false;
        }

        if (subject == null && ((SubjectPrincipal)obj).getSubject() != null) {
            return false;
        } else if (subject != null
            && !subject.equals(((SubjectPrincipal)obj).getSubject())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        if (subject != null) {
            hashCode = 31 * hashCode + subject.hashCode();
        }

        return hashCode;

    }
}
