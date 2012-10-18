/*=========================================================================

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

=========================================================================*/

package br.bireme.mlts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Heitor Barbieri
 * @date 20120919
 */

@WebServlet(name = "MoreLikeThatServlet",urlPatterns = {"/MoreLikeThat"})
public class MoreLikeThatServlet extends HttpServlet {

    private MoreLikeThat mlt = null;
    
    @Override
    public void init(final ServletConfig servletConfig) 
                                                       throws ServletException {
        final String sdir = servletConfig.getInitParameter("INDEX_DIR");
        if (sdir == null) {
            throw new ServletException("missing index directory (INDEX_DIR) "
                                                                + "parameter.");
        }
        final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
        try {
            mlt = new MoreLikeThat(new File(sdir), analyzer);
        } catch (IOException ex) {
            throw new ServletException(ex);
        }
        
        final String maxHits = servletConfig.getInitParameter("MAX_HITS");
        if (maxHits != null) {
            mlt.setMaxHits(Integer.parseInt(maxHits));
        }
        final String minTermFreq = 
                                servletConfig.getInitParameter("MIN_TERM_FREQ");
        if (minTermFreq != null) {
            mlt.setMinTermFreq(Integer.parseInt(minTermFreq));
        }
        final String minDocFreq = 
                                 servletConfig.getInitParameter("MIN_DOC_FREQ");
        if (minDocFreq != null) {
            mlt.setMinDocFreq(Integer.parseInt(minDocFreq));
        }
        final String maxDocFreq = 
                                 servletConfig.getInitParameter("MAX_DOC_FREQ");
        if (maxDocFreq != null) {
            mlt.setMaxDocFreq(Integer.parseInt(maxDocFreq));
        }
        final String minWordLen = 
                              servletConfig.getInitParameter("MIN_WORD_LENGTH");
        if (minWordLen != null) {
            mlt.setMinWordLen(Integer.parseInt(minWordLen));
        }
        final String maxWordLen = 
                              servletConfig.getInitParameter("MAX_WORD_LENGTH");
        if (maxWordLen != null) {
            mlt.setMaxWordLen(Integer.parseInt(maxWordLen));
        }
        final String maxQueryTerms = 
                              servletConfig.getInitParameter("MAX_QUERY_TERMS");
        if (maxQueryTerms != null) {
            mlt.setMaxQueryTerms(Integer.parseInt(maxQueryTerms));
        }
        final String maxNumTokensParsed = 
                        servletConfig.getInitParameter("MAX_NUM_TOKENS_PARSED");
        if (maxNumTokensParsed != null) {
            mlt.setMaxNumTokensParsed(Integer.parseInt(maxNumTokensParsed));
        }
        final String minScore = servletConfig.getInitParameter("MIN_SCORE");
        if (minScore != null) {
            mlt.setMinScore(Float.parseFloat(minScore));
        }
    }
    
    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(final HttpServletRequest request, 
                                     final HttpServletResponse response)
                                          throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            final String content = request.getParameter("content");
            final String fieldsName = request.getParameter("fieldsName");            
            
            if (content == null) {
                throw new ServletException("missing 'content' parameter");
            }
            if (fieldsName == null) {
                throw new ServletException("missing 'fieldsName' parameter");
            }
            final String[] fldsName = fieldsName.trim().split(" *, *");
            final StringReader reader = new StringReader(content);
            final List<MoreLikeThat.DocJ> docs = 
                             mlt.moreLikeThat(reader, fldsName);     
            final JSONObject jobj = new JSONObject();
            
