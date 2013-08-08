package com.ppmeet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context arg0, Intent data) {
		if("com.ppmeet.alarm".equals(data.getAction())){
			
			arg0.sendBroadcast(new Intent("com.ppmeet.time"));
			
		}
		
	}
}
