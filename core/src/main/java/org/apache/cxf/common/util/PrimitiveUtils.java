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

import java.util.HashMap;
import java.util.Map;

public final class PrimitiveUtils {
    private static final Map<Class<?>, Class<?>> AUTOBOXED_PRIMITIVES_MAP = new HashMap<>();
    static {
        AUTOBOXED_PRIMITIVES_MAP.put(byte.class, Byte.class);
        AUTOBOXED_PRIMITIVES_MAP.put(short.class, Short.class);
        AUTOBOXED_PRIMITIVES_MAP.put(int.class, Integer.class);
        AUTOBOXED_PRIMITIVES_MAP.put(long.class, Long.class);
        AUTOBOXED_PRIMITIVES_MAP.put(float.class, Float.class);
        AUTOBOXED_PRIMITIVES_MAP.put(double.class, Double.class);
        AUTOBOXED_PRIMITIVES_MAP.put(boolean.class, Boolean.class);
        AUTOBOXED_PRIMITIVES_MAP.put(void.class, Void.class);
    }

    private PrimitiveUtils() {

    }

    public static boolean canPrimitiveTypeBeAutoboxed(Class<?> primitiveClass, Class<?> type) {
        return primitiveClass.isPrimitive() && type == AUTOBOXED_PRIMITIVES_MAP.get(primitiveClass);
    }

    public static Class<?> getClass(String value) {
        Class<?> clz = null;
        if ("int".equals(value)) {
            clz = int.class;
        }
        if ("byte".equals(value)) {
            clz = byte.class;
        }
        if ("short".equals(value)) {
            clz = short.class;
        }
        if ("long".equals(value)) {
            clz = long.class;
        }
        if ("float".equals(value)) {
            clz = float.class;
        }
        if ("double".equals(value)) {
            clz = double.class;
        }
        if ("boolean".equals(value)) {
            clz = boolean.class;
        }
        if ("char".equals(value)) {
            clz = char.class;
        }
        return clz;
    }

    public static <T> Object read(String value, Class<T> type) {
        if (!(Character.TYPE.equals(type) || Character.class.equals(type))
            && value != null && value.isEmpty()) {
            //pass empty string to number type will result in Exception
            value = "0";
        }
        Object ret = value;
        if (Integer.TYPE.equals(type) || Integer.class.equals(type)) {
            ret = Integer.valueOf(value);
        } else if (Byte.TYPE.equals(type) || Byte.class.equals(type)) {
            ret = Byte.valueOf(value);
        } else if (Short.TYPE.equals(type) || Short.class.equals(type)) {
            ret = Short.valueOf(value);
        } else if (Long.TYPE.equals(type) || Long.class.equals(type)) {
            ret = Long.valueOf(value);
        } else if (Float.TYPE.equals(type) || Float.class.equals(type)) {
            ret = Float.valueOf(value);
        } else if (Double.TYPE.equals(type) || Double.class.equals(type)) {
            ret = Double.valueOf(value);
        } else if (Boolean.TYPE.equals(type) || Boolean.class.equals(type)) {
            ret = Boolean.valueOf(value);
        } else if ((Character.TYPE.equals(type) || Character.class.equals(type)) && value != null) {
            ret = value.charAt(0);
        }
        return ret;
    }
}
