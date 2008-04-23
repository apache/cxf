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

package org.apache.cxf.tools.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * This class provides common functionality for the main functions for the tools, as used from shell scripts
 * or ant.
 */
public final class CommandInterfaceUtils {

    // when testing, don't change logging config.
    private static boolean testInProgress;

    private CommandInterfaceUtils() {
    }

    public static void commandCommonMain() {
        if (!testInProgress) {
            // force commons-logging into j.u.l so we can
            // configure it.
            System.setProperty("org.apache.commons.logging.Log",
                               "org.apache.commons.logging.impl.Jdk14Logger");
            InputStream commandConfig = CommandInterfaceUtils.class
                .getResourceAsStream("commandLogging.properties");
            try {
                try {
                    LogManager.getLogManager().readConfiguration(commandConfig);
                } finally {
                    commandConfig.close();
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

    public static void setTestInProgress(boolean testInProgress) {
        CommandInterfaceUtils.testInProgress = testInProgress;
    }
}
