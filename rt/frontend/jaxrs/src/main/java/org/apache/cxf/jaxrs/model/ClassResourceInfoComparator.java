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

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.message.Message;

public class ClassResourceInfoComparator implements Comparator<ClassResourceInfo> {
    
    private Message message;
    private ResourceComparator rc; 

    public ClassResourceInfoComparator(Message m) {
        this.message = m;
        if (message != null) {
            Object o = m.getExchange().get(Endpoint.class).get("org.apache.cxf.jaxrs.comparator");
            if (o != null) {
                rc = (ResourceComparator)o;
            }
        }
    }
    
    public int compare(ClassResourceInfo cr1, ClassResourceInfo cr2) {
        
        if (rc != null) {
            int result = rc.compare(cr1, cr2, message);
            if (result != 0) {
                return result;
            }
        }
        
        return URITemplate.compareTemplates(
               cr1.getURITemplate(), 
               cr2.getURITemplate());
    }
}
    

