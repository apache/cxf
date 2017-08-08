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
package org.apache.cxf.xkms.handlers;


/**
 * A couple of utility methods for working with X500 Distinguished Names
 */
public final class DnUtils {

    private DnUtils() {

    }

    public static String extractMostSignificantAttribute(String identifier) {
        // first find the first comma-delimited value
        String[] split = identifier.split(",");
        String msaVal = null;
        String cnVal = null;
        String ouVal = null;
        for (String val : split) {
            val = normalizeAttribute(val);
            if (null == msaVal) {
                msaVal = val;
            }
            if ((cnVal == null) && val.startsWith("cn=")) {
                cnVal = val;
            }
            if ((ouVal == null) && val.startsWith("ou=")) {
                ouVal = val;
            }
        }
        if (cnVal != null) {
            return cnVal;
        } else if (ouVal != null) {
            return ouVal;
        } else {
            return msaVal;
        }
    }

    public static String extractMostSignificantAttributeValue(String identifier) {
        String attr = extractMostSignificantAttribute(identifier);
        String[] split;
        if (attr != null) {
            split = attr.split("=");
            // normalize the prefix if present
            if (split.length == 2) {
                return split[1].trim();
            }
            return attr.trim();
        }
        return attr;
    }

    private static String normalizeAttribute(String val) {
        String[] split;
        String normalized = val;
        if (null != val) {
            split = val.split("=");
            // normalize the prefix if present
            if (split.length == 2) {
                String prefix = split[0].toLowerCase().trim();
                String value = split[1].trim();
                normalized = prefix + "=" + value;
            } else {
                normalized = val.trim();
            }
        }
        return normalized;
    }

    public static class DnAttribute {

        private String prefix;
        private String name;
        private String full;

        public DnAttribute(String attributeDefinition) {
            full = attributeDefinition;
            String[] split = attributeDefinition.split("=");
            if (1 == split.length) {
                this.name = split[0];
            } else if (2 == split.length) {
                this.prefix = split[0];
                this.name = split[1];
            }
        }

        public String prefix() {
            return prefix;
        }

        public String name() {
            return name;
        }

        public String full() {
            return full;
        }

        @Override
        public String toString() {
            return this.full;
        }

    }

}
