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

package org.apache.cxf.ext.logging;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.cxf.message.Message;


/**
 * Adds XML/HTML attribute value masking on top of the parent MaskSensitiveHelper.
 * 
 */
public class AttributeMaskingHelper extends MaskSensitiveHelper {

    private static final String ATTR_NAME_TEMPLATE = "-ATTR_NAME-";

    // Re-declare namespace prefix class per Namespaces in XML (private in parent; reproduce here)
    private static final String PATTERN_XML_NAMESPACE_PREFIX = "[\\w.\\-\\u00B7\\u00C0-\\u00D6\\u00D8-\\u00F6"
            + "\\u00F8-\\u02FF\\u0300-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u203F-\\u2040\\u2070-\\u218F"
            + "\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]+";

    // Case-sensitive attribute pattern; supports optional namespace prefix; preserves original quotes
    // Groups: 1=full attr name (w/ optional prefix), 2=open quote, 3=value, 4=close quote (backref to 2)
    private static final String MATCH_PATTERN_XML_ATTR_TEMPLATE =
            "(\\b(?:" + PATTERN_XML_NAMESPACE_PREFIX + ":)?" + ATTR_NAME_TEMPLATE + ")\\s*=\\s*(\"|')(.*?)(\\2)";
    private static final String REPLACEMENT_XML_ATTR_TEMPLATE = "$1=$2XXX$4";

    private static final String XML_CONTENT = "xml";
    private static final String HTML_CONTENT = "html";

    private static class ReplacementPair {
        private final Pattern matchPattern;
        private final String replacement;
        ReplacementPair(String matchPattern, String replacement) {
            
            this.matchPattern = Pattern.compile(matchPattern, Pattern.DOTALL);
            this.replacement = replacement;
        }
    }

    private final Set<ReplacementPair> replacementsXMLAttributes = new HashSet<>();

    /** Adds attribute names to be masked in XML/HTML logs (values replaced with "XXX"). */
    public void addSensitiveAttributeNames(final Set<String> inSensitiveAttirbuteNames) {
        if (inSensitiveAttirbuteNames == null || inSensitiveAttirbuteNames.isEmpty()) {
            return;
        }
        for (final String attr : inSensitiveAttirbuteNames) {
            final String match = MATCH_PATTERN_XML_ATTR_TEMPLATE.replace(ATTR_NAME_TEMPLATE, Pattern.quote(attr));
            final String repl  = REPLACEMENT_XML_ATTR_TEMPLATE.replace(ATTR_NAME_TEMPLATE, escapeForReplacement(attr));
            replacementsXMLAttributes.add(new ReplacementPair(match, repl));
        }
    }

    /** Optional convenience resetter if you want it. */
    public void setSensitiveAttributeNames(final Set<String> inSensitiveAttributeNames) {
        replacementsXMLAttributes.clear();
        addSensitiveAttributeNames(inSensitiveAttributeNames);
    }

    @Override
    public String maskSensitiveElements(final Message message, final String originalLogString) {
        // First, do all base-class masking (elements/JSON/headers)
        String masked = super.maskSensitiveElements(message, originalLogString);
        if (masked == null || message == null) {
            return masked;
        }
        final String contentType = (String) message.get(Message.CONTENT_TYPE);
        if (contentType == null) {
            return masked;
        }
        final String lower = contentType.toLowerCase();
        if (lower.contains(XML_CONTENT) || lower.contains(HTML_CONTENT)) {
            // Then apply attribute-value masking
            return applyMasks(masked, replacementsXMLAttributes);
        }
        return masked;
    }

    // --- helpers (local copy; parent versions are private) ---

    private static String escapeForReplacement(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.replace("\\", "\\\\").replace("$", "\\$");
    }

    private String applyMasks(String input, Set<ReplacementPair> pairs) {
        String out = input;
        for (final ReplacementPair rp : pairs) {
            out = rp.matchPattern.matcher(out).replaceAll(rp.replacement);
        }
        return out;
    }
}
