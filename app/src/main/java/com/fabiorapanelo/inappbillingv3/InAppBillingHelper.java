package com.fabiorapanelo.inappbillingv3;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;

//https://developer.android.com/google/play/billing/index.html
//https://developer.android.com/google/play/billing/billing_integrate.html
//https://developer.android.com/google/play/billing/billing_testing.html
public class InAppBillingHelper implements InAppBillingConstants {

    private Context context;
    private IInAppBillingService service;
    private ServiceConnection serviceConnection;

    public InAppBillingHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public void startSetup(final AsyncListener listener) {

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                service = IInAppBillingService.Stub.asInterface(iBinder);
                String packageName = context.getPackageName();
                try {
                    int response = service.isBillingSupported(API_VERSION, packageName, ITEM_TYPE_INAPP);

                    if (response != BILLING_RESPONSE_RESULT_OK) {
                        listener.onFinished(new AsyncResult(response, "Error checking for billing v3 support."));
                    } else {
                        listener.onFinished(new AsyncResult(BILLING_RESPONSE_RESULT_OK, "Conectado ao In App Billing V" + API_VERSION));
                    }
                }
                catch (RemoteException e) {
                    listener.onFinished(new AsyncResult(BILLING_RESPONSE_RESULT_ERROR, "RemoteException while setting up in-app billing."));
                    return;
                }
            }
        };

        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        List<ResolveInfo> intentServices = context.getPackageManager().queryIntentServices(serviceIntent, 0);
        if (intentServices != null && !intentServices.isEmpty()) {
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        else {
            listener.onFinished(new AsyncResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Billing service unavailable on device."));
        }
    }

    public int buy(Activity activity, String sku, String itemType, int requestCode){

        try {
            Bundle buyIntentBundle = service.getBuyIntent(API_VERSION, context.getPackageName(), sku, itemType, "");

            int response = getResponseCodeFromBundle(buyIntentBundle);
            if (response == BILLING_RESPONSE_RESULT_OK) {

                PendingIntent pendingIntent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
                activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                        requestCode, new Intent(),
                        Integer.valueOf(0), Integer.valueOf(0),
                        Integer.valueOf(0));
            }

            return response;
        }
        catch (IntentSender.SendIntentException|RemoteException e) {
            e.printStackTrace();
        }

        return BILLING_RESPONSE_RESULT_ERROR;

    }

    public List<Purchase> getOwnedItems(String itemType) throws RemoteException, JSONException {

        List<Purchase> purchases = new ArrayList<Purchase>();
        String continueToken = null;

        do {
            Bundle ownedItems = service.getPurchases(API_VERSION, context.getPackageName(), itemType, continueToken);

            int response = getResponseCodeFromBundle(ownedItems);
            if (response == BILLING_RESPONSE_RESULT_OK &&
                    ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST) &&
                    ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST) &&
                    ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST)) {

                ArrayList<String> ownedSkus = ownedItems.getStringArrayList(RESPONSE_INAPP_ITEM_LIST);
                ArrayList<String> purchaseDataList = ownedItems.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST);
                ArrayList<String> signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST);

                for (int i = 0; i < purchaseDataList.size(); ++i) {
                    String purchaseData = purchaseDataList.get(i);
                    String signature = signatureList.get(i);
                    String sku = ownedSkus.get(i);

                    Purchase purchase = new Purchase(itemType, purchaseData, signature);
                    purchases.add(purchase);
                }
                continueToken = ownedItems.getString(INAPP_CONTINUATION_TOKEN);
            } else {
                continueToken = null;
            }

        } while (!TextUtils.isEmpty(continueToken));

        return purchases;
    }

    public int consume(Purchase purchase)  {
        try {
            String token = purchase.getToken();
            String sku = purchase.getSku();

            int response = service.consumePurchase(API_VERSION, context.getPackageName(), token);
            return response;
        }
        catch (RemoteException e) {
            return BILLING_RESPONSE_RESULT_ERROR;
        }
    }

    public int getResponseCodeFromBundle(Bundle b) {
        Object o = b.get(RESPONSE_CODE);
        if (o == null) {
            return BILLING_RESPONSE_RESULT_OK;
        }
        else if (o instanceof Integer) return ((Integer)o).intValue();
        else if (o instanceof Long) return (int)((Long)o).longValue();
        else {
            throw new RuntimeException("Unexpected type for bundle response code: " + o.getClass().getName());
        }
    }

    public String getResponseDescription(int code) {
        String[] iab_msgs = ("0:OK/1:User Canceled/2:Unknown/" +
                "3:Billing Unavailable/4:Item unavailable/" +
                "5:Developer Error/6:Error/7:Item Already Owned/" +
                "8:Item not owned").split("/");
        String[] iabhelper_msgs = ("0:OK/-1001:Remote exception during initialization/" +
                "-1002:Bad response received/" +
                "-1003:Purchase signature verification failed/" +
                "-1004:Send intent failed/" +
                "-1005:User cancelled/" +
                "-1006:Unknown purchase response/" +
                "-1007:Missing token/" +
                "-1008:Unknown error/" +
                "-1009:Subscriptions not available/" +
                "-1010:Invalid consumption attempt").split("/");

        if (code <= IABHELPER_ERROR_BASE) {
            int index = IABHELPER_ERROR_BASE - code;
            if (index >= 0 && index < iabhelper_msgs.length) return iabhelper_msgs[index];
            else return String.valueOf(code) + ":Unknown IAB Helper Error";
        }
        else if (code < 0 || code >= iab_msgs.length)
            return String.valueOf(code) + ":Unknown";
        else
            return iab_msgs[code];
    }
}
