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

public final class PrimitiveUtils {
    
    private PrimitiveUtils() {
        
    }
    
    public static Class getClass(String value) {
        Class clz = null;        
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

    public static Object read(String value, Class type) {
        Object ret = value;
        if (Integer.TYPE.equals(type)) {
            ret = Integer.valueOf(value);
        }
        if (Byte.TYPE.equals(type)) {
            ret = Byte.valueOf(value);
        }
        if (Short.TYPE.equals(type)) {
            ret = Short.valueOf(value);
        }
        if (Long.TYPE.equals(type)) {
            ret = Long.valueOf(value);
        }
        if (Float.TYPE.equals(type)) {
            ret = Float.valueOf(value);
        }
        if (Double.TYPE.equals(type)) {
            ret = Double.valueOf(value);
        }
        if (Boolean.TYPE.equals(type)) {
            ret = Boolean.valueOf(value);
        }
        if (Character.TYPE.equals(type)) {
            ret = value.charAt(0);
        }
        // TODO others.
        return ret;
    }
}
