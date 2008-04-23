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
package org.apache.cxf.aegis.util;

import java.util.Random;

/**
 * @author Hani Suleiman Date: Jun 10, 2005 Time: 3:20:28 PM
 */
public final class UID {
    private static int counter;
    private static Random random = new Random(System.currentTimeMillis());

    private UID() {
        //utility
    }
    
    public static String generate() {
        return String.valueOf(System.currentTimeMillis()) + counter++ + random.nextInt();
    }
}
