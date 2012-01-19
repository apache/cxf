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
package org.apache.cxf.jaxrs.utils.multipart;

import java.io.IOException;

public class MultipartReadException extends IOException {
    private String expectedContentId;    
    private String expectedContentType;
    private String errorMessage;
    public MultipartReadException(String expectedContentId, 
                                  String expectedContentType,
                                  String errorMessage) {
        this.expectedContentId = expectedContentId;
        this.expectedContentType = expectedContentType;
        this.errorMessage = errorMessage;
    }
    
    public String getExpectedContentId() {
        return expectedContentId;
    }
    
    public String getExpectedContentType() {
        return expectedContentType;
    }
    
    public String getMessage() {
        return errorMessage;
    }
}
