package pt.ulisboa.tecnico.cmov.conversationalist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("kakaka", "ssss");
        Intent i = new Intent(context, BackgroundNotificationService.class);
        context.startService(i);
    }
}
