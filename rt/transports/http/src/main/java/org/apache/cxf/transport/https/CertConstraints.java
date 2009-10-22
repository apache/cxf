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

import java.util.ArrayList;
import java.util.Collection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a set of constraints that can be placed on an X.509 certificate,
 * in the form of a regular expression on a SubjectDN or IssuerDN.
 *
 * A CertConstraints object is initialized using a CertificateConstraintsType, which has
 * a definition in schema and is so generated.
 */
public class CertConstraints {
    
    public enum Combinator { ANY, ALL };

    private static class DNConstraints {
        
        /**
         * the matching combinator (ANY or ALL)
         */
        private final Combinator combinator;
        
        /**
         * a collection of compiled regular expression patterns
         */
        private final Collection<Pattern> dnPatterns = new ArrayList<java.util.regex.Pattern>();
        
        /**
         * Creates a DNConstraints from a list of Strings
         */
        DNConstraints(
            final java.util.List<String> patterns,
            final Combinator patternCombinator
        ) throws java.util.regex.PatternSyntaxException {
            if (patterns == null) {
                combinator = Combinator.ALL;
                return;
            }
            combinator = patternCombinator;
            for (String expression : patterns) {
                dnPatterns.add(Pattern.compile(expression));
            }
        }
        
        /**
         * @return      true if the DN name matches all patterns in the DNConstraints,
         *              if the combinator is ALL, or any such pattern, if the combinator is
         *              ANY.  Note that if the combinator is ALL and the list of patterns
         *              is empty, then any dn will match (by definition of the universal quantifier)
         */
        final boolean
        matches(
            final javax.security.auth.x500.X500Principal dn
        ) {
            boolean atLeastOnePatternMatches = false;
            boolean atLeastOnePatternDoesNotMatch = false;
            //
            // try matching dn against the patterns in this class
            //
            for (Pattern dnPattern : dnPatterns) {
                final Matcher matcher = dnPattern.matcher(dn.getName());
                if (matcher.matches()) {
                    atLeastOnePatternMatches = true;
                    if (combinator == Combinator.ANY) {
                        break;
                    }
                } else {
                    atLeastOnePatternDoesNotMatch = true;
                    if (combinator == Combinator.ALL) {
                        break;
                    }
                }
            }
            //
            // check combinator logic
            //
            switch (combinator) {
            case ALL:
                return !atLeastOnePatternDoesNotMatch;
            case ANY:
                return atLeastOnePatternMatches;
            default:
                throw new RuntimeException("LOGIC ERROR: Unreachable code");
            }
        }
    }
    
    /**
     * The DNConstraints on the SubjectDN
     */
    private final DNConstraints subjectDNConstraints;
    
    /**
     * The DNConstraints on the IssuerDN
     */
    private final DNConstraints issuerDNConstraints;
    
    /**
     * Create a CertificateConstraints from a CertificateConstraintsType specification
     */
    public
    CertConstraints(
        final java.util.List<String> subjectConstraints,
        final Combinator subjectConstraintsCombinator,
        final java.util.List<String> issuerConstraints,
        final Combinator issuerConstraintsCombinator
    ) throws java.util.regex.PatternSyntaxException {
        this.subjectDNConstraints = 
            new DNConstraints(subjectConstraints, subjectConstraintsCombinator);
        this.issuerDNConstraints = 
            new DNConstraints(issuerConstraints, issuerConstraintsCombinator);
    }
    
    /**
     * @return      true if the certificate's SubjectDN matches the constraints defined in the
     *              subject DNConstraints and the certificate's IssuerDN matches the issuer
     *              DNConstraints; false, otherwise
     */
    public boolean
    matches(
        final java.security.cert.X509Certificate cert
    ) {
        return 
            this.subjectDNConstraints.matches(cert.getSubjectX500Principal())
            && this.issuerDNConstraints.matches(cert.getIssuerX500Principal());
    }
}
