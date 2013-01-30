package dicomux.query.web;

import org.json.JSONObject;
import org.json.JSONException;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.io.*;

public class HttpClient {
	private URL baseUrl;
	
	protected HttpClient(URL baseUrl) {
		this.baseUrl = baseUrl;
	}
	
    protected InputStream open(URL url) throws IOException {
    	URLConnection connection = url.openConnection();
    	connection.setConnectTimeout(1500);
    	connection.setReadTimeout(8000);
    	return connection.getInputStream();
    }

	public InputStream callDirect(String methodAndParams) throws IOException {
        URL url = new URL(baseUrl, methodAndParams);
        return open(url);
	}
	
    public QueryResult call(String method, String ... params) throws IOException {
        String queryString = generateQueryString(params);
        InputStream input = callDirect(method + queryString);

        try {
            JSONObject result = readJSONObject(input);
            return new QueryResult(result);
        } catch (JSONException e) {
            throw new IOException("Unable to parse JSON result", e);
        }
    }
    
    private String generateQueryString(String ... params) throws UnsupportedEncodingException {
        if(params.length % 2 != 0)
            throw new IllegalArgumentException("params must be in the form {'key', 'value', ...}");


        StringBuffer queryString = new StringBuffer("?");
        for(int i=0; i<params.length; i+=2) {
            queryString.append(URLEncoder.encode(params[i], "UTF-8"));
            queryString.append('=');
            queryString.append(URLEncoder.encode(params[i+1], "UTF-8"));
            queryString.append('&');
        }
        queryString.deleteCharAt(queryString.length()-1);

        return queryString.toString();
    }

    private JSONObject readJSONObject(InputStream input) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuffer data = new StringBuffer();

        String line;
        while( (line=reader.readLine()) != null) {
            data.append(line);
            data.append('\n');
        }

        return new JSONObject(data.toString());
    }
    
    public String toString() {
    	return baseUrl.toString();
    }
}