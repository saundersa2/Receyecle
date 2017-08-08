package com.receyecle.app;

/**
 * Created by perrasr on 7/18/17.
 */

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class CapturedImageRequest extends StringRequest{
    private static final String LOGIN_REQUEST_URL = "https://receyecle.000webhostapp.com/recycle.php";
    private Map<String, String> params;

    public CapturedImageRequest(String classifier, Response.Listener<String> listener){
        super(Request.Method.POST, LOGIN_REQUEST_URL, listener, null);
        params = new HashMap<>();
        params.put("classifier", classifier);

    }


    @Override
    public Map<String, String> getParams() {
        return params;
    }
}