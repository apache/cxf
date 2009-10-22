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
 
package org.apache.cxf.transport.https;

import java.util.List;

import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.configuration.security.CombinatorType;
import org.apache.cxf.configuration.security.DNConstraintsType;

/**
 * A set of static methods that operate on the generated CertificateConstraintsType.
 */
public final class CertConstraintsJaxBUtils {
    
    private CertConstraintsJaxBUtils() {
        // complete
    }
    
    /**
     * Create a CertConstraints object from a JAXB CertificateConstraintsType
     */
    public static CertConstraints createCertConstraints(
        CertificateConstraintsType certConstraints
    ) {
        List<String> subjectRegexps = getSubjectConstraints(certConstraints);
        CertConstraints.Combinator subjectCombinator = 
            getSubjectConstraintsCombinator(certConstraints);
        List<String> issuerRegexps = getIssuerConstraints(certConstraints);
        CertConstraints.Combinator issuerCombinator =
            getIssuerConstraintsCombinator(certConstraints);

        return new CertConstraints(
            subjectRegexps, subjectCombinator, issuerRegexps, issuerCombinator);
    }
    
    /**
     * Get a List of Strings that corresponds to the subject regular expression
     * constraints from a JAXB CertificateConstraintsType
     */
    public static List<String> getSubjectConstraints(
        CertificateConstraintsType certConstraints
    ) {
        if (certConstraints != null && certConstraints.isSetSubjectDNConstraints()) {
            DNConstraintsType constraints = certConstraints.getSubjectDNConstraints();
            return constraints.getRegularExpression();
        }
        return java.util.Collections.emptyList();
    }
    
    /**
     * Get a List of Strings that corresponds to the issuer regular expression
     * constraints from a JAXB CertificateConstraintsType
     */
    public static List<String> getIssuerConstraints(
        CertificateConstraintsType certConstraints
    ) {
        if (certConstraints != null && certConstraints.isSetIssuerDNConstraints()) {
            DNConstraintsType constraints = certConstraints.getIssuerDNConstraints();
            return constraints.getRegularExpression();
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Get a (subject) CertConstrains.Combinator from a JAXB CertificateConstraintsType
     */
    public static CertConstraints.Combinator getSubjectConstraintsCombinator(
        CertificateConstraintsType certConstraints
    ) {
        if (certConstraints != null && certConstraints.isSetSubjectDNConstraints()) {
            DNConstraintsType constraints = certConstraints.getSubjectDNConstraints();
            if (constraints != null && constraints.isSetCombinator()) {
                CombinatorType combinator = constraints.getCombinator();
                if (combinator == CombinatorType.ANY) {
                    return CertConstraints.Combinator.ANY;
                }
            }
        }
        return CertConstraints.Combinator.ALL;
    }
    
    /**
     * Get a (issuer) CertConstrains.Combinator from a JAXB CertificateConstraintsType
     */
    public static CertConstraints.Combinator getIssuerConstraintsCombinator(
        CertificateConstraintsType certConstraints
    ) {
        if (certConstraints != null && certConstraints.isSetIssuerDNConstraints()) {
            DNConstraintsType constraints = certConstraints.getIssuerDNConstraints();
            if (constraints != null && constraints.isSetCombinator()) {
                CombinatorType combinator = constraints.getCombinator();
                if (combinator == CombinatorType.ANY) {
                    return CertConstraints.Combinator.ANY;
                }
            }
        }
        return CertConstraints.Combinator.ALL;
    }
    
    
}
