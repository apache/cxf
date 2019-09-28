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
package org.apache.cxf.sts.token.validator;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.interceptor.security.RolePrefixSecurityContextImpl;

/**
 * A default implementation to extract roles from a Subject
 */
public class DefaultSubjectRoleParser implements SubjectRoleParser {

    private String roleClassifier;
    private String roleClassifierType = "prefix";

    /**
     * Return the set of User/Principal roles from the Subject.
     * @param principal the optional Principal
     * @param subject the JAAS Subject
     * @return the set of User/Principal roles from the Subject.
     */
    public Set<Principal> parseRolesFromSubject(Principal principal, Subject subject) {
        if (subject != null) {
            if (roleClassifier != null && !"".equals(roleClassifier)) {
                RolePrefixSecurityContextImpl securityContext =
                    new RolePrefixSecurityContextImpl(subject, roleClassifier, roleClassifierType);
                return securityContext.getUserRoles();
            }
            return new DefaultSecurityContext(principal, subject).getUserRoles();
        }

        return Collections.emptySet();
    }

    public String getRoleClassifier() {
        return roleClassifier;
    }

    /**
     * Set the Subject Role Classifier to use. If this value is not specified, then it tries to
     * get roles from the supplied JAAS Subject (if not null) using the DefaultSecurityContext
     * in cxf-rt-core. Otherwise it uses this value in combination with the
     * SUBJECT_ROLE_CLASSIFIER_TYPE to get the roles from the Subject.
     * @param roleClassifier the Subject Role Classifier to use
     */
    public void setRoleClassifier(String roleClassifier) {
        this.roleClassifier = roleClassifier;
    }

    public String getRoleClassifierType() {
        return roleClassifierType;
    }

    /**
     * Set the Subject Role Classifier Type to use. Currently accepted values are "prefix" or
     * "classname". Must be used in conjunction with the SUBJECT_ROLE_CLASSIFIER. The default
     * value is "prefix".
     * @param roleClassifierType the Subject Role Classifier Type to use
     */
    public void setRoleClassifierType(String roleClassifierType) {
        this.roleClassifierType = roleClassifierType;
    }

}