<%-- 
CDDL HEADER START

The contents of this file are subject to the terms of the
Common Development and Distribution License (the "License").  
You may not use this file except in compliance with the License.

See LICENSE.txt included in this distribution for the specific
language governing permissions and limitations under the License.

When distributing Covered Code, include this CDDL HEADER in each
file and include the License file at LICENSE.txt.
If applicable, add the following below this CDDL HEADER, with the
fields enclosed by brackets "[]" replaced with your own identifying
information: Portions Copyright [yyyy] [name of copyright owner]

CDDL HEADER END

Copyright 2005 Sun Microsystems, Inc.  All rights reserved.
Use is subject to license terms.

ident	"@(#)enoent.jsp 1.3     05/12/02 SMI"

--%><%@ page import = "javax.servlet.*,
javax.servlet.http.*,
java.lang.*,
java.io.*,
org.opensolaris.opengrok.configuration.*"
%><%@ page session="false" %><%@ page isErrorPage="true" %><%

String context = request.getContextPath();
RuntimeEnvironment env = RuntimeEnvironment.getInstance();
env.setUrlPrefix(context + "/s?");
env.register();
String rawSource = env.getSourceRootPath();
String configError = "";
if (rawSource.equals("")) {
    configError = "SRC_ROOT parameter has not been configured in web.xml! Please configure your webapp.";
} else {
    if (!env.getSourceRootFile().isDirectory()) {
        configError = "SRC_ROOT parameter in web.xml does not point to a valid directory! Please configure your webapp.";
    }
}
%><?xml version="1.0" encoding="iso-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US" lang="en-US">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
    <title>File not found</title>
    <link rel="icon" href="<%=context%>/img/icon.png" type="image/png"/>
    <link rel="stylesheet" type="text/css" href="<%=context%>/style.css"/>
    <link rel="stylesheet" type="text/css" href="<%=context%>/print.css" media="print" />
    <link rel="alternate stylesheet" type="text/css" media="all" title="Paper White" href="<%=context%>/print.css"/>
</head>
<body><div id="page">
<form action="<%=context%>/search">
    <div id="header">
        <%= getServletContext().getInitParameter("HEADER") %>
    </div>
<div id="Masthead"></div>
<div id="bar"><a id="home" href="<%=context%>">Home</a> | <input id="search" name="q" class="q"/> <input type="submit" value="Search" class="submit"/> </div>
<h3 class="error">Error 404: File not found!</h3>
The requested resource is not available. <%=configError%>
<div style="display:block;height:10em">&nbsp;</div><%@include file="foot.jspf"%>
