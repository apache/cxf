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
package org.apache.cxf.maven_plugin.javatowadl;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.util.DocTrees;



import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

public final class DumpJavaDoc implements Doclet {
    private String dumpFileName;
    private Reporter reporter;
    
    private final class DumpJavaDocFileOption implements Option {
        @Override
        public int getArgumentCount() {
            return 1;
        }

        @Override
        public String getDescription() {
            return "Specify the file to dump Javadoc for later use";
        }

        @Override
        public Kind getKind() {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return Collections.singletonList("-dumpJavaDocFile");
        }

        @Override
        public String getParameters() {
            return "theFileToDumpJavaDocForLaterUse";
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            dumpFileName = arguments.get(0);
            return true;
        }
    }

    public DumpJavaDoc() {

    }

    @Override
    public void init(Locale locale, Reporter r) {
        this.reporter = r;
    }

    @Override
    public String getName() {
        return "DumpJavaDoc";
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public boolean run(DocletEnvironment docEnv) {
        final Elements utils = docEnv.getElementUtils();
        final DocTrees docTrees = docEnv.getDocTrees();
        
        try (OutputStream os = Files.newOutputStream(Paths.get(dumpFileName))) {
            final Properties javaDocMap = new Properties();
            for (Element element : docEnv.getIncludedElements()) {
                if (element.getKind() == ElementKind.CLASS) {
                    final TypeElement classDoc = (TypeElement) element;
                    final DocCommentTree classCommentTree = docTrees.getDocCommentTree(classDoc);
                    
                    if (classCommentTree != null) {
                        javaDocMap.put(classDoc.toString(), getAllComments(classCommentTree.getFullBody()));
                    }
                    
                    for (Element member: classDoc.getEnclosedElements()) {
                        // Skip all non-public methods
                        if (!member.getModifiers().contains(Modifier.PUBLIC)) {
                            continue;
                        }
                        
                        if (member.getKind() == ElementKind.METHOD) {
                            final ExecutableElement method = (ExecutableElement) member;
                            final DocCommentTree methodCommentTree = docTrees.getDocCommentTree(method);
                            final String qualifiedName = utils.getBinaryName(classDoc) + "." + method.getSimpleName();
                            
                            if (methodCommentTree == null) {
                                javaDocMap.put(qualifiedName, "");
                            } else  {
                                javaDocMap.put(qualifiedName, getAllComments(methodCommentTree.getFullBody()));
                                for (DocTree tree: methodCommentTree.getBlockTags()) {
                                    if (tree.getKind() == DocTree.Kind.RETURN) {
                                        final ReturnTree returnTree = (ReturnTree) tree;
                                        javaDocMap.put(qualifiedName + ".returnCommentTag", 
                                            getAllComments(returnTree.getDescription()));
                                    } else if (tree.getKind() == DocTree.Kind.PARAM) {
                                        final ParamTree paramTree = (ParamTree) tree;
                                        final int index = getParamIndex(method, paramTree);
                                        // CHECKSTYLE:OFF
                                        if (index >= 0) {
                                            javaDocMap.put(qualifiedName + ".paramCommentTag." + index, 
                                                getAllComments(paramTree.getDescription()));
                                        }
                                        // CHECKSTYLE:ON
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            javaDocMap.store(os, "");
            os.flush();
        } catch (final IOException ex) {
            reporter.print(Diagnostic.Kind.ERROR, ex.getMessage());
        }
        
        return true;
    }
    
    private int getParamIndex(final ExecutableElement method, final ParamTree paramTree) {
        final List<? extends VariableElement> parameters = method.getParameters();
        
        for (int i = 0; i < parameters.size(); ++i) {
            if (paramTree.getName().getName().contentEquals(parameters.get(i).getSimpleName())) {
                return i;
            }
        } 
        
        return -1;
    }

    private String getAllComments(final Collection<? extends DocTree> comments) {
        return comments
            .stream()
            .map(DocTree::toString)
            .collect(Collectors.joining());
    }
    
    @Override
    public Set<Option> getSupportedOptions() {
        return Collections.singleton(new DumpJavaDocFileOption());
    }
}
