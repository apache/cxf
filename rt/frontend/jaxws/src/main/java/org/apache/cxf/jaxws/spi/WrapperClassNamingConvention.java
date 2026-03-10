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
package org.apache.cxf.jaxws.spi;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.cxf.common.util.PackageUtils;
/**
 * Provides names for storing generated wrapper classes.
 *
 * @since 4.1.1
 */
public interface WrapperClassNamingConvention {

    /**
     * Returns a package name unique for the given {@code sei} and {@code anonymous}
     * parameters suitable for storing generated wrapper classes.
     *
     * @param sei the service endpoint interface for which the package name should be created
     * @param anonymous whether the generated wrapper types are anonymous
     * @return a valid Java package name
     */
    String getWrapperClassPackageName(Class<?> sei, boolean anonymous);

    /**
     * Default naming scheme since CXF 4.2.0.
     * <p>
     * The package name returned by {@link #getWrapperClassPackageName(Class, boolean)} are unique
     * per given {@code sei}.
     * <p>
     * Examples:
     * <table>
     * <tr>
     * <th>SEI</th><th>anonymous</th>
     * <th>{@code getWrapperClassPackageName()} return value</th>
     * </tr>
     * <tr>
     * <td>{@code org.example.Service}</td>
     * <td>{@code false}</td>
     * <td>{@code org.example.jaxws_asm.service}</td>
     * </tr>
     * <tr>
     * <td>{@code org.example.OuterClass$Service}</td>
     * <td>{@code false}</td>
     * <td>{@code org.example.jaxws_asm.outerclass_service}</td>
     * </tr>
     * <tr>
     * <td>{@code org.example.Service}</td>
     * <td>{@code true}</td>
     * <td>{@code org.example.jaxws_asm_an.service}</td>
     * </tr>
     * </table>
     *
     * @since 4.1.1
     */
    class DefaultWrapperClassNamingConvention implements WrapperClassNamingConvention {
        public static final String DEFAULT_PACKAGE_NAME = "defaultnamespace";
        private static final Pattern JAVA_PACKAGE_NAME_SANITIZER_PATTERN = Pattern.compile("[^a-zA-Z0-9_]");

        @Override
        public String getWrapperClassPackageName(Class<?> sei, boolean anonymous) {
            final String className = sei.getName();
            final int start = className.startsWith("[L") ? 2 : 0;
            final int end = className.lastIndexOf('.');
            final String pkg;
            final String cl;
            if (end >= 0) {
                pkg = className.substring(start, end);
                cl = className.substring(end + 1);
            } else {
                pkg = DefaultWrapperClassNamingConvention.DEFAULT_PACKAGE_NAME;
                cl = className;
            }

            return pkg
                    + (anonymous ? ".jaxws_asm_an." : ".jaxws_asm.")
                    + JAVA_PACKAGE_NAME_SANITIZER_PATTERN.matcher(cl).replaceAll("_").toLowerCase(Locale.ROOT);
        }

    }

    /**
     * An implementation restoring the behavior of CXF before version 4.2.0.
     * <p>
     * Unlike with {@link DefaultWrapperClassNamingConvention}, this implementation's
     * {@link #getWrapperClassPackageName(Class, boolean)} takes only package name
     * of the given {@code sei} into account.
     * Therefore naming clashes may occur if two SEIs are in the same package
     * and both of them have a method with the same name but possibly different signature.
     * <p>
     * Examples:
     * <table>
     * <tr>
     * <th>SEI</th>
     * <th>anonymous</th>
     * <th>{@code getWrapperClassPackageName()} return value</th>
     * </tr>
     * <tr>
     * <td>{@code org.example.Service}</td>
     * <td>{@code false}</td>
     * <td>{@code org.example.jaxws_asm}</td>
     * </tr>
     * <tr>
     * <td>{@code org.example.OuterClass$Service}</td>
     * <td>{@code false}</td>
     * <td>{@code org.example.jaxws_asm}</td>
     * </tr>
     * <tr>
     * <td>{@code org.example.Service}</td>
     * <td>{@code true}</td>
     * <td>{@code org.example.jaxws_asm_an}</td>
     * </tr>
     * </table>
     *
     * @since 4.1.1
     */
    class LegacyWrapperClassNamingConvention implements WrapperClassNamingConvention {

        @Override
        public String getWrapperClassPackageName(Class<?> sei, boolean anonymous) {
            return getPackageName(sei) + ".jaxws_asm" + (anonymous ? "_an" : "");
        }

        private String getPackageName(Class<?> sei) {
            String pkg = PackageUtils.getPackageName(sei);
            return pkg.length() == 0 ? DefaultWrapperClassNamingConvention.DEFAULT_PACKAGE_NAME : pkg;
        }

    }

}
