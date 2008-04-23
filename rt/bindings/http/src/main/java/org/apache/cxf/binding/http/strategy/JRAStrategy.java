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
package org.apache.cxf.binding.http.strategy;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.apache.cxf.binding.http.URIMapper;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.codehaus.jra.Delete;
import org.codehaus.jra.Get;
import org.codehaus.jra.HttpResource;
import org.codehaus.jra.Post;
import org.codehaus.jra.Put;

import static org.apache.cxf.binding.http.HttpConstants.DELETE;
import static org.apache.cxf.binding.http.HttpConstants.GET;
import static org.apache.cxf.binding.http.HttpConstants.POST;
import static org.apache.cxf.binding.http.HttpConstants.PUT;

/**
 * A strategy to map BindingOperationInfos to URI/Verb combos utilizing the
 * <a href="http://jra.codehaus.org">Java Rest Annotations</a>.
 */
public class JRAStrategy implements ResourceStrategy {
    private static final Logger LOG = LogUtils.getL7dLogger(JRAStrategy.class);
    
    public boolean map(BindingOperationInfo bop, Method m, URIMapper mapper) {
        HttpResource r = m.getAnnotation(HttpResource.class);
        if (r == null) {
            return false;
        }
        
        String verb;
        if (m.isAnnotationPresent(Get.class)) {
            verb = GET;
        } else if (m.isAnnotationPresent(Post.class)) {
            verb = POST;
        } else if (m.isAnnotationPresent(Put.class)) {
            verb = PUT;
        } else if (m.isAnnotationPresent(Delete.class)) {
            verb = DELETE;
        } else {
            verb = POST;
        }
        
        mapper.bind(bop, r.location(), verb);
        
        LOG.info("Mapping method " + m.getName() + " to resource " + r.location() + " and verb " + verb);

        return true;
    }
    
}
