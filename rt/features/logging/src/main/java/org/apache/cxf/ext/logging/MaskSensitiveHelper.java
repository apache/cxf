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
    private static final String ELEMENT_NAME_TEMPLATE = "-ELEMENT_NAME-";
    private static final String MATCH_PATTERN_XML_TEMPLATE = "(<-ELEMENT_NAME-.*?>)(.*?)(</-ELEMENT_NAME->)";
    private static final String REPLACEMENT_XML_TEMPLATE = "$1XXX$3";
    private static final String MATCH_PATTERN_JSON_TEMPLATE = "\"-ELEMENT_NAME-\"[ \\t]*:[ \\t]*\"(.*?)\"";
    private static final String REPLACEMENT_JSON_TEMPLATE = "\"-ELEMENT_NAME-\": \"XXX\"";
    private static final String MASKED_HEADER_VALUE = "XXX";

    private static final String XML_CONTENT = "xml";
    private static final String HTML_CONTENT = "html";
    private static final String JSON_CONTENT = "json";

    private static class ReplacementPair {
        private final Pattern matchPattern;
        private final String replacement;

        ReplacementPair(String matchPattern, String replacement) {
            this.matchPattern = Pattern.compile(matchPattern);
            this.replacement = replacement;
        }
    }

    private final Set<ReplacementPair> replacementsXML = new HashSet<>();
    private final Set<ReplacementPair> replacementsJSON = new HashSet<>();

    public void setSensitiveElementNames(final Set<String> inSensitiveElementNames) {
        replacementsXML.clear();
        replacementsJSON.clear();
        addSensitiveElementNames(inSensitiveElementNames);
    }
    
    public void addSensitiveElementNames(final Set<String> inSensitiveElementNames) {
        for (final String sensitiveName : inSensitiveElementNames) {
            addReplacementPair(MATCH_PATTERN_XML_TEMPLATE, REPLACEMENT_XML_TEMPLATE, sensitiveName, replacementsXML);
            addReplacementPair(MATCH_PATTERN_JSON_TEMPLATE, REPLACEMENT_JSON_TEMPLATE, sensitiveName, replacementsJSON);
        }
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
        if (replacementsXML.isEmpty() && replacementsJSON.isEmpty()
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
            return applyMasks(originalLogString, replacementsXML);
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
}
