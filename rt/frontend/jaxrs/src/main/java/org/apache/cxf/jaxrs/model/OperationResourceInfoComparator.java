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
import java.util.List;

import javax.ws.rs.core.MediaType;

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
                
        if (e1.getHttpMethod() == null && e2.getHttpMethod() == null) {
            // with two subresource locators, those with more capturing groups win
            int g1 = e1.getURITemplate().getNumberOfGroups();
            int g2 = e2.getURITemplate().getNumberOfGroups();
            // descending order 
            return g1 < g2 ? 1 : g1 > g2 ? -1 : 0;
        }
        
        List<MediaType> mimeType1 = e1.getConsumeTypes();
        List<MediaType> mimeType2 = e2.getConsumeTypes();
        
        // TODO: we actually need to check all consume and produce types here ?
        int result = JAXRSUtils.compareMediaTypes(mimeType1.get(0), 
                                                  mimeType2.get(0));
        if (result == 0) {
            //use the media type of output data as the secondary key.
            List<MediaType> mimeTypeP1 = e1.getProduceTypes();
            List<MediaType> mimeTypeP2 = e2.getProduceTypes();
            result =  JAXRSUtils.compareMediaTypes(mimeTypeP1.get(0), 
                                                mimeTypeP2.get(0));
        }
        
        return result;
    }

}
