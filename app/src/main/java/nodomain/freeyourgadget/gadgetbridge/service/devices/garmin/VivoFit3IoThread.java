/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Julien Pivotto, Uwe Hermann

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nodomain.freeyourgadget.gadgetbridge.Logging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.NotifyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.PlainAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCallback;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractGattCallback;

import nodomain.freeyourgadget.gadgetbridge.devices.garmin.VivoFit3Constants;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

class VivoFit3IoThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(VivoFit3IoThread.class);
		private BtLEQueue mQueue;
		private BluetoothAdapter mAdapter;
		private GattCallback mCallback;

		private VivoFit3Protocol mProtocol;
		private VivoFit3Support mSupport;

    private Map<UUID, BluetoothGattCharacteristic> mAvailableCharacteristics  = new HashMap<>();

		private ByteBuffer responseAccumulator = ByteBuffer.allocate(0);


		@FunctionalInterface
		public interface MyRunnable {
			public void run(byte[] bytes);
		}

		private Map<Short, Queue<MyRunnable>> callbacksMap = new HashMap<>();

    VivoFit3IoThread(VivoFit3Support support, GBDevice gbDevice, VivoFit3Protocol gbDeviceProtocol, BluetoothAdapter btAdapter, Context context) {
			super(gbDevice, context);
			mSupport = support;
			mAdapter = btAdapter;
			mProtocol = gbDeviceProtocol;
			mCallback = new AbstractGattCallback() {
				@Override
				public void onServicesDiscovered(BluetoothGatt gatt) {
					synchronized (mAvailableCharacteristics) {
						for (BluetoothGattService service : gatt.getServices()) {
							for (BluetoothGattCharacteristic charac : service.getCharacteristics()) {
								mAvailableCharacteristics.put(charac.getUuid(), charac);
							}
						}
						mAvailableCharacteristics.notify();
					}
				}

				//@Override
        //public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
				//	if (status != 0) {
				//	}
				//}

				public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic charac) {
					byte[] resp = charac.getValue();
					ByteBuffer tmpResponseAccumulator = ByteBuffer
						.allocate(responseAccumulator.array().length + resp.length)
						.put(responseAccumulator.array())
						.put(resp)
						;
					synchronized (responseAccumulator) {
						responseAccumulator = tmpResponseAccumulator;
					}
					if (resp[resp.length - 1] == 0x00) {
						byte[] responseArray;
						synchronized (responseAccumulator) {
							responseArray = responseAccumulator.array();
							responseAccumulator = ByteBuffer.allocate(0);
						}
						
						ByteBuffer tmpBb = ByteBuffer
							.wrap(VivoFit3Protocol.decodeCOBS(responseArray))
							.order(ByteOrder.LITTLE_ENDIAN);
						short id = tmpBb.getShort(2);
						if (id != 0x1388) {
							write(mProtocol.ack(tmpBb.array()));
						} else {
							short responseId = tmpBb.getShort(4);
							Queue<MyRunnable> responseCallbacks = callbacksMap.get(responseId);
							if (responseCallbacks == null) {
								LOG.debug("responsecallbacks is null for id " + responseId);
								return true;
							}
							MyRunnable responseCallback = responseCallbacks.poll();
							if (responseCallback == null) {
								LOG.debug("responsecallback is null for id " + responseId);
								return true;
							}

							byte[] currentArray = tmpBb.array();
							byte[] newResponseArray = new byte[currentArray.length - 8];
							System.arraycopy(currentArray, 7, newResponseArray, 0, currentArray.length - 9);
							responseCallback.run(newResponseArray);
						}
					}
					return true;
				}
			};
    }

		protected boolean initialize() {
			VivoFit3OutputStream settingsFile = new VivoFit3OutputStream("", (byte) 0x09, this);
			settingsFile.write(new byte[] {
				(byte) 0x0e,(byte) 0x10,(byte) 0x1d,(byte) 0x08,(byte) 0xcd,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x2e,(byte) 0x46,(byte) 0x49,(byte) 0x54,(byte) 0x59,(byte) 0xe3,(byte) 0x40,(byte) 0x00,(byte) 0x01,(byte) 0x00,(byte) 0x00,(byte) 0x06,(byte) 0x00,(byte) 0x01,(byte) 0x00,(byte) 0x01,(byte) 0x02,(byte) 0x84,(byte) 0x02,(byte) 0x02,(byte) 0x84,(byte) 0x04,(byte) 0x04,(byte) 0x86,(byte) 0x03,(byte) 0x04,(byte) 0x8c,(byte) 0x05,(byte) 0x02,(byte) 0x84,(byte) 0x00,(byte) 0x02,(byte) 0x00,(byte) 0x01,(byte) 0xff,(byte) 0xfe,(byte) 0x36,(byte) 0x75,(byte) 0x38,(byte) 0x60,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x01,(byte) 0x00,(byte) 0x01,(byte) 0x40,(byte) 0x00,(byte) 0x01,(byte) 0x00,(byte) 0x03,(byte) 0x10,(byte) 0x02,(byte) 0x01,(byte) 0x02,(byte) 0x01,(byte) 0x01,(byte) 0x00,(byte) 0x04,(byte) 0x02,(byte) 0x84,(byte) 0x03,(byte) 0x01,(byte) 0x02,(byte) 0x18,(byte) 0x01,(byte) 0x02,(byte) 0x1b,(byte) 0x01,(byte) 0x00,(byte) 0x1d,(byte) 0x04,(byte) 0x86,(byte) 0x1c,(byte) 0x04,(byte) 0x86,(byte) 0x1f,(byte) 0x02,(byte) 0x84,(byte) 0x20,(byte) 0x02,(byte) 0x84,(byte) 0x0e,(byte) 0x01,(byte) 0x00,(byte) 0x08,(byte) 0x01,(byte) 0x02,(byte) 0x2b,(byte) 0x01,(byte) 0x00,(byte) 0x2d,(byte) 0x01,(byte) 0x0a,(byte) 0x2c,(byte) 0x01,(byte) 0x02,(byte) 0x33,(byte) 0x01,(byte) 0x00,(byte) 0x00,(byte) 0x12,(byte) 0x01,(byte) 0x02,(byte) 0x12,(byte) 0xaf,(byte) 0x64,(byte) 0x00,(byte) 0x00,(byte) 0x01,(byte) 0x27,(byte) 0x50,(byte) 0x00,(byte) 0x00,(byte) 0x54,(byte) 0x60,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x01,(byte) 0x00,(byte) 0x00,(byte) 0x09,(byte) 0x0b,(byte) 0x00,(byte) 0x40,(byte) 0x00,(byte) 0x01,(byte) 0x00,(byte) 0x02,(byte) 0x0c,(byte) 0x2f,(byte) 0x01,(byte) 0x00,(byte) 0x04,(byte) 0x01,(byte) 0x00,(byte) 0x32,(byte) 0x01,(byte) 0x0a,(byte) 0x28,(byte) 0x04,(byte) 0x84,(byte) 0x39,(byte) 0x04,(byte) 0x84,(byte) 0x46,(byte) 0x01,(byte) 0x02,(byte) 0x23,(byte) 0x01,(byte) 0x00,(byte) 0x3b,(byte) 0x02,(byte) 0x84,(byte) 0x3a,(byte) 0x02,(byte) 0x84,(byte) 0x2e,(byte) 0x01,(byte) 0x00,(byte) 0x5a,(byte) 0x04,(byte) 0x86,(byte) 0x59,(byte) 0x01,(byte) 0x00,(byte) 0x00,(byte) 0x01,(byte) 0x01,(byte) 0x00,(byte) 0x00,(byte) 0xbf,(byte) 0x00,(byte) 0x15,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x02,(byte) 0x00,(byte) 0x00,(byte) 0xf0,(byte) 0x07,(byte) 0xd0,(byte) 0x01,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x01,(byte) 0x03,(byte) 0x40,(byte) 0x00,(byte) 0x01,(byte) 0x00,(byte) 0x9f,(byte) 0x03,(byte) 0x08,(byte) 0x01,(byte) 0x00,(byte) 0x00,(byte) 0x01,(byte) 0x00,(byte) 0x01,(byte) 0x01,(byte) 0x0d,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x37,(byte) 0x25,
			});
			settingsFile.flush();
			// file system download xml file (1552)
			// filter and download another file (1870-1873)
			// create file & upload to file (1881-1885)
			return true;
		}

    @Override
    protected boolean connect() {
			gbDevice.sendDeviceUpdateIntent(getContext());
			mQueue = new BtLEQueue(mAdapter, getDevice(), mCallback, getContext());
			mQueue.setAutoReconnect(true);
			mQueue.connect();
			synchronized (mAvailableCharacteristics) {
				try {
					mAvailableCharacteristics.wait();
				} catch (InterruptedException e) {}
			}
			getDevice().setState(GBDevice.State.INITIALIZING);
			getDevice().sendDeviceUpdateIntent(getContext());
			new TransactionBuilder("Initialize device")
			.add(new NotifyAction(mAvailableCharacteristics.get(UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb")), true))
			.add(new NotifyAction(mAvailableCharacteristics.get(UUID.fromString("4acbcd28-7425-868e-f447-915c8f00d0cb")), true))
			.queue(mQueue);
			// send fit definition
			// send fit data
			if (mSupport.firstTime) {

				mSupport.onPairStart(); // system event (pair start)
				if (initialize()) {
					mSupport.onPairComplete(); // system event (pair complete)
				} else {
					// TODO: onpairfailure
				}

				gbDevice.sendDeviceUpdateIntent(getContext());
			}

			gbDevice.setState(GBDevice.State.INITIALIZED);
			gbDevice.sendDeviceUpdateIntent(getContext());
			return true;
    }

    @Override
		public void run() {
			connect();

			mSupport.onSetTime(); // set device settings (time ) (1866)

			new TransactionBuilder("End connection")
			.add(new PlainAction() {
				public boolean run(BluetoothGatt gatt) {
					quit();
					return true;
				}
			}).queue(mQueue);
    }


		private static List<byte[]> splitArray(byte[] array, int max){
			// from stackoverflow, lost the link

			int x = array.length / max;
			int r = (array.length % max); // remainder

			int lower = 0;
			int upper = 0;

			List<byte[]> list = new ArrayList<byte[]>();

			int i=0;

			for(i=0; i<x; i++){

				upper += max;

				list.add(Arrays.copyOfRange(array, lower, upper));

				lower = upper;
			}

			if(r > 0){

				list.add(Arrays.copyOfRange(array, lower, (lower + r)));

			}

			return list;
		}

		static private byte[] encodeCOBS(byte[] message) {
			// https://en.wikipedia.org/wiki/Consistent_Overhead_Byte_Stuffing
			ByteBuffer messageBuffer = ByteBuffer.allocate(message.length + 3);
			messageBuffer.put((byte) 0x00);
			messageBuffer.put((byte) 0x00);
			messageBuffer.put(message);
			messageBuffer.put((byte) 0x00);

			byte[] newMessage = messageBuffer.array();


			LOG.debug("cobs encode: " + Logging.formatBytes(newMessage));

			int nextIndex = 0x01;
			while (nextIndex != newMessage.length - 1) {
				int currentIndex = nextIndex;
				for (nextIndex++; newMessage[nextIndex] != 0; nextIndex++) {}
				newMessage[currentIndex] = (byte) (nextIndex - currentIndex);
			}

			return newMessage;

		}

		static private byte[] encodeLength(byte[] message) {
			return ByteBuffer.allocate(message.length + Short.BYTES)
				.order(ByteOrder.LITTLE_ENDIAN)
				// crc depends on the length bytes, length depends on crc bytes
				// existing, have to do it first somewhere, so here
				.putShort((short) (message.length + Short.BYTES + Short.BYTES)) // length of this number itself, length of the future crc
				.put(message)
				.array();
		}


		public static final int crcHelper(int i, byte b) {
			final int[] a = new int[]{0, 52225, 55297, 5120, 61441, 15360, 10240, 58369, 40961, 27648, 30720, 46081, 20480, 39937, 34817, 17408};
			i = (((i >> 4) & 4095) ^ a[i & 15]) ^ a[b & 15];
			return (((i >> 4) & 4095) ^ a[i & 15]) ^ a[(b >> 4) & 15];
		}

		static private byte[] encodeCRC(byte[] message) {
			int acc = 0;
			for (byte messagePart : message) {
				acc = crcHelper(acc, messagePart);
			}

			return ByteBuffer.allocate(message.length + Short.BYTES)
				.order(ByteOrder.LITTLE_ENDIAN)
				.put(message)
				.putShort((short) acc) // just spoof it for now, the watch doesnt actually check
				.array();
		}

		synchronized public void writeWithResponseCallback(@NonNull byte[] bytes, @Nullable MyRunnable callback) {
			if (callback == null) {
				callback = new MyRunnable() {
					public void run(byte[] bytes) {
						LOG.debug("Default response : " + Logging.formatBytes(bytes));
					}
				};
			}
			ByteBuffer tmpBb = ByteBuffer
				.wrap(bytes)
				.order(ByteOrder.LITTLE_ENDIAN);

			short id = tmpBb.getShort(0);
			if (id != 0x1388) {
				if (callbacksMap.get(id) == null) {
					callbacksMap.put(id, new LinkedList<MyRunnable>());
				}
				Queue<MyRunnable> responseCallbacks = callbacksMap.get(id);
				responseCallbacks.add(callback);
				LOG.debug("Adding callback with id " + id + "to queue");
			}




			TransactionBuilder builder = new TransactionBuilder("write");
			for (byte[] subarray : splitArray(encodeCOBS(encodeCRC(encodeLength(bytes))), 20)) {
				builder.write(mAvailableCharacteristics.get(VivoFit3Constants.UUID_SERVICE_VIVOFIT3_WRITER), subarray);
			}
			builder.queue(mQueue);

			write(mProtocol.getQueuedBytes()); // exiting condition is this returns null;
		}

    @Override
    synchronized public void write(@Nullable byte[] bytes) {
			if (bytes == null) {
				return;
			}
			writeWithResponseCallback(bytes, null);


    }

    @Override
    public void quit() {
			LOG.debug("MARCO ______________thread quit_____________");
			mSupport.onSyncComplete(); // system event (sync complete)
			new TransactionBuilder("Disconnect")
			.add(new PlainAction() {
				public boolean run(BluetoothGatt gatt) {
					mQueue.disconnect();
					gbDevice.setState(GBDevice.State.WAITING_FOR_RECONNECT);
					gbDevice.sendDeviceUpdateIntent(getContext());
					return true;
				}
			}).queue(mQueue);
    }
}
