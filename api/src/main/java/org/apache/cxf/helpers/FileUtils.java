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

package org.apache.cxf.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.common.util.SystemPropertyAction;

public final class FileUtils {
    private static final int RETRY_SLEEP_MILLIS = 10;
    private static File defaultTempDir;
    
    
    private FileUtils() {
        
    }
    
    public static synchronized File getDefaultTempDir() {
        if (defaultTempDir != null
            && defaultTempDir.exists()) {
            return defaultTempDir;
        }
        
        String s = SystemPropertyAction.getPropertyOrNull(FileUtils.class.getName() + ".TempDirectory");
        if (s != null) {
            //assume someone outside of us will manage the directory
            File f = new File(s);
            if (f.mkdirs()) {
                defaultTempDir = f;                
            }
        }
        if (defaultTempDir == null) {
            defaultTempDir = createTmpDir();         
        }
        return defaultTempDir;
    }
    
    public static File createTmpDir() {
        int x = (int)(Math.random() * 1000000);
        String s = SystemPropertyAction.getProperty("java.io.tmpdir");
        File checkExists = new File(s);
        if (!checkExists.exists() || !checkExists.isDirectory()) {
            throw new RuntimeException("The directory " 
                                   + checkExists.getAbsolutePath() 
                                   + " does not exist, please set java.io.tempdir"
                                   + " to an existing directory");
        }
        if (!checkExists.canWrite()) {
            throw new RuntimeException("The directory " 
                                   + checkExists.getAbsolutePath() 
                                   + " is not writable, please set java.io.tempdir"
                                   + " to a writable directory");
        }
        File f = new File(checkExists, "cxf-tmp-" + x);
        int count = 0;
        while (!f.mkdir()) {
            
            if (count > 10000) {
                throw new RuntimeException("Could not create a temporary directory in "
                                           + s + ",  please set java.io.tempdir"
                                           + " to a writable directory");
            }
            x = (int)(Math.random() * 1000000);
            f = new File(checkExists, "cxf-tmp-" + x);
            count++;
        }
        File newTmpDir  = f;
        final File f2 = f;
        Thread hook = new Thread() {
            @Override
            public void run() {
                removeDir(f2, true);
            }
        };
        Runtime.getRuntime().addShutdownHook(hook); 
        return newTmpDir;
    }

    public static void mkDir(File dir) {
        if (dir == null) {
            throw new RuntimeException("dir attribute is required");
        }

        if (dir.isFile()) {
            throw new RuntimeException("Unable to create directory as a file "
                                    + "already exists with that name: " + dir.getAbsolutePath());
        }

        if (!dir.exists()) {
            boolean result = doMkDirs(dir);
            if (!result) {
                String msg = "Directory " + dir.getAbsolutePath()
                             + " creation was not successful for an unknown reason";
                throw new RuntimeException(msg);
            }
        }
    }

    /**
     * Attempt to fix possible race condition when creating directories on
     * WinXP, also Windows2000. If the mkdirs does not work, wait a little and
     * try again.
     */
    private static boolean doMkDirs(File f) {
        if (!f.mkdirs()) {
            try {
                Thread.sleep(RETRY_SLEEP_MILLIS);
                return f.mkdirs();
            } catch (InterruptedException ex) {
                return f.mkdirs();
            }
        }
        return true;
    }

    public static void removeDir(File d) {
        removeDir(d, false);
    }
    private static void removeDir(File d, boolean inShutdown) {
        String[] list = d.list();
        if (list == null) {
            list = new String[0];
        }
        for (int i = 0; i < list.length; i++) {
            String s = list[i];
            File f = new File(d, s);
            if (f.isDirectory()) {
                removeDir(f, inShutdown);
            } else {
                delete(f, inShutdown);
            }
        }
        delete(d, inShutdown);
    }

    public static void delete(File f) {
        delete(f, false);
    }
    public static void delete(File f, boolean inShutdown) {
        if (!f.delete()) {
            if (isWindows()) {
                System.gc();
            }
            try {
                Thread.sleep(RETRY_SLEEP_MILLIS);
            } catch (InterruptedException ex) {
                // Ignore Exception
            }
            if (!f.delete() && !inShutdown) {
                f.deleteOnExit();
            }
        }
    }

    private static boolean isWindows() {
        String osName = SystemPropertyAction.getProperty("os.name").toLowerCase(Locale.US);
        return osName.indexOf("windows") > -1;
    }

    public static File createTempFile(String prefix, String suffix) throws IOException {
        return createTempFile(prefix, suffix, null, false);
    }
    
    public static File createTempFile(String prefix, String suffix, File parentDir,
                               boolean deleteOnExit) throws IOException {
        File result = null;
        File parent = (parentDir == null)
            ? getDefaultTempDir()
            : parentDir;
            
        if (suffix == null) {
            suffix = ".tmp";
        }
        if (prefix == null) {
            prefix = "cxf";
        } else if (prefix.length() < 3) {
            prefix = prefix + "cxf";
        }
        result = File.createTempFile(prefix, suffix, parent);

        //if parentDir is null, we're in our default dir
        //which will get completely wiped on exit from our exit
        //hook.  No need to set deleteOnExit() which leaks memory.
        if (deleteOnExit && parentDir != null) {
            result.deleteOnExit();
        }
        return result;
    }
    
    public static String getStringFromFile(File location) {
        InputStream is = null;
        String result = null;

        try {
            is = new FileInputStream(location);
            result = normalizeCRLF(is);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    //do nothing
                }
            }
        }

        return result;
    }

    public static String normalizeCRLF(InputStream instream) {
        BufferedReader in = new BufferedReader(new InputStreamReader(instream));
        StringBuilder result = new StringBuilder();
        String line = null;

        try {
            line = in.readLine();
            while (line != null) {
                String[] tok = line.split("\\s");

                for (int x = 0; x < tok.length; x++) {
                    String token = tok[x];
                    result.append("  " + token);
                }
                line = in.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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

    public static List<File> getFiles(File dir, final String pattern) {
        return getFiles(dir, pattern, null);
    }
    public static List<File> getFilesRecurse(File dir, final String pattern) {
        return getFilesRecurse(dir, pattern, null);
    }

    public static List<File> getFiles(File dir, final String pattern, File exclude) {
        return getFilesRecurse(dir, Pattern.compile(pattern), exclude, false, new ArrayList<File>());
    }
    public static List<File> getFilesRecurse(File dir, final String pattern, File exclude) {
        return getFilesRecurse(dir, Pattern.compile(pattern), exclude, true, new ArrayList<File>());    
    }
    private static List<File> getFilesRecurse(File dir, 
                                              Pattern pattern,
                                              File exclude, boolean rec,
                                              List<File> fileList) {
        for (File file : dir.listFiles()) {
            if (file.equals(exclude)) {
                continue;
            }
            if (file.isDirectory() && rec) {
                getFilesRecurse(file, pattern, exclude, rec, fileList);
            } else {
                Matcher m = pattern.matcher(file.getName());
                if (m.matches()) {
                    fileList.add(file);                                
                }
            }
        }
        return fileList;
    }

    public static List<String> readLines(File file) throws Exception {
        if (!file.exists()) {
            return new ArrayList<String>();
        }
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String> results = new ArrayList<String>();
        try {
            String line = reader.readLine();
            while (line != null) {
                results.add(line);
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }
        return results;
    }
}
