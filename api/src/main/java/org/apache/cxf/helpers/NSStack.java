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

package org.apache.cxf.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class NSStack {

    private static final String NS_PREFIX_PREFIX = "ns";

    private final List<List<NSDecl>> stack = new ArrayList<List<NSDecl>>();
    private List<NSDecl> top; 
    private int size; 
    private int nsPrefixCount = 1;

    public synchronized void push() {
        top = new ArrayList<NSDecl>();
        stack.add(top);
        size++;
    }

    /**
     * Leave a scope: this removes any NS declarations that were added
     * in the last scope. Note that I don't bother to validate that you
     * don't call popScope too many times; that's your problem.
     */
    public synchronized void pop() {
        stack.remove(--size);
        top = null;
        if (size != 0) {
            top = stack.get(size - 1);
        }
    }

    /**
     * Add a new declaration to the current scope. This is visible within
     * the current scope as well as from any nested scopes.
     *
     * @param prefix the prefix to be used for this namespace
     * @param URI the namespace name of this namespace.
     */
    public synchronized void add(String prefix, String uri) {
        top.add(new NSDecl(prefix, uri));
    }

    /**
     * Add a new declaration to the current scope using a unique prefix
     * and return the prefix. This is useful when one just wants to add a
     * decl and doesn't want to have to deal with creating unique prefixes.
     * If the namespace name is already declared and in scope, then the
     * previously declared prefix is returned.
     *
     * @param URI the namespace name of this namespace
     * @return the unique prefix created or previously declared
     *         for this namespace
     */
    public synchronized String add(String uri) {
        String uniquePrefix = getPrefix(uri);

        if (uniquePrefix == null) {
            do {
                uniquePrefix = NS_PREFIX_PREFIX + nsPrefixCount++;
            } while (getURI(uniquePrefix) != null);
            add(uniquePrefix, uri);
        }
        return uniquePrefix;
    }

    /**
     * Return the prefix associated with the given namespace name by
     * looking thru all the namespace declarations that are in scope.
     *
     * @param URI the namespace name for whom a declared prefix is desired
     * @return the prefix or null if namespace name not found
     */
    public synchronized String getPrefix(String uri) {
        for (int i = size - 1; i >= 0; i--) {
            List<NSDecl> scope = stack.get(i);
            ListIterator<NSDecl> lsIterator =  scope.listIterator();

            while (lsIterator.hasNext()) {
                NSDecl nsd = lsIterator.next();

                if (nsd.getUri().equals(uri)) {
                    return nsd.getPrefix();
                }
            }
        }
        return null;
    }
   
    /**
     * Return the namespace name associated with the given prefix by
     * looking thru all the namespace declarations that are in scope.
     *
     * @param prefix the prefix for whom a declared namespace name is desired
     * @return the namespace name or null if prefix not found
     */
    public synchronized String getURI(String prefix) {
        for (int i = size - 1; i >= 0; i--) {
            List<NSDecl> scope = stack.get(i);
            ListIterator<NSDecl> lsIterator = scope.listIterator();

            while (lsIterator.hasNext()) {
                NSDecl nsd = lsIterator.next();

                if (nsd.getPrefix().equals(prefix)) {
                    return nsd.getUri();
                }
            }
        }
        return null;
    }

}

