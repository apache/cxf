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
package org.apache.cxf.jaxrs.ext.atom;

import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.MessageContext;

/**
 * A callback-style provider which can be used to map an object to Atom Feed or Entry
 * without having to deal directly with types representing Atom feeds or entries
 * 
 * @param <T> Type of objects which will be mapped to feeds or entries
 */
public abstract class AbstractAtomElementBuilder<T> {
    
    private MessageContext mc;
    
    /**
     * Sets MessageContext
     * @param context message context
     */
    public void setMessageContext(MessageContext context) {
        mc = context;
    }
    
    /**
     * returns MessageContext
     * @return message context
     */
    public MessageContext getMessageContext() {
        return mc;
    }
    
    /**
     * 
     * @param pojo Object which is being mapped
     * @return element title
     */
    public String getTitle(T pojo) {
        return null;
    }
    
    /**
     * 
     * @param pojo Object which is being mapped
     * @return element author
     */
    public String getAuthor(T pojo) {
        return null;
    }
    
    /**
     * 
     * @param pojo Object which is being mapped
     * @return element id
     */
    public String getId(T pojo) {
        return null;
    }

    /**
     * 
     * @param pojo Object which is being mapped
     * @return base uri
     */
    public String getBaseUri(T pojo) {
        return null;
    }
    
    /**
     * 
     * @param pojo Object which is being mapped
     * @return element updated date
     */
    public String getUpdated(T pojo) {
        return null;
    }
    
    
    /**
     * 
     * @param pojo Object which is being mapped
     * @return element categories
     */
    public List<String> getCategories(T pojo) {
        return null;
    }
    
    
    //CHECKSTYLE:OFF
    /**
     * Returns a map of link URI to link relations type pairs 
     * See {@link <a href="http://www.iana.org/assignments/link-relations/link-relations.xml">Atom Link Relations</a>}.
     *
     * @param pojo Object which is being mapped
     * @return the map of link URI to link relations type pairs 
     */
    //CHECKSTYLE:ON
    public Map<String, String> getLinks(T pojo) {
        return null;
    }
}
