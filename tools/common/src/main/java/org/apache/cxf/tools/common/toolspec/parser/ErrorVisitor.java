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

package org.apache.cxf.tools.common.toolspec.parser;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;


public class ErrorVisitor {
    public static final long serialVersionUID = 1L;

    private static final Logger LOG = LogUtils.getL7dLogger(ErrorVisitor.class);

    private final Set<Object> errors = new HashSet<Object>();

    public static class MissingOption implements CommandLineError {
        private final Option o;

        public MissingOption(Option op) {
            this.o = op;
        }

        public String toString() {
            return "Missing option: " + o.getPrimarySwitch();
        }

        public Option getOption() {
            return o;
        }

        public String getOptionSwitch() {
            return o.getPrimarySwitch();
        }
    }

    public static class DuplicateOption implements CommandLineError {
        private final String option;

        public DuplicateOption(String opt) {
            option = opt;
        }

        public String toString() {
            return "Duplicated option: " + option;
        }

        public String getOptionSwitch() {
            return option;
        }
    }

    public static class DuplicateArgument implements CommandLineError {
        private final String argument;

        public DuplicateArgument(String arg) {
            this.argument = arg;
        }

        public String toString() {
            return "Duplicated argument: " + argument;
        }

        public String getOptionSwitch() {
            return argument;
        }
    }

    public static class UnexpectedOption implements CommandLineError {
        private final String option;

        public UnexpectedOption(String opt) {
            this.option = opt;
        }

        public String toString() {
            return "Unexpected option: " + option;
        }

        public String getOptionSwitch() {
            return option;
        }
    }

    public static class UnexpectedArgument implements CommandLineError {
        private final String arg;

        public UnexpectedArgument(String a) {
            this.arg = a;
        }

        public String toString() {
            return "Unexpected argument: " + arg;
        }

        public String getArgument() {
            return arg;
        }
    }

    public static class InvalidOption implements CommandLineError {
        private final String option;

        public InvalidOption(String opt) {
            this.option = opt;
        }

        public String toString() {
            return "Invalid option: " + option + " is missing its associated argument";
        }

        public String getOptionSwitch() {
            return option;
        }
    }

    public static class MissingArgument implements CommandLineError {
        private final String arg;

        public MissingArgument(String a) {
            this.arg = a;
        }

        public String toString() {
            return "Missing argument: " + arg;
        }

        public String getArgument() {
            return arg;
        }
    }

    public static class UserError implements CommandLineError {
        private final String msg;

        public UserError(String m) {
            this.msg = m;
        }

        public String toString() {
            return msg;
        }

        public String getMessage() {
            return msg;
        }
    }

    public Collection getErrors() {
        return errors;
    }

    public void add(CommandLineError err) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Adding error: " + err);
        }
        errors.add(err);
    }

    public String toString() {
        StringBuilder res = new StringBuilder();

        for (Iterator it = errors.iterator(); it.hasNext();) {
            res.append(it.next().toString()).append(System.getProperty("line.separator"));
        }
        return res.toString();
    }

}
