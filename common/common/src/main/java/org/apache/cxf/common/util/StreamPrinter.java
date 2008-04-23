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

package org.apache.cxf.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

public class StreamPrinter extends Thread {
    InputStream is;
    String msg;
    OutputStream os;

    StreamPrinter(InputStream stream, String type) {
        this(stream, type, null);
    }

    StreamPrinter(InputStream stream, String type, OutputStream redirect) {
        is = stream;
        msg = type;
        os = redirect;
    }

    public void run() {
        try {
            PrintWriter pw = null;
            if (os != null) {
                pw = new PrintWriter(os);
            }
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();
            while (line != null) {
                if (pw != null) {
                    pw.println(msg + " " + line);
                }
                line = br.readLine();
            }
            if (pw != null) {
                pw.flush();
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
