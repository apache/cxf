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

package org.apache.cxf.common.security;

import java.security.Principal;
import java.util.Enumeration;

public interface GroupPrincipal extends Principal {

    /**
     * Adds the specified member to the group.
     *
     * @param user the principal to add to this group.
     *
     * @return true if the member was successfully added,
     * false if the principal was already a member.
     */
    boolean addMember(Principal user);

    /**
     * Removes the specified member from the group.
     *
     * @param user the principal to remove from this group.
     *
     * @return true if the principal was removed, or
     * false if the principal was not a member.
     */
    boolean removeMember(Principal user);

    /**
     * Returns true if the passed principal is a member of the group.
     * This method does a recursive search, so if a principal belongs to a
     * group which is a member of this group, true is returned.
     *
     * @param member the principal whose membership is to be checked.
     *
     * @return true if the principal is a member of this group,
     * false otherwise.
     */
    boolean isMember(Principal member);


    /**
     * Returns an enumeration of the members in the group.
     * The returned objects can be instances of either Principal
     * or Group (which is a subclass of Principal).
     *
     * @return an enumeration of the group members.
     */
    Enumeration<? extends Principal> members();

}