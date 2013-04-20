package jp.meridiani.apps.bluetoothconnect.receiver;

import java.util.Set;

import jp.meridiani.apps.bluetoothconnect.Constants;
import jp.meridiani.apps.bluetoothconnect.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class FireReceiver extends BroadcastReceiver {

	private IntentFilter     mFilter;
	private BluetoothDevice  mDesireDevice;

	private class StateReceiver extends BroadcastReceiver {
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
        	    		}
        	    	}
        	    	break;
        		default:
        			break;
            	}
        	}
        }
	}

	private class TimeoutReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(this.getClass().getName(), "onReceive");
	    	String action = intent.getAction();
	    	if (!Constants.ACTION_TIMEOUT.equals(action)) {
	    		return;
	    	}
	    	String deviceName = intent.getStringExtra(Constants.BUNDLE_DEVICE_NAME);
	    	if (deviceName == null) return;
	    	String deviceAddr = intent.getStringExtra(Constants.BUNDLE_DEVICE_ADDR);
	    	if (deviceAddr == null) return;
	    	boolean connectAudio = intent.getBooleanExtra(Constants.BUNDLE_CONNECT_AUDIO, false);
	    	if (deviceAddr == null) return;
	    	boolean connectHeadset = intent.getBooleanExtra(Constants.BUNDLE_CONNECT_HEADSET, false);
	    	if (deviceAddr == null) return;

	    	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
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
        String deviceName = bundle.getString(Constants.BUNDLE_DEVICE_NAME);
        String deviceAddr = bundle.getString(Constants.BUNDLE_DEVICE_ADDR);
        if (deviceAddr == null || deviceAddr.length() < 1) {
        	return;
        }

        boolean connectAudio = bundle.getBoolean(Constants.BUNDLE_CONNECT_AUDIO);
        boolean connectHeadset = bundle.getBoolean(Constants.BUNDLE_CONNECT_HEADSET);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (!adapter.isEnabled()) {
    		Toast.makeText(context, context.getString(R.string.msg_bluetooth_disable), Toast.LENGTH_LONG).show();
    		return;
        }
        Set<BluetoothDevice>deviceList = adapter.getBondedDevices();
        mDesireDevice = null;
        for (BluetoothDevice device : deviceList) {
        	if (mDesireDevice.getAddress().equals(device.getAddress())) {
        		mDesireDevice = device;
        	}
        }
        if (mDesireDevice == null) {
    		Toast.makeText(context, context.getString(R.string.msg_not_configured, deviceName), Toast.LENGTH_LONG).show();
        	return;
        }
		Toast.makeText(context, context.getString(R.string.msg_connecting, mDesireDevice.getName()), Toast.LENGTH_LONG).show();

		if (connectAudio) {
			isConnected(context, adapter, BluetoothProfile.A2DP);
		}
		if (connectHeadset) {
			isConnected(context, adapter, BluetoothProfile.HEADSET);
		}
		
        // timer start
        AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 15000, makePendingIntent(context));
    }

	private static class Listener implements BluetoothProfile.ServiceListener {
		private BluetoothProfile mProfileProxy = null;

		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			mProfileProxy = proxy;
		}

		@Override
		public void onServiceDisconnected(int profile) {
			mProfileProxy = null;
			
		}
		public BluetoothProfile getProfileProxy() {
			return mProfileProxy;
		}
		
	}

	private static BluetoothProfile getProxyProfile(Context context, BluetoothAdapter adapter, int profile) {
		Listener listener = new Listener();
    	adapter.getProfileProxy(context, new Listener(), profile);

    	return listener.getProfileProxy() ;
	}

	private static boolean isConnected(BluetoothProfile proxy, int profile, BluetoothDevice device) {

    	if (proxy == null) {
			// no present service (has no profile?)
    		return false;
		}

    	switch (proxy.getConnectionState(device)) {
    	case BluetoothProfile.STATE_CONNECTED:
			// already connected
			Toast.makeText(context, context.getString(R.string.msg_already_connect, device.getName()), Toast.LENGTH_LONG).show();
			return true;
    	case BluetoothProfile.STATE_CONNECTING:
			// connecting. wait
			Toast.makeText(context, context.getString(R.string.msg_connecting, device.getName()), Toast.LENGTH_LONG).show();
			return true;
		default:
			return false;
		}
    }

	private static void connect(BluetoothProfile proxy, int profile, BluetoothDevice device) {

		// connecting
		switch (profile) {
		case BluetoothProfile.A2DP:
			((BluetoothA2dp)proxy).connect(device);
			break;
		case BluetoothProfile.HEADSET:
			((BluetoothA2dp)proxy).connect(device);
			break;
		}
		
			Toast.makeText(context, context.getString(R.string.msg_connecting, device.getName()), Toast.LENGTH_LONG).show();
			return ;
    }

    private PendingIntent makePendingIntent(Context context) {
		Log.d(this.getClass().getName(), "makePendingIntent");
	    Intent i = new Intent();
	    i.setAction(Constants.ACTION_TIMEOUT);
	    i.setClass(context.getApplicationContext(), TimeoutReceiver.class);
	    i.putExtra(Constants.BUNDLE_DEVICE_NAME, mDesireDevice.getName());
	    i.putExtra(Constants.BUNDLE_DEVICE_ADDR, mDesireDevice.getAddress());
	    return PendingIntent.getBroadcast(context.getApplicationContext(), 0, i, 0);
    }

	private void cancelTimer(Context context) {
		Log.d(this.getClass().getName(), "cancelTimer");
		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(makePendingIntent(context));
	}
}
