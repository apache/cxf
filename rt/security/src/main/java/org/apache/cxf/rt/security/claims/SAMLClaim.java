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

package org.apache.cxf.rt.security.claims;

/**
 * This represents a Claim that is coupled to a SAML Assertion
 */
public class SAMLClaim extends Claim {

    /**
     * This configuration tag specifies the default attribute name where the roles are present
     * The default is "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role".
     */
    public static final String SAML_ROLE_ATTRIBUTENAME_DEFAULT =
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    private static final long serialVersionUID = 5530712294179589442L;

    private String nameFormat;
    private String name;
    private String friendlyName;

    public String getNameFormat() {
        return nameFormat;
    }

    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();

        if (nameFormat != null) {
            hashCode = 31 * hashCode + nameFormat.hashCode();
        }
        if (name != null) {
            hashCode = 31 * hashCode + name.hashCode();
        }
        if (friendlyName != null) {
            hashCode = 31 * hashCode + friendlyName.hashCode();
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof SAMLClaim)) {
            return false;
        }

        if (nameFormat == null && ((SAMLClaim)obj).getNameFormat() != null) {
            return false;
        } else if (nameFormat != null
            && !nameFormat.equals(((SAMLClaim)obj).getNameFormat())) {
            return false;
        }

        if (name == null && ((SAMLClaim)obj).getName() != null) {
            return false;
        } else if (name != null
             && !name.equals(((SAMLClaim)obj).getName())) {
            return false;
        }

        if (friendlyName == null && ((SAMLClaim)obj).getFriendlyName() != null) {
            return false;
        } else if (friendlyName != null
            && !friendlyName.equals(((SAMLClaim)obj).getFriendlyName())) {
            return false;
        }

        return true;
    }
}
