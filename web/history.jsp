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

ident	"@(#)history.jsp 1.1     05/11/11 SMI"

--%><%@ page import = "javax.servlet.*,
java.lang.*,
javax.servlet.http.*,
java.util.*,
java.io.*,
java.text.*,  
org.opensolaris.opengrok.analysis.*,
org.opensolaris.opengrok.web.*,
org.opensolaris.opengrok.history.*,
java.util.regex.*
"
%><%@include file="mast.jsp"%><%
if (path.length() > 0 && valid) {
    boolean striked = false;
    String userPage = getInitParameter("USER_PAGE");
    String bugPage = getInitParameter("BUG_PAGE");
    Pattern bugPattern = Pattern.compile("\\b([12456789][0-9]{6})\\b");
    Format df = new SimpleDateFormat("dd-MMM-yyyy");
    HistoryReader hr = null;
    Date tstart = new Date();
    if(isDir) {
        String[] apaths = request.getParameterValues("also");
        String apath = path;
        if (apaths!= null && apaths.length>0) {
            StringBuilder paths = new StringBuilder(path);
            for(int i=0; i< apaths.length; i++) {
                paths.append(' ');
                paths.append(apaths[i]);
            }
            apath = paths.toString();
        }
        hr = new DirectoryHistoryReader(getServletContext().getInitParameter("DATA_ROOT") + "/index", apath, getServletContext().getInitParameter("SRC_ROOT"));
    } else {
        File f = new File(rawSource + parent, basename);
        hr = HistoryGuru.getInstance().getHistoryReader(f);
    }
    if (hr != null) {

%><form action="<%=context%>/diff<%=path%>">
<table cellspacing="0" cellpadding="2" border="0" width="100%" class="src">
<tr><td colspan="4"><span class="pagetitle">History log of <a href="<%= context +"/xref" + path %>"><%=basename%></a></span></td></tr>
<tr class="thead"><%
if(!isDir) {
%><td>Revision</td><th><input type="submit" value=" Compare "/></th><%
}
%><td>Date</td><td>Author</td><td>Comments</td></tr><%
boolean alt = true;
while (hr.next()) {
    String rev = hr.getRevision();
    alt = !alt;
%><tr  valign="top" <%= alt ?  "class=\"alt\"" : "" %>><%
if(!isDir) {
        if(hr.isActive()) {
%><td>&nbsp;<a name="<%=rev%>" href="<%= context +"/xref" + path + "?r=" + rev %>"><%=rev%></a>&nbsp;</td><td align="center"><input type="radio" name="r1" value="<%=rev%>"/>
<input type="radio" name="r2" value="<%=rev%>"/></td><%
        } else {
            striked = true;
  %><td><strike>&nbsp;<%=rev%>&nbsp; </strike></td><td>&nbsp;</td><%
        }
}
%><td><%= df.format(hr.getDate()) %>&nbsp;</td>
<td>
<%

if(userPage != null && ! userPage.equals("")) {
	%><a href="<%= userPage + hr.getAuthor() %>"><%= hr.getAuthor() %></a><%
} else {
	%><%= hr.getAuthor() %><%
}

%>&nbsp;</td><td><%=
(bugPage != null && ! bugPage.equals("")) ?
    bugPattern.matcher(Util.Htmlize(hr.getComment())).replaceAll("<a href=\"" + bugPage + "$1\">$1</a>")
    :  Util.Htmlize(hr.getComment())
%><%
ArrayList files = hr.getFiles();
if(files != null) {%><br/><%
    Iterator ifiles = files.iterator();
    while(ifiles.hasNext()) {
        String ifile = (String)ifiles.next();
        String jfile = ifile.startsWith(path) ? ifile.substring(path.length()+1) : ifile;
        %><a class="h" href="<%=context%>/xref<%=ifile%>"><%=jfile%></a><br/><%
    }
}
%></td></tr><%
}
	%></table></form><%
        hr.close();
        if(striked) {
            %><p><b>Note:</b> No associated file changes are available for revisions with strike-through numbers (eg. <strike>1.45</strike>)</p><%
        }
        %><p class="rssbadge"><a href="<%=context%>/rss<%=path%>"><img src="<%=context%>/img/rss.png" width="80" height="15" alt="RSS XML Feed" title="RSS XML Feed of latest changes"/></a></p><%
    } else {
        response.sendError(404, "No history");
        return;
    }
}
%><%@include file="foot.jsp"%>
