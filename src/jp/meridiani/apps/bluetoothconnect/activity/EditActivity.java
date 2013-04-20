package jp.meridiani.apps.bluetoothconnect.activity;

import java.util.Set;

import jp.meridiani.apps.bluetoothconnect.Constants;
import jp.meridiani.apps.bluetoothconnect.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.ToggleButton;

public class EditActivity extends Activity implements OnItemSelectedListener, OnItemClickListener {

	private String mSelectedDevice;
	private ListView mDeviceListView;
	private ToggleButton mBluetoothButton;
	private Button mSelectButton;
	private Button mCancelButton;
	private boolean mCanceled;
	private boolean mBluetoothEnabledFirst;
	private static final IntentFilter BT_STATE_CHANGED = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
	
	private class Dev {
		BluetoothDevice mDevice;
		public Dev(BluetoothDevice device) {
			mDevice = device;
		}
		public String toString() {
			return mDevice.getName();
		}
		public BluetoothDevice getDevice() {
			return mDevice;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// receive intent and extra data
		Intent intent = getIntent();
		if (!com.twofortyfouram.locale.Intent.ACTION_EDIT_SETTING.equals(intent.getAction())) {
			super.finish();
			return;
		}

		Bundle bundle = getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

		mSelectedDevice = null;
		if (bundle != null) {
			mSelectedDevice = bundle.getString(Constants.BUNDLE_DEVICE_ADDR);
		}
		if (mSelectedDevice == null) {
			mSelectedDevice = "";
		}

		mCanceled = false;

		// set view
		setContentView(R.layout.activity_edit);
		mDeviceListView = (ListView)findViewById(R.id.DeviceList);
		mBluetoothButton = (ToggleButton)findViewById(R.id.bluetooth_button);
		mSelectButton = (Button)findViewById(R.id.select_button);
		mCancelButton = (Button)findViewById(R.id.cancel_button);

		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter == null) {
			mCanceled = true;
			finish();
			return;
		}

		mBluetoothButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				setBTEnable(isChecked);
			}
		});

		mBluetoothButton.setChecked(adapter.isEnabled());

		mSelectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onSelectClick((Button)v);
			}
		});
		mCancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onCancelClick((Button)v);
			}
		});

		updateDeviceList();

		getApplicationContext().registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
					return;
				}
				onBTStateChanged(intent);
			}

		}, BT_STATE_CHANGED);
	}

	@Override
    public void finish()
    {
		int selPos = -1;
		if (mDeviceListView.getCount() > 0) {
			selPos = mDeviceListView.getCheckedItemPosition();
		}
		String deviceAddr = null;
		String deviceName = null;
		if (selPos >= 0) {
			BluetoothDevice device = ((Dev)mDeviceListView.getAdapter().getItem(selPos)).getDevice();
			deviceAddr = device.getAddress();
			deviceName = device.getName();
		}

        Intent resultIntent = new Intent();
        if (! mCanceled && deviceAddr != null && deviceAddr.length() > 0) {
            Bundle resultBundle = new Bundle();
            resultBundle.putString(Constants.BUNDLE_DEVICE_ADDR, deviceAddr);

            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, deviceName);
            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);

            setResult(RESULT_OK, resultIntent);
        }
        else {
            setResult(RESULT_CANCELED, resultIntent);
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothEnabledFirst && adapter.isEnabled()) {
        	setBTEnable(false);
        }
    	super.finish();
    }

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	}

	private void setBTEnable(boolean enable) {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (enable) {
			if (!adapter.isEnabled()) {
				adapter.enable();
			}
		}
		else {
			if (adapter.isEnabled()) {
				adapter.disable();
			}
		}
	}

	private void updateDeviceList() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

		Set<BluetoothDevice>deviceList = adapter.getBondedDevices();

		ArrayAdapter<Dev> aa = new ArrayAdapter<Dev>(this,
							android.R.layout.simple_list_item_single_choice);
		int selPos = 0;
		for ( BluetoothDevice device : deviceList) {
			aa.add(new Dev(device));
			if (mSelectedDevice.equals(device.getAddress())) {
				selPos = aa.getCount() - 1;
			}
		}

		ListView deviceListView = (ListView)findViewById(R.id.DeviceList);
		deviceListView.setAdapter(aa);
		deviceListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		deviceListView.setItemChecked(selPos, true);
		deviceListView.setOnItemSelectedListener(this);
		deviceListView.setOnItemClickListener(this);
	}

	private void onSelectClick(Button b) {
		mCanceled = false;
		finish();
	}

	private void onCancelClick(Button b) {
		mCanceled = true;
		finish();
	}

	private void onBTStateChanged(Intent intent) {
		int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
		switch (state) {
		case BluetoothAdapter.STATE_ON:
			updateDeviceList();
			break;
		case BluetoothAdapter.STATE_OFF:
			updateDeviceList();
			break;
		}
	}
}
