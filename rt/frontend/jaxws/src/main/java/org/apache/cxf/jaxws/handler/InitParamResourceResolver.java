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

package org.apache.cxf.jaxws.handler;

import java.io.InputStream;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.resource.ResourceResolver;

public class InitParamResourceResolver implements ResourceResolver {

    private static final Logger LOG = LogUtils.getL7dLogger(InitParamResourceResolver.class, "APIMessages");
    
    Map<String, String> params;
    
    public InitParamResourceResolver(Map<String, String> map) {
        params = map;
    }
    
    
    public <T> T resolve(String resourceName, Class<T> resourceType) {
        
        String value = params.get(resourceName);
        return convertToType(value, resourceType);
    }

    public InputStream getAsStream(String name) {
        // returning these as a stream does not make much sense
        return null;
    }


    /**
     * Convert the string representation of value to type T
     */
    private <T> T convertToType(String value, Class<T> type) {
        
        /*
        char, byte, short, long, float, double, boolean
        */
        T ret = null;
        try { 
            if (String.class.equals(type)) {
                ret = type.cast(value);
            } else if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
                ret =  type.cast(Integer.valueOf(value));
            } else if (Byte.class.equals(type) || Byte.TYPE.equals(type)) {
                ret = type.cast(Byte.valueOf(value));
            } else if (Short.class.equals(type) || Short.TYPE.equals(type)) {
                ret = type.cast(Short.valueOf(value));
            } else if (Long.class.equals(type) || Long.TYPE.equals(type)) {
                ret =  type.cast(Long.valueOf(value));
            } else if (Float.class.equals(type) || Float.TYPE.equals(type)) {
                ret = type.cast(Float.valueOf(value));
            } else if (Double.class.equals(type) || Double.TYPE.equals(type)) {
                ret = type.cast(Double.valueOf(value));
            } else if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
                ret = type.cast(Boolean.valueOf(value));
            } else if (Character.class.equals(type) || Character.TYPE.equals(type)) {
                ret = type.cast(value.charAt(0));
            } else {
                LOG.severe("do not know how to treat type: " + type);
            } 
        } catch (NumberFormatException ex) {
            LOG.severe("badly formed init param: " + value);
        }
        return ret;
    }
}
