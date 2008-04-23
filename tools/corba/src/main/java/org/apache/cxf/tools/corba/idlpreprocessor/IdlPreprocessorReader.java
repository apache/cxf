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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.URL;
import java.util.Stack;

/**
 * A Reader that implements the #include functionality of the preprocessor.
 * Starting from one URL, it generates one stream of characters by tracking
 * #defines, #ifdefs, etc. and following #includes accordingly.
 * 
 * <p>
 * This reader augments the stream with
 * <a href="http://gcc.gnu.org/onlinedocs/gcc-3.2.3/cpp/Preprocessor-Output.html">
 * location information</a> when the source URL is switched.
 * This improves error reporting (with correct file and linenumber information) in the
 * subsequent compilation steps like IDL parsing and also allows the implentation
 * of code generation options like the -emitAll flag available in the JDK idlj tool.
 * </p>
 */
public final class IdlPreprocessorReader extends Reader {

    /**
     * Maximum depth of {@link #includeStack} to prevent infinite recursion.
     */
    private static final int MAX_INCLUDE_DEPTH = 64;

    /**
     * GNU standard preprocessor output flag for signalling a new file.
     * 
     * @see http://gcc.gnu.org/onlinedocs/gcc-3.2.3/cpp/Preprocessor-Output.html
     */
    private static final char PUSH = '1';

    /**
     * GNU standard preprocessor output flag for signalling returning to a file.
     * 
     * @see http://gcc.gnu.org/onlinedocs/gcc-3.2.3/cpp/Preprocessor-Output.html
     */
    private static final char POP = '2';

    private static final String LF = System.getProperty("line.separator");

    private final IncludeResolver includeResolver;

    private final Stack<IncludeStackEntry> includeStack = new Stack<IncludeStackEntry>();

    /**
     * Stack of Booleans, corresponding to nested 'if' preprocessor directives.
     * The top of the stack signals whether the current idl code is skipped.
     * 
     * @see #skips()
     */
    private final Stack<Boolean> ifStack = new Stack<Boolean>();

    private final DefineState defineState;

    private final StringBuilder buf = new StringBuilder();

    private int readPos;

    /**
     * Creates a new IncludeReader.
     * 
     * @param startURL
     * @param startLocation
     * @param includeResolver
     * @param defineState
     * @throws IOException
     */
    public IdlPreprocessorReader(URL startURL,
                                 String startLocation,
                                 IncludeResolver resolver,
                                 DefineState state)
        throws IOException {
        this.includeResolver = resolver;
        this.defineState = state;
        pushInclude(startURL, startLocation);
        fillBuffer();
    }

    /**
     * @param url
     * @throws IOException
     */
    private void pushInclude(URL url, String location) throws IOException {
        final IncludeStackEntry includeStackEntry = new IncludeStackEntry(url, location);
        includeStack.push(includeStackEntry);
        final int lineNumber = getReader().getLineNumber();
        signalFileChange(location, lineNumber, PUSH);
    }

    /**
     * @see Reader#close()
     */
    public void close() throws IOException {
        buf.setLength(0);
    }

    /**
     * @see Reader#read(char[], int, int)
     */
    public int read(char[] cbuf, int off, int len) throws IOException {

        final int buflen = buf.length();
        if (readPos >= buflen) {
            return -1;
        }

        int numCharsRead = Math.min(len, buflen - readPos);
        buf.getChars(readPos, readPos + numCharsRead, cbuf, off);
        readPos += numCharsRead;
        return numCharsRead;
    }

    /**
     * @see Reader#read()
     */
    public int read() throws IOException {

        if (buf.length() == 0) {
            return -1;
        } else {
            return buf.charAt(readPos++);
        }
    }

    private void fillBuffer() throws IOException {
        while (!includeStack.isEmpty()) {
            LineNumberReader reader = getReader();
            final int lineNo = reader.getLineNumber();
            String line = reader.readLine();

            if (line == null) {
                popInclude();
                continue;
            }
            line = processComments(line);
            
            if (!line.trim().startsWith("#")) {
                if (!skips()) {
                    buf.append(line);
                }
                buf.append(LF);
                continue;
            }

            final IncludeStackEntry ise = includeStack.peek();
            line = line.trim();
            line = processPreprocessorComments(buf, line);

            if (line.startsWith("#include")) {
                handleInclude(line, lineNo, ise);
            } else if (line.startsWith("#ifndef")) {
                handleIfndef(line);
            } else if (line.startsWith("#ifdef")) {
                handleIfdef(line);
            } else if (line.startsWith("#endif")) {
                handleEndif(lineNo, ise);
            } else if (line.startsWith("#else")) {
                handleElse(lineNo, ise);
            } else if (line.startsWith("#define")) {
                handleDefine(line);
            } else {
                throw new PreprocessingException("unknown preprocessor instruction", ise.getURL(), lineNo);
            }
        }
    }

