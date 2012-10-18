<%-- =========================================================================

    Copyright © 2012 BIREME/PAHO/WHO

    This file is part of MoreLikeThat servlet.

    MoreLikeThat is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    MoreLikeThat is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public 
    License along with Bruma. If not, see <http://www.gnu.org/licenses/>.

=========================================================================--%>

<%-- 
    Document   : index
    Created on : 19/09/2012, 12:58:38
    Author     : Heitor Barbieri
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>MoreLikeThatServlet</title>
    </head>
    <body>
        <h1>MoreLikeThat Servlet!</h1>
        <p>This servlets finds stored documents that contains fields with 
            similar content of an input piece of text.</p>
        <p>Parameters:</p>
        <ul>
            <li>'content' = piece of text used to look for similar documents.</li>
            <li>'fieldsName' = comma separated list of document fields into 
                 which the text will be matched.</li>
        </ul>
        <p>Example:</p>
        <ul>
            <li>localhost:8080/&lt;context&gt;/MoreLikeThat?content=Children Health&amp;fieldsName=title,abstract</li>
        </ul>
    </body>
</html>
