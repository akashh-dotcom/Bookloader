<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:param name="check">0</xsl:param>	
	<xsl:template match="/book">
	<toc>
	<tocinfo><risinfo><isbn><xsl:value-of select="translate(bookinfo/isbn,'-_','' )"/></isbn></risinfo></tocinfo>
	<title><xsl:value-of select="bookinfo/title"/>	</title>	
	<xsl:apply-templates select="node()"	mode="front"></xsl:apply-templates>	
	<xsl:apply-templates select="node()"	></xsl:apply-templates>	
	<xsl:apply-templates select="node()"	mode="back"></xsl:apply-templates>	
	</toc>
	</xsl:template>	

<xsl:template match="preface" mode="front"	>
	<xsl:call-template name="noid" />	
	<tocfront><xsl:attribute name="linkend"><xsl:value-of select="@id" /></xsl:attribute><xsl:call-template name="title.value" /></tocfront>
</xsl:template>	

<xsl:template match="dedication" mode="front">
	<xsl:call-template name="noid" />
	<tocfront><xsl:attribute name="linkend"><xsl:value-of select="@id" /></xsl:attribute><xsl:value-of select="name()" /></tocfront>
</xsl:template>

<xsl:template match="bookinfo"  mode="front">
<xsl:call-template name="noid" />
	<tocfront><xsl:attribute name="linkend"><xsl:value-of select="@id" /></xsl:attribute>About</tocfront>
</xsl:template>	

<xsl:template match="appendix|glossary|index"  mode="back">
<xsl:call-template name="noid" />
	<tocback><xsl:attribute name="linkend"><xsl:value-of select="@id" /></xsl:attribute><xsl:value-of select="name()" /></tocback	>
</xsl:template>	

	<xsl:template name="fmttitle">
	<xsl:param name="lableName">
		<xsl:if test="@label != '' "><emphasis role="strong"><xsl:value-of select="@label" />: </emphasis></xsl:if>
	</xsl:param>	
	<xsl:call-template name="noid" />
	<tocentry>
	<xsl:attribute name="linkend"><xsl:value-of select="@id" /></xsl:attribute><xsl:value-of select="$lableName" />
	<xsl:call-template name="title.value" />
	</tocentry>
	</xsl:template>
			
<xsl:template name="title.value">
<xsl:choose>
		<xsl:when test="titleabbrev != ''"><xsl:apply-templates select="titleabbrev" mode="name"	/></xsl:when>
		<xsl:when test="title != ''"><xsl:apply-templates select="title" mode="name"	/>	</xsl:when>
		<xsl:when test="node()/titleabbrev != ''"><xsl:apply-templates select="node()/titleabbrev" mode="name"	/></xsl:when>
		<xsl:when test="node()/title != ''"><xsl:apply-templates select="node()/title" mode="name"	/></xsl:when>
		<xsl:when test="bibliography/@id" >Bibliography</xsl:when>
		<xsl:when test="preface/@id" >Preface</xsl:when>
		<xsl:otherwise>
		<!-- <xsl:message>no name for <xsl:value-of select="name(.)"	/><xsl:value-of select="@role"	/></xsl:message>	-->
		<xsl:choose>
			<xsl:when test="@role != '' " /> 	
			<xsl:when test="substring(name(.), 1, 4) = 'sect' ">Section</xsl:when>
			<xsl:otherwise><xsl:value-of select="name(.)"	/></xsl:otherwise>	
		</xsl:choose>
			</xsl:otherwise>	
	</xsl:choose>
</xsl:template>		
	
	
<xsl:template match="chapter">
<tocchap>
<xsl:call-template name="fmttitle" />
<xsl:apply-templates select="sect1" ></xsl:apply-templates>
</tocchap>
</xsl:template>

<xsl:template match="sect1">
<toclevel1>
<xsl:call-template name="fmttitle" />
<xsl:apply-templates select="sect2" ></xsl:apply-templates>
</toclevel1>
</xsl:template>



<xsl:template match="sect2">
<toclevel2>
<xsl:call-template name="fmttitle" />
<xsl:apply-templates select="sect3" ></xsl:apply-templates>
</toclevel2>
</xsl:template>

<xsl:template match="sect3">
<toclevel3>
<xsl:call-template name="fmttitle" />
<xsl:apply-templates select="sect4" ></xsl:apply-templates>
</toclevel3>
</xsl:template>

<xsl:template match="sect4">
<toclevel4>
<xsl:call-template name="fmttitle" />
<xsl:apply-templates select="sect5" ></xsl:apply-templates>
</toclevel4>
</xsl:template>


<xsl:template match="sect5">
<toclevel5>
<xsl:call-template name="fmttitle" />
</toclevel5>
</xsl:template>

<xsl:template match="subpart">
<tocsubpart>
<xsl:if test="partintro != '' "><xsl:attribute name="role" >partintro</xsl:attribute></xsl:if>
<xsl:call-template name="fmttitle" />
<xsl:apply-templates select="node()" ></xsl:apply-templates>
</tocsubpart>
</xsl:template>

<xsl:template match="part">
<tocpart>
<xsl:if test="partintro != '' "><xsl:attribute name="role" >partintro</xsl:attribute></xsl:if>
<xsl:call-template name="fmttitle" />
<xsl:apply-templates select="node()" ></xsl:apply-templates>
</tocpart>
</xsl:template>

<xsl:template name="noid">
<xsl:choose>
	<xsl:when test="@id != '' or $check = 0 "></xsl:when>
	<xsl:otherwise><unmatched ><xsl:attribute name="name"><xsl:value-of select="../@id" />:<xsl:value-of select="../title" /></xsl:attribute>	</unmatched>
</xsl:otherwise>	
</xsl:choose>		
</xsl:template>		

<xsl:template match="footnote" mode="name">Suppressed footnote: <xsl:message><xsl:value-of select="footnote/@id" /></xsl:message></xsl:template>	
<xsl:template match="text" mode="name"><xsl:value-of select="." /></xsl:template>	
<xsl:template match="*" mode="name"><xsl:apply-templates mode="name" /></xsl:template>	

<xsl:template match="*" />		
<xsl:template match="*" mode="front"/>		
<xsl:template match="*" mode="back"/>		
</xsl:stylesheet>
