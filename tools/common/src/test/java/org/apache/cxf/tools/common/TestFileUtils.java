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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;

public final class TestFileUtils {

    private TestFileUtils() {
    }

    public static String getStringFromFile(File location) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(location.toPath())) {
            return normalizeCRLF(in);
        }
    }

    public static String getStringFromStream(InputStream is) throws IOException {
        return normalizeCRLF(new BufferedReader(new InputStreamReader(is)));
    }

    private static String normalizeCRLF(BufferedReader in) throws IOException {
        StringBuilder result = new StringBuilder();
        String line = in.readLine();
        while (line != null) {
            for (String token : line.split("\\s")) {
                result.append(' ').append(token);
            }
            line = in.readLine();
        }

        String rtn = result.toString();

        rtn = ignoreTokens(rtn, "<!--", "-->");
        rtn = ignoreTokens(rtn, "/*", "*/");
        return rtn;
    }

    private static String ignoreTokens(final String contents,
                                       final String startToken, final String endToken) {
        String rtn = contents;
        int headerIndexStart = rtn.indexOf(startToken);
        int headerIndexEnd = rtn.indexOf(endToken);
        if (headerIndexStart != -1 && headerIndexEnd != -1 && headerIndexStart < headerIndexEnd) {
            rtn = rtn.substring(0, headerIndexStart - 1)
                + rtn.substring(headerIndexEnd + endToken.length() + 1);
        }
        return rtn;
    }

}
