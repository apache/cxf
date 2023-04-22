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

package org.apache.cxf.tools.corba.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class FileOutputStreamFactory implements OutputStreamFactory {
    String dirName = "";
    List<String> fileNames;
    FileOutputStreamFactory parent;

    public FileOutputStreamFactory() {
        fileNames = new LinkedList<>();
    }


    public FileOutputStreamFactory(String dir) {
        this(dir, null);
        fileNames = new LinkedList<>();
    }


    public FileOutputStreamFactory(String dir, FileOutputStreamFactory p) {
        dirName = dir;

        if (dirName == null) {
            dirName = "";
        }

        if (!"".equals(dirName)
            && !".".equals(dirName)) {
            if (!dirName.endsWith(File.separator)) {
                dirName += File.separator;
            }

            File file = new File(dirName);

            //create directory
            file.mkdirs();
        }

        parent = p;
    }

    public String getDirectoryName() {
        return dirName;
    }


    private void addFileName(String name) {
        fileNames.add(name);

        if (parent != null) {
            parent.addFileName(name);
        }
    }


    private String getClassDirectory(String packageName) {
        String result = convertPackageNameToDirectory(packageName);

        if (!".".equals(dirName)) {
            result = dirName + result;
        }

        return result;
    }


    private String convertPackageNameToDirectory(String packageName) {
        int pos1 = 0;
        int pos2 = packageName.indexOf('.', pos1);

        StringBuilder result = new StringBuilder("");
        while (pos2 != -1) {
            result.append(packageName.substring(pos1, pos2)).append(File.separator);
            pos1 = pos2 + 1;
            pos2 = packageName.indexOf('.', pos1);
        }

        result.append(packageName.substring(pos1));

        return result.toString();
    }


    public OutputStream createFakeOutputStream(String name)
        throws IOException {
        addFileName(name);

        return new ByteArrayOutputStream();
    }


    public OutputStream createFakeOutputStream(String packageName, String name)
        throws IOException {
        String packageDirName = convertPackageNameToDirectory(packageName);

        if (!"".equals(packageDirName) && !packageDirName.endsWith(File.separator)) {
            packageDirName += File.separator;
        }

        addFileName(packageDirName + name);

        return new ByteArrayOutputStream();
    }


    public OutputStream createOutputStream(String packageName, String name)
        throws IOException {
        String packageDirName = convertPackageNameToDirectory(packageName);

        if (!"".equals(packageDirName) && !packageDirName.endsWith(File.separator)) {
            packageDirName += File.separator;
        }

        String dname = packageDirName;

        if (!".".equals(dirName)) {
            dname = dirName + packageDirName;
        }

        if (!"".equals(dname) && !".".equals(dname)) {
            File file = new File(dname);
            file.mkdirs();
        }

        addFileName(packageDirName + name);

        return Files.newOutputStream(Paths.get(dname + name));
    }


    public OutputStream createOutputStream(String name)
        throws IOException {
        addFileName(name);

        String dname = name;

        if (!".".equals(dirName)) {
            dname = dirName + name;
        }

        return  Files.newOutputStream(Paths.get(dname));
    }


    public OutputStreamFactory createSubpackageOutputStreamFactory(String name)
        throws IOException {
        String dname = name;

        if (!".".equals(dirName)) {
            dname = dirName + name;
        }

        return new FileOutputStreamFactory(dname, this);
    }


    public Iterator<String> getStreamNames() throws IOException {
        return fileNames.iterator();
    }


    public void clearStreams() {
        fileNames.clear();

        if (parent != null) {
            parent.clearStreams();
        }
    }


    public boolean isOutputStreamExists(String packageName, String name) {
        String dname = getClassDirectory(packageName);

        if (!"".equals(dname) && !dname.endsWith(File.separator)) {
            dname += File.separator;
        }

        File file = new File(dname + name);

        return file.exists();
    }
}


