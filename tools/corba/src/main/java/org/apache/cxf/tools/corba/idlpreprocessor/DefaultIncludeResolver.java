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

package org.apache.cxf.tools.corba.idlpreprocessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;


/**
 *
 */
public class DefaultIncludeResolver implements IncludeResolver {

    private static final Logger LOG = LogUtils.getL7dLogger(DefaultIncludeResolver.class);
    private final File[] userIdlDirs;

    public DefaultIncludeResolver(File... idlDirs) {
        for (final File dir : idlDirs) {
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("not a directory: " + dir.getAbsoluteFile());
            }
        }
        userIdlDirs = idlDirs;
    }

    /**
     * @see IncludeResolver#findSystemInclude(java.lang.String)
     */
    public URL findSystemInclude(String spec) {
        return findUserInclude(spec);
    }

    /**
     * @see IncludeResolver#findUserInclude(java.lang.String)
     */
    public URL findUserInclude(String spec) {

        for (final File searchDir : userIdlDirs) {

            URI searchDirURI = searchDir.toURI();
            try {
                // offload slash vs backslash to URL machinery
                URL searchDirURL = searchDirURI.toURL();
                final URL url = new URL(searchDirURL, spec);

                // Check if file in URL exists, otherwise try next searchDir
                try {
                    // If we can open a stream, the file exists
                    InputStream str = url.openStream();
                    str.close();
                    return url;
                } catch (IOException ioe) {
                    if (LOG.isLoggable(Level.WARNING)) {
                        LOG.fine("Not able to resolve "
                                 + spec
                                 + "from  "
                                 + searchDirURL.toString());
                    }
                }
            } catch (MalformedURLException e) {
                final PreprocessingException preprocessingException = new PreprocessingException(
                    "Unable to resolve user include '" + spec + "' in '"
                        + Arrays.toString(userIdlDirs) + "'", null, 0);
                preprocessingException.initCause(e);
                throw preprocessingException;
            }
        }

        throw new PreprocessingException("unable to resolve " + spec, null, 0);
    }
}
