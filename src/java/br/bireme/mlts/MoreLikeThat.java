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

import br.bireme.mlts.utils.Document2JSON;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.json.simple.JSONObject;

/**
 *
 * @author Heitor Barbieri
 * @date 20120918
 */
public class MoreLikeThat {
    public static final int DEF_MAX_HITS = 10;
    public static final float DEF_MIN_SCORE = 0.4f;
        
    /**
     * Class representing a similar Lucene document
     */
    public class DocJ {
        public final ScoreDoc hit;   /* document hit */
        public final JSONObject doc; /* document */

        public DocJ(final ScoreDoc hit,
                     final JSONObject doc) {
            this.hit = hit;
            this.doc = doc;
        }
    }
        
    /**
     * Class representing a similar Lucene document
     */
    public class DocX {
        public final int id;       /* document id */
        public final float qscore; /* query score */
        public final Map<String,List<String>> doc; /* document fields */

        public DocX(final int id,
                     final float qscore,
                     final Map<String,List<String>> doc) {
            this.id = id;
            this.qscore = qscore;
            this.doc = doc;
        }
    }
    
    public class DocXComparator implements Comparator<DocX> {
        @Override
        public int compare(DocX o1,
                            DocX o2) {
            final float score = (o1.qscore - o2.qscore);
            final int ret;

            if (score == 0) {
                ret = o1.id - o2.id;
            } else if (score < 0) {
                ret = -1;
            } else {
                ret = 1;
            }
            return ret;
        }
    }
    
    private final Directory directory;
    private final IndexReader ir;
    private final IndexSearcher is;
    private final Analyzer analyzer;
    private final Set<?> stopwords;

    private int maxHits;
    private int minTermFreq;
    private int minDocFreq;
    private int maxDocFreq;
    private int maxDocFreqPct;
    private int minWordLen;
    private int maxWordLen;
    private int maxQueryTerms;
    private int maxNumTokensParsed;
    private float minScore;

    public int getMaxHits() {
        return maxHits;
    }

    public void setMaxHits(int maxHits) {
        if (maxHits <= 0) {
            throw new IllegalArgumentException("maxHits <= 0");
        }
        this.maxHits = maxHits;
    }

    public int getMinTermFreq() {
        return minTermFreq;
    }

    public void setMinTermFreq(int minTermFreq) {
        if (minTermFreq <= 0) {
            throw new IllegalArgumentException("minTermFreq <= 0");
        }
        this.minTermFreq = minTermFreq;
    }

    public int getMinDocFreq() {
        return minDocFreq;
    }

    public void setMinDocFreq(int minDocFreq) {
        if (minDocFreq <= 0) {
            throw new IllegalArgumentException("minDocFreq <= 0");
        }
        this.minDocFreq = minDocFreq;
    }

    public int getMaxDocFreq() {
        return maxDocFreq;
    }

    public void setMaxDocFreq(int maxDocFreq) {
        if (maxDocFreq <= 0) {
            throw new IllegalArgumentException("maxDocFreq <= 0");
        }
        this.maxDocFreq = maxDocFreq;
        this.maxDocFreqPct = -1;
    }

    public int getMaxDocFreqPct() {
        return maxDocFreqPct;
    }

    public void setMaxDocFreqPct(int maxDocFreqPct) {
        if (maxDocFreqPct <= 0) {
            throw new IllegalArgumentException("maxDocFreqPct <= 0");
        }
        this.maxDocFreqPct = maxDocFreqPct;
        this.maxDocFreqPct = -1;
    }

    public int getMinWordLen() {
        return minWordLen;
    }

    public void setMinWordLen(int minWordLen) {
        if (minWordLen <= 0) {
            throw new IllegalArgumentException("minWordLen <= 0");
        }
        this.minWordLen = minWordLen;
    }

    public int getMaxWordLen() {
        return maxWordLen;
    }

    public void setMaxWordLen(int maxWordLen) {
        if (maxWordLen <= 0) {
            throw new IllegalArgumentException("maxWordLen <= 0");
        }
        this.maxWordLen = maxWordLen;
    }

    public int getMaxQueryTerms() {
        return maxQueryTerms;
    }

    public void setMaxQueryTerms(int maxQueryTerms) {
        if (maxQueryTerms <= 0) {
            throw new IllegalArgumentException("maxQueryTerms <= 0");
        }
        this.maxQueryTerms = maxQueryTerms;
    }

    public int getMaxNumTokensParsed() {
        return maxNumTokensParsed;
    }

    public void setMaxNumTokensParsed(int maxNumTokensParsed) {
        if (maxNumTokensParsed <= 0) {
            throw new IllegalArgumentException("maxNumTokensParsed <= 0");
        }
        this.maxNumTokensParsed = maxNumTokensParsed;
    }

