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

package org.apache.cxf.common.classloader;

import java.security.SecureClassLoader;

/**
 * The FireWallClassLoader is a classloader that can block request from going up
 * in the classloader hierarchy.
 * <P>
 * Normally, when a classloader receives a request for a resource, it will
 * consult its parent class loader first for that resource. The parent class
 * loader is typically the System ClassLoader. If the parent class loader cannot
 * provide the requested resource, the child class loader will be consulted for
 * the request. <I>Note: the parent class loader must not be confused by the
 * superclass of a certain class loader (e.g. SecureClassLoader). The parent
 * classloader is identified at constuction time and passed in as an constructor
 * argument.</I>
 * <P>
 * Consulting the parent classloader first can be inconvenient for certain
 * applications that want guarantees about which classloader is used to load a
 * certain class. This could be because you want to be certain about where the
 * resource came from, or you want to protect yourself against (other versions)
 * of the same class that could be served by the System ClassLoader (e.g.
 * because someone put them on the classpath or in the extensions directory).
 * <P>
 * For these cases, the FireWallClassLoader can be used.
 * 
 * <PRE>
 * 
 * System ClassLoader | FireWallClassLoader | User's ClassLoader
 * 
 * </PRE>
 * 
 * The FireWallClassLoader is placed between the user's class loader and the
 * parent class loader. It has a set of filters that define what classes are
 * allowed to go through. These filters describe (a groups of) packages, or a
 * specific classes or resources that are allowed through to the parent
 * classloader. Take as example this filter set:
 * 
 * <pre>
 * [&quot;com.iona.&quot;, &quot;javax.servlet.jsp.&quot;]
 * </pre>
 * 
 * This will allow requests to any class/resource staring with com.iona. or
 * javax.servlet.jsp. through to the parent classloader and block all other
 * requests.
 * <P>
 * A very common set of filters would be a set that allows nothing through
 * except the classes used by the JDK. The {@link JDKFireWallClassLoaderFactory}
 * factory class can create such FireWallClassLoader.
 * <P>
 * The FireWallClassLoader does not load any classes.
 */
public class FireWallClassLoader extends SecureClassLoader {
    private final String[] filters;
    private final String[] fnFilters;
    private final String[] negativeFilters;
    private final String[] negativeFNFilters;

    /**
     * Constructor.
     * 
     * @param parent The Parent ClassLoader to use.
     * @param fs A set of filters to let through. The filters and be either in
     *            package form (<CODE>org.omg.</CODE> or <CODE>org.omg.*</CODE>)
     *            or specify a single class (<CODE>junit.framework.TestCase</CODE>).
     *            <P>
     *            When the package form is used, all classed in all subpackages
     *            of this package are let trough the firewall. When the class
     *            form is used, the filter only lets that single class through.
     *            Note that when that class depends on another class, this class
     *            does not need to be mentioned as a filter, because if the
     *            originating class is loaded by the parent classloader, the
     *            FireWallClassLoader will not receive requests for the
     *            dependant class.
     */
    public FireWallClassLoader(ClassLoader parent, String[] fs) {
        this(parent, fs, new String[0]);
    }

