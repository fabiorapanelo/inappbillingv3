package com.fabiorapanelo.inappbillingv3;

import java.util.HashMap;
import java.util.Map;

public class AsyncResult {

    private int status;
    private String message;
    private Map<String, Object> bundle = new HashMap<String, Object>();

    public AsyncResult(int status, String message){
        this.message = message;
        this.status = status;
    }

    public String getMessage(){
        return message;
    }

    public void addObjectToBundle(String key, Object object){
        bundle.put(key, object);
    }

    public Object getObjectFromBundle(String key){

        return bundle.get(key);
    }

    public boolean isSuccess(){
        return status == InAppBillingHelper.BILLING_RESPONSE_RESULT_OK;
    }
}
