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
package org.apache.cxf.sts.claims;

import java.net.URI;

public final class ClaimTypes {
    /**
     * The base XML namespace URI that is used by the claim types
     * http://docs.oasis-open.org/imi/identity/v1.0/os/identity-1.0-spec-os.pdf
     */
    public static final URI URI_BASE =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims");

    /**
     * (givenName in [RFC 2256]) Preferred name or first name of a Subject.
     * According to RFC 2256: This attribute is used to hold the part of a person's name
     * which is not their surname nor middle name.
     */
    public static final URI FIRSTNAME =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname");

    /**
     * (sn in [RFC 2256]) Surname or family name of a Subject.
     * According to RFC 2256: This is the X.500 surname attribute which contains the family name of a person.
     */
    public static final URI LASTNAME =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname");

    /**
     * (mail in inetOrgPerson) Preferred address for the "To:" field of email
     * to be sent to the Subject, usually of the form <user>@<domain>.
     * According to inetOrgPerson using [RFC 1274]: This attribute type specifies
     * an electronic mailbox attribute following the syntax specified in RFC 822.
     */
    public static final URI EMAILADDRESS =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");

    /**
     * (street in [RFC 2256]) Street address component of a Subject‟s address information.
     * According to RFC 2256: This attribute contains the physical address of the object
     * to which the entry corresponds, such as an address for package delivery.
     */
    public static final URI STREETADDRESS =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/streetaddress");

    /**
     * (/ in [RFC 2256]) Locality component of a Subject's address information.
     * According to RFC 2256: This attribute contains the name of a locality, such as a city, county or other
     * geographic region.
     */
    public static final URI LOCALITY =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/locality");

    /**
     * (st in [RFC 2256]) Abbreviation for state or province name of a Subject's address information.
     * According to RFC 2256: “This attribute contains the full name of a state or province.
     * The values SHOULD be coordinated on a national level and if well-known shortcuts exist.
     */
    public static final URI STATE_PROVINCE =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/stateorprovince");

    /**
     * (postalCode in X.500) Postal code or zip code component of a Subject's address information.
     * According to X.500(2001): The postal code attribute type specifies the postal code of the named
     * object.
     */
    public static final URI POSTALCODE =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/postalcode");

    /**
     * (c in [RFC 2256]) Country of a Subject.
     * According to RFC 2256: This attribute contains a two-letter ISO 3166 country code.
     */
    public static final URI COUNTRY =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/country");

    /**
     * (homePhone in inetOrgPerson) Primary or home telephone number of a Subject.
     * According to inetOrgPerson using [RFC 1274]: This attribute type specifies a home telephone number
     * associated with a person.
     */
    public static final URI HOMEPHONE =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/homephone");

    /**
     * (telephoneNumber in X.500 Person) Secondary or work telephone number of a Subject.
     * According to X.500(2001): This attribute type specifies an office/campus telephone number associated
     * with a person.
     */
    public static final URI OTHERPHONE =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/otherphone");

    /**
     * (mobile in inetOrgPerson) Mobile telephone number of a Subject.
     * According to inetOrgPerson using [RFC 1274]: This attribute type specifies a mobile telephone number
     * associated with a person.
     */
    public static final URI MOBILEPHONE =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mobilephone");

    /**
     * The date of birth of a Subject in a form allowed by the xs:date data type.
     */
    public static final URI DATEOFBIRTH =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/dateofbirth");

    /**
     * Gender of a Subject that can have any of these exact URI values
     *   '0' (meaning unspecified), '1' (meaning Male) or '2' (meaning Female)
     */
    public static final URI GENDER =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/gender");

    /**
     * A private personal identifier (PPID) that identifies the Subject to a Relying Party.
     */
    public static final URI PRIVATE_PERSONAL_IDENTIFIER =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier");

    /**
     * The Web page of a Subject expressed as a URL.
     */
    public static final URI WEB_PAGE =
        URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/webpage");

    private ClaimTypes() {
        // complete
    }
}
