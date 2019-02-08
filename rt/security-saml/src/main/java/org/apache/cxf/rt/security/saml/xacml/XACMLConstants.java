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

package org.apache.cxf.rt.security.saml.xacml;



/**
 * XACML 1.x and 2.0 Constants.
 */
public final class XACMLConstants {

    //
    // Attributes
    //

    public static final String CURRENT_TIME =
        "urn:oasis:names:tc:xacml:1.0:environment:current-time";
    public static final String CURRENT_DATE =
        "urn:oasis:names:tc:xacml:1.0:environment:current-date";
    public static final String CURRENT_DATETIME =
        "urn:oasis:names:tc:xacml:1.0:environment:current-dateTime";

    //
    // Identifiers
    //

    public static final String SUBJECT_DNS_NAME =
        "urn:oasis:names:tc:xacml:1.0:subject:authn-locality:dns-name";
    public static final String SUBJECT_IP_ADDR =
        "urn:oasis:names:tc:xacml:1.0:subject:authn-locality:ip-address";
    public static final String SUBJECT_AUTHN_METHOD =
        "urn:oasis:names:tc:xacml:1.0:subject:authentication-method";
    public static final String SUBJECT_AUTHN_TIME =
        "urn:oasis:names:tc:xacml:1.0:subject:authentication-time";
    public static final String SUBJECT_KEY_INFO =
        "urn:oasis:names:tc:xacml:1.0:subject:key-info";
    public static final String SUBJECT_REQ_TIME =
        "urn:oasis:names:tc:xacml:1.0:subject:request-time";
    public static final String SUBJECT_START_TIME =
        "urn:oasis:names:tc:xacml:1.0:subject:session-start-time";
    public static final String SUBJECT_ID =
        "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
    public static final String SUBJECT_ID_QUALIFIER =
        "urn:oasis:names:tc:xacml:1.0:subject:subject-id-qualifier";
    public static final String SUBJECT_CAT_ACCESS_SUBJECT =
        "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
    public static final String SUBJECT_CAT_CODEBASE =
        "urn:oasis:names:tc:xacml:1.0:subject-category:codebase";
    public static final String SUBJECT_CAT_INTERMED_SUBJECT =
        "urn:oasis:names:tc:xacml:1.0:subject-category:intermediary-subject";
    public static final String SUBJECT_CAT_REC_SUBJECT =
        "urn:oasis:names:tc:xacml:1.0:subject-category:recipient-subject";
    public static final String SUBJECT_CAT_REQ_MACHINE =
        "urn:oasis:names:tc:xacml:1.0:subject-category:requesting-machine";
    public static final String RESOURCE_LOC =
        "urn:oasis:names:tc:xacml:1.0:resource:resource-location";
    public static final String RESOURCE_ID =
        "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    // Non-standard (CXF-specific) tags for sending information about SOAP services to the PDP
    public static final String RESOURCE_WSDL_OPERATION_ID =
        "urn:cxf:apache:org:wsdl:operation-id";
    public static final String RESOURCE_WSDL_SERVICE_ID =
        "urn:cxf:apache:org:wsdl:service-id";
    public static final String RESOURCE_WSDL_ENDPOINT =
        "urn:cxf:apache:org:wsdl:endpoint";

    public static final String RESOURCE_FILE_NAME =
        "urn:oasis:names:tc:xacml:1.0:resource:simple-file-name";
    public static final String ACTION_ID =
        "urn:oasis:names:tc:xacml:1.0:action:action-id";
    public static final String ACTION_IMPLIED =
        "urn:oasis:names:tc:xacml:1.0:action:implied-action";
    public static final String SUBJECT_ROLE =
        "urn:oasis:names:tc:xacml:2.0:subject:role";


    //
    // Datatypes
    //

    public static final String XS_STRING =
        "http://www.w3.org/2001/XMLSchema#string";
    public static final String XS_BOOLEAN =
        "http://www.w3.org/2001/XMLSchema#boolean";
    public static final String XS_INT =
        "http://www.w3.org/2001/XMLSchema#integer";
    public static final String XS_DOUBLE =
        "http://www.w3.org/2001/XMLSchema#double";
    public static final String XS_TIME =
        "http://www.w3.org/2001/XMLSchema#time";
    public static final String XS_DATE =
        "http://www.w3.org/2001/XMLSchema#date";
    public static final String XS_DATETIME =
        "http://www.w3.org/2001/XMLSchema#dateTime";
    public static final String XS_ANY_URI =
        "http://www.w3.org/2001/XMLSchema#anyURI";
    public static final String XS_HEX =
        "http://www.w3.org/2001/XMLSchema#hexBinary";
    public static final String XS_BASE64 =
        "http://www.w3.org/2001/XMLSchema#base64Binary";
    public static final String RFC_822_NAME =
        "urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name";
    public static final String X500_NAME =
        "urn:oasis:names:tc:xacml:1.0:data-type:x500Name";