    public void setMinScore(float minScore) {
        if (minScore <= 0) {
            throw new IllegalArgumentException("minScore <= 0");
        }
        this.minScore = minScore;
    }

    public float getMinScore() {
        return minScore;
    }
    
    /**
     * Constructor of the class
     * @param dir directory of the Lucene index
     * @param analyz  analyzer used during search and similar
     * @throws IOException
     */
    public MoreLikeThat(final File dir,
                          final Analyzer analyz) throws IOException {
        if (dir == null) {
            throw new NullPointerException("dir");
        }
        if (analyz == null) {
            throw new NullPointerException("analyz");
        }

        if (! dir.isDirectory()) {
            throw new IllegalArgumentException(dir.getCanonicalPath() 
                                                       + " is not a directory");
        }
        directory = new SimpleFSDirectory(dir); /* RAMDirectory(new SimpleFSDirectory(dir)); //MMapDirectory(dir);*/
        ir = IndexReader.open(directory);
        if (ir.numDocs() == 0) {
            throw new IllegalArgumentException("zero document index");
        }
        is = new IndexSearcher(ir);
        analyzer = analyz;
        stopwords = (analyzer instanceof StopwordAnalyzerBase)
                ?  ((StopwordAnalyzerBase)analyzer).getStopwordSet() : null;
        defaultValues();
    }

    /**
     * Adjust this class with default values.
    */
    public final void defaultValues() {
        maxHits = DEF_MAX_HITS;
        minTermFreq = 1; //MoreLikeThis.DEFAULT_MIN_TERM_FREQ;
        minDocFreq = 2; //MoreLikeThis.DEFAULT_MIN_DOC_FREQ;
        maxDocFreq = org.apache.lucene.search.similar.MoreLikeThis.DEFAULT_MAX_DOC_FREQ;
        maxDocFreqPct = -1;
        minWordLen = 3; //MoreLikeThis.DEFAULT_MIN_WORD_LENGTH;
        maxWordLen = MoreLikeThis.DEFAULT_MAX_WORD_LENGTH;
        maxQueryTerms = 100; //MoreLikeThis.DEFAULT_MAX_QUERY_TERMS;
        maxNumTokensParsed = MoreLikeThis.DEFAULT_MAX_NUM_TOKENS_PARSED;
        minScore = DEF_MIN_SCORE;
    }
    
