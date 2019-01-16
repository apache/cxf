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

package org.apache.cxf.bus.extension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

public class TextExtensionFragmentParser {
    private static final Logger LOG = LogUtils.getL7dLogger(TextExtensionFragmentParser.class);

    final ClassLoader loader;
    public TextExtensionFragmentParser(ClassLoader loader) {
        this.loader = loader;
    }

    public List<Extension> getExtensions(final URL url) {
        try (InputStream is = url.openStream()) {
            return getExtensions(is);
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Reads extension definitions from a Text file and instantiates them
     * The text file has the following syntax
     * classname:interfacename:deferred(true|false):optional(true|false)
     *
     * @param is stream to read the extension from
     * @return list of Extensions
     * @throws IOException
     */
    public List<Extension> getExtensions(InputStream is) throws IOException {
        List<Extension> extensions = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line = reader.readLine();
        while (line != null) {
            final Extension extension = getExtensionFromTextLine(line);
            if (extension != null) {
                extensions.add(extension);
            }
            line = reader.readLine();
        }
        return extensions;
    }

    private Extension getExtensionFromTextLine(String line) {
        line = line.trim();
        if (line.isEmpty() || line.charAt(0) == '#') {
            return null;
        }
        final Extension ext = new Extension(loader);
        final String[] parts = line.split(":");
        ext.setClassname(parts[0]);
        if (ext.getClassname() == null) {
            return null;
        }
        if (parts.length >= 2) {
            String interfaceName = parts[1];
            if (interfaceName != null && interfaceName.isEmpty()) {
                interfaceName = null;
            }
            ext.setInterfaceName(interfaceName);
        }
        if (parts.length >= 3) {
            ext.setDeferred(Boolean.parseBoolean(parts[2]));
        }
        if (parts.length >= 4) {
            ext.setOptional(Boolean.parseBoolean(parts[3]));
        }
        return ext;
    }

}
