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

package org.apache.cxf.jaxrs.model;

import java.util.Comparator;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;

public class OperationResourceInfoComparator implements Comparator<OperationResourceInfo> {
    
    public int compare(OperationResourceInfo e1, OperationResourceInfo e2) {
        
        if (e1.getHttpMethod() != null && e2.getHttpMethod() == null
            || e1.getHttpMethod() == null && e2.getHttpMethod() != null) {
            // subresource method takes precedence over a subresource locator
            return e1.getHttpMethod() != null ? -1 : 1;
        }

            
        String l1 = e1.getURITemplate().getLiteralChars();
        String l2 = e2.getURITemplate().getLiteralChars();
        if (!l1.equals(l2)) {
            // descending order 
            return l1.length() < l2.length() ? 1 : -1; 
        }
        
        int g1 = e1.getURITemplate().getNumberOfGroups();
        int g2 = e2.getURITemplate().getNumberOfGroups();
        if (g1 != g2) {
            // descending order 
            return g1 < g2 ? 1 : -1;
        }
        
        
        int result = JAXRSUtils.compareSortedMediaTypes(
                          e1.getConsumeTypes(), 
                          e2.getConsumeTypes());
        if (result == 0) {
            //use the media type of output data as the secondary key.
            result = JAXRSUtils.compareSortedMediaTypes(e1.getProduceTypes(), 
                                                        e2.getProduceTypes());
        }
        
        return result;
    }

}
