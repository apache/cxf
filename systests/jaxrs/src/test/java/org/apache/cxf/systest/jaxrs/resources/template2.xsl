<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:books="http://www.w3.org/books">

 <xsl:param name="id" select="''"/>
 <xsl:param name="name" select="''"/>
 <xsl:param name="name2" select="''"/>

 <xsl:template match="@*|node()">
   <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
   </xsl:copy>
 </xsl:template>
 
 <xsl:template match="id">
     <xsl:copy>
	   <xsl:value-of select="$id"/>
	</xsl:copy>
 </xsl:template>
 
 <xsl:template match="name">
	<xsl:copy>
	   <xsl:value-of select="."/>
	   <xsl:value-of select="$name"/>
	   <xsl:value-of select="$name2"/>
	</xsl:copy>
 </xsl:template>
 
 
</xsl:stylesheet> 
