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

package org.apache.cxf.tools.wsdlto.databinding.jaxb;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JPackage;

import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.tools.util.OutputStreamCreator;

public class TypesCodeWriter extends CodeWriter {

    /** The target directory to put source code. */
    private File target;
    private OutputStreamCreator osc;

    private List<String> excludeFileList = new ArrayList<>();
    private List<String> excludePkgList;

    private List<File> generatedFiles = new ArrayList<>();

    public TypesCodeWriter(File ftarget, List<String> excludePkgs, String e,
                           OutputStreamCreator outputStreamCreator)
        throws IOException {
        target = ftarget;
        excludePkgList = excludePkgs;
        setEncoding(e);
        osc = outputStreamCreator == null ? new OutputStreamCreator() : outputStreamCreator;
    }



    private void setEncoding(String s) {
        if (s != null) {
            try {
                //requires XJC 2.2.5 or newer
                Field f = CodeWriter.class.getDeclaredField("encoding");
                ReflectionUtil.setAccessible(f).set(this, s);
            } catch (Throwable t) {
                //ignore - should be caught in JAXBDataBinding.checkEncoding already
            }
        }
    }

    public OutputStream openBinary(JPackage pkg, String fileName) throws IOException {
        File f = getFile(pkg, fileName);
        generatedFiles.add(f);
        return osc.createOutputStream(getFile(pkg, fileName));
    }

    public List<File> getGeneratedFiles() {
        return generatedFiles;
    }

    protected File getFile(JPackage pkg, String fileName) throws IOException {
        String dirName = pkg.name().replace('.', File.separatorChar);
        File dir = pkg.isUnnamed() ? target : new File(target, dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File fn = new File(dir, fileName);
        if (excludePkgList.contains(pkg.name())) {
            excludeFileList.add(dirName + File.separator + fileName);
        }
        if (fn.exists() && !fn.delete()) {
            throw new IOException(fn + ": Can't delete previous version");
        }
        return fn;
    }

    public void close() throws IOException {

    }

    public List<String> getExcludeFileList() {
        return excludeFileList;
    }

}