    /**
     * Constructor.
     * 
     * @param parent The Parent ClassLoader to use.
     * @param fs A set of filters to let through. The filters and be either in
     *            package form (<CODE>org.omg.</CODE> or <CODE>org.omg.*</CODE>)
     *            or specify a single class (<CODE>junit.framework.TestCase</CODE>).
     *            <P>
     *            When the package form is used, all classed in all subpackages
     *            of this package are let trough the firewall. When the class
     *            form is used, the filter only lets that single class through.
     *            Note that when that class depends on another class, this class
     *            does not need to be mentioned as a filter, because if the
     *            originating class is loaded by the parent classloader, the
     *            FireWallClassLoader will not receive requests for the
     *            dependant class.
     * @param negativeFs List of negative filters to use. Negative filters take
     *            precedence over positive filters. When a class or resource is
     *            requested that matches a negative filter it is not let through
     *            the firewall even if an allowing filter would exist in the
     *            positive filter list.
     */
    public FireWallClassLoader(ClassLoader parent, String[] fs, String[] negativeFs) {
        super(parent);

        this.filters = processFilters(fs);
        this.negativeFilters = processFilters(negativeFs);

        this.fnFilters = filters2FNFilters(this.filters);
        this.negativeFNFilters = filters2FNFilters(this.negativeFilters);

        boolean javaCovered = false;
        if (this.filters == null) {
            javaCovered = true;
        } else {
            for (int i = 0; i < this.filters.length; i++) {
                if (this.filters[i].equals("java.")) {
                    javaCovered = true;
                }
            }
        }

        if (this.negativeFilters != null) {
            String java = "java.";
            // try all that would match java: j, ja, jav, java and java.
            for (int i = java.length(); i >= 0; i--) {
                for (int j = 0; j < this.negativeFilters.length; j++) {
                    if (negativeFilters[j].equals(java.substring(0, i))) {
                        javaCovered = false;
                    }
                }
            }
        }

        if (!javaCovered) {
            throw new SecurityException("It's unsafe to construct a " 
                        + "FireWallClassLoader that does not let the java. " 
                        + "package through.");
        }
    }

    private static String[] processFilters(String[] fs) {
        if (fs == null || fs.length == 0) {
            return null;
        }

        String[] f = new String[fs.length];
        for (int i = 0; i < fs.length; i++) {
            String filter = fs[i];
            if (filter.endsWith("*")) {
                filter = filter.substring(0, filter.length() - 1);
            }
            f[i] = filter;
        }
        return f;
    }

    private static String[] filters2FNFilters(String[] fs) {
        if (fs == null || fs.length == 0) {
            return null;
        }

        String[] f = new String[fs.length];
        for (int i = 0; i < fs.length; i++) {
            f[i] = fs[i].replace('.', '/');
        }
        return f;
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (negativeFilters != null) {
            for (int i = 0; i < negativeFilters.length; i++) {
                if (name.startsWith(negativeFilters[i])) {
                    throw new ClassNotFoundException(name);
                }
            }
        }

        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                if (name.startsWith(filters[i])) {
                    return super.loadClass(name, resolve);
                }
            }
        } else {
            return super.loadClass(name, resolve);
        }
        throw new ClassNotFoundException(name);
    }

    /*protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (negativeFilters != null) {
            for (int i = 0; i < negativeFilters.length; i++) {
                if (name.startsWith(negativeFilters[i])) {
                    throw new ClassNotFoundException(name);
                }
            }
        }

        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                if (name.startsWith(filters[i])) {
                    return super.findClass(name);
                }
            }
        } else {
            return super.loadClass(name);
        }
        throw new ClassNotFoundException(name);
    }*/

    
    public java.net.URL getResource(String name) {
        if (negativeFNFilters != null) {
            for (int i = 0; i < negativeFNFilters.length; i++) {
                if (name.startsWith(negativeFNFilters[i])) {
                    return null;
                }
            }
        }

        if (fnFilters != null) {
            for (int i = 0; i < fnFilters.length; i++) {
                if (name.startsWith(fnFilters[i])) {
                    return super.getResource(name);
                }
            }
        } else {
            return super.getResource(name);
        }
        return null;
    }

    /**
     * Returns the list of filters used by this FireWallClassLoader. The list is
     * a copy of the array internally used.
     * 
     * @return The filters used.
     */
    public String[] getFilters() {
        if (filters == null) {
            return null;
        }

        return (String[])filters.clone();
    }

    /**
     * Returns the list of negative filters used by this FireWallClassLoader.
     * The list is a copy of the array internally used.
     * 
     * @return The filters used.
     */
    public String[] getNegativeFilters() {
        if (negativeFilters == null) {
            return null;
        }

        return (String[])negativeFilters.clone();
    }

}
