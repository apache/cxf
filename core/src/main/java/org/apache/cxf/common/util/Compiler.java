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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.cxf.helpers.FileUtils;

public class Compiler {
    private long maxMemory = Runtime.getRuntime().maxMemory();
    private boolean verbose;
    private String target;
    private String outputDir;
    private String classPath;
    private String encoding;
    private boolean forceFork = Boolean.getBoolean(Compiler.class.getName() + "-fork");
    private File classpathTmpFile;
    private List<String> errors = new LinkedList<>();
    private List<String> warnings = new LinkedList<>();

    public Compiler() {
    }

    public List<String> getErrors() {
        return errors;
    }
    public List<String> getWarnings() {
        return warnings;
    }

    public void setMaxMemory(long l) {
        maxMemory = l;
    }
    public void setVerbose(boolean b) {
        verbose = b;
    }
    public void setTarget(String s) {
        target = s;
    }
    public void setOutputDir(File s) {
        if (s != null) {
            outputDir = s.getAbsolutePath().replace(File.pathSeparatorChar, '/');
        } else {
            outputDir = null;
        }
    }
    public void setOutputDir(String s) {
        outputDir = s.replace(File.pathSeparatorChar, '/');
    }
    public void setClassPath(String s) {
        classPath = StringUtils.isEmpty(s) ? null : s;
    }

    // https://issues.apache.org/jira/browse/CXF-8049
    private String getSystemClassPath() {
        String javaClasspath = SystemPropertyAction.getProperty("java.class.path");

        if (!StringUtils.isEmpty(javaClasspath)) {
            List<String> correctedEntries = new ArrayList<>();

            String[] toks = javaClasspath.split(File.pathSeparator);
            
            for (String tok: toks) {
                // if any classpath entry contains a whitespace char, 
                // enclose the entry in double quotes
                if (tok.matches(".*\\s+.*")) {
                    correctedEntries.add("\"" + tok + "\"");
                } else {
                    correctedEntries.add(tok);
                }
            }

            return String.join(File.pathSeparator, correctedEntries);
        }

        return javaClasspath;
    }

    protected void addArgs(List<String> list) {
        if (!StringUtils.isEmpty(encoding)) {
            list.add("-encoding");
            list.add(encoding);
        }
        if (!StringUtils.isEmpty(target)) {
            list.add("-target");
            list.add(target);
            list.add("-source");
            list.add(target);
        }

        if (!StringUtils.isEmpty(outputDir)) {
            list.add("-d");
            list.add(outputDir);
        }

        if (StringUtils.isEmpty(classPath)) {
            String javaClasspath = getSystemClassPath();
            boolean classpathSetted = !StringUtils.isEmpty(javaClasspath);
            if (!classpathSetted) {
                File f = new File(getClass().getClassLoader().getResource(".").getFile());
                f = new File(f, "../lib");
                if (f.exists() && f.isDirectory()) {
                    list.add("-extdirs");
                    list.add(f.toString());
                }
            } else {
                list.add("-classpath");
                list.add(javaClasspath);
            }
        } else {
            list.add("-classpath");
            list.add(classPath);
        }

    }
    public boolean compileFiles(File[] files) {
        List<String> f = new ArrayList<>(files.length);
        for (File file : files) {
            f.add(file.getAbsolutePath());
        }
        return compileFiles(f.toArray(new String[0]));
    }
    public boolean compileFiles(List<File> files) {
        List<String> f = new ArrayList<>(files.size());
        for (File file : files) {
            f.add(file.getAbsolutePath());
        }
        return compileFiles(f.toArray(new String[0]));
    }
    public boolean compileFiles(String[] files) {
        String endorsed = SystemPropertyAction.getProperty("java.endorsed.dirs");
        if (!forceFork) {
            return useJava6Compiler(files);
        }

        List<String> list = new ArrayList<>();

        // Start of honoring java.home for used javac
        String fsep = File.separator;
        String javacstr = "javac";
        String platformjavacname = "javac";

        if (SystemPropertyAction.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
            platformjavacname = "javac.exe";
        }

        if (new File(SystemPropertyAction.getProperty("java.home") + fsep + platformjavacname).exists()) {
            // check if java.home is jdk home
            javacstr = SystemPropertyAction.getProperty("java.home") + fsep + platformjavacname;
        } else if (new File(SystemPropertyAction.getProperty("java.home") + fsep + ".." + fsep + "bin" + fsep
                            + platformjavacname).exists()) {
            // check if java.home is jre home
            javacstr = SystemPropertyAction.getProperty("java.home") + fsep + ".." + fsep + "bin" + fsep
                       + platformjavacname;
        } else if (new File(SystemPropertyAction.getProperty("java.home") + fsep + "bin" + fsep
                            + platformjavacname).exists()) {
            //java9
            javacstr = SystemPropertyAction.getProperty("java.home") + fsep + "bin" + fsep
                + platformjavacname;
        }
        list.add(javacstr);
        // End of honoring java.home for used javac

        if (!StringUtils.isEmpty(endorsed)) {
            list.add("-endorseddirs");
            list.add(endorsed);
        }

        //fix for CXF-2081, set maximum heap of this VM to javac.
        list.add("-J-Xmx" + maxMemory);

        addArgs(list);
        int classpathIdx = list.indexOf("-classpath");
        String classpath = list.get(classpathIdx + 1);
        checkLongClasspath(classpath, list, classpathIdx);
        int idx = list.size();
        Collections.addAll(list, files);

        return internalCompile(list.toArray(new String[0]), idx);
    }

