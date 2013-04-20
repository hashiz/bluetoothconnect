package jp.meridiani.apps.bluetoothconnect.receiver;

import java.util.List;

import jp.meridiani.apps.bluetoothconnect.Constants;
import jp.meridiani.apps.bluetoothconnect.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class FireReceiver extends BroadcastReceiver {

	private IntentFilter      mFilter;
	private boolean           mReset;
	private BluetoothAdapter mDesireDevice;

	public class StateReceiver extends BroadcastReceiver {
		public StateReceiver() {
			Log.d(this.getClass().getName(), "StateReceiver");
		}

		@Override
        public void onReceive(Context context, Intent intent) {
			Log.d(this.getClass().getName(), "onReceive");
        	String action = intent.getAction();
        	if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            	int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                switch (state) {
            	case BluetoothAdapter.STATE_CONNECTED:
        	    	{
        	    		BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        	    		if (device.getAddress() == mDesireDevice.getAddress()) {
        	    			Toast.makeText(context, context.getString(R.string.msg_connected, mDesireDevice.getName()), Toast.LENGTH_LONG).show();
        	    			
        	    			Log.d(this.getClass().getName(), "Connected");

        	    			context.getApplicationContext().unregisterReceiver(this);
        	    			cancelTimer(context);
        	    			enableNetworks(context);
        	    		}
        	    	}
        	    	break;
        		default:
        			break;
            	}
        	}
        }
	}

	public FireReceiver() {
		Log.d(this.getClass().getName(), "FireReceiver");
        mFilter = new IntentFilter();
        mFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        mFilter.addAction(Constants.ACTION_TIMEOUT);
    }

    @Override
	public void onReceive(Context context, Intent intent) {
		Log.d(this.getClass().getName(), "onReceive");
		if (!com.twofortyfouram.locale.Intent.ACTION_FIRE_SETTING.equals(intent.getAction())) {
        	return;
        }
        Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);
        if (bundle == null) {
        	return;
        }
        String deviceAddr = bundle.getString(Constants.BUNDLE_DEVICE_ADDR);
        if (deviceAddr == null || deviceAddr.length() < 1) {
        	return;
        }

        mReset = false;

        WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) {
    		Toast.makeText(context, context.getString(R.string.msg_wifi_disable), Toast.LENGTH_LONG).show();
    		return;
        }
        List<WifiConfiguration>wifiList = wifi.getConfiguredNetworks();
        mDesireWifiConf = null;
        int lastPriority = 0;
        for (WifiConfiguration wifiConf : wifiList) {
        	if (ssid.equals(wifiConf.SSID)) {
        		if (wifiConf.status == WifiConfiguration.Status.CURRENT) {
            		Toast.makeText(context, context.getString(R.string.msg_already_connect, ssid), Toast.LENGTH_LONG).show();
            		return;
        		}
        		mDesireWifiConf = wifiConf;
        	}
        	if (wifiConf.priority > lastPriority) {
        		lastPriority = wifiConf.priority;
        	}
        }
        if (mDesireWifiConf == null) {
    		Toast.makeText(context, context.getString(R.string.msg_not_configured, ssid), Toast.LENGTH_LONG).show();
        	return;
        }
		Toast.makeText(context, context.getString(R.string.msg_connecting, mDesireWifiConf.SSID), Toast.LENGTH_LONG).show();

		// set priority to top
		mDesireWifiConf.priority = lastPriority + 1;
		wifi.updateNetwork(mDesireWifiConf);
		wifi.saveConfiguration();
		
		// disable other networks
        wifi.enableNetwork(mDesireWifiConf.networkId, true);
        mReset = true;

		// register
		context.getApplicationContext().registerReceiver(new StateReceiver(), mFilter);

        wifi.reconnect();

        // timer start
        AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 15000, makePendingIntent(context));
    }

    private PendingIntent makePendingIntent(Context context) {
		Log.d(this.getClass().getName(), "makePendingIntent");
	    Intent i = new Intent();
	    i.setAction(Constants.ACTION_TIMEOUT);
	    i.setClass(context.getApplicationContext(), TimeoutReceiver.class);
	    i.putExtra(Constants.BUNDLE_SSID, mDesireWifiConf.SSID);
	    i.putExtra(Constants.BUNDLE_NETWORKID, mDesireWifiConf.networkId);
	    return PendingIntent.getBroadcast(context.getApplicationContext(), 0, i, 0);
    }

	private void enableNetworks(Context context) {
		Log.d(this.getClass().getName(), "enableNetworks");
		if (mReset) {
	        WifiManager wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
	        List<WifiConfiguration> wifiList = wifi.getConfiguredNetworks();
	        for (WifiConfiguration wifiConf : wifiList) {
	       		wifi.enableNetwork(wifiConf.networkId, false);
	        }
	        mReset = false;
		}
	}

	private void cancelTimer(Context context) {
		Log.d(this.getClass().getName(), "cancelTimer");
		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(makePendingIntent(context));
	}
}
