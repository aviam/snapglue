package com.nigealm.utils;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class JSONUtils
{

	public static JSONArray convertDocumentListToJSONArray(FindIterable<Document> docs)
	{
		MongoCursor<Document> cursor = docs.iterator();
		JSONArray res = new JSONArray();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
			String json = StringEscapeUtils.unescapeJava(doc.toJson());
			JSONObject jsonObject = null;
			try
			{
				jsonObject = new JSONObject(json);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
			res.put(jsonObject);
		}
		return res;

	}

	public static List<Document> convertIterableToList(MongoCursor<Document> cursor)
    {
        List<Document> res = new ArrayList<>();
        while (cursor.hasNext())
        {
            Document doc = cursor.next();
            res.add(doc);
        }
        return res;
    }

	public static JSONArray convertDocumentListToJSONArray(List<Document> docs)
	{
		Iterator<Document> cursor = docs.iterator();
		JSONArray res = new JSONArray();
		while (cursor.hasNext())
		{
			Document doc = cursor.next();
			JSONObject jsonObject = convertDocumentToJSONObject(doc);
			if (jsonObject != null)
				res.put(jsonObject);
		}
		return res;
	}

	public static JSONObject convertDocumentToJSONObject(Document doc)
	{
		try
		{
            String json = StringEscapeUtils.unescapeJava(doc.toJson());
            JSONObject res = new JSONObject(json);
			return res;
		}
		catch (JSONException e)
		{
			return null;
		}
	}

	public static JSONArray getJSONArrayFromJSONObject(JSONObject src, String arrayHeader)
	{
		try
		{
			return src.getJSONArray(arrayHeader);
		}
		catch (JSONException e)
		{
			return new JSONArray();
		}
	}

	public static JSONObject getJSONObjectFromJSONObject(JSONObject src, String arrayHeader)
	{
		try
		{
			return src.getJSONObject(arrayHeader);
		}
		catch (JSONException e)
		{
			return new JSONObject();
		}
	}

	public static Document convert(JSONObject object)
	{
		Document doc = Document.parse(object.toString());
		return doc;
	}
}
