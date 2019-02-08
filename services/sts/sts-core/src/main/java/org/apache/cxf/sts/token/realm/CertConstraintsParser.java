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

package org.apache.cxf.sts.token.realm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.cxf.common.logging.LogUtils;

/**
 * This class provides the functionality to match a given X509Certificate against a list of
 * regular expressions.
 */
public class CertConstraintsParser {
    private static final Logger LOG = LogUtils.getL7dLogger(CertConstraintsParser.class);

    /**
     * a collection of compiled regular expression patterns for the subject DN
     */
    private Collection<Pattern> subjectDNPatterns = new ArrayList<>();

    /**
     * Set a list of Strings corresponding to regular expression constraints on the subject DN
     * of a certificate
     */
    public void setSubjectConstraints(List<String> constraints) {
        if (constraints != null) {
            subjectDNPatterns = new ArrayList<>();
            for (String constraint : constraints) {
                try {
                    subjectDNPatterns.add(Pattern.compile(constraint.trim()));
                } catch (PatternSyntaxException ex) {
                    LOG.severe(ex.getMessage());
                    throw ex;
                }
            }
        }
    }

    public Collection<Pattern> getCompiledSubjectContraints() {
        return subjectDNPatterns;
    }

    /**
     * @return      true if the certificate's SubjectDN matches the constraints defined in the
     *              subject DNConstraints; false, otherwise. The certificate subject DN only
     *              has to match ONE of the subject cert constraints (not all).
     */
    public boolean matches(
        final java.security.cert.X509Certificate cert
    ) {
        if (!subjectDNPatterns.isEmpty()) {
            if (cert == null) {
                LOG.fine("The certificate is null so no constraints matching was possible");
                return false;
            }
            String subjectName = cert.getSubjectX500Principal().getName();
            boolean subjectMatch = false;
            for (Pattern subjectDNPattern : subjectDNPatterns) {
                final Matcher matcher = subjectDNPattern.matcher(subjectName);
                if (matcher.matches()) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Subject DN " + subjectName + " matches with pattern " + subjectDNPattern);
                    }
                    subjectMatch = true;
                    break;
                }
            }
            if (!subjectMatch) {
                return false;
            }
        }

        return true;
    }
}
