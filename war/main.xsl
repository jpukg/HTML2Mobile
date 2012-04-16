<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- Edited by XMLSpy® -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<html>
  <head>
	<link rel="stylesheet" href="http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.css" />
	<script src="http://code.jquery.com/jquery-1.6.4.min.js"></script>
	<script src="http://code.jquery.com/mobile/1.0.1/jquery.mobile-1.0.1.min.js"></script>
  </head>
  <body>
<div data-role="page" data-theme="b" id="jqm-home">
    <div data-role="content"> 
  
<!-- Main List -->  
<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 

	<!-- Links List -->
		<xsl:apply-templates select="html2mobile/links"/>
	<!-- End Links List -->
	
	<!-- Content List -->
		<xsl:apply-templates select="html2mobile/content"/>
	<!-- End Content List -->
	
	<!-- Form -->
		<xsl:apply-templates select="html2mobile/forms"/>
	<!-- Form -->
	
	<!-- Media List -->
		<xsl:apply-templates select="html2mobile/media"/>
	<!-- End Media List -->

<!-- End Main List -->
</ul>
	</div>
</div>
</body>
</html>
</xsl:template>

<xsl:template match="links">
	<li>Links (<xsl:value-of select="count"/>)
		<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
			<xsl:apply-templates select="linkgroup"/>
		</ul>
	</li>
</xsl:template>

<xsl:template match="linkgroup">
	<li>Link Group Title (<xsl:value-of select="count"/>)
		<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
			<xsl:apply-templates select="link"/>
			<li>[End Of Links]</li>
		</ul>
	</li>
</xsl:template>

<xsl:template match="link">
	<xsl:if test="text != ''">
		<li>
			<a href="#">
				<xsl:value-of select="text"/>
			</a>
		</li>
	</xsl:if>
</xsl:template>

<xsl:template match="content">
	<li>Content <!-- (<xsl:value-of select="count"/>) -->
		<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e">
			<xsl:apply-templates select="section"/>
		</ul>
	</li>
</xsl:template>

<xsl:template match="section">
	<li><xsl:value-of select="summary"/>
		<ul><li><xsl:value-of select="text"/></li></ul>
	</li>
</xsl:template>

<xsl:template match="forms">
	<li>Forms (<xsl:value-of select="count"/>)
			<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
				<xsl:apply-templates select="form"/>
			<!-- End Form Content -->
			</ul>
	</li>
</xsl:template>

<xsl:template match="form">
	<li>
		<div style="border:1px solid #000;padding:10px;">
			<xsl:value-of select="code" disable-output-escaping="yes"/>
		</div>
	</li>
</xsl:template>


<xsl:template match="media">
	<li>Media
		<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 

			<!-- Picture List -->
			<li>Pictures (<xsl:value-of select="pictures/count"/>)
				<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
					<xsl:apply-templates select="pictures/picture"/>
				<!-- End Picture List -->
				</ul>
			</li>

			<!-- Video List -->
			<li>Videos (<xsl:value-of select="videos/count"/>)
				<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
					<xsl:apply-templates select="videos/video"/>
				<!-- End Video List -->
				</ul>
			</li>

			<!-- Audio List -->
			<li>Audio (<xsl:value-of select="audios/count"/>)
				<ul data-role="listview" data-inset="true" data-theme="d" data-divider-theme="e"> 
					<xsl:apply-templates select="audios/audio"/>
				<!-- End Audio List -->
				</ul>
			</li>
		</ul>
	</li>
</xsl:template>

<xsl:template match="picture">
	<li><img src='{src}' /></li>
</xsl:template>

<xsl:template match="video">
	<li><xsl:value-of select="src"/></li>
</xsl:template>

<xsl:template match="audio">
	<li><xsl:value-of select="src"/></li>
</xsl:template>


</xsl:stylesheet>


