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
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.cxf.message.Message;

public class MaskSensitiveHelper {
    private static final String ATTR_NAME_TEMPLATE = "-ATTR_NAME-";
    private static final String ELEMENT_NAME_TEMPLATE = "-ELEMENT_NAME-";
    // see https://www.w3.org/TR/REC-xml-names/#NT-NCName for allowed chars in namespace prefix
    private static final String PATTERN_XML_NAMESPACE_PREFIX = "[\\w.\\-\\u00B7\\u00C0-\\u00D6\\u00D8-\\u00F6"
            + "\\u00F8-\\u02FF\\u0300-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u203F-\\u2040\\u2070-\\u218F"
            + "\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]+";
    private static final String MATCH_PATTERN_XML_TEMPLATE =
        "(<(" + PATTERN_XML_NAMESPACE_PREFIX + ":)?-ELEMENT_NAME-\\b(?:(?!/>)" 
            + "[^>])*?>)(.*?)(</(" + PATTERN_XML_NAMESPACE_PREFIX + ":)?-ELEMENT_NAME->)";
    private static final String REPLACEMENT_XML_TEMPLATE = "$1XXX$4";
    private static final String MATCH_PATTERN_JSON_TEMPLATE_ARRAY 
        = "\"-ELEMENT_NAME-\"[ \\t]*:[ \\t]*[\\[]((\\s*\".\"),?)+[\\]]";
    private static final String REPLACEMENT_JSON_TEMPLATE_ARRAY 
        = "\"-ELEMENT_NAME-\": [\"X\",\"X\",\"X\"]";
    private static final String MATCH_PATTERN_JSON_TEMPLATE = "\"-ELEMENT_NAME-\"[ \\t]*:[ \\t]*\"(.*?)\"";
    private static final String REPLACEMENT_JSON_TEMPLATE = "\"-ELEMENT_NAME-\": \"XXX\"";
    private static final String MASKED_HEADER_VALUE = "XXX";
    
    // Case-sensitive attribute pattern; supports optional namespace prefix; preserves original quotes
    // Groups: 1=full attr name (w/ optional prefix), 2=open quote, 3=value, 4=close quote (backref to 2)
    private static final String MATCH_PATTERN_XML_ATTR_TEMPLATE =
            "(\\b(?:" + PATTERN_XML_NAMESPACE_PREFIX + ":)?" + ATTR_NAME_TEMPLATE + ")\\s*=\\s*(\"|')(.*?)(\\2)";
    private static final String REPLACEMENT_XML_ATTR_TEMPLATE = "$1=$2XXX$4";

    private static final String XML_CONTENT = "xml";
    private static final String HTML_CONTENT = "html";
    private static final String JSON_CONTENT = "json";

    private static class ReplacementPair {
        private final Pattern matchPattern;
        private final String replacement;

        ReplacementPair(String matchPattern, String replacement) {
            this.matchPattern = Pattern.compile(matchPattern, Pattern.DOTALL);
            this.replacement = replacement;
        }
    }

    private final Set<ReplacementPair> replacementsXMLElements = new HashSet<>();
    private final Set<ReplacementPair> replacementsJSON = new HashSet<>();
    private final Set<ReplacementPair> replacementsXMLAttributes = new HashSet<>();

    public void setSensitiveElementNames(final Set<String> inSensitiveElementNames) {
        replacementsXMLElements.clear();
        replacementsJSON.clear();
        addSensitiveElementNames(inSensitiveElementNames);
    }

    public void addSensitiveElementNames(final Set<String> inSensitiveElementNames) {
        for (final String sensitiveName : inSensitiveElementNames) {
            addReplacementPair(MATCH_PATTERN_XML_TEMPLATE, REPLACEMENT_XML_TEMPLATE,
                sensitiveName, replacementsXMLElements);
            addReplacementPair(MATCH_PATTERN_JSON_TEMPLATE_ARRAY, REPLACEMENT_JSON_TEMPLATE_ARRAY,
                sensitiveName, replacementsJSON);
            addReplacementPair(MATCH_PATTERN_JSON_TEMPLATE, REPLACEMENT_JSON_TEMPLATE,
                sensitiveName, replacementsJSON);
        }
    }
    
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

    private void addReplacementPair(final String matchPatternTemplate,
                                    final String replacementTemplate,
                                    final String sensitiveName,
                                    final Set<ReplacementPair> replacements) {
        final String matchPatternXML = matchPatternTemplate.replaceAll(ELEMENT_NAME_TEMPLATE, sensitiveName);
        final String replacementXML = replacementTemplate.replaceAll(ELEMENT_NAME_TEMPLATE, sensitiveName);
        replacements.add(new ReplacementPair(matchPatternXML, replacementXML));
    }

    public String maskSensitiveElements(
            final Message message,
            final String originalLogString) {
        if (replacementsXMLElements.isEmpty() && replacementsJSON.isEmpty()
            && replacementsXMLAttributes.isEmpty()
                || originalLogString == null || message == null) {
            return originalLogString;
        }
        final String contentType = (String) message.get(Message.CONTENT_TYPE);
        if (contentType == null) {
            return originalLogString;
        }
        final String lowerCaseContentType = contentType.toLowerCase();
        if (lowerCaseContentType.contains(XML_CONTENT)
                || lowerCaseContentType.contains(HTML_CONTENT)) {
            String replacedElement = applyMasks(originalLogString, replacementsXMLElements);
            return replacedElement == null ? replacedElement 
                : applyMasks(replacedElement, replacementsXMLAttributes);
        } else if (lowerCaseContentType.contains(JSON_CONTENT)) {
            return applyMasks(originalLogString, replacementsJSON);
        }
        return originalLogString;
    }

    public void maskHeaders(
            final Map<String, String> headerMap,
            final Set<String> sensitiveHeaderNames) {
        sensitiveHeaderNames.stream()
                .forEach(h -> {
                    headerMap.computeIfPresent(h, (key, value) -> MASKED_HEADER_VALUE);
                });
    }

    private String applyMasks(String originalLogString, Set<ReplacementPair> replacementPairs) {
        String resultString = originalLogString;
        for (final ReplacementPair replacementPair : replacementPairs) {
            resultString = replacementPair.matchPattern.matcher(resultString).replaceAll(replacementPair.replacement);
        }
        return resultString;
    }
    
    private static String escapeForReplacement(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.replace("\\", "\\\\").replace("$", "\\$");
    }
}
