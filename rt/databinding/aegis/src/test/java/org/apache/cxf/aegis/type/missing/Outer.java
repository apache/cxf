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
package org.apache.cxf.aegis.type.missing;

import java.io.Serializable;

/**
 * A Outer, for purposes of matching and indexing.
 */
public class Outer implements Serializable {

    // Note that the accessors in here don't return null pointers for strings.
    // This improves the behavior of web services that return examples of this
    // object.

    /**
     * 
     */
    private static final long serialVersionUID = -2435297692897827392L;
    /**
     * The name string itself.
     */
    String data;
    /**
     * A unique ID for the name. Often a key from some other database.
     */
    String uid;
    /**
     * A flag indicating that this name is the primary name of it's entity.
     */
    boolean primary;

    /**
     * The type of name in the taxonomy of entity types.
     * 
     * @see com.basistech.rlp.RLPNEConstants for constants for this field.
     */
    int entityType;

    /**
     * The unique identifier of the entity containing this name, or none.
     * 
     * @see Entity
     */
    String entityUID;

    /**
     * In some environments, names store additional data.
     */
    String extra;

    /**
     * Any pre-computed inners for the name.
     * 
     * @@uml.property name="inners"
     * @@uml.associationEnd multiplicity="(0 -1)"
     */
    Inner[] inners;

    /**
     * Construct an empty name object.
     */
    public Outer() {
        extra = "";
    }

   /**
     * @return arbitrary data stored with this name.
     */
    public String getExtra() {
        return extra == null ? "" : extra;
    }

    /**
     * Set arbitrary data stored with this name.
     * 
     * @param extra the extra to set
     */
    public void setExtra(String extra) {
        this.extra = extra;
    }

    /**
     * Set a unique ID for this name. This API does not check or enforce
     * uniqueness.
     * 
     * @param uid
     */
    public void setUID(String auid) {
        this.uid = auid;
    }

    /**
     * @return the unique ID for this name.
     */
    public String getUID() {
        return uid;
    }

    /**
     * Set the textual content of the name. This call does not automatically set
     * any other properties, such as script or language.
     * 
     * @param data the data to set.
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * @return the textual content of the name.
     */
    public String getData() {
        return data;
    }

    /**
     * Set the 'named entity' type of this name.
     * {@link com.basistech.rlp.RLPNEConstants} for possible values. This value
     * influences the interpretating and matching of the name. Use the value
     * {@link com.basistech.rlp.RLPNEConstants#NE_TYPE_NONE} if there is no type
     * available.
     * 
     * @param entityType
     */
    public void setEntityType(int entityType) {
        this.entityType = entityType;
    }

    /**
     * @return the 'named entity' type of this name.
     */
    public int getEntityType() {
        return entityType;
    }

    /**
     * Set an entity UID for this name. Entities group multiple names for a
     * single real-world item. All the names of a single entity are connected
     * via their entity unique ID.
     * 
     * @param entityUID the UID.
     */
    public void setEntityUID(String entityUID) {
        this.entityUID = entityUID;
    }

    /**
     * @return the entity unique ID.
     */
    public String getEntityUID() {
        return entityUID;
    }



    /**
     * Set the 'primary' flag for this name. If names are grouped by entities
     * {@link #setEntityUID(String)}, one of the names of an entity may be
     * marked primary. This API does not check that only one name is marked.
     * 
     * @param primary the primary flag.
     */
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * @return the primary flag.
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * Set pre-calculated inners for this name.
     * 
     * @param inners the inners.
     */
    public void setTransliterations(Inner[] transliterations) {
        this.inners = transliterations;
    }

    /**
     * @return pre-calculated inners for this name.
     */
    public Inner[] getTransliterations() {
        return inners;
    }

}
