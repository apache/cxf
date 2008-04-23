<?xml version="1.0"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements. See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership. The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied. See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:xalan="http://xml.apache.org/xslt">

    <xsl:output method="xml" indent="yes" xalan:indent-amount="4"/>
    <xsl:strip-space elements="*"/>
    

    <!-- copy attributes from any node -->
    <xsl:template match="@*" mode="attribute_copy">
        <xsl:attribute name="{name(.)}">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>

    <xsl:template match="path">
        <project name="source-build-paths">
            <property>
                <xsl:attribute name="file">${user.home}/.m2/maven.properties</xsl:attribute>
            </property>
            <property name="maven.repo.local">
                <xsl:attribute name="value">${user.home}/.m2/repository</xsl:attribute>
            </property>
            <path id="srcbuild.classpath.path">
                <xsl:copy-of select="*"/>
            </path>
            <property name="srcbuild.classpath" refid="srcbuild.classpath.path"/>
            <property name="cxf.lib.dir">
                <xsl:attribute name="location">${user.home}/.m2/repository</xsl:attribute>
            </property>

        </project>
    </xsl:template>

    <xsl:template match="echo">
    </xsl:template>

</xsl:stylesheet>