    /**
     * Loads MoreLikeThis properties from a file. One property per line
     * with the following format:  <prop>=<value>
     * @param prop file with properties
     * @throws IOException 
     */
    public void loadProperties(final File prop) throws IOException {
        if (prop == null) {
            throw new NullPointerException("property file");
        }
        final BufferedReader reader = new BufferedReader(new FileReader(prop));
        
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();            
            if (line.charAt(0) != '#') {
                final String[] split = line.split("=", 2);
                final String key = split[0];
                
                if (key.equals("MAX_HITS")) {
                    setMaxHits(Integer.parseInt(split[1]));
                } else if (key.equals("MIN_TERM_FREQ")) {
                    setMinTermFreq(Integer.parseInt(split[1]));
                } else if (key.equals("MIN_DOC_FREQ")) {
                    setMinDocFreq(Integer.parseInt(split[1]));
                } else if (key.equals("MAX_DOC_FREQ")) {
                    setMaxDocFreq(Integer.parseInt(split[1]));
                } else if (key.equals("MIN_WORD_LENGTH")) {
                    setMinWordLen(Integer.parseInt(split[1]));
                } else if (key.equals("MAX_WORD_LENGTH")) {
                    setMaxWordLen(Integer.parseInt(split[1]));
                } else if (key.equals("MAX_QUERY_TERMS")) {
                    setMaxQueryTerms(Integer.parseInt(split[1]));
                } else if (key.equals("MAX_NUM_TOKENS_PARSED")) {
                    setMaxNumTokensParsed(Integer.parseInt(split[1]));
                } else if (key.equals("MIN_SCORE")) {
                    setMinScore(Float.parseFloat(split[1]));
                } else {
                    throw new IOException("invalid parameter:" + split[0]);
                }
            }            
        }
        reader.close();
    }

    /**
     * Closes open index.
     * @throws IOException
     */
    public void close() throws IOException {
        if (is != null) {
            is.close();
        }
        ir.close();
        directory.close();
    }
    
    /**
     * Finds similar documents
     * @param likeText string used to find similar documents
     * @param fieldName the name of the document field used to compare with 
     * @return a list of documents.
     * @throws IOException 
     */
    public List<DocJ> moreLikeThat(final Reader likeText,
                                      final String[] fieldsName) 
                                                            throws IOException {
        if (likeText == null) {
            throw new NullPointerException("likeText");
        }
        if (fieldsName == null) {
            throw new NullPointerException("fieldsName");
        }
        final MoreLikeThis mlt = new MoreLikeThis(ir);
                
        mlt.setAnalyzer(analyzer);
        if (maxDocFreq >= 0) {
            mlt.setMaxDocFreq(maxDocFreq);
        }
        if (maxDocFreqPct >= 0) {
            mlt.setMaxDocFreqPct(maxDocFreqPct);
        }
        mlt.setMaxNumTokensParsed(maxNumTokensParsed);
        mlt.setMaxQueryTerms(maxQueryTerms);
        mlt.setMaxWordLen(maxWordLen);
        mlt.setMinDocFreq(minDocFreq);
        mlt.setMinTermFreq(minTermFreq);
        mlt.setMinWordLen(minWordLen);
        if (stopwords != null) {
            mlt.setStopWords(stopwords);
        }
        mlt.setFieldNames(fieldsName);
        
        final Query query = mlt.like(likeText, null/*fieldName*/);        
        final ScoreDoc[] hits = is.search(query, maxHits).scoreDocs;        
        final List<DocJ> ret = new ArrayList<DocJ>();
        for (ScoreDoc sdoc : hits) {
            ret.add(getDocJ(sdoc));
        }
        
        return ret;
    }
        
    /**
     * Finds similar documents
     * @param likeText string used to find similar documents
     * @param fieldName the name of the document field used to compare with 
     * @param getDocContent brings the document likeText if true
     * likeText. @see Docx
     * @return an ordered set of documents.
     * @throws IOException 
     */
    public TreeSet<DocX> moreLikeThat(final Reader likeText,
                                        final String[] fieldsName,
                                        final boolean getDocContent) 
                                                            throws IOException {
        if (likeText == null) {
            throw new NullPointerException("likeText");
        }
        if (fieldsName == null) {
            throw new NullPointerException("fieldsName");
        }
        final MoreLikeThis mlt = new MoreLikeThis(ir);
        final ScoreDoc[] hits;
        
        mlt.setAnalyzer(analyzer);
        if (maxDocFreq >= 0) {
            mlt.setMaxDocFreq(maxDocFreq);
        }
        if (maxDocFreqPct >= 0) {
            mlt.setMaxDocFreqPct(maxDocFreqPct);
        }
        mlt.setMaxNumTokensParsed(maxNumTokensParsed);
        mlt.setMaxQueryTerms(maxQueryTerms);
        mlt.setMaxWordLen(maxWordLen);
        mlt.setMinDocFreq(minDocFreq);
        mlt.setMinTermFreq(minTermFreq);
        mlt.setMinWordLen(minWordLen);
        if (stopwords != null) {
            mlt.setStopWords(stopwords);
        }
        mlt.setFieldNames(fieldsName);
        
        final Query query = mlt.like(likeText, null/*fieldName*/);
        final TreeSet<DocX> ret = new TreeSet<DocX>(new DocXComparator());
        
        hits = is.search(query, maxHits).scoreDocs;        
        for (ScoreDoc sdoc : hits) {
            final Map<String,List<String>> doc = 
                                       getDocContent ? getDocument(sdoc) : null;
            ret.add(new DocX(sdoc.doc, sdoc.score, doc));
        }
        
        return ret;
    }
    
    private Map<String,List<String>> getDocument(final ScoreDoc sdoc) 
                                                  throws CorruptIndexException, 
                                                                   IOException {
        assert sdoc != null;        
            
        final Map<String,List<String>> ret = new HashMap<String,List<String>>();
        final Document hitDoc = is.doc(sdoc.doc);
        
        if (hitDoc == null) {
            throw new IOException("null hit document");
        }
        for (Fieldable fld : hitDoc.getFields()) {
            final String name = fld.name().trim();
            final List<String> value;
            
            if (ret.containsKey(name)) {
                value = ret.get(name);
            } else {
                value = new ArrayList<String>();
                ret.put(name, value);
            }
            value.add(fld.stringValue().trim());
        }
        return ret;
    }
    
    private DocJ getDocJ(final ScoreDoc sdoc) throws CorruptIndexException, 
                                                                   IOException {
        assert sdoc != null;        
            
        final Document hitDoc = is.doc(sdoc.doc);        
        if (hitDoc == null) {
            throw new IOException("null hit document");
        }
        
        return new DocJ(sdoc, 
                        Document2JSON.getJSON(Document2JSON.getMap(hitDoc)));
    }
}
