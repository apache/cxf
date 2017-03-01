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

package org.apache.cxf.rt.security.saml.xacml2;

import java.util.UUID;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.xacml.XACMLObjectBuilder;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.profile.saml.SAMLProfileConstants;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionQueryType;

/**
 * A set of utility methods to construct XACML SAML Request statements, based on the
 * SAML 2.0 profile of XACML v2.0 specification.
 */
public final class SamlRequestComponentBuilder {
    private static volatile XACMLObjectBuilder<XACMLAuthzDecisionQueryType> xacmlAuthzDecisionQueryTypeBuilder;

    private static volatile SAMLObjectBuilder<Issuer> issuerBuilder;

    private static volatile XMLObjectBuilderFactory builderFactory =
        XMLObjectProviderRegistrySupport.getBuilderFactory();

    private SamlRequestComponentBuilder() {
        // complete
    }

    /**
     * Create an AuthzDecisionQuery using the defaults
     */
    public static XACMLAuthzDecisionQueryType createAuthzDecisionQuery(
        String issuerValue,
        RequestType request,
        String namespace
    ) {
        return createAuthzDecisionQuery(false, false, issuerValue, request, namespace);
    }

    @SuppressWarnings("unchecked")
    public static XACMLAuthzDecisionQueryType createAuthzDecisionQuery(
        boolean inputContextOnly,
        boolean returnContext,
        String issuerValue,
        RequestType request,
        String namespace
    ) {
        if (xacmlAuthzDecisionQueryTypeBuilder == null) {
            xacmlAuthzDecisionQueryTypeBuilder = (XACMLObjectBuilder<XACMLAuthzDecisionQueryType>)
                builderFactory.getBuilder(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML20);
        }
        XACMLAuthzDecisionQueryType authzQuery =
            xacmlAuthzDecisionQueryTypeBuilder.buildObject(
                namespace,
                XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_LOCAL_NAME,
                SAMLProfileConstants.SAML20XACMLPROTOCOL_PREFIX
            );
        authzQuery.setID("_" + UUID.randomUUID().toString());
        authzQuery.setVersion(SAMLVersion.VERSION_20);
        authzQuery.setIssueInstant(new DateTime());
        authzQuery.setInputContextOnly(Boolean.valueOf(inputContextOnly));
        authzQuery.setReturnContext(Boolean.valueOf(returnContext));

        if (issuerValue != null) {
            Issuer issuer = createIssuer(issuerValue);
            authzQuery.setIssuer(issuer);
        }

        authzQuery.setRequest(request);

        return authzQuery;
    }


    /**
     * Create an Issuer object
     *
     * @param issuerValue of type String
     * @return an Issuer object
     */
    @SuppressWarnings("unchecked")
    public static Issuer createIssuer(String issuerValue) {
        if (issuerBuilder == null) {
            issuerBuilder = (SAMLObjectBuilder<Issuer>)
                builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);

        }
        Issuer issuer = issuerBuilder.buildObject();
        //
        // The SAML authority that is making the claim(s) in the assertion. The issuer SHOULD
        // be unambiguous to the intended relying parties.
        issuer.setValue(issuerValue);
        return issuer;
    }

}
