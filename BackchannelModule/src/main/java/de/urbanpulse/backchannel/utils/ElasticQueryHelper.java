package de.urbanpulse.backchannel.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class ElasticQueryHelper {

    public JsonObject queryElasticAggregation(JsonObject obj, String operationName)
    {
        JsonObject elasticRequest = new JsonObject();
        try
        {
            JsonObject queryObj = new JsonObject();
            JsonObject timestampObj = new JsonObject();
            JsonObject rangeObj = new JsonObject();
            JsonObject paramObj = obj.getJsonObject("parameter");
            JsonObject conditionObj = obj.getJsonObject("condition");
            timestampObj.put("gte",paramObj.getString("since"));
            timestampObj.put("lt",paramObj.getString("until"));
            rangeObj.put("timestamp",timestampObj);
            queryObj.put("range",rangeObj);

            JsonArray arr = paramObj.getJsonArray("fields");



            JsonObject aggsObj = new JsonObject();
            for(int i = 0; i < arr.size(); i++)
            {
                JsonObject avgFieldObject = new JsonObject();
                JsonObject avgObject = new JsonObject();
                avgObject.put("field",arr.getValue(i));
                //avgFieldObject.put("avg" ,avgObject);
                //aggsObj.put("mean_" + arr.getValue(i),avgFieldObject);
                avgFieldObject.put(operationName ,avgObject);
                aggsObj.put(operationName + arr.getValue(i),avgFieldObject);
            }

            JsonArray filterArray = new JsonArray();
            if(conditionObj != null) {
                Iterator<String> condition = conditionObj.fieldNames().iterator();
                JsonObject terms = new JsonObject();
                while (condition.hasNext()) {

                    JsonObject termsObj = new JsonObject();
                    String temp = condition.next();
                    termsObj.put(temp, conditionObj.getJsonArray(temp));
                    terms.put("terms", termsObj);
                    //terms.put("",object);

                }
                filterArray.add(terms);
            }

            filterArray.add(queryObj);

            JsonObject boolObj = new JsonObject();
            boolObj.put("filter",filterArray);


            //elasticRequest.put("query",queryObj);
            JsonObject boolObject = new JsonObject();
            boolObject.put("bool",boolObj);
            elasticRequest.put("query",boolObject);
            elasticRequest.put("aggs",aggsObj);

        }
        catch(Exception exp)
        {
            exp.printStackTrace();
        }
        return elasticRequest;
    }

    public JsonObject queryElasticAggregationNew(JsonObject obj, String conditionField, String singleCond)
    {
        JsonObject elasticRequest = new JsonObject();
        try
        {
            JsonObject queryObj = new JsonObject();
            JsonObject timestampObj = new JsonObject();
            JsonObject rangeObj = new JsonObject();
            JsonObject paramObj = obj.getJsonObject("parameter");
            JsonObject conditionObj = obj.getJsonObject("condition");
            timestampObj.put("gte",paramObj.getString("since"));
            if(paramObj.getString("since").equals(paramObj.getString("until")))
                timestampObj.put("lte",paramObj.getString("until"));
            else
                timestampObj.put("lt",paramObj.getString("until"));
            rangeObj.put("timestamp",timestampObj);
            queryObj.put("range",rangeObj);

            JsonArray arr = paramObj.getJsonArray("fields");



            JsonObject aggsObj = new JsonObject();
            for(int i = 0; i < arr.size(); i++)
            {
                JsonObject avgFieldObject = new JsonObject();
                JsonObject avgObject = new JsonObject();
                avgObject.put("field",arr.getValue(i));
                avgFieldObject.put("avg" ,avgObject);
                aggsObj.put("mean_" + arr.getValue(i),avgFieldObject);
            }

            JsonArray filterArray = new JsonArray();
            if(conditionObj != null) {

                JsonObject terms = new JsonObject();
                JsonObject termsObj = new JsonObject();
                termsObj.put(conditionField, singleCond);
                terms.put("term", termsObj);
                //terms.put("",object);
                filterArray.add(terms);
            }

            filterArray.add(queryObj);

            JsonObject boolObj = new JsonObject();
            boolObj.put("filter",filterArray);


            //elasticRequest.put("query",queryObj);
            JsonObject boolObject = new JsonObject();
            boolObject.put("bool",boolObj);
            elasticRequest.put("query",boolObject);
            elasticRequest.put("aggs",aggsObj);

        }
        catch(Exception exp)
        {
            exp.printStackTrace();
        }
        return elasticRequest;
    }

    public JsonObject queryElasticAggrSingle(JsonObject obj, String conditionField, String singleCond)
    {
        JsonObject elasticRequest = new JsonObject();
        try
        {
            JsonObject queryObj = new JsonObject();
            JsonObject timestampObj = new JsonObject();
            JsonObject rangeObj = new JsonObject();
            JsonObject paramObj = obj.getJsonObject("parameter");
            JsonObject conditionObj = obj.getJsonObject("condition");
            timestampObj.put("gte",paramObj.getString("since"));
            if(paramObj.getString("since").equals(paramObj.getString("until")))
                timestampObj.put("lte",paramObj.getString("until"));
            else
                timestampObj.put("lt",paramObj.getString("until"));
            rangeObj.put("timestamp",timestampObj);
            queryObj.put("range",rangeObj);

            JsonArray arr = paramObj.getJsonArray("fields");



            JsonObject aggsObj = new JsonObject();
            for(int i = 0; i < arr.size(); i++)
            {
                JsonObject avgFieldObject = new JsonObject();
                JsonObject avgObject = new JsonObject();
                avgObject.put("field",arr.getValue(i));
                avgFieldObject.put("avg" ,avgObject);
                aggsObj.put("mean_" + arr.getValue(i),avgFieldObject);
            }

            JsonArray filterArray = new JsonArray();
            if(conditionObj != null) {

                JsonObject terms = new JsonObject();
                JsonObject termsObj = new JsonObject();
                termsObj.put(conditionField, singleCond);
                terms.put("term", termsObj);
                //terms.put("",object);
                filterArray.add(terms);
            }

            filterArray.add(queryObj);

            JsonObject boolObj = new JsonObject();
            boolObj.put("filter",filterArray);


            //elasticRequest.put("query",queryObj);
            JsonObject boolObject = new JsonObject();
            boolObject.put("bool",boolObj);
            elasticRequest.put("query",boolObject);
            elasticRequest.put("aggs",aggsObj);

        }
        catch(Exception exp)
        {
            exp.printStackTrace();
        }
        return elasticRequest;
    }

    /**
     *
     * @param obj
     * @return
     */
    public JsonObject queryElasticGreoupBy(JsonObject obj, String filedName)
    {
        JsonObject elasticRequest = new JsonObject();
        try
        {
            JsonObject queryObj = new JsonObject();
            JsonObject timestampObj = new JsonObject();
            JsonObject rangeObj = new JsonObject();
            JsonObject paramObj = obj.getJsonObject("parameter");
            JsonObject conditionObj = obj.getJsonObject("condition");
            timestampObj.put("gte",paramObj.getString("since"));
            if(paramObj.getString("since").equals(paramObj.getString("until")))
                timestampObj.put("lte",paramObj.getString("until"));
            else
                timestampObj.put("lt",paramObj.getString("until"));
            rangeObj.put("timestamp",timestampObj);
            queryObj.put("range",rangeObj);

            JsonArray arr = paramObj.getJsonArray("fields");



            JsonObject aggsObj = new JsonObject();
            for(int i = 0; i < arr.size(); i++)
            {
                JsonObject avgFieldObject = new JsonObject();
                JsonObject avgObject = new JsonObject();
                avgObject.put("field",arr.getValue(i));
                avgFieldObject.put("avg" ,avgObject);
                aggsObj.put("mean_" + arr.getValue(i),avgFieldObject);
            }

            JsonObject group = new JsonObject();
            JsonObject termsSize = new JsonObject();
            JsonObject aggregations = new JsonObject();
            termsSize.put("field", filedName+ ".keyword");
            termsSize.put("size", 10000);
            group.put("terms",termsSize);
            group.put("aggs",aggsObj);
            aggregations.put("group_by_"+filedName,group);
            /*termsSize.put("field",)
            group.put("terms", )*/

            JsonArray filterArray = new JsonArray();
            if(conditionObj != null) {
                Iterator<String> condition = conditionObj.fieldNames().iterator();
                JsonObject terms = new JsonObject();
                while (condition.hasNext()) {

                    JsonObject termsObj = new JsonObject();
                    String temp = condition.next();
                    termsObj.put(temp, conditionObj.getJsonArray(temp));
                    terms.put("terms", termsObj);
                    //terms.put("",object);

                }
                filterArray.add(terms);
            }

            filterArray.add(queryObj);

            JsonObject boolObj = new JsonObject();
            boolObj.put("filter",filterArray);


            //elasticRequest.put("query",queryObj);
            JsonObject boolObject = new JsonObject();
            boolObject.put("bool",boolObj);
            elasticRequest.put("query",boolObject);
            elasticRequest.put("query",boolObject);
            elasticRequest.put("aggregations",aggregations);


        }
        catch(Exception exp)
        {
            exp.printStackTrace();
        }
        return elasticRequest;
    }

    public JsonObject queryElasticReq(JsonObject obj, String filedName, String operationName)
    {
        JsonObject elasticRequest = new JsonObject();
        try
        {
            JsonObject queryObj = new JsonObject();
            JsonObject timestampObj = new JsonObject();
            JsonObject rangeObj = new JsonObject();
            JsonObject paramObj = obj.getJsonObject("parameter");
            JsonObject conditionObj = obj.getJsonObject("condition");
            timestampObj.put("gte",paramObj.getString("since"));
            if(paramObj.getString("since").equals(paramObj.getString("until")))
                timestampObj.put("lte",paramObj.getString("until"));
            else
                timestampObj.put("lt",paramObj.getString("until"));
            rangeObj.put("timestamp",timestampObj);
            queryObj.put("range",rangeObj);

            JsonArray arr = paramObj.getJsonArray("fields");



            JsonObject aggsObj = new JsonObject();
            for(int i = 0; i < arr.size(); i++)
            {
                JsonObject avgFieldObject = new JsonObject();
                JsonObject avgObject = new JsonObject();
                avgObject.put("field",arr.getValue(i));
                avgFieldObject.put(operationName ,avgObject);
                aggsObj.put(operationName + arr.getValue(i),avgFieldObject);
            }

            JsonObject group = new JsonObject();
            JsonObject termsSize = new JsonObject();
            JsonObject aggregations = new JsonObject();
            termsSize.put("field", filedName+ ".keyword");
            termsSize.put("size", 10000);
            group.put("terms",termsSize);
            group.put("aggs",aggsObj);
            aggregations.put("group_by_"+filedName,group);
            /*termsSize.put("field",)
            group.put("terms", )*/

            JsonArray filterArray = new JsonArray();
            if(conditionObj != null) {
                Iterator<String> condition = conditionObj.fieldNames().iterator();
                JsonObject terms = new JsonObject();
                while (condition.hasNext()) {

                    JsonObject termsObj = new JsonObject();
                    String temp = condition.next();
                    termsObj.put(temp, conditionObj.getJsonArray(temp));
                    terms.put("terms", termsObj);
                    //terms.put("",object);

                }
                filterArray.add(terms);
            }

            filterArray.add(queryObj);

            JsonObject boolObj = new JsonObject();
            boolObj.put("filter",filterArray);


            //elasticRequest.put("query",queryObj);
            JsonObject boolObject = new JsonObject();
            boolObject.put("bool",boolObj);
            elasticRequest.put("query",boolObject);
            elasticRequest.put("aggregations",aggregations);


        }
        catch(Exception exp)
        {
            exp.printStackTrace();
        }
        return elasticRequest;
    }

    public JsonObject queryElasticTom(JsonObject obj)
    {
        JsonObject elasticRequest = new JsonObject();
        try
        {
            JsonObject queryObj = new JsonObject();
            JsonObject timestampObj = new JsonObject();
            JsonObject rangeObj = new JsonObject();
            JsonObject paramObj = obj.getJsonObject("parameter");
            JsonObject conditionObj = obj.getJsonObject("condition");
            timestampObj.put("gte",paramObj.getString("since"));
            timestampObj.put("lt",paramObj.getString("until"));
            rangeObj.put("timestamp",timestampObj);
            queryObj.put("range",rangeObj);

            JsonArray arr = paramObj.getJsonArray("fields");



            JsonObject aggsObj = new JsonObject();
            for(int i = 0; i < arr.size(); i++)
            {
                JsonObject avgFieldObject = new JsonObject();
                JsonObject avgObject = new JsonObject();
                avgObject.put("field",arr.getValue(i));
                avgFieldObject.put("cardinality" ,avgObject);
                aggsObj.put("listIds",avgFieldObject);
            }

            JsonArray filterArray = new JsonArray();
            JsonArray mustArray = new JsonArray();

            Iterator<String> condition = conditionObj.fieldNames().iterator();
            JsonObject terms = new JsonObject();
            while (condition.hasNext()) {

                JsonObject termsObj = new JsonObject();
                String temp = condition.next();
                termsObj.put(temp, conditionObj.getString(temp));
                terms.put("term", termsObj);
                //terms.put("",object);

            }
            mustArray.add(terms);


            filterArray.add(queryObj);

            JsonObject boolObj = new JsonObject();
            boolObj.put("must",mustArray);
            boolObj.put("filter",filterArray);


            //elasticRequest.put("query",queryObj);
            JsonObject boolObject = new JsonObject();
            boolObject.put("bool",boolObj);
            elasticRequest.put("query",boolObject);
            elasticRequest.put("aggs",aggsObj);

        }
        catch(Exception exp)
        {
            exp.printStackTrace();
        }
        return elasticRequest;
    }

    public JsonArray parseResultJSON(JsonObject obj,String field)
    {
        JsonArray buckets = null;
        try{
            JsonObject aggr = obj.getJsonObject("aggregations");
            JsonObject group = aggr.getJsonObject("group_by_" + field);
            buckets = group.getJsonArray("buckets");
            if(buckets != null)
                System.out.println("Result SIZE : " + buckets.size());
            else
                System.out.println("No Result returned .. ");
            for(int i=0; i < buckets.size(); i++)
            {
                JsonObject temp = buckets.getJsonObject(i);
                temp.remove("doc_count");
                temp.put("Result",temp.getString("key"));
                temp.remove("key");
            }
        }
        catch(Exception exp)
        {
            exp.printStackTrace();
        }
        return buckets;
    }
}