            if (docs.size() > 0) {
                final MoreLikeThat.DocJ first = docs.get(0);
                final JSONObject fobj = first.doc;                
                final JSONObject auxjobj1 = new JSONObject();
                final JSONObject auxjobj2 = new JSONObject();
                final JSONObject auxjobj3 = new JSONObject();
                final JSONObject auxjobj4 = new JSONObject();
                final JSONArray list0 = new JSONArray();
                final JSONArray list1 = new JSONArray();
                final JSONArray list2 = new JSONArray();
                final Object obj = fobj.get("id");
                final String _id = (obj == null) 
                             ? Integer.toString(first.hit.doc) : obj.toString();
                
                auxjobj2.put("q", content);
                for (String fld : fldsName) {
                    list0.add(fld);
                }
                auxjobj2.put("fields", list0);
                auxjobj1.put("params", auxjobj2);
                jobj.put("responseHeader", auxjobj1);
                
                auxjobj3.put("numFound", 1);
                auxjobj3.put("start", 0);
                auxjobj3.put("maxScore", first.hit.score);
                auxjobj3.put("docs", list1);
                fobj.put("score", first.hit.score);
                fobj.put("id", _id);
                list1.add(fobj);                
                jobj.put("match", auxjobj3);
                
                auxjobj4.put("numFound", docs.size());
                auxjobj4.put("start", 0);
                auxjobj4.put("maxScore", first.hit.score);
                auxjobj4.put("docs", list2);
                
                for (MoreLikeThat.DocJ doc : docs) {            
                    final JSONObject cobj = doc.doc;
                    final Object obj2 = cobj.get("id");
                    final String _id2 = (obj2 == null) 
                              ? Integer.toString(doc.hit.doc) : obj2.toString();
                    cobj.put("score", doc.hit.score);
                    cobj.put("id", _id2);
                    list2.add(cobj);
                }
                jobj.put("response", auxjobj4);
            }
            out.println(jobj.toJSONString());
        } finally {            
            out.close();
        }
    }
    
    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest2(final HttpServletRequest request, 
                                     final HttpServletResponse response)
                                          throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            final String content = request.getParameter("content");
            final String fieldsName = request.getParameter("fieldsName");            
            final String getDocument = request.getParameter("getDocument");
            
            if (content == null) {
                throw new ServletException("missing 'content' parameter");
            }
            if (fieldsName == null) {
                throw new ServletException("missing 'fieldsName' parameter");
            }
            final String[] fldsName = fieldsName.trim().split(" *, *");
            final boolean getDocContent = (getDocument == null) ? false :
                                         Boolean.parseBoolean(getDocument);                                           
            final StringReader reader = new StringReader(content);
            final TreeSet<MoreLikeThat.DocX> docs = 
                             mlt.moreLikeThat(reader, fldsName, getDocContent);
            final int size = docs.size();
            int cur = 0;
            
            if (size == 1) {
                out.println("{");
            } else {
                out.println("{ [");
            }
            for (MoreLikeThat.DocX doc : docs) { 
                final List<String> _ids = doc.doc.get("id");
                final String _id = ((_ids == null) || (_ids.isEmpty()) 
                                     ? Integer.toString(doc.id) : _ids.get(0));
                out.print("    {\"id\" : ");
                out.print(_id);
                out.print(", \"score\" : ");
                out.print(Float.toString(doc.qscore));
                if (getDocContent) {
                    final int msize = doc.doc.size();
                    int mcur = 0;
                    
                    out.print(", \"doc\" : {");
                    for (Map.Entry<String,List<String>> entry : 
                                                           doc.doc.entrySet()) {
                        final List<String> list = entry.getValue();
                        final int lSize = list.size();
                        int lcur = 0;
                        
                        out.print("\"");
                        out.print(entry.getKey());
                        out.print("\" : ");
                        if (lSize > 1) {
                            out.print("[");
                        }
                        for (String con : list) {
                            out.print("\"");
                            out.print(con);
                            out.print("\"");
                            if (++lcur < lSize) {
                                out.print(", ");
                            }
                        }
                        if (lSize > 1) {
                            out.print("]");
                        }
                        if (++mcur < msize) {
                            out.print(", ");
                        }
                    }
                    out.print("}");
                }
                if (++cur < size) {
                    out.println("},");
                } else {
                    out.println("}");
                }
            }
            if (size == 1) {
                out.println("}");
            } else {
                out.println("] }");
            }
        } finally {            
            out.close();
        }
    }
    
    public static String getParamValue(final String file,
                                         final String paramName,
                                         final String defaultValue) 
                                                           throws IOException {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (paramName == null) {
            throw new NullPointerException("paramName");
        }
        final String regExp = "< *" + paramName.trim() + " *>([^<]*)</ *" 
                                    + paramName.trim() + " *>"; 
        final Matcher mat = Pattern.compile(regExp).matcher("");
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String value = defaultValue;
        
        while (true) {
            final String line = reader.readLine();
            if (line == null) {
                break;
            }
            mat.reset(line);
            if (mat.find()) {
                value = mat.group(1);
                break;
            }
        }
        
        return value;
    }
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(final HttpServletRequest request, 
                          final HttpServletResponse response)
                                          throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(final HttpServletRequest request, 
                           final HttpServletResponse response)
                                          throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "MoreLikeThat servlet. Returns similar documents from a piece of"
                                                                     + " text.";
    }// </editor-fold>
}
