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

import java.util.List;

import org.apache.cxf.message.Message;

public class MaskSensitiveHelper {
    private static final String ELEMENT_NAME_TEMPLATE = "-ELEMENT_NAME-";
    private static final String MATCH_PATTERN_XML = "<-ELEMENT_NAME->(.*?)</-ELEMENT_NAME->";
    private static final String MATCH_PATTERN_JSON = "\"-ELEMENT_NAME-\"[ \\t]*:[ \\t]*\"(.*?)\"";
    private static final String REPLACEMENT_PATTERN_XML = "<-ELEMENT_NAME->XXX</-ELEMENT_NAME->";
    private static final String REPLACEMENT_PATTERN_JSON = "\"-ELEMENT_NAME-\": \"XXX\"";

    private static final String XML_CONTENT = "xml";
    private static final String HTML_CONTENT = "html";
    private static final String JSON_CONTENT = "json";

    final List<String> sensitiveElementNames;

    public MaskSensitiveHelper(final List<String> sensitiveElementNames) {
        this.sensitiveElementNames = sensitiveElementNames;
    }

    public String maskSensitiveElements(
            final Message message,
            final String originalLogString) {
        if ((sensitiveElementNames == null) || sensitiveElementNames.isEmpty()) {
            return originalLogString;
        }
        String contentType = (String) message.get(Message.CONTENT_TYPE);
        if (contentType.toLowerCase().contains(XML_CONTENT)
                || contentType.toLowerCase().contains(HTML_CONTENT)) {
            return applyExpression(originalLogString, MATCH_PATTERN_XML, REPLACEMENT_PATTERN_XML);
        } else if (contentType.toLowerCase().contains(JSON_CONTENT)) {
            return applyExpression(originalLogString, MATCH_PATTERN_JSON, REPLACEMENT_PATTERN_JSON);
        } else {
            return originalLogString;
        }
    }

    private String applyExpression(
            final String originalLogString,
            final String matchPatternTemplate,
            final String replacementTemplate) {
        String resultString = originalLogString;
        for (final String elementName : sensitiveElementNames) {
            final String matchPattern = matchPatternTemplate.replaceAll(ELEMENT_NAME_TEMPLATE, elementName);
            final String replacement = replacementTemplate.replaceAll(ELEMENT_NAME_TEMPLATE, elementName);
            resultString = resultString.replaceAll(matchPattern, replacement);
        }
        return resultString;
    }
}
