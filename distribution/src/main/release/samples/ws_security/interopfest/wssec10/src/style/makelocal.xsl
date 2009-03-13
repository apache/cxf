<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/">
    <xsl:output method="xml"/>

    <xsl:template match="xs:import">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="@schemaLocation='http://131.107.72.15/Security_WsSecurity_Service_Indigo/WsSecurity10.svc?xsd=xsd0'">
                    <xsl:attribute name="schemaLocation">WsSecurity10_0.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://131.107.72.15/Security_WsSecurity_Service_Indigo/WsSecurity10.svc?xsd=xsd1'">
                    <xsl:attribute name="schemaLocation">WsSecurity10_1.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://131.107.72.15/Security_WsSecurity_Service_Indigo/WsSecurity10.svc?xsd=xsd2'">
                    <xsl:attribute name="schemaLocation">WsSecurity10_2.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://131.107.72.15/Security_WsSecurity_Service_Indigo/WsSecurity10.svc?xsd=xsd3'">
                    <xsl:attribute name="schemaLocation">WsSecurity10_3.xsd</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="schemaLocation"><xsl:value-of select="@schemaLocation"/></xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates select="@namespace"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="wsdl:import">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="@location='http://131.107.72.15/Security_WsSecurity_Service_Indigo/WsSecurity10.svc?wsdl'">
                    <xsl:attribute name="location">WsSecurity10.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="location">WsSecurity10_policy.wsdl</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates select="@namespace"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="soap:address">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="@location='https://kirillgdev04/Security_WsSecurity_Service_Indigo/WsSecurity10.svc/UserNameOverTransport'">
                    <xsl:attribute name="location">https://131.107.72.15/Security_WsSecurity_Service_Indigo/WsSecurity10.svc/UserNameOverTransport</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="@*"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xs:element[@ref='xs:schema']">
    </xsl:template>
    <xsl:template match="@*">
        <xsl:copy/>
    </xsl:template>
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
