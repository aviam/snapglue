package com.nigealm.agent.impl;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.nigealm.common.utils.Tracer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Gil on 25/04/2015.
 * Class to handle json
 */
public class JsonUtils {

    private final static Tracer tracer = new Tracer(JsonUtils.class);

    private static final String[] DATE_FORMATS = new String[]{
            "MMM d, yyyy h:mm:ss a", "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "dd-MMM-yyyy hh:mm", "yyyy-MM-"};

    private static Gson getGSon() {
        GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(
                Date.class, new DateSerializer()).registerTypeAdapter(String.class, new TypeAdapter<String>() {
            @Override
            public void write(JsonWriter jsonWriter, String s) throws IOException {
                jsonWriter.value(s);
            }

            @Override
            public String read(JsonReader jsonReader) throws IOException {
                if (jsonReader.peek() == JsonToken.NULL) {
                    jsonReader.nextNull();
                    return "";
                }
                if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
                    jsonReader.skipValue();
                    return "";
                }
                return jsonReader.nextString();
            }
        });
        return gsonBuilder.create();
    }

    public static JSONArray getJSONArrayFromResponse(String responsesJson) throws JSONException {
        JSONArray commitsArray = new JSONArray();
        if (responsesJson == null) {
            return commitsArray;
        }
        if (responsesJson.startsWith("{")) {
            commitsArray.put(new JSONObject(responsesJson));
        } else if (responsesJson.startsWith("[")) {
            commitsArray = new JSONArray(responsesJson);
        }
        return commitsArray;
    }

    public static <T> List<T> parseJson(String json, Class<T> classType) {
        if (json == null){
            return new ArrayList<>();
        }
        if (json.startsWith("{")){
            return parseSingleJsonObject(json,classType);
        } else if (json.startsWith("[")){
            return parseObjectsFromJsonArray(json, classType);
        } else{
            return new ArrayList<>();
        }
    }

    private static <T> List<T> parseObjectsFromJsonArray(String json, Class<T> classType) {
        ArrayList<T> data = new ArrayList<>();
        JsonParser parser = new JsonParser();
        JsonArray jArray = parser.parse(json).getAsJsonArray();
        for (JsonElement currElem : jArray) {
            T object = getGSon().fromJson(
                    currElem, classType);
            data.add(object);
        }
        return data;
    }

    private static <T> List<T> parseSingleJsonObject(String json, Class<T> classType) {
        ArrayList<T> data = new ArrayList<>();
        T jsonObject = getGSon().fromJson(
                json, classType);
        data.add(jsonObject);
        return data;
    }

    private static class DateSerializer implements JsonDeserializer<Date> {

        public Date deserialize(JsonElement jsonElement, Type typeOF,
                                JsonDeserializationContext context) throws JsonParseException {
            for (String format : DATE_FORMATS) {
                try {
                    return new SimpleDateFormat(format, Locale.US)
                            .parse(jsonElement.getAsString());
                } catch (ParseException e) {
                    tracer.exception("deserialize", e);
                }
            }
            throw new JsonParseException("UnParseable date: \""
                    + jsonElement.getAsString() + "\". Supported formats: "
                    + Arrays.toString(DATE_FORMATS));
        }
    }

    public static void mergeJSONArrays(JSONArray array, JSONArray elementToBeAdd) throws JSONException {
        for (int i = 0; i< elementToBeAdd.length(); i++){
            array.put(elementToBeAdd.get(i));
        }
    }

    public static String toJson(Object o){
        return getGSon().toJson(o);
    }

}
