package org.allegiance.ESUtility;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User:    Abhishek , Yuan Zhao
 * Date:    1/8/14
 * Time:    3:15 PM
 * Project: ESUtility
 */
public class BufferedClient {
    private static final Map<String,Map<String,Object>> buffer = new HashMap<String,Map<String,Object>>();
    public static Settings settings;

    public static void setSettings(Settings _settings)
    {
        settings = _settings;
    }

    public static void executeESBulk(Map<String, Object> map, String docId) throws IOException
    {
        buffer.put(docId,map);
        //Flush every 100
        if (buffer.size() >= settings.bufferThreshold) {
            flushBatch();
        }
    }

    private static void flushBatch() throws IOException {
       // LOG.info("flush batch");
       int hitsSkipped = 0;
       BulkRequestBuilder bulkRequest = settings.client.prepareBulk();

        for(Map.Entry<String, Map<String,Object>> pair : buffer.entrySet()){
            bulkRequest.add(settings.client.prepareIndex(settings.newIndex.equals("") ? settings.index : settings.newIndex,
                    settings.mappingType, pair.getKey()).setSource(pair.getValue()));
        }

       settings.documentsCount = settings.documentsCount - (long)buffer.size();
       System.out.println("Flushed a batch of " + buffer.size() + " - Remaining " + settings.documentsCount + " documents");
       //buffer.clear();

       BulkResponse bulkResponse;
       if(buffer.size() != hitsSkipped)
       {
           bulkResponse = bulkRequest.execute().actionGet();
            if (bulkResponse.hasFailures()){
                throw new IOException("BulkAPI encountered failures");
             }
             else buffer.clear();
       }
      if(hitsSkipped > 0)
        System.out.println(hitsSkipped + " documents skipped due to error");
    }

    public static void close() throws IOException {
        if(buffer.size() > 0)
            flushBatch();

        buffer.clear();
    }

}
