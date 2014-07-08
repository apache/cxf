<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns="http://university.edu/teacher"
                xmlns:ns="http://university.edu/teacher"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:uid="org.apache.cxf.systest.ws.transfer.UIDManager"
                exclude-result-prefixes="uid"
                version="1.0">
    <xsl:output method="xml"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="/ns:teacher">
        <xsl:element name="teacher">
            <xsl:apply-templates select="@*|node()" />
            <xsl:element name="uid">
                <xsl:value-of select="uid:getUID()" />
            </xsl:element>
        </xsl:element>
    </xsl:template>

</xsl:stylesheet>
