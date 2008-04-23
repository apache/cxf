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
package org.apache.cxf.binding.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.codehaus.jra.ResourceUtil;

public class URIMapper {
    private List<ResourceInfo> resources = new ArrayList<ResourceInfo>();
    private Map<OperationInfo, String> locations = 
        new HashMap<OperationInfo, String>();
    private Map<OperationInfo, String> verbs = 
        new HashMap<OperationInfo, String>();
    
    public BindingOperationInfo getOperation(String uri, String verb, Message m) {
        List<ResourceInfo> bestMatch = new ArrayList<ResourceInfo>();
        int bestScore = 0;
        for (ResourceInfo r : resources) {
            if (r.getVerb().equals(verb)) {
                int newScore = ResourceUtil.getMatchScore(uri, r.getUri());
                if (newScore > bestScore) {
                    bestMatch.clear();
                    bestScore = newScore;
                }
                if (newScore >= bestScore) {
                    bestMatch.add(r);
                }
            }
        }
        
        if (bestScore > -1 && !bestMatch.isEmpty()) {
            if (bestMatch.size() == 1) {
                return bestMatch.get(0).getOperation();
            }
            
            //two or more with the same score... find the one with longest match 
            // NOT counting any tail match
            bestScore = -1;
            ResourceInfo newBest = null;
            for (ResourceInfo r : bestMatch) {
                String newUri = r.getUri();
                if (newUri.charAt(newUri.length() - 1) == '}') {
                    newUri = newUri.substring(0, newUri.lastIndexOf('{'));
                }
                int newScore = ResourceUtil.getMatchScore(uri, newUri);
                if (newScore > bestScore) {
                    bestScore = newScore;
                    newBest = r;
                }
            }
            return newBest.getOperation();
        }
        
        return null;
    }

    public void bind(BindingOperationInfo bop, String uri, String verb) {
        ResourceInfo info = new ResourceInfo();
        info.setUri(uri);
        info.setVerb(verb);
        info.setOperation(bop);
        locations.put(bop.getOperationInfo(), uri);
        verbs.put(bop.getOperationInfo(), verb);
        if (bop.getOperationInfo().getUnwrappedOperation() != null) {
            locations.put(bop.getUnwrappedOperation().getOperationInfo(), uri);
            verbs.put(bop.getUnwrappedOperation().getOperationInfo(), verb);
        }
        resources.add(info);
    }
    
    public String getLocation(BindingOperationInfo bop) {
        return locations.get(bop.getOperationInfo());
    }
    
    public String getVerb(BindingOperationInfo bop) {
        return verbs.get(bop.getOperationInfo());
    }
    
    public static class ResourceInfo {
        private String uri;
        private String verb;
        private BindingOperationInfo operation;
        
        public BindingOperationInfo getOperation() {
            return operation;
        }
        public void setOperation(BindingOperationInfo operation) {
            this.operation = operation;
        }
        public String getUri() {
            return uri;
        }
        public void setUri(String uri) {
            this.uri = uri;
        }
        public String getVerb() {
            return verb;
        }
        public void setVerb(String verb) {
            this.verb = verb;
        }
    }

    public List getParameters(MessageInfo msgInfo, String path) {
        String resource = locations.get(msgInfo.getOperation());
        Map<String, String> paramMap = ResourceUtil.getURIParameters(path, resource);
        
        List<Object> params = new ArrayList<Object>(msgInfo.getMessageParts().size());
        
        int i = 0;
        for (MessagePartInfo p : msgInfo.getMessageParts()) {
            params.add(i, paramMap.get(p.getName().getLocalPart()));
            i++;
        }
        
        return params;
    }
}
