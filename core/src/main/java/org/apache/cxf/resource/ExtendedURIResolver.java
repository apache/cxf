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

package org.apache.cxf.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import org.xml.sax.InputSource;


public class ExtendedURIResolver {

    protected org.apache.cxf.resource.URIResolver currentResolver;
    protected String lastestImportUri;
    protected final Deque<InputStream> resourceOpened = new ArrayDeque<>();

    public ExtendedURIResolver() {
        currentResolver = new org.apache.cxf.resource.URIResolver();
    }

    public InputSource resolve(String curUri, String baseUri) {
        try {
            currentResolver.resolve(baseUri, curUri, getClass());
            if (currentResolver.isResolved()) {
                if (currentResolver.getURI() != null && currentResolver.getURI().isAbsolute()) {
                    // When importing a relative file,
                    // setSystemId with an absolute path so the
                    // resolver finds any files which that file
                    // imports with locations relative to it.
                    curUri = currentResolver.getURI().toString();
                }
                if (currentResolver.isFile()) {
                    curUri = currentResolver.getFile().getAbsoluteFile().toURI().toString();
                }
                InputStream in = currentResolver.getInputStream();
                resourceOpened.add(in);
                InputSource source = new InputSource(in);
                source.setSystemId(curUri);
                source.setPublicId(curUri);
                return source;
            }
        } catch (IOException e) {
            // move on...
        } finally {
            lastestImportUri = curUri;
            // the uri may have been updated since we were called
            // so only store it away when everything else is done
        }
        return null;
        // return new InputSource(schemaLocation);
    }

    public void close() {
        while (!resourceOpened.isEmpty()) {
            try {
                InputStream in = resourceOpened.pop();
                if (in != null) {
                    in.close();
                }
            } catch (IOException ioe) {
                // move on...
            }
        }
    }

    public String getLatestImportURI() {
        return this.getURI();
    }

    public String getURI() {
        if (currentResolver.getURI() != null) {
            return currentResolver.getURI().toString();
        }
        return lastestImportUri;
    }

}
