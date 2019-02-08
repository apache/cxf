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
package org.apache.cxf.jaxrs.provider.atom;

/**
 * A callback-style provider which can be used to map an object to Atom Feed
 * without having to deal directly with types representing Atom feeds
 *
 * @param <T> Type of objects which will be mapped to feeds or entries
 */
public abstract class AbstractFeedBuilder<T> extends AbstractAtomElementBuilder<T> {

    /**
     *
     * @param pojo Object which is being mapped
     * @return feed icon uri
     */
    public String getIcon(T pojo) {
        return null;
    }

    /**
     *
     * @param pojo Object which is being mapped
     * @return feed logo uri
     */
    public String getLogo(T pojo) {
        return null;
    }


}