    protected boolean useJava6Compiler(String[] files) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                "No compiler detected, make sure you are running on top of a JDK instead of a JRE.");
        }
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> fileList = fileManager.getJavaFileObjectsFromStrings(Arrays
            .asList(files));

        return internalJava6Compile(compiler, wrapJavaFileManager(fileManager), setupDiagnosticListener(),
                                    fileList);
    }

    protected JavaFileManager wrapJavaFileManager(StandardJavaFileManager standardJavaFileManger) {
        return standardJavaFileManger;
    }

    protected DiagnosticListener<JavaFileObject> setupDiagnosticListener() {
        return new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                switch (diagnostic.getKind()) {
                case ERROR:
                    errors.add(diagnostic.toString());
                    if (verbose) {
                        System.err.println(diagnostic.toString());
                    }
                    break;
                case WARNING:
                case MANDATORY_WARNING:
                    warnings.add(diagnostic.toString());
                    if (verbose) {
                        System.err.println(diagnostic.toString());
                    }
                    break;
                default:
                    break;
                }
            }
        };
    }

    protected boolean internalJava6Compile(JavaCompiler compiler, JavaFileManager fileManager,
                                           DiagnosticListener<JavaFileObject> listener,
                                           Iterable<? extends JavaFileObject> fileList) {
        List<String> args = new ArrayList<>();
        addArgs(args);
        CompilationTask task = compiler.getTask(null, fileManager, listener, args, null, fileList);
        Boolean ret = task.call();
        try {
            fileManager.close();
        } catch (IOException e) {
            System.err.print("[ERROR] IOException during compiling.");
            e.printStackTrace();
        }
        return ret;
    }

    public boolean internalCompile(String[] args, int sourceFileIndex) {
        File tmpFile = null;
        try {
            final String[] cmdArray;
            if (isLongCommandLines(args) && sourceFileIndex >= 0) {
                tmpFile = FileUtils.createTempFile("cxf-compiler", null);
                try (PrintWriter out = new PrintWriter(new FileWriter(tmpFile))) {
                    for (int i = sourceFileIndex; i < args.length; i++) {
                        if (args[i].indexOf(' ') > -1) {
                            args[i] = args[i].replace(File.separatorChar, '/');
                            //
                            // javac gives an error if you use forward slashes
                            // with package-info.java. Refer to:
                            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6198196
                            //
                            if (args[i].indexOf("package-info.java") > -1
                                && SystemPropertyAction.getProperty("os.name")
                                    .toLowerCase().indexOf("windows") > -1) {
                                out.println('"' + args[i].replaceAll("/", "\\\\\\\\") + '"');
                            } else {
                                out.println('"' + args[i] + '"');
                            }
                        } else {
                            out.println(args[i]);
                        }
                    }
                    out.flush();
                }
                cmdArray = new String[sourceFileIndex + 1];
                System.arraycopy(args, 0, cmdArray, 0, sourceFileIndex);
                cmdArray[sourceFileIndex] = "@" + tmpFile;
            } else {
                cmdArray = new String[args.length];
                System.arraycopy(args, 0, cmdArray, 0, args.length);
            }

            if (SystemPropertyAction.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
                for (int i = 0; i < cmdArray.length; i++) {
                    if (cmdArray[i].indexOf("package-info") == -1) {
                        cmdArray[i] = cmdArray[i].replace('\\', '/');
                    }
                }
            }

            final Process p = Runtime.getRuntime().exec(cmdArray);

            if (p.getErrorStream() != null) {
                StreamPrinter errorStreamPrinter = new StreamPrinter(p.getErrorStream(), "", System.out);
                errorStreamPrinter.start();
            }

            if (p.getInputStream() != null) {
                StreamPrinter infoStreamPrinter = new StreamPrinter(p.getInputStream(), "[INFO]", System.out);
                infoStreamPrinter.start();
            }

            return p.waitFor() == 0 ? true : false;
        } catch (SecurityException e) {
            System.err.println("[ERROR] SecurityException during exec() of compiler \"" + args[0] + "\".");
        } catch (InterruptedException e) {
            // ignore

        } catch (IOException e) {
            System.err.print("[ERROR] IOException during exec() of compiler \"" + args[0] + "\"");
            System.err.println(". Check your path environment variable.");
        } finally {
            if (tmpFile != null && tmpFile.exists()) {
                FileUtils.delete(tmpFile);
            }
            if (classpathTmpFile != null && classpathTmpFile.exists()) {
                FileUtils.delete(classpathTmpFile);
            }
        }

        return false;
    }

    private boolean isLongCommandLines(String[] args) {
        StringBuilder strBuffer = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            strBuffer.append(args[i]);
        }
        return strBuffer.length() > 4096;
    }

    private boolean isLongClasspath(String classpath) {
        return classpath.length() > 2048;
    }

    private void checkLongClasspath(String classpath, List<String> list, int classpathIdx) {
        if (isLongClasspath(classpath)) {
            try {
                classpathTmpFile = FileUtils.createTempFile("cxf-compiler-classpath", null);
                try (PrintWriter out = new PrintWriter(new FileWriter(classpathTmpFile))) {
                    out.println(classpath);
                    out.flush();
                }
                list.set(classpathIdx + 1, "@" + classpathTmpFile);
            } catch (IOException e) {
                System.err.print("[ERROR] can't write long classpath to @argfile");
            }
        }
    }

    public void setEncoding(String string) {
        encoding = string;
    }

}
