package com.fabiorapanelo.inappbillingv3;

import android.app.Activity;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;

import java.util.List;

public class MainActivity extends AppCompatActivity implements InAppBillingConstants {

    private InAppBillingHelper inAppBillingHelper;
    private boolean hasItem = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inAppBillingHelper = new InAppBillingHelper(this);
        inAppBillingHelper.startSetup(new AsyncListener() {
            public void onFinished(AsyncResult result) {
                Toast.makeText(getApplicationContext(), result.getMessage(), Toast.LENGTH_SHORT).show();

                if (result.isSuccess()) {
                    try {
                        List<Purchase> purchases = inAppBillingHelper.getOwnedItems(ITEM_TYPE_INAPP);
                        for(Purchase purchase: purchases){
                            if(ITEM_PURCHASED.equals(purchase.getSku())){
                                hasItem = true;
                                break;
                            }
                        }

                    } catch (RemoteException | JSONException e) {
                        Toast.makeText(getApplicationContext(), "Não foi possivel recuperar os items comprados", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
    }

    public void buyInApp(View view){

        if(!hasItem) {
            int response = inAppBillingHelper.buy(this, ITEM_PURCHASED, ITEM_TYPE_INAPP, REQUEST_CODE_BUY_IN_APP);

            if (response != BILLING_RESPONSE_RESULT_OK) {
                String errorDescription = inAppBillingHelper.getResponseDescription(response);
                Toast.makeText(getApplicationContext(), errorDescription, Toast.LENGTH_SHORT).show();
            } else {
                hasItem = true;
            }
        } else {
            Toast.makeText(getApplicationContext(), "Item já comprado", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int responseCode = inAppBillingHelper.getResponseCodeFromBundle(data.getExtras());
        if (requestCode == REQUEST_CODE_BUY_IN_APP && resultCode == Activity.RESULT_OK &&
                responseCode == BILLING_RESPONSE_RESULT_OK) {

            String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
            String dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);

            if (purchaseData == null || dataSignature == null) {
                return;
            }

            Purchase purchase = null;
            try {
                purchase = new Purchase(ITEM_TYPE_INAPP, purchaseData, dataSignature);
                String sku = purchase.getSku();

                Toast.makeText(getApplicationContext(), "Item comprado:" + sku, Toast.LENGTH_LONG).show();
            }
            catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        } else {
            Toast.makeText(getApplicationContext(), "Nao foi possivel comprar o item", Toast.LENGTH_LONG).show();
        }
    }
}
