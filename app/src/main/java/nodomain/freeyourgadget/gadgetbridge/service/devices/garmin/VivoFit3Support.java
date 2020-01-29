/*  Copyright (C) 2020 Marco Sirabella

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

import android.net.Uri;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.zip.Checksum;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.BlockingDeque;
import java.util.Vector;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.lang.Byte;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

import nodomain.freeyourgadget.gadgetbridge.devices.garmin.VivoFit3Constants;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCallback;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.*;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.operations.VivoFit3Operation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.operations.VivoFit3DeviceInfoOperation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.io.*;

public class VivoFit3Support extends AbstractBTLEDeviceSupport {
	private static final Logger LOG = LoggerFactory.getLogger(VivoFit3Support.class);

	public VivoFit3Support() {
		super(LOG);
		addSupportedService(VivoFit3Constants.UUID_SERVICE);
	}

	@Override
	public boolean connectFirstTime() {
		TransactionBuilder builder = createTransactionBuilder("Setting up device");
		setState(builder, GBDevice.State.INITIALIZING);
		setState(builder, GBDevice.State.INITIALIZED);
		try {
			performConnected(builder.getTransaction());
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private BluetoothGattCharacteristic readCharacteristic;
	@Override
	protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
		setState(builder, GBDevice.State.INITIALIZING);
		readCharacteristic = getCharacteristic(VivoFit3Constants.UUID_CHARACTERISTIC_READER);

		cycleInputStream(builder);

		return builder;
	}

	private static class CRC16 implements Checksum {
		private static short polynomial = (short) 0x8005;
		private short acc = 0xABCD;

		public void reset() {
			acc = 0;
		}

		public void update(int b) {
			// b is actually a byte
			// TODO
		}
		public void update(byte[] b, int off, int len) {
			for (int i = off; i < off + len; i++) {
				update(b[i]);
			}
		}
		public long getValue() {
			return acc;
		}
	}

	private static class Uploader extends OutputStream {
		VivoFit3Support support;
		Uploader(VivoFit3Support support) {
			super();
			this.support = support;
		}
		public void write(int b) {
			// TODO: accumulate & send bytes in 20 byte increments
		}
	}
	public OutputStream getUploadStream() {
		return new LengthPrefixer(new CRCAdder(new COBSEncoder(new Uploader(this)), new CRC16()));
	}

	private class GattDownloadStream extends InputStream implements GattCallback {
		private BlockingDeque<Byte> queue = new LinkedBlockingDeque<>();
		private boolean closed = false;
		public int read() throws IOException {
			if (closed) {
				return -1;
			}
			try {
				return queue.takeFirst() & 0xFF;
			} catch (InterruptedException e) {
				throw new IOException(e);
			}
		}
		@Override
		public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			byte[] value = characteristic.getValue();
			for (int i = 0; i < value.length; i++) {
				if (!queue.offerLast(value[i])) {
					return false;
				};
			}
			if (value[value.length - 1] == 0x00) {
				cycleInputStream();
				// end of the line
			}
			return true;
    }

		// taken from AbstractGattCallback
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt) {
		}

		@Override
		public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			return false;
		}

		@Override
		public boolean onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			return false;
		}

		@Override
		public boolean onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			return false;
		}

		@Override
		public boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			return false;
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
		}

		@Override
		public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
		}
	}

	private static class Dispatcher implements Runnable {
		final private VivoFit3Support support;
		final InputStream in;
		public Dispatcher(VivoFit3Support support, InputStream in) {
			this.support = support;
			this.in = in;
			LOG.debug("Dispatcher(" + String.valueOf(in));
		}
		public void run() {
			try(ByteBufferObjectInputStream in = new ByteBufferObjectInputStream(this.in)) {
				in.buffer.order(ByteOrder.LITTLE_ENDIAN);
				LOG.debug("starting Dispatcher.run()");
				short len = in.readShort();
				LOG.debug("found len:  " + String.valueOf(len));
				short type = in.readShort();
				LOG.debug("found type: 0x" + Integer.toHexString(type));
				assert type == 0x13a0;
				VivoFit3Operation op = new VivoFit3DeviceInfoOperation(support);
				op.readExternal(in);
				op.perform();
			} catch (IOException e) {
				LOG.debug("IOException 226");
				/* pass */
			}
		}
	}

	public void cycleInputStream(TransactionBuilder builder) {
		final GattDownloadStream in = new GattDownloadStream();
		builder.setGattCallback(in);
		builder.notify(readCharacteristic, true);
		Dispatcher op;
		op = new Dispatcher(this, new CRCChecker(new COBSDecoder(in), new CRC16()));
		new Thread(op).start();
	}
	public void cycleInputStream() {
		TransactionBuilder builder = createTransactionBuilder("RX Stream");
		cycleInputStream(builder);
		try {
			performImmediately(builder);
		} catch (IOException e) {
			LOG.error("IOException 252");
		}
	}

	private TransactionBuilder setState(TransactionBuilder builder, GBDevice.State state) {
		return builder.add(new SetDeviceStateAction(getDevice(), state, getContext()));
	}

	private void setInitialized(TransactionBuilder builder) {
		setState(builder, GBDevice.State.INITIALIZED);
	}

	@Override
	public boolean useAutoConnect() {
		return true; // false ?
	}

	@Override
	public void onAppConfiguration(UUID uuid, String config, Integer id) {
		LOG.debug(config);
		// TODO: do this
	}

	public void onInstallApp(Uri uri) {
		// TODO: do this
		LOG.debug(uri.toString());
	}

	@Override
	public void onSendWeather(WeatherSpec weatherSpec) { /* nothing */ }

	@Override
	public void onSendConfiguration(String config) { /* nothing */ }
	@Override
	public void onReadConfiguration(String config) { /* nothing */ }


	@Override
	public void onReset(int flags) { /* nothing */ }




	@Override
	public void onNotification(NotificationSpec notificationSpec) { /* nothing */ }
	@Override
	public void onDeleteNotification(int id) { /* nothing */ }
	@Override
	public void onSetTime() {
		LOG.debug("__MARCO__ onSetTime");
		/* nothing */ 
	}
	@Override
	public void onSetCallState(CallSpec callSpec) { /* nothing */ }
	@Override
	public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) { /* nothing */ }
	@Override
	public void onSetMusicState(MusicStateSpec stateSpec) { /* nothing */ }
	@Override
	public void onSetMusicInfo(MusicSpec musicSpec) { /* nothing */ }
	@Override
	public void onEnableRealtimeSteps(boolean enable) { /* nothing */ }
	@Override
	public void onAppInfoReq() { /* nothing */ }
	@Override
	public void onAppStart(UUID uuid, boolean start) { /* nothing */ }
	@Override
	public void onAppDelete(UUID uuid) { /* nothing */ }
	@Override
	public void onAppReorder(UUID[] uuids) { /* nothing */ }
	@Override
	public void onFetchRecordedData(int dataTypes) { /* nothing */ }
	@Override
	public void onEnableRealtimeHeartRateMeasurement(boolean enable) { /* nothing */ }
	@Override
	public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) { /* nothing */ }
	@Override
	public void onDeleteCalendarEvent(byte type, long id) { /* nothing */ }
	@Override
	public void onFindDevice(boolean start) { /* nothing */ }
	@Override
	public void onScreenshotReq() { /* nothing */ }
	@Override
	public void onEnableHeartRateSleepSupport(boolean enable) { /* nothing */ }
	@Override
	public void onTestNewFunction() { /* nothing */ }
	@Override
	public void onSetConstantVibration(int intensity) { /* nothing */ }
	@Override
	public void onSetHeartRateMeasurementInterval(int seconds) { /* nothing */ }
	@Override
	public void onHeartRateTest() { /* nothing */ }
	@Override
	public void onSetAlarms(ArrayList<? extends Alarm> alarms) { /* nothing */ }
}
