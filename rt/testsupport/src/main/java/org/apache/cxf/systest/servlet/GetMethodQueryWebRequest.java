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
package org.apache.cxf.systest.servlet;

import com.meterware.httpunit.GetMethodWebRequest;

//Customer WebRequest for sending the "http://localhost/xxx?wsdl" request
public class GetMethodQueryWebRequest extends GetMethodWebRequest {
    private String urlString;
    
    public GetMethodQueryWebRequest(String urlStr) {        
        super(urlStr);
        urlString = urlStr;        
    }
    
    public String getQueryString() {
        int pos = urlString.indexOf('?');
        if (pos > 0) {
            return urlString.substring(pos + 1);
        } else {
            return "";
        }                
    }
    
    

}
