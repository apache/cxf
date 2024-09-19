/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.apache.cxf.tck;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

public class CxfApplicationArchiveProcessor implements ApplicationArchiveProcessor {
    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if ("jaxrs_ee_rs_container_requestcontext_security_web.war".equals(archive.getName())) {
            WebArchive webArchive = (WebArchive) archive;
            webArchive.addAsWebInfResource("jaxrs_ee_rs_container_requestcontext_security_web.war.sun-web.xml", "sun-web.xml");
        } else if ("jaxrs_ee_core_securitycontext_basic_web.war".equals(archive.getName())) {
            WebArchive webArchive = (WebArchive) archive;
            webArchive.addAsWebInfResource("jaxrs_ee_core_securitycontext_basic_web.war.sun-web.xml", "sun-web.xml");
        }
    }
}
