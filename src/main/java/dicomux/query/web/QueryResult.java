package dicomux.query.web;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

public class QueryResult {
    private JSONObject json;

    protected QueryResult(JSONObject json) {
        this.json = json;
    }

    public boolean getSuccess() {
        try {
            return getBoolean("success");
        } catch (JSONException e) {
            return false;
        }
    }

    public String getMessage() {
        try {
            return getString("message");
        } catch (JSONException e) {
            return e.getMessage();
        }
    }

    public JSONObject getJSONObject() {
        return json;
    }

    public String toString() {
        return json.toString();
    }

    //--

    public boolean getBoolean(String s) throws JSONException {
        return json.getBoolean(s);
    }

    public double getDouble(String s) throws JSONException {
        return json.getDouble(s);
    }

    public int getInt(String s) throws JSONException {
        return json.getInt(s);
    }

    public JSONArray getJSONArray(String s) throws JSONException {
        return json.getJSONArray(s);
    }

    public JSONObject getJSONObject(String s) throws JSONException {
        return json.getJSONObject(s);
    }

    public long getLong(String s) throws JSONException {
        return json.getLong(s);
    }

    public String getString(String s) throws JSONException {
        return json.getString(s);
    }

    public boolean has(String s) {
        return json.has(s);
    }

    public boolean isNull(String s) {
        return json.isNull(s);
    }
}
