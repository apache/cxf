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

import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.manager.ResourceManager;

/**
 * The simple ResourceResolver, which always returns a predefined resource
 * location.
 */
public class SimpleResourceResolver implements ResourceResolver {

    protected ResourceManager resourceManager;

    protected String resourceURL;

    public SimpleResourceResolver() {

    }

    public SimpleResourceResolver(String resourceURL) {
        this.resourceURL = resourceURL;
    }

    public SimpleResourceResolver(String resourceURL, ResourceManager resourceManager) {
        this.resourceURL = resourceURL;
        this.resourceManager = resourceManager;
    }

    @Override
    public ResourceReference resolve(Create body) {
        return new ResourceReference(resourceURL, resourceManager);
    }

    public String getResourceURL() {
        return resourceURL;
    }

    public void setResourceURL(String resourceURL) {
        this.resourceURL = resourceURL;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

}