    private String processComments(String line) {
        int pos = line.indexOf("**/");
        //The comments need to be end with */, so if the line has ****/,
        //we need to insert space to make it *** */
        if ((pos != -1) && (pos != 0)) {
            line = line.substring(0, pos) + " " + line.substring(pos + 1);
        }
        return line;
    }

    private String processPreprocessorComments(StringBuilder buffer, String line) {
        int pos = line.indexOf("//");
        if ((pos != -1) && (pos != 0)) {
            buffer.append(line.substring(pos));
            line = line.substring(0, pos);
        }
        pos = line.indexOf("/*");
        if ((pos != -1) && (pos != 0)) {
            buffer.append(line.substring(pos));
            line = line.substring(0, pos);
        }
        return line;
    }
    
    /**
     * TODO: support multiline definitions, functions, etc. 
     */
    private void handleDefine(String line) {
        buf.append(LF);
        if (skips()) {
            return;
        }
        String def = line.substring("#define".length()).trim();
        int idx = def.indexOf(' ');
        if (idx == -1) {
            defineState.define(def, null);
        } else {
            String symbol = def.substring(0, idx);
            String value = def.substring(idx + 1).trim();
            defineState.define(symbol, value);
        }
    }

    private void handleElse(int lineNo, final IncludeStackEntry ise) {
        if (ifStack.isEmpty()) {
            throw new PreprocessingException("unexpected #else", ise.getURL(), lineNo);
        }
        boolean top = ifStack.pop();
        ifStack.push(!top);
        buf.append(LF);
    }

    private void handleEndif(int lineNo, final IncludeStackEntry ise) {
        if (ifStack.isEmpty()) {
            throw new PreprocessingException("unexpected #endif", ise.getURL(), lineNo);
        }
        ifStack.pop();
        buf.append(LF);
    }

    private void handleIfdef(String line) {
        String symbol = line.substring("#ifdef".length()).trim();
        boolean isDefined = defineState.isDefined(symbol);
        registerIf(!isDefined);
        buf.append(LF);
    }

    private void handleIfndef(String line) {
        String symbol = line.substring("#ifndef".length()).trim();
        boolean isDefined = defineState.isDefined(symbol);
        registerIf(isDefined);
        buf.append(LF);
    }

    private void handleInclude(String line, int lineNo, final IncludeStackEntry ise) throws IOException {
        
        if (skips()) {
            buf.append(LF);
            return;
        }

        if (includeStack.size() >= MAX_INCLUDE_DEPTH) {
            throw new PreprocessingException("more than " + MAX_INCLUDE_DEPTH
                    + " nested #includes - assuming infinite recursion, aborting", ise.getURL(), lineNo);
        }

        String arg = line.replaceFirst("#include", "").trim();
        if (arg.length() == 0) {
            throw new PreprocessingException("#include without an argument", ise.getURL(), lineNo);
        }

        char first = arg.charAt(0);
        final int lastIdx = arg.length() - 1;
        char last = arg.charAt(lastIdx);
        if (arg.length() < 3 || !(first == '<' && last == '>') && !(first == '"' && last == '"')) {
            throw new PreprocessingException(
                    "argument for '#include' must be enclosed in '< >' or '\" \"'", ise.getURL(), lineNo);
        }
        String spec = arg.substring(1, lastIdx);
        URL include = (first == '<') ? includeResolver.findSystemInclude(spec)
            : includeResolver.findUserInclude(spec);

        if (include == null) {
            throw new PreprocessingException("unable to resolve include '" + spec + "'", ise.getURL(),
                                             lineNo);
        }
        pushInclude(include, spec);
    }

    private void popInclude() throws IOException {
        final IncludeStackEntry poppedStackEntry = includeStack.pop();
        if (!includeStack.isEmpty()) {
            buf.append(LF);
        }
        try {
            if (includeStack.size() > 0) {
                final IncludeStackEntry newTopEntry = includeStack.peek();
                final LineNumberReader reader = getReader();
                final int lineNumber = reader.getLineNumber();
                final String location = newTopEntry.getLocation();
                signalFileChange(location, lineNumber, POP);
            }
        } finally {
            poppedStackEntry.getReader().close();
        }
    }

    private boolean skips() {
        if (ifStack.isEmpty()) {
            return false;
        }

        return ifStack.peek();
    }

    private void registerIf(boolean skip) {
        ifStack.push(skip);
    }

    private LineNumberReader getReader() {
        IncludeStackEntry topOfStack = includeStack.peek();
        return topOfStack.getReader();
    }

    /**
     * Creates GNU standard preprocessor flag for signalling a file change.
     * 
     * @see http://gcc.gnu.org/onlinedocs/gcc-3.2.3/cpp/Preprocessor-Output.html
     */
    private void signalFileChange(String location, int lineNumber, char flag) {
        buf.append("# ").append(lineNumber).append(' ').append(location).append(' ').append(flag).append(LF);
    }

}
