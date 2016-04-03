package com.example.adminibm.coolweather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by ADMINIBM on 2016/4/3.
 */
public class AutoUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context,Intent intent){
        Intent i=new Intent(context,AutoUpdateReceiver.class);
        context.startActivity(i);
    }
}
