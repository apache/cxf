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

package org.apache.cxf.common.util;

import java.util.Map;

import org.apache.cxf.message.Message;

/**
 * Holder of generic property related methods
 */
public final class PropertyUtils {
    private PropertyUtils() {
    }

    public static boolean isTrue(Map<String, Object> props, String key) {
        if (props == null || key == null) {
            return false;
        }
        return isTrue(props.get(key));
    }

    /**
     * It might seem odd to return 'true' if a property == FALSE, but it
     * is required sometimes.
     *
     * @param props
     * @param key
     * @return false if value is either the String "false" or Boolean.FALSE.  Otherwise returns
     * true.
     */
    public static boolean isFalse(Map<String, Object> props, String key) {
        if (props == null || key == null) {
            return false;
        }
        return isFalse(props.get(key));
    }

    /**
     * Returns true if a value is either the String "true" (regardless of case)  or Boolean.TRUE.
     * @param property
     * @return true if value is either the String "true" or Boolean.TRUE.  Otherwise returns false.
     */
    public static boolean isTrue(Object property) {
        if (property == null) {
            return false;
        }

        return Boolean.TRUE.equals(property) || "true".equalsIgnoreCase(property.toString());
    }

    /**
     * It might seem odd to return 'true' if a property == FALSE, but it is required sometimes.
     *
     * Returns false if a value is either the String "false" (regardless of case)  or Boolean.FALSE.
     * @param property
     * @return false if value is either the String "false" or Boolean.FALSE.  Otherwise returns
     * true.
     */
    public static boolean isFalse(Object property) {
        if (property == null) {
            return false;
        }

        return Boolean.FALSE.equals(property) || "false".equalsIgnoreCase(property.toString());
    }

    public static Long getLong(Message message, String key) {
        Object o = message.getContextualProperty(key);
        if (o instanceof Long) {
            return (Long)o;
        } else if (o instanceof Number) {
            return ((Number)o).longValue();
        } else if (o instanceof String) {
            return Long.valueOf(o.toString());
        }
        return null;
    }

    public static Integer getInteger(Message message, String key) {
        Object o = message.getContextualProperty(key);
        if (o instanceof Integer) {
            return (Integer)o;
        } else if (o instanceof Number) {
            return ((Number)o).intValue();
        } else if (o instanceof String) {
            return Integer.valueOf((String)o);
        }
        return null;
    }
}
