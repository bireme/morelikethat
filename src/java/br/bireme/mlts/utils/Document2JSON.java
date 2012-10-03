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

package br.bireme.mlts.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Heitor Barbieri
 * @date 20120925
 */
public class Document2JSON {
    public static String getString(final Document doc) {
        if (doc == null) {
            throw new NullPointerException("doc");
        }
        return getJSON(getMap(doc)).toJSONString();
    }
    
    public static Map<String,List<Fieldable>> getMap(final Document doc) {
        if (doc == null) {
            throw new NullPointerException("doc");
        }
        
        final Map<String,List<Fieldable>> ret = 
                                    new LinkedHashMap<String,List<Fieldable>>();
        final List<Fieldable> fields = doc.getFields();
        for (Fieldable fld : fields) {
            List<Fieldable> lfld = ret.get(fld.name());
            if (lfld == null) {
                lfld = new ArrayList<Fieldable>();
                ret.put(fld.name(), lfld);
            }
            lfld.add(fld);
        }
        
        return ret;
    }
    
    public static JSONObject getJSON(final Map<String,List<Fieldable>> map) {
        if (map == null) {
            throw new NullPointerException("map");
        }
        
        final JSONObject jobj = new JSONObject();
        
        for (Map.Entry<String,List<Fieldable>> entry : map.entrySet()) {
            final String key = entry.getKey();
            final List<Fieldable> value = entry.getValue();
            final boolean multivalue = value.size() > 1;            

            if (multivalue) {
                final JSONArray array = new JSONArray();
                jobj.put(key, array);                
                for (Fieldable fld : value) {
                    array.add(getObject(fld));
                }
            } else {
                jobj.put(key, getObject(value.get(0)));
            }                        
        }
        return jobj;
    }
    
    private static Object getObject(final Fieldable fld) {
        assert fld != null;
        
        final Object ret;
        
        if (fld instanceof NumericField) {
            ret = ((NumericField)fld).getNumericValue();
        } else {
            ret = ((Field)fld).stringValue().trim();
        }
        
        return ret;
    }
}
