<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
    xmlns:soap12="http://schemas.xmlsoap.org/wsdl/soap12/"
    xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing" >
    <xsl:output method="xml"/>

    <xsl:template match="xs:import">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WsSecurity11.svc?xsd=xsd0'">
                    <xsl:attribute name="schemaLocation">WsSecurity11_0.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WsSecurity11.svc?xsd=xsd1'">
                    <xsl:attribute name="schemaLocation">WsSecurity11_1.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WsSecurity11.svc?xsd=xsd2'">
                    <xsl:attribute name="schemaLocation">WsSecurity11_2.xsd</xsl:attribute>
                </xsl:when>
                <xsl:when test="@schemaLocation='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WsSecurity11.svc?xsd=xsd3'">
                    <xsl:attribute name="schemaLocation">WsSecurity11_3.xsd</xsl:attribute>
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
                <xsl:when test="@location='http://131.107.153.205/Security_WsSecurity_Service_Indigo/WsSecurity11.svc?wsdl'">
                    <xsl:attribute name="location">WsSecurity11.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:when test="@location='http://ndgo-introp-s24/Security_WsSecurity_Service_Indigo/WsSecurity11.svc?wsdl'">
                    <xsl:attribute name="location">WsSecurity11.wsdl</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="location">WsSecurity11_policy.wsdl</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates select="@namespace"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="soap12:address|soap:address">
        <xsl:copy>
            <xsl:choose>
                <xsl:when test="starts-with(@location,'http://ndgo-introp-s24/')">
                    <xsl:attribute name="location">http://131.107.153.205/<xsl:value-of select="substring(@location,24)"/></xsl:attribute>
                </xsl:when>
                <xsl:when test="starts-with(@location,'https://ndgo-introp-s24/')">
                    <xsl:attribute name="location">https://131.107.153.205/<xsl:value-of select="substring(@location,25)"/></xsl:attribute>
                </xsl:when>
                
                <xsl:otherwise>
                    <xsl:apply-templates select="@location"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="xs:element[@ref='xs:schema']">
    </xsl:template>
    <xsl:template match="wsdl:port/wsa:EndpointReference">
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
