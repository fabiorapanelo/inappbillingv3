package com.fabiorapanelo.inappbillingv3;

import android.app.Activity;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements InAppBillingConstants {

    private InAppBillingHelper inAppBillingHelper;

    private Button buyInAppButton;
    private Button consumeButton;

    private List<Purchase> purchases = new ArrayList<Purchase>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buyInAppButton = (Button) this.findViewById(R.id.buyInAppButton);
        consumeButton = (Button) this.findViewById(R.id.consumeButton);

        inAppBillingHelper = new InAppBillingHelper(this);
        inAppBillingHelper.startSetup(new AsyncListener() {
            public void onFinished(AsyncResult result) {
                Toast.makeText(getApplicationContext(), result.getMessage(), Toast.LENGTH_SHORT).show();

                if (result.isSuccess()) {
                    try {
                        purchases = inAppBillingHelper.getOwnedItems(ITEM_TYPE_INAPP);

                        if(hasItem(ITEM_PURCHASED)){
                            setBuyButtonVisible(false);
                        } else {
                            setBuyButtonVisible(true);
                        }

                    } catch (RemoteException | JSONException e) {
                        Toast.makeText(getApplicationContext(), "Não foi possivel recuperar os items comprados", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });
    }

    public void buyInApp(View view){

        int response = inAppBillingHelper.buy(this, ITEM_PURCHASED, ITEM_TYPE_INAPP, REQUEST_CODE_BUY_IN_APP);
        if (response != BILLING_RESPONSE_RESULT_OK) {
            String errorDescription = inAppBillingHelper.getResponseDescription(response);
            Toast.makeText(getApplicationContext(), errorDescription, Toast.LENGTH_SHORT).show();
        }
    }

    public void consume(View view){


        Purchase purchase = this.getPurchaseItem(ITEM_PURCHASED);
        int response = inAppBillingHelper.consume(purchase);

        if (response != BILLING_RESPONSE_RESULT_OK) {
            String errorDescription = inAppBillingHelper.getResponseDescription(response);
            Toast.makeText(getApplicationContext(), errorDescription, Toast.LENGTH_SHORT).show();
        } else {
            this.setBuyButtonVisible(true);
            Toast.makeText(getApplicationContext(), "Item consumido!", Toast.LENGTH_SHORT).show();
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

            try {
                Purchase purchase = new Purchase(ITEM_TYPE_INAPP, purchaseData, dataSignature);
                purchases.add(purchase);
                Toast.makeText(getApplicationContext(), "Item comprado:" + purchase.getSku(),
                        Toast.LENGTH_LONG).show();
                this.setBuyButtonVisible(false);
            }
            catch (JSONException e) {}
        } else {
            Toast.makeText(getApplicationContext(), "Nao foi possivel comprar o item",
                    Toast.LENGTH_LONG).show();
        }
    }


    private boolean hasItem(String item){

        for(Purchase purchase: purchases){
            if(item.equals(purchase.getSku())){
                return true;
            }
        }

        return false;
    }


    private Purchase getPurchaseItem(String item){

        for(Purchase purchase: purchases){
            if(item.equals(purchase.getSku())){
                return purchase;
            }
        }

        return null;
    }

    public void setBuyButtonVisible(boolean value){

        if(value){
            consumeButton.setVisibility(View.GONE);
            buyInAppButton.setVisibility(View.VISIBLE);
        } else {
            consumeButton.setVisibility(View.VISIBLE);
            buyInAppButton.setVisibility(View.GONE);
        }

    }
}
