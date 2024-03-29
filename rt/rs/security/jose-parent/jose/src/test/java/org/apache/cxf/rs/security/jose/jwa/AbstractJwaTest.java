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
package org.apache.cxf.rs.security.jose.jwa;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;

public class AbstractJwaTest {

    

    protected String loadResource(String resource) {
        StringBuilder input = new StringBuilder();
        InputStream stream = AbstractDecryptTest.class.getResourceAsStream(resource);
        Assert.assertNotNull("Resource " + resource + " could not be loaded", stream);
        new BufferedReader(new InputStreamReader(stream)).lines().forEach(line -> input.append(line));
        return input.toString();
    }

}