    //
    // Functions
    //
    public static final String FUNC_STRING_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:string-equal";
    public static final String FUNC_BOOL_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:boolean-equal";
    public static final String FUNC_INT_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:integer-equal";
    public static final String FUNC_DOUBLE_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:double-equal";
    public static final String FUNC_DATE_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:date-equal";
    public static final String FUNC_TIME_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:time-equal";
    public static final String FUNC_DATETIME_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:dateTime-equal";
    public static final String FUNC_ANY_URI_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:anyURI-equal";
    public static final String FUNC_X500_NAME_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:x500Name-equal";
    public static final String FUNC_RFC_822_NAME_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:rfc822Name-equal";
    public static final String FUNC_HEX_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:hexBinary-equal";
    public static final String FUNC_BASE64_EQUAL =
        "urn:oasis:names:tc:xacml:1.0:function:base64Binary-equal";

    public static final String FUNC_INT_GT =
        "urn:oasis:names:tc:xacml:1.0:function:integer-greater-than";
    public static final String FUNC_INT_GTE =
        "urn:oasis:names:tc:xacml:1.0:function:integer-greater-than-or-equal";
    public static final String FUNC_INT_LT =
        "urn:oasis:names:tc:xacml:1.0:function:integer-less-than";
    public static final String FUNC_INT_LTE =
        "urn:oasis:names:tc:xacml:1.0:function:integer-less-than-or-equal";
    public static final String FUNC_DOUBLE_GT =
        "urn:oasis:names:tc:xacml:1.0:function:double-greater-than";
    public static final String FUNC_DOUBLE_GTE =
        "urn:oasis:names:tc:xacml:1.0:function:double-greater-than-or-equal";
    public static final String FUNC_DOUBLE_LT =
        "urn:oasis:names:tc:xacml:1.0:function:double-less-than";
    public static final String FUNC_DOUBLE_LTE =
        "urn:oasis:names:tc:xacml:1.0:function:double-less-than-or-equal";

    public static final String FUNC_STRING_GT =
        "urn:oasis:names:tc:xacml:1.0:function:string-greater-than";
    public static final String FUNC_STRING_GTE =
        "urn:oasis:names:tc:xacml:1.0:function:string-greater-than-or-equal";
    public static final String FUNC_STRING_LT =
        "urn:oasis:names:tc:xacml:1.0:function:string-less-than";
    public static final String FUNC_STRING_LTE =
        "urn:oasis:names:tc:xacml:1.0:function:string-less-than-or-equal";
    public static final String FUNC_TIME_GT =
        "urn:oasis:names:tc:xacml:1.0:function:time-greater-than";
    public static final String FUNC_TIME_GTE =
        "urn:oasis:names:tc:xacml:1.0:function:time-greater-than-or-equal";
    public static final String FUNC_TIME_LT =
        "urn:oasis:names:tc:xacml:1.0:function:time-less-than";
    public static final String FUNC_TIME_LTE =
        "urn:oasis:names:tc:xacml:1.0:function:time-less-than-or-equal";
    public static final String FUNC_DATETIME_GT =
        "urn:oasis:names:tc:xacml:1.0:function:dateTime-greater-than";
    public static final String FUNC_DATETIME_GTE =
        "urn:oasis:names:tc:xacml:1.0:function:dateTime-greater-than-or-equal";
    public static final String FUNC_DATETIME_LT =
        "urn:oasis:names:tc:xacml:1.0:function:dateTime-less-than";
    public static final String FUNC_DATETIME_LTE =
        "urn:oasis:names:tc:xacml:1.0:function:dateTime-less-than-or-equal";
    public static final String FUNC_DATE_GT =
        "urn:oasis:names:tc:xacml:1.0:function:date-greater-than";
    public static final String FUNC_DATE_GTE =
        "urn:oasis:names:tc:xacml:1.0:function:date-greater-than-or-equal";
    public static final String FUNC_DATE_LT =
        "urn:oasis:names:tc:xacml:1.0:function:date-less-than";
    public static final String FUNC_DATE_LTE =
        "urn:oasis:names:tc:xacml:1.0:function:date-less-than-or-equal";


    private XACMLConstants() {
        // complete
    }
}
