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

package org.apache.cxf.tools.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.ToolException;

public class FileWriterUtil {
    private static final Logger LOG = LogUtils.getL7dLogger(FileWriterUtil.class);
    private final File target;

    public FileWriterUtil(String targetDir) throws ToolException {
        target = new File(targetDir);
        if (!(target.exists()) || !(target.isDirectory())) {
            Message msg = new Message("DIRECTORY_NOT_EXIST", LOG, target);
            throw new ToolException(msg);
        }
    }

    public File getFileToWrite(String packageName, String fileName) throws IOException {
        File dir = buildDir(packageName);
        File fn = new File(dir, fileName);
        if (fn.exists() && !fn.delete()) {
            throw new IOException(fn + ": Can't delete previous version");
        }
        return fn;
    }

    public static Writer getWriter(File fn) throws IOException {
        return new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(fn)), "UTF-8");
    }

    public static Writer getWriter(File fn, String encoding) throws IOException {
        if (encoding == null) {
            encoding = "UTF-8";
        }
        return new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(fn)), encoding);
    }
    
    public Writer getWriter(String packageName, String fileName) throws IOException {
        return getWriter(getFileToWrite(packageName, fileName));
    }

    public boolean isCollision(String packageName, String fileName) throws ToolException {
        File dir = buildDir(packageName);
        return fileExist(dir, fileName);
    }

    public File buildDir(String packageName) {
        File dir;
        if (packageName == null) {
            dir = target;
        } else {
            dir = new File(target, toDir(packageName));
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private boolean fileExist(File dir, String fileName) {
        return new File(dir, fileName).exists();
    }

    private String toDir(String packageName) {
        return packageName.replace('.', File.separatorChar);
    }

}
