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
import java.util.Deque;
import java.util.ArrayDeque;
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

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.operations.*;
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
	private BluetoothGattCharacteristic writeCharacteristic;
	@Override
	protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
		setState(builder, GBDevice.State.INITIALIZING);
		readCharacteristic = getCharacteristic(VivoFit3Constants.UUID_CHARACTERISTIC_READER);
		writeCharacteristic = getCharacteristic(VivoFit3Constants.UUID_CHARACTERISTIC_WRITER);
		builder.notify(readCharacteristic, true);
		builder.setGattCallback(this);

		cycleInputStream();

		return builder;
	}

	private static class CRC16 implements Checksum {
		private static short polynomial = (short) 0x8005;
		private short acc = (short) 0xABCD;

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
		final private TransactionBuilder builder;
		final private BluetoothGattCharacteristic write;
		final private Deque<Byte> deque = new ArrayDeque<>(20); // size of btle packet
		Uploader(TransactionBuilder builder, BluetoothGattCharacteristic write) {
			super();
			this.builder = builder;
			this.write = write;
		}
		public void write(int b) throws IOException {
			// LOG.debug("upload writing : 0x" + Integer.toHexString(b & 0xFF));
			deque.add((byte) b);
			if (deque.size() == 20) {
				send();
			}
		}
		// TODO: this should really be flush
		private void send() throws IOException {
			int size = deque.size();
			byte[] b = new byte[size];
			for (int i = 0; i < size; i++) {
				b[i] = deque.remove();
			}
			builder.write(write, b);
		}
		public void close() throws IOException {
			send();
		}
	}
	public OutputStream getUploadStream(TransactionBuilder builder) {
		return new LengthPrefixer(new CRCAdder(new COBSEncoder(new Uploader(builder, this.writeCharacteristic)), new CRC16()));
	}

	private static class Dispatcher implements Runnable {
		final private VivoFit3Support support;
		final InputStream in;
		public Dispatcher(VivoFit3Support support, InputStream in) {
			this.support = support;
			this.in = in;
		}
		public void run() {
			TransactionBuilder builder = support.createTransactionBuilder("Reply");
			try(ByteBufferObjectInputStream in = new ByteBufferObjectInputStream(this.in)) {
				in.buffer.order(ByteOrder.LITTLE_ENDIAN);
				VivoFit3Operation op = VivoFit3Operation.dispatch(support, in);
				if (op == null) {
					LOG.error("dispatch returned none");
					return;
				}
				try {
					op.readExternal(in);
				} catch (ClassNotFoundException e) {
					LOG.error("ClassNotFoundException 187");
				}
				op.respond(builder);
			} catch (IOException e) {
				LOG.debug("IOException 226");
				/* pass */
			}
			builder.queue(support.getQueue());
		}
	}

	private OutputStream queue_stream;
	private void cycleInputStream() {
		LOG.debug("MARCO doing input stream");
		final PipedInputStream in = new PipedInputStream();
		final PipedOutputStream out = new PipedOutputStream();
		try {
			out.connect(in);
		} catch (IOException e) {
			LOG.error("really shouldn't happen");
		}
		queue_stream = out;
		// final GattDownloadStream in = new GattDownloadStream();
		Dispatcher op = new Dispatcher(this, new CRCChecker(new COBSDecoder(in), new CRC16()));
		new Thread(op).start();
	}

	public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		byte[] value = characteristic.getValue();
		LOG.debug("MARCO@@@@@@@@@@ characteristicchanged 1");
		try {
			queue_stream.write(value);
		} catch (IOException e) {
			return false;
		}
		LOG.debug("MARCO@@@@@@@@@@ characteristicchanged 2");
		if (value[value.length - 1] == 0x00) {
			try {
				queue_stream.close();
			} catch (IOException e) {
				// this isn't really a problem
				LOG.error("couldn't close queue stream");
			}
			LOG.debug("END OF STREAM, cycling");
			cycleInputStream();
			// end of the line
		}
		return true;
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
		// LOG.debug(config);
		// TODO: do this
	}

	public void onInstallApp(Uri uri) {
		// TODO: do this
		// LOG.debug(uri.toString());
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
	public void dispose() {
		LOG.info("Dispose");
		try {
			new VivoFit3SystemEventOperation(this, new byte[] {0x01, 0x01, 0x00}).perform();
		} catch (IOException e) {};
		super.dispose();
	}


	@Override
	public void onNotification(NotificationSpec notificationSpec) { /* nothing */ }
	@Override
	public void onDeleteNotification(int id) { /* nothing */ }
	@Override
	public void onSetTime() {
		LOG.debug("@@@@@@@@@@__MARCO__@@@@@@@@ onSetTime");
		try {
			new VivoFit3TimeSetOperation(this).perform();
		} catch (IOException e) {};
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
