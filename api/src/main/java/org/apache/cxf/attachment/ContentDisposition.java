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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentDisposition {
    private static final String CD_HEADER_PARAMS_EXPRESSION = 
        "(([\\w]+( )?=( )?\"[^\"]+\")|([\\w]+( )?=( )?[^;]+))";
    private static final Pattern CD_HEADER_PARAMS_PATTERN =
        Pattern.compile(CD_HEADER_PARAMS_EXPRESSION);
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
        
        Matcher m = CD_HEADER_PARAMS_PATTERN.matcher(tempValue);
        while (m.find()) {
            String[] pair = m.group().trim().split("=");
            params.put(pair[0].trim(), 
                       pair.length == 2 ? pair[1].trim().replace("\"", "") : "");
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
