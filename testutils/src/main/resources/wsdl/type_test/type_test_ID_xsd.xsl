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
    xmlns:xalan="http://xml.apache.org/xslt"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:wsse="http://schemas.xmlsoap.org/ws/2003/06/secext"
    xmlns:itst="http://tests.iona.com/ittests"
    xmlns:x1="http://apache.org/type_test/types1"
    xmlns:x2="http://apache.org/type_test/types2"
    xmlns:x3="http://apache.org/type_test/types3"
    xmlns="http://www.w3.org/2001/XMLSchema"
    >

  <xsl:output method="xml" indent="yes" xalan:indent-amount="4"/>
  <xsl:strip-space elements="*"/>

  <!-- group selection parameter -->  
  <xsl:param name="groupID"/>

  <!-- copy attributes from any node -->
  <xsl:template match="@*" mode="attribute_copy">
    <xsl:attribute name="{name(.)}">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:template>

  <!-- 0 - root schema node -->
  <xsl:template match="/xsd:schema">
    <xsd:schema xmlns="http://www.w3.org/2001/XMLSchema"
                xmlns:jaxb="http://java.sun.com/xml/ns/jaxb"
                jaxb:version="2.0">
      <xsl:attribute name="targetNamespace">
        <xsl:value-of select="concat('http://apache.org/type_test/types', $groupID)"/>
      </xsl:attribute>
      <xsl:apply-templates select="@*" mode="attribute_copy"/>
      <xsl:apply-templates select="itst:it_test_group[@ID=$groupID]" mode="test_group"/>
    </xsd:schema>
  </xsl:template>

  <!-- 0.1 group of types -->
  <xsl:template match="itst:it_test_group" mode="test_group">
    <xsl:apply-templates select="xsd:attribute" mode="schema_type"/>
    <xsl:apply-templates select="xsd:attributeGroup" mode="schema_type"/>
    <xsl:apply-templates select="xsd:group" mode="schema_type"/>
    <xsl:apply-templates select="xsd:element" mode="schema_type"/>
    <xsl:apply-templates select="xsd:simpleType" mode="schema_type"/>
    <xsl:apply-templates select="xsd:complexType" mode="schema_type"/>
  </xsl:template>
  
  <!-- 0.1.1 - schema type or construct -->
  <xsl:template match="itst:it_test_group/*" mode="schema_type">
      <xsl:element name="{name(.)}">
        <!-- drop "it_no_test" from the attributes -->
        <xsl:apply-templates select="@*[name()!='itst:it_no_test']" mode="attribute_copy"/>
        <xsl:copy-of select="*"/>
      </xsl:element>
  </xsl:template>

</xsl:stylesheet>

