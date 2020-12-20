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
package org.apache.cxf.common.spi;

/** Implement this interface to store class generated in order during build phase
 *  inject it back before runtime to avoid class generation.
 *  produce dot class file thanks to save method.
 *  You can check WrapperNamespaceClassGeneratorTest.testGeneratedFirst for usage
 *  Here is list of extensions to set in order to avoid class loading after generation during build time.
 *  bus.setExtension(new WrapperHelperClassLoader(bus), WrapperHelperCreator.class);
 *  bus.setExtension(new ExtensionClassLoader(bus), ExtensionClassCreator.class);
 *  bus.setExtension(new ExceptionClassLoader(bus), ExceptionClassCreator.class);
 *  bus.setExtension(new WrapperClassLoader(bus), WrapperClassCreator.class);
 *  bus.setExtension(new FactoryClassLoader(bus), FactoryClassCreator.class);
 *  bus.setExtension(new GeneratedNamespaceClassLoader(bus), NamespaceClassCreator.class);
 * @author olivier dufour
 */
public interface GeneratedClassClassLoaderCapture {
    void capture(String className, byte[] bytes);
}
