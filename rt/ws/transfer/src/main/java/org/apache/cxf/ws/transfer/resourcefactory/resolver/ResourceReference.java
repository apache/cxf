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

package org.apache.cxf.ws.transfer.resourcefactory.resolver;

import org.apache.cxf.ws.transfer.manager.ResourceManager;

/**
 * Class intended as return value from the ResourceResolver.
 * @see ResourceResolver
 */
public class ResourceReference {

    private ResourceManager resourceManager;

    private String resourceURL;

    public ResourceReference() {

    }

    public ResourceReference(
            String resourceURL,
            ResourceManager resourceManager) {
        this.resourceURL = resourceURL;
        this.resourceManager = resourceManager;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public String getResourceURL() {
        return resourceURL;
    }

    public void setResourceURL(String resourceURL) {
        this.resourceURL = resourceURL;
    }

}
