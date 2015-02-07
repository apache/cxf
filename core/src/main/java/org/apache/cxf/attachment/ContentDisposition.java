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

package org.apache.cxf.attachment;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentDisposition {
    private static final String CD_HEADER_PARAMS_EXPRESSION =
        "(([\\w]+( )?\\*?=( )?\"[^\"]+\")|([\\w]+( )?\\*?=( )?[^;]+))";
    private static final Pattern CD_HEADER_PARAMS_PATTERN =
            Pattern.compile(CD_HEADER_PARAMS_EXPRESSION);

    private static final String CD_HEADER_EXT_PARAMS_EXPRESSION =
            "(UTF-8|ISO-8859-1)''((?:%[0-9a-f]{2}|\\S)+)";
    private static final Pattern CD_HEADER_EXT_PARAMS_PATTERN =
            Pattern.compile(CD_HEADER_EXT_PARAMS_EXPRESSION);

    private String value;
    private String type;
    private Map<String, String> params = new LinkedHashMap<String, String>();

    public ContentDisposition(String value) {
        this.value = value;

        String tempValue = value;

        int index = tempValue.indexOf(';');
        if (index > 0 && !(tempValue.indexOf('=') < index)) {
            type = tempValue.substring(0, index).trim();
            tempValue = tempValue.substring(index + 1);
        }

        String extendedFilename = null;
        Matcher m = CD_HEADER_PARAMS_PATTERN.matcher(tempValue);
        while (m.find()) {
            String[] pair = m.group().trim().split("=");
            String paramName = pair[0].trim();
            String paramValue = pair.length == 2 ? pair[1].trim().replace("\"", "") : "";
            // filename* looks like the only CD param that is human readable
            // and worthy of the extended encoding support. This is easy enough
            // to change to support others, but considering the list below, I
            // think it's sufficient.
            /*
                http://www.iana.org/assignments/cont-disp/cont-disp.xhtml#cont-disp-2

                filename            name to be used when creating file [RFC2183]
                creation-date       date when content was created [RFC2183]
                modification-date   date when content was last modified [RFC2183]
                read-date           date when content was last read [RFC2183]
                size                approximate size of content in octets [RFC2183]
                name                original field name in form [RFC2388]
                voice               type or use of audio content [RFC2421]
                handling            whether or not processing is required [RFC3204]
             */
            if ("filename*".equals(paramName)) {
                // try to decode the value if it matches the spec
                try {
                    Matcher matcher = CD_HEADER_EXT_PARAMS_PATTERN.matcher(paramValue);
                    if (matcher.matches()) {
                        String encodingScheme = matcher.group(1);
                        String encodedValue = matcher.group(2);
                        paramValue = Rfc5987Util.decode(encodedValue, encodingScheme);
                        extendedFilename = paramValue;
                    }
                } catch (UnsupportedEncodingException e) {
                    // would be odd not to support UTF-8 or 8859-1
                }
            }
            params.put(paramName, paramValue);
        }
        if (extendedFilename != null) {
            params.put("filename", extendedFilename);
        }
    }

    public String getType() {
        return type;
    }

    public String getParameter(String name) {
        return params.get(name);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(params);
    }

    public String toString() {
        return value;
    }
}
