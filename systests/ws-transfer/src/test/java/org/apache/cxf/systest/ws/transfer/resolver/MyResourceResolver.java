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

package org.apache.cxf.systest.ws.transfer.resolver;

import org.w3c.dom.Element;

import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resourcefactory.resolver.ResourceReference;
import org.apache.cxf.ws.transfer.resourcefactory.resolver.ResourceResolver;
import org.apache.cxf.ws.transfer.shared.faults.InvalidRepresentation;

public class MyResourceResolver implements ResourceResolver {

    protected String studentURL;

    protected ResourceManager studentManager;

    protected String teachersURL;

    public MyResourceResolver(String studentURL, ResourceManager studentManager, String teachersURL) {
        this.studentURL = studentURL;
        this.studentManager = studentManager;
        this.teachersURL = teachersURL;
    }

    @Override
    public ResourceReference resolve(Create body) {
        Element representationEl = (Element) body.getRepresentation().getAny();
        if ("student".equals(representationEl.getLocalName())) {
            return new ResourceReference(studentURL, studentManager);
        } else if ("teacher".equals(representationEl.getLocalName())) {
            return new ResourceReference(teachersURL, null);
        } else {
            throw new InvalidRepresentation();
        }
    }

}
