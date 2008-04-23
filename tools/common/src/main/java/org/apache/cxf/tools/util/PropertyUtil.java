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

package org.apache.cxf.tools.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cxf.common.util.StringUtils;

public class PropertyUtil {
    private static final String DEFAULT_DELIM = "=";
    private Map<String, String>  maps = new HashMap<String, String>();

    public void load(InputStream is, String delim) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = br.readLine();
        while (!StringUtils.isEmpty(line)) {
            StringTokenizer st = new StringTokenizer(line, delim);
            String key = null;
            String value = null;
            if (st.hasMoreTokens()) {
                key  = st.nextToken().trim();
            }
            if (st.hasMoreTokens()) {
                value = st.nextToken().trim();
            }

            maps.put(key, value);

            line = br.readLine();
        }
        br.close();
    }
    
    public void load(InputStream is) throws IOException {
        load(is, DEFAULT_DELIM);
    }
    
    public String getProperty(String key) {
        return this.maps.get(key);
    }

    public Map<String, String> getMaps() {
        return this.maps;
    }
}
