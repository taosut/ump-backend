/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.elk;

import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import org.elasticsearch.common.xcontent.XContentBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.logging.model.DeleteByQuery5;

/**
 *
 * @author kiendt
 */
@Service
public class BaseElkService {

    private static final Logger logger = LoggerFactory.getLogger(BaseElkService.class);

    @Autowired
    JestClient elasticSearchClient;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    public void insertDocument(Object dataElk, String indexElk, String typeElk) {
        try {
            XContentBuilder content = jsonBuilder()
                    .startObject();
            Class<?> thisClass = null;
            thisClass = Class.forName(dataElk.getClass().getName());
            Field[] aClassFields = thisClass.getDeclaredFields();
            for (Field f : aClassFields) {
                String fName = f.getName();
                if (!fName.equals("_id")) {
                    content = content.field(fName, f.get(dataElk));
                }
            }

            String source = content.endObject().string();//       
            try {
                Index index = new Index.Builder(source).index(indexElk).type(typeElk).build();
                JestResult result = elasticSearchClient.execute(index);
                System.out.println(result.getJsonString());
            } catch (IOException ex) {
                ex.printStackTrace();
                logger.error("createStatiticsELK", ex.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object updateDocument(String id, Object dataElk, String indexElk, String typeElk) {
        try {
            XContentBuilder content = jsonBuilder()
                    .startObject();
            Class<?> thisClass = null;
            thisClass = Class.forName(dataElk.getClass().getName());
            Field[] aClassFields = thisClass.getDeclaredFields();
            for (Field f : aClassFields) {
                String fName = f.getName();
                if (!fName.equals("_id")) {
                    content = content.field(fName, f.get(dataElk));
                }
            }

            String source = content.endObject().string();//       
            try {
                Index index = new Index.Builder(source).index(indexElk).type(typeElk).id(id).build();
                JestResult result = elasticSearchClient.execute(index);
                System.out.println(result.getJsonString());
            } catch (IOException ex) {
                ex.printStackTrace();
                logger.error("createStatiticsELK", ex.toString());
            }
            return dataElk;
        } catch (Exception e) {
            logger.error("BaseElkService insert, {}", e.getCause());
        }
        return null;
    }

    public void deleteDoucmentById(String id, Object dataElk, String indexElk, String typeElk) {
        try {
            BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
            boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("_id", id));
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            try {
                DeleteByQuery5 deleteQuery = new DeleteByQuery5.Builder(searchSourceBuilder.toString())
                        .addIndex(indexElk)
                        .addType(typeElk)
                        .build();
                elasticSearchClient.execute(deleteQuery);
            } catch (IOException ex) {
                logger.error("Delete Index {} , error: {}", indexElk, ex.getMessage());
            }
        } catch (Exception e) {
            logger.error("Delete Index {} , error: {}", indexElk, e.getMessage());
        }
    }

    public JsonObject query(String query, String indexElk, String typeElk) {
        try {
            Search search = new Search.Builder(query)
                    .addIndex(indexElk)
                    .addType(typeElk)
                    .build();
            SearchResult result = elasticSearchClient.execute(search);
            JsonObject data = result.getJsonObject();
            return data;
        } catch (IOException ex) {
            logger.error("Error when query on {}, error : {}", indexElk, ex.getCause());
        }
        return null;
    }
    
}
