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
package org.apache.cxf.wsn.util;


import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 * Generator for Globally unique Strings.
 */

public class IdGenerator {

    private static final Logger LOGGER = LogUtils.getL7dLogger(IdGenerator.class);

    private static final String UNIQUE_STUB;

    private static int instanceCount;

    private static String hostName;

    private String seed;

    public IdGenerator() {
        this("ID:");
    }

    /**
     * Construct an IdGenerator
     *
     */
    public IdGenerator(String prefix) {
        synchronized (UNIQUE_STUB) {
            this.seed = prefix + UNIQUE_STUB + (instanceCount++) + ":";
        }
    }

    static {
        String stub = "";
        boolean canAccessSystemProps = true;
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPropertiesAccess();
            }
        } catch (SecurityException se) {
            canAccessSystemProps = false;
        }

        if (canAccessSystemProps) {
            try {
                hostName = InetAddress.getLocalHost().getHostAddress();
                ServerSocket ss = new ServerSocket(0);
                stub = hostName + "-"
                    + Long.toHexString(ss.getLocalPort() ^ System.currentTimeMillis())
                    + "-";
                Thread.sleep(100);
                ss.close();
            } catch (Exception ioe) {
                LOGGER.log(Level.WARNING, "Could not generate unique stub", ioe);
            }
        } else {
            hostName = "localhost";
            stub = hostName + Long.toHexString(System.currentTimeMillis()) + "-";
        }
        UNIQUE_STUB = stub;
    }

    /**
     * As we have to find the hostname as a side-affect of generating a unique
     * stub, we allow it's easy retrevial here
     *
     * @return the local host name
     */

    public static String getHostName() {
        return hostName;
    }

    /**
     * Generate a unique id. The variable part of the id is generated from a
     * cryptographically strong random source rather than a sequential counter:
     * the ids are used as unguessable endpoint addresses for resources such as
     * subscriptions, and a client knowing one id must not be able to derive
     * the ids handed out to other clients.
     *
     * @return a unique id
     */

    public String generateId() {
        return this.seed + UUID.randomUUID();
    }

    /**
     * Generate a unique ID - that is friendly for a URL or file system
     *
     * @return a unique id
     */
    public String generateSanitizedId() {
        String result = generateId();
        result = result.replace(':', '-');
        result = result.replace('_', '-');
        result = result.replace('.', '-');
        return result;
    }

}
