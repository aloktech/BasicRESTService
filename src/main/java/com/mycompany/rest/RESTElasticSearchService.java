package com.mycompany.rest;

import com.mycompany.rest.util.DayLight;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.Bulk.Builder;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Alok
 */
@Path("elastic")
public class RESTElasticSearchService {

    private final String connectionUrl = "http://localhost:9200";

    @POST
    @Path("create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createIndex(String data) {

        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(connectionUrl)
                .multiThreaded(true)
                .build());
        JestClient client = factory.getObject();

        JSONObject jData = new JSONObject(data);

        String index = jData.getString("index");

        try {
            client.execute(new CreateIndex.Builder(index).build());

        } catch (IOException ex) {
            Logger.getLogger(RESTElasticSearchService.class.getName()).log(Level.SEVERE, null, ex);
        }
        JSONObject status = new JSONObject();
        status.put("status", "OK");
        status.put("index", index);

        System.out.println(status.toString());

        return Response.status(Response.Status.OK)
                .entity(status.toString())
                .build();
    }

    @POST
    @Path("upload/{index}/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response uploadData(String data, @PathParam("index") String index, @PathParam("type") String type) {

        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(connectionUrl)
                .multiThreaded(true)
                .build());
        JestClient client = factory.getObject();

        JSONObject status = new JSONObject();
        try {
            boolean indexExists = client.execute(new IndicesExists.Builder(index).build()).isSucceeded();
            if (!indexExists) {
                client.execute(new CreateIndex.Builder(index).build());
            }

            Builder bulkIndexBuilder = new Bulk.Builder();
            Calendar cal = GregorianCalendar.getInstance();
            JSONArray array = new JSONArray(data);
            for (int ind = 0; ind < array.length(); ind++) {
                JSONObject tempJson = array.getJSONObject(ind);
                if (tempJson.has("time")) {
                    String[] time = tempJson.getString("time").split("_");
                    cal.set(Calendar.YEAR, Integer.parseInt(time[2]));
                    cal.set(Calendar.MONTH, Integer.parseInt(time[1]) - 1);
                    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(time[0]));
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[3]));
                    cal.set(Calendar.MINUTE, Integer.parseInt(time[4]));
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    tempJson.put("timeInMillis", cal.getTimeInMillis());
                    tempJson.put("date", Integer.parseInt(time[2]) + "-" + Integer.parseInt(time[1]) + "-" + Integer.parseInt(time[0]));
                    if (time.length > 3) {
                        tempJson.put("time", Integer.parseInt(time[3]) + "-" + Integer.parseInt(time[4]));
                    }

                    bulkIndexBuilder.addAction(new Index.Builder(tempJson.toString()).index(index).type(type).id(String.valueOf(cal.getTimeInMillis())).build());
                }
            }
            client.execute(bulkIndexBuilder.build());
            status.put("status", "OK");
            status.put("index", index);
            status.put("type", type);
            status.put("count", array.length());
            status.put("time", new Date());
        } catch (IOException ex) {
            Logger.getLogger(RESTElasticSearchService.class.getName()).log(Level.SEVERE, null, ex);
            status.put("status", "KO");
        }

        System.out.println(status.toString());

        return Response.status(Response.Status.OK)
                .entity(status.toString())
                .build();
    }

    @POST
    @Path("extract")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response extractData(String inputData) {
        String index = "temp-humid-db", type = "temp-humid";
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(connectionUrl)
                .multiThreaded(true)
                .build());
        JestClient client = factory.getObject();

        Calendar cal = GregorianCalendar.getInstance();
        long startTime, endTime;
        JSONObject data = new JSONObject(inputData);
        String startTimeStr = data.getString("startTime"), endTimeStr = data.getString("endTime");
        startTime = extractTime(startTimeStr, cal, DayLight.START);
        endTime = extractTime(endTimeStr, cal, DayLight.END);
        String query;

        query = "{\n"
                + " \"size\": 10000, \n"
                + " \"query\" : {\n"
                + " \"filtered\" : {\n"
                + " \"filter\" : {\n"
                + " \"range\" : {\n"
                + " \"timeInMillis\" : {\n"
                + " \"gte\" : "
                + startTime
                + ",\n"
                + " \"lt\" : "
                + endTime
                + "\n"
                + " }\n"
                + " }\n"
                + " }\n"
                + " }\n"
                + " },\n"
                + " \"sort\": { \"timeInMillis\": { \"order\": \"asc\" }}\n"
                + "}\n"
                + "";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(index)
                .addType(type)
                .build();

        JestResult result;
        try {
            result = client.execute(search);
            return Response.status(Response.Status.OK)
                    .entity(result.getJsonString())
                    .build();
        } catch (IOException ex) {
            Logger.getLogger(RESTElasticSearchService.class.getName()).log(Level.SEVERE, null, ex);
        }

        return Response.status(Response.Status.OK)
                .entity("")
                .build();
    }

    private static long extractTime(String timeStr, Calendar cal, DayLight dayLight) throws NumberFormatException {
        long startTime;
        String[] time = timeStr.split("_");
        cal.set(Calendar.YEAR, Integer.parseInt(time[2]));
        cal.set(Calendar.MONTH, Integer.parseInt(time[1]) - 1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(time[0]));
        if (time.length > 3) {
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[3]));
            cal.set(Calendar.MINUTE, Integer.parseInt(time[4]));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else if (dayLight == DayLight.START) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 0);
        }
        startTime = cal.getTimeInMillis();
        return startTime;
    }
}
