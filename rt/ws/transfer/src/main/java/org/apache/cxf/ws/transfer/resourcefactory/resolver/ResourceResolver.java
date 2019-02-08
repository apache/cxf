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

/**
 * The interface for resolving, where the Resource will be created.
 */
public interface ResourceResolver {

    /**
     * Method for resolving, where the Resource will be created.
     * @param body SOAP body
     * @return ResourceReference object. If the Resource should be created locally,
     * so the ResourceReference object must contain address and reference to the
     * ResourceManager object. Otherwise the ResourceReference must contain only
     * the address to the ResourceRemote endpoint.
     */
    ResourceReference resolve(Create body);

}
