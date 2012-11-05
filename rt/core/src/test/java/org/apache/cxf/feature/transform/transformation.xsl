<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:cus="http://customerservice.example.com/">
	<!-- Identity Template # Copy everything -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
	<!-- Rename element -->
	<xsl:template match="cus:getCustomersByName">
		<xsl:element name="getCustomersByName1" namespace="http://customerservice.example.com/">
			<xsl:apply-templates select="@*|node()"/>
		</xsl:element>
	</xsl:template>

</xsl:stylesheet>
