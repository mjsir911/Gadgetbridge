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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.SequenceInputStream;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.*;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.operations.VivoFit3Operation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.operations.VivoFit3DeviceInfoOperation;

public class VivoFit3Support extends AbstractBTLEDeviceSupport {
	private static final Logger LOG = LoggerFactory.getLogger(VivoFit3Support.class);

	public VivoFit3Support() {
		super(LOG);
		dispatch = new DispatchStream(this);
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

	@Override
	protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
		LOG.debug("initializeDevice");
		setState(builder, GBDevice.State.INITIALIZING);

		BluetoothGattCharacteristic readCharacteristic = getCharacteristic(VivoFit3Constants.UUID_CHARACTERISTIC_READER);

		builder.setGattCallback(this);
		builder.notify(readCharacteristic, true);

		// expect a device info packet (ack with info)
		// new ExpectOperation<DeviceInfoOperation>(this, builder).perform();
		// new setState(builder, GBDevice.State.INITIALIZED);
		// expect a 0x13a3 packet (can ignore?)
		// expect a 0x1393 packet (fit definition packet)
		// expect a 0x1394 packet (fit data packet)
		// send a 0x1393
		// send a 0x1394
		// ask for supported file types with 0x13a7
		// new TimeSetOperation(this, builder).perform();
		// and also send a 0x13a6 (system event message, 0x03)
		//
		// builder.add(new WaitAction(2000));
		// setState(builder, GBDevice.State.WAITING_FOR_RECONNECT);
		return builder;
	}

	private static class COBSDecoder extends FilterOutputStream {
		public COBSDecoder(OutputStream out) {
			super(out);
		}

		private byte counter = (byte) 0x77;
		private boolean skipNext;
		public void write(int b) throws IOException {
			if (counter < 0) {
				throw new IOException("asd");
			}

			counter--;

			if (b == 0) {
				LOG.debug("starting crc decode : " + String.valueOf(counter));
				/* start or end or err */
				if (counter != 0) {
					/* start */
					counter = 1;
					skipNext = true;
				} else {
					/* end or error */
					LOG.debug("end");
					counter = -1;
				}
				return;
			}

			// LOG.debug("writing byte : " + Integer.toHexString(b) + " : " + Integer.toHexString(counter));
			if (counter == 0) {
				/* we're at a 0 or a skip */
				if (!skipNext) {
					super.write(0);
				}
				counter = (byte) b; // why write() takes an int I'll never know
				skipNext = counter == 0xFF;
				return;
			}
			super.write(b);
		}
	}
	private static class COBSEncoder extends BufferedInputStream {
		private static int maxSize = 255; // Byte.MAX_VALUE & (byte) 0xFF;
			public COBSEncoder(InputStream in) {
				super(wrapInput(in), maxSize);
			}

			private static InputStream wrapInput(InputStream in) {
				Vector<InputStream> streams = new Vector<>(3);
				streams.add(new ByteArrayInputStream(new byte[] {0, 0}));
				streams.add(in);
				streams.add(new ByteArrayInputStream(new byte[] {0}));
				return new SequenceInputStream(streams.elements());
			}

			private int countToNext = 2;
			public int read() throws IOException {
				LOG.debug("COBSEncode.read()");
				countToNext--;
				int next = super.read();
				if (next == -1) {
					return -1;
				}
				if (countToNext == 0) {
					// assert next == 0;
					mark(maxSize);
					countToNext = 1;
					while ((next = super.read()) > 0 && countToNext < maxSize) { countToNext++; } // just keep going
					if (next == -1) {
						countToNext = 0;
					}
					reset();
					return countToNext;
				}
				return next;
			};
	}
	private class DispatchStream extends OutputStream {
		VivoFit3Support support;
		public DispatchStream(VivoFit3Support support) {
			super();
			this.support = support;
		}

		private InputStream in;
		private OutputStream out;

		private OutputStream getStream() {
			if (out != null) {
				assert in != null : in;
				return out;
			}

			PipedInputStream pipe_in = new PipedInputStream();
			PipedOutputStream pipe_out = new PipedOutputStream();
			try {
				pipe_in.connect(pipe_out);
			} catch (IOException e) {
				LOG.error("blargh");
			}
			in = pipe_in;
			out = new COBSDecoder(pipe_out);

			new Thread(new Runnable() {
				public void run() {
					VivoFit3Operation op;
					try {
						int len = in.read() | (in.read() >> 8);
						LOG.debug("MARCO VIVOFIT3 DISPATCH : " + String.valueOf(len));
						in = new BufferedInputStream(in, len);

						ByteBufferObjectInputStream bis = new ByteBufferObjectInputStream(in);
						bis.buffer.order(ByteOrder.LITTLE_ENDIAN);
						op = new VivoFit3DeviceInfoOperation(support);
						op.readExternal(bis);
						op.perform();
					} catch (IOException e) {
						LOG.error("blagh" + e.getMessage());
						return;
					} finally {
						out = null;
						in = null;
					}
				}
			}).start();
			return out;
		}

		public void write(int b) throws IOException {
			getStream().write(b);
		}
	}

	private static byte[] checkCRC(byte[] input) {
		// just strip out the crc without checking
		// for future reference crc algo is crc16-ansii 0x8002
		byte[] ret = new byte[input.length - 2];
		System.arraycopy(input, 0, ret, 0, input.length - 2);
		return ret;
	};

	private OutputStream dispatch;
	@Override
	public boolean onCharacteristicChanged(BluetoothGatt gatt,
	                                       BluetoothGattCharacteristic characteristic) {
		try {
			dispatch.write(characteristic.getValue());
		} catch (IOException e) {
			LOG.error("IOException");
			return false;
		}
		// LOG.debug(Logging.formatBytes(value));
		return true;
	}

	private OutputStream uploadStream;
	{
		final PipedInputStream pipe_in = new PipedInputStream();
		PipedOutputStream pipe_out = new PipedOutputStream();
		try {
			pipe_in.connect(pipe_out);
		} catch (IOException e) {
			LOG.error("blargh2");
		}
		uploadStream = pipe_out;
		new Thread(new Runnable() {
			public void run() {
				DataInputStream in = new DataInputStream(new COBSEncoder(new CRCEncoder(new LengthPrefixer(pipe_in, 2))));
				while (true) {
					LOG.debug("uploadStream loop");
					byte[] data = new byte[20];
					try {
						in.readFully(data);
					} catch (IOException e) {
						LOG.error("IOException 5");
						break;
					} finally {
					//} catch (EOFException e) {
					//	/* pass, eof is fine */
						LOG.error("Sending bytes: " + Logging.formatBytes(data));
					}
				}
			}
		}).start();
	}
	public OutputStream getUploadStream() {
		return uploadStream;
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
