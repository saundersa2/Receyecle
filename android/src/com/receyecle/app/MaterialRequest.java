package com.receyecle.app;

/**
 * Created by perrasr on 7/18/17.
 */

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class MaterialRequest extends StringRequest{
    private static final String LOGIN_REQUEST_URL = "https://receyecle.000webhostapp.com/recycle_multmater.php";
    private Map<String, String> params;

    public MaterialRequest(String classifier, String material, Response.Listener<String> listener){
        super(Method.POST, LOGIN_REQUEST_URL, listener, null);
        params = new HashMap<>();
        params.put("classifier", classifier);
        params.put("material", material);

    }


    @Override
    public Map<String, String> getParams() {
        return params;
    }
}