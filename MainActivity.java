package com.jjinno.bluetoothtestk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

/**
 * http://manojprasaddevelopers.blogspot.com/2012/02/bluetooth-data-transfer-
 * example.html
 */
public class MainActivity extends Activity {

  /* configureBluetooth */
	private BluetoothAdapter bluetooth;
	private BluetoothSocket socket;
	private UUID uuid = UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666");

	/**
	 * Start by creating a field variable to store an Array List of discovered
	 * Bluetooth Devices.
	 */
	private ArrayList<BluetoothDevice> foundDevices;

	/**
	 * Fill in the setupListView stub. Create a new Array Adapter that binds the
	 * List View to the found devices array.
	 */
	private ArrayAdapter<BluetoothDevice> aa;
	private ListView list;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Get the Bluetooth Adapter
		configureBluetooth();
		// Setup the ListView of discovered devices
		setupListView();
		// Setup search button
		setupSearchButton();
		// Setup listen button
		setupListenButton();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/**
	 * It will be called once a connection is established to enable the Views
	 * used for reading and writing messages.
	 * 
	 * Start by extending the switchUI method. Add a new key listener to the
	 * text-entry Edit Text to lis- ten for a D-pad click. When one is detected,
	 * read its contents and send them across the Bluetooth communications
	 * socket.
	 */
	private void switchUI() {
		final TextView messageText = (TextView) findViewById(R.id.text_messages);
		final EditText textEntry = (EditText) findViewById(R.id.text_message);
		messageText.setVisibility(View.VISIBLE);
		list.setVisibility(View.GONE);
		textEntry.setEnabled(true);
		textEntry.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
				if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
					sendMessage(socket, textEntry.getText().toString());
					textEntry.setText("");
					return true;
				}
				return false;
			}
		});
	}

	private void sendMessage(BluetoothSocket socket, String msg) {
		OutputStream outStream;
		try {
			outStream = socket.getOutputStream();
			byte[] byteString = (msg + " ").getBytes();
			byteString[byteString.length - 1] = 0;
			outStream.write(byteString);
		} catch (IOException e) {
			Log.d("BLUETOOTH_COMMS", e.getMessage());
		}
	}

	/**
	 * Fill in the configureBluetooth stub to get access to the local Bluetooth
	 * Adapter and store it in a field variable. Take this opportunity to create
	 * a field variable for a Bluetooth Socket. This will be used to store
	 * either the server or client communications socket once a channel has been
	 * established. You should also define a UUID to identify your application
	 * when con- nections are being established.
	 */
	private void configureBluetooth() {
		bluetooth = BluetoothAdapter.getDefaultAdapter();
	}

	/**
	 * Complete the setupSearchButton stub to register the Broadcast Receiver
	 * from the previous step and initiate a discovery session.
	 */
	private void setupSearchButton() {
		Button searchButton = (Button) findViewById(R.id.button_search);
		searchButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				registerReceiver(discoveryResult, new IntentFilter(
						BluetoothDevice.ACTION_FOUND));
				if (!bluetooth.isDiscovering()) {
					foundDevices.clear();
					bluetooth.startDiscovery();
				}
			}
		});
	}

	private static int DISCOVERY_REQUEST = 1;

	private void setupListenButton() {
		Button listenButton = (Button) findViewById(R.id.button_listen);
		listenButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent disc;
				disc = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				startActivityForResult(disc, DISCOVERY_REQUEST);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DISCOVERY_REQUEST) {
			boolean isDiscoverable = resultCode > 0;
			if (isDiscoverable) {
				String name = "bluetoothserver";
				try {
					final BluetoothServerSocket btserver = bluetooth
							.listenUsingRfcommWithServiceRecord(name, uuid);
					AsyncTask<Integer, Void, BluetoothSocket> acceptThread = new AsyncTask<Integer, Void, BluetoothSocket>() {
						protected BluetoothSocket doInBackground(
								Integer... params) {

							try {
								socket = btserver.accept(params[0] * 1000);
								return socket;
							} catch (IOException e) {
								Log.d("BLUETOOTH", e.getMessage());
							}

							return null;
						}

						@Override
						protected void onPostExecute(BluetoothSocket result) {
							if (result != null)
								switchUI();
						}

					};
					acceptThread.execute(resultCode);
				} catch (IOException e) {
					Log.d("BLUETOOTH", e.getMessage());
				}
			}
		}
	}

	/**
	 * The final step to completing the connection-handling code is to extend
	 * the setupListView method from Step 7b. Extend this method to include an
	 * onItemClickListener that will attempt to asynchronously initiate a
	 * client-side connection with the selected remote Bluetooth Device. If it
	 * is successful, keep a reference to the socket it creates and make a call
	 * to the switchUI method created in Step 5.
	 */
	private void setupListView() {
		aa = new ArrayAdapter<BluetoothDevice>(this,
				android.R.layout.simple_list_item_1, foundDevices);
		list = (ListView) findViewById(R.id.list_discovered);
		list.setAdapter(aa);
		list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				AsyncTask<Integer, Void, Void> connectTask = new AsyncTask<Integer, Void, Void>() {
					@Override
					protected Void doInBackground(Integer... params) {
						try {
							BluetoothDevice device = foundDevices
									.get(params[0]);
							socket = device
									.createRfcommSocketToServiceRecord(uuid);
							socket.connect();
						} catch (IOException e) {
							Log.d("BLUETOOTH_CLIENT", e.getMessage());
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						// was switchViews(); verify
						switchUI();
					}
				};
				connectTask.execute(index);
			}
		});
	}

	/**
	 * Create a new Broadcast Receiver that listens for Bluetooth Device
	 * discovery broad- casts, adds each discovered device to the array of found
	 * devices created in Step 7-1, and notifies the Array Adapter created in
	 * Step 7-2.
	 */
	BroadcastReceiver discoveryResult = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			BluetoothDevice remoteDevice;
			remoteDevice = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			if (bluetooth.getBondedDevices().contains(remoteDevice)) {
				foundDevices.add(remoteDevice);
				aa.notifyDataSetChanged();
			}
		}
	};

	/**
	 * 11. In order to receive messages you will need to create an asynchronous
	 * listener that monitors the Bluetooth Socket for incoming messages.
	 * 
	 * 11.1.Start by creating a new MessagePoster class that implements
	 * Runnable. It should accept two parameters, a Text View and a message
	 * string. The received message should be inserted into the Text View
	 * parameter. This class will be used to post incoming messages to the UI
	 * from a background thread.
	 * 
	 * @author JJinno
	 * 
	 */
	private class MessagePoster implements Runnable {
		private TextView textView;
		private String message;

		public MessagePoster(TextView textView, String message) {
			this.textView = textView;
			this.message = message;
		}

		public void run() {
			textView.setText(message);
		}
	}

	/**
	 * 11.2. Now create a new BluetoothSocketListener that implements Runnable.
	 * It should take a Bluetooth Socket to listen to, a Text View to post
	 * incoming messages to, and a Handler to synchronize when posting updates.
	 * When a new message is received, use the MessagePoster Runnable you
	 * created in the previous step to post the new message in the Text View.
	 * 
	 * Finally, make one more addition to the swichUI method, this time creating
	 * and starting the new BluetoothSocketListener you created in the previous
	 * step.
	 */
	private class BluetoothSocketListener implements Runnable {
		private BluetoothSocket socket;
		private TextView textView;
		private Handler handler = new Handler();

		private void switchUI() {
			final TextView messageText = (TextView) findViewById(R.id.text_messages);
			final EditText textEntry = (EditText) findViewById(R.id.text_message);
			messageText.setVisibility(View.VISIBLE);
			list.setVisibility(View.GONE);
			textEntry.setEnabled(true);
			textEntry.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
					if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN)
							&& (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
						sendMessage(socket, textEntry.getText().toString());
						textEntry.setText("");
						return true;
					}
					return false;
				}
			});

			BluetoothSocketListener bsl = new BluetoothSocketListener(socket,
					handler, messageText);
			Thread messageListener = new Thread(bsl);
			messageListener.start();
		}

		public BluetoothSocketListener(BluetoothSocket socket, Handler handler,
				TextView textView) {
			this.socket = socket;
			this.textView = textView;
			this.handler = handler;
		}

		@Override
		public void run() {
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];
			try {
				InputStream instream = socket.getInputStream();
				int bytesRead = -1;
				String message = "";
				while (true) {
					message = "";
					bytesRead = instream.read(buffer);
					if (bytesRead != -1) {
						while ((bytesRead == bufferSize)
								&& (buffer[bufferSize - 1] != 0)) {
							message = message
									+ new String(buffer, 0, bytesRead);
							bytesRead = instream.read(buffer);
						}
						message = message
								+ new String(buffer, 0, bytesRead - 1);
						handler.post(new MessagePoster(textView, message));
						socket.getInputStream();
					}
				}
			} catch (IOException e) {
				Log.d("BLUETOOTH_COMMS", e.getMessage());
			}
		}

	}
}
