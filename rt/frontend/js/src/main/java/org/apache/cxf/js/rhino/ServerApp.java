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

package org.apache.cxf.js.rhino;

import java.io.File;
import java.io.FileFilter;

import java.net.MalformedURLException;
import java.net.URL;

public class ServerApp {
    public static final String NO_ADDR_ERR = "error: an endpoint address must be provided";
    public static final String NO_FILES_ERR = "error: no JavaScript files specified";
    public static final String WRONG_ADDR_ERR = "error: -a requires a URL argument";
    public static final String WRONG_BASE_ERR = "error: -b requires a base URL argument";
    public static final String ILLEGAL_OPTIONS_ERR = "error: -a and -b cannot be used together";
    public static final String UNKNOWN_OPTION = "error: unknown option";

    private boolean verbose;
    private boolean bOptSeen;
    private String epAddr;

    protected void start(String[] args) throws Exception {
        ProviderFactory ph = createProviderFactory();
        FileFilter jsFilter = new JSFilter();
        int i = 0;
        boolean fileSeen = false;
        boolean msgPrinted = false;
        for (;;) {
            if (i == args.length) {
                break;
            }
            if (args[i].startsWith("-")) {
                i = checkOption(args, i);
                if (verbose && !msgPrinted) {
                    msgPrinted = true;
                    if (verbose) {
                        System.out.println("entering server");
                    }
                }
            } else {
                File f = new File(args[i]);
                if (f.isFile() && jsFilter.accept(f)) {
                    fileSeen = true;
                    if (verbose) {
                        System.out.println("processing file " + f.getCanonicalPath());
                    }
                    ph.createAndPublish(f, epAddr, bOptSeen);
                } else if (f.isDirectory()) {
                    File[] flist = f.listFiles(jsFilter);
                    for (File file : flist) {
                        fileSeen = true;
                        if (verbose) {
                            System.out.println("processing file " + file.getCanonicalPath());
                        }
                        ph.createAndPublish(file, epAddr, bOptSeen);
                    }
                }
            }
            i++;
        }
        if (!fileSeen) {
            throw new Exception(NO_FILES_ERR);
        }
    }

    public static void main(String[] args) throws Exception {
        ServerApp app = null;
        try {
            app = new ServerApp();
            app.start(args);
        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        }
        if (app.verbose) {
            System.out.println("server ready...");
        }
        Thread.sleep(5 * 60 * 1000);
        if (app.verbose) {
            System.out.println("server timed out, exiting");
        }
        System.exit(0);
    }

    protected ProviderFactory createProviderFactory() {
        return new ProviderFactory();
    }

    private int checkOption(String[] args, int index) throws Exception {
        if ("-v".equals(args[index])) {
            verbose = true;
        } else if ("-a".equals(args[index])) {
            bOptSeen = false;
            if (++index == args.length) {
                throw new Exception(WRONG_ADDR_ERR);
            }
            try {
                new URL(args[index]);
            } catch (MalformedURLException m) {
                throw new Exception(WRONG_ADDR_ERR, m);
            }
            epAddr = args[index];
        } else if ("-b".equals(args[index])) {
            bOptSeen = true;
            if (++index == args.length) {
                throw new Exception(WRONG_BASE_ERR);
            }
            try {
                new URL(args[index]);
            } catch (MalformedURLException m) {
                throw new Exception(WRONG_BASE_ERR, m);
            }
            epAddr = args[index];
        } else {
            throw new Exception(UNKNOWN_OPTION + ": " + args[index]);
        }
        return index;
    }

    private static class JSFilter implements FileFilter {
        public final boolean accept(File f) {
            if (f.isFile()) {
                String name = f.getName();
                return name.endsWith(".js") || name.endsWith(".jsx");
            } else {
                return false;
            }
        }
    }
}
