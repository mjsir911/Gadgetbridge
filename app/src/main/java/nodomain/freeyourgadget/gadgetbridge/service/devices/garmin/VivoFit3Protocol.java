/*  Copyright (C) 2018 Marco Sirabella

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

import  	android.bluetooth.BluetoothAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nodomain.freeyourgadget.gadgetbridge.Logging;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import android.os.Build;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;

import java.util.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneOffsetTransition;
import java.time.ZoneId;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class VivoFit3Protocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(VivoFit3Protocol.class);

		private static final short AcknowledgmentMessageID =        0x1388;
		private static final short DeviceInformationMessageID =     0x13a0;
		private static final short QueuedDownloadRequestMessageID = 0x13a3;
		private static final short SystemEventMessageID =           0x13a6;
		private static enum  SystemEventType {
			SYNC_COMPLETE(0x00),
			SYNC_FAIL(0x01),
			FACTORY_RESET(0x02),
			PAIR_START(0x03),
			PAIR_COMPLETE(0x04),
			PAIR_FAIL(0x05),
			SYNC_READY(0x08),
			DEVICE_DISCONNECT(0x0b),
			TIME_UPDATED(0x10);

			private final byte value;
			SystemEventType(int v) {
				value = (byte) v;
			}
			public byte getValue() {
				return value;
			}
		}
		private static final short FitDefinitionMessageID =         0x1393;
		private static final short FitDataMessageID =               0x1394;

		private final Queue<byte[]> actionQueue = new LinkedBlockingQueue<>();

		public VivoFit3Protocol(GBDevice device) {
			super(device);
		}

		public static byte[] decodeCOBS(byte[] input) {
			byte[] ret = new byte[input.length - 3];
			System.arraycopy(input, 2, ret, 0, input.length - 3);

			int i = input[1] - 1;
			while (i != ret.length) {
				int tmpI = i + ret[i];
				ret[i] = 0x00;
				i = tmpI;
			}
			return ret;
		}

		public byte[] getQueuedBytes() {
			return actionQueue.poll();
		}

		public byte[] ack(byte[] message) {
			ByteBuffer bb = ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN);
			short len = bb.getShort(0);
			if (len != message.length) {
				return null;
			}
			short id = bb.getShort(2);

			byte[] ret;
			switch (id) {
				default:
					LOG.debug("unhandled message: " + Logging.formatBytes(message));
					return null;
				case DeviceInformationMessageID:
					ret = encodeDeviceInfo();
					break;
				case FitDataMessageID:
					actionQueue.offer(encodeFitData());
					// todo: queue up two more
					ret = encodeGenericAck(id);
					break; // so, in the actual snoop file both the enqueued encodefitdata and encodefitdatadefinition happen at the same time. Here, they might happen right after the recieved signal.
				case FitDefinitionMessageID:
					actionQueue.offer(encodeFitDefinition());
					// todo: queue up two more
					ret = encodeGenericAck(id);
					break;
				case QueuedDownloadRequestMessageID:
					ret = encodeGenericAck(id);
					break;
			}
			return ByteBuffer
				.allocate(ret.length + 4)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) AcknowledgmentMessageID)
				.put(ret, 0, 2) // put first two bytes (aka the ID)
				.put((byte) 0x00) // idk
				.put(ret, 2, ret.length - 2)
				.put((byte) (ret.length - 2 == 0 ? 0x00 : 0x01)) // idk
				.array();
		}

		private byte[] encodeFitDefinition() {
			// my height and weight and stuff is probably in here somewhere
			return new byte[] {
				// (byte) 0x75,(byte) 0x00, // length
				(byte) 0x93,(byte) 0x13,
				(byte) 0x40,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,(byte) 0x7f,(byte) 0x0b,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,(byte) 0x01,(byte) 0x01,
				(byte) 0x00,(byte) 0x02,(byte) 0x01,
				(byte) 0x00,(byte) 0x04,(byte) 0x01,
				(byte) 0x00,(byte) 0x05,(byte) 0x01,
				(byte) 0x00,(byte) 0x06,(byte) 0x01,
				(byte) 0x00,(byte) 0x07,(byte) 0x01,
				(byte) 0x00,(byte) 0x08,(byte) 0x01,
				(byte) 0x00,(byte) 0x09,(byte) 0x01,
				(byte) 0x00,(byte) 0x0a,(byte) 0x01,
				(byte) 0x00,(byte) 0x0d,(byte) 0x01,
				(byte) 0x00,(byte) 0x41,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,(byte) 0x14,(byte) 0x04,
				(byte) 0x00,
					(byte) 0x04,(byte) 0x85,(byte) 0x01,
					(byte) 0x04,(byte) 0x85,(byte) 0xfd,
					(byte) 0x04,(byte) 0x86,(byte) 0x0e,
					(byte) 0x04,(byte) 0x86,(byte) 0x42,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,(byte) 0x15,(byte) 0x02,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,(byte) 0x01,(byte) 0x01,
				(byte) 0x00,(byte) 0x43,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,(byte) 0x13,
				(byte) 0x00,(byte) 0x44,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,(byte) 0x22,
				(byte) 0x00,(byte) 0x45,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,
					(byte) 0x81,(byte) 0x01,(byte) 0xfd,
					(byte) 0x04,(byte) 0x86,(byte) 0x46,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,
					(byte) 0x80,(byte) 0x01,(byte) 0xfd,
					(byte) 0x04,(byte) 0x86,(byte) 0x47,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,(byte) 0x12,
				(byte) 0x00,(byte) 0x48,
				(byte) 0x00,(byte) 0x01,
				(byte) 0x00,(byte) 0x0c,
				(byte) 0x00,
				//(byte) 0x1d,(byte) 0x61  // checksum
			};
		}

		private byte[] encodeFitData() {
			// or maybe my height is here?
			// 94:13:00:01:01:ff:00:00:00:01:01:01:01:00
			// 94:13:00:01:01:ff:00:00:00:00:01:01:01:00
			// 94:13:00:01:01:ff:00:00:00:01:01:01:01:00
			// 94:13:00:01:01:ff:00:00:00:00:01:01:01:00
			return new byte[] {
				// (byte) 0x12,(byte) 0x00, // len
				(byte) 0x94,(byte) 0x13,
				(byte) 0x00,(byte) 0x01,(byte) 0x01,(byte) 0xff,
				(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x01,(byte) 0x01,(byte) 0x01,
				(byte) 0x00,
				// (byte) 0x5e,(byte) 0x0b // crc
			};
			//
		}

		private byte[] encodeGenericAck(short messageId) {
			return ByteBuffer
				.allocate(2)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort(messageId)
				.array();
		}


		private byte[] encodeDeviceInfo() {
			String name = BluetoothAdapter.getDefaultAdapter().getName();
			String model = Build.MODEL;
			String manufacturer = Build.MANUFACTURER;

			return ByteBuffer
				.allocate(17 + name.length() + model.length() + manufacturer.length())
				.order(ByteOrder.LITTLE_ENDIAN)

				.putShort(DeviceInformationMessageID)
				.putShort((short) 112) // protocol version
				.putShort((short) -1) // product number
				.putInt((int) 12345) // unit ID
				.putShort((short) 4363) // software version
				.putShort((short) 16384) // max packet size
				.put((byte) name.length())
				.put(name.getBytes())
				.put((byte) manufacturer.length())
				.put(manufacturer.getBytes())
				.put((byte) model.length())
				.put(model.getBytes())
				.array();
		}

		private byte[] encodeSystemEvent(SystemEventType identifier, byte[] data) {
			if (data == null || data.length == 0) {
				data = new byte[] {0x00}; // default
			}
			return ByteBuffer.allocate(data.length + 4)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort(SystemEventMessageID)
				.put(identifier.getValue())
				.put((byte) data.length)
				.put(data)
				.array();
		}

		public byte[] encodeSyncComplete() {
			return encodeSystemEvent(SystemEventType.SYNC_COMPLETE, null);
		};

		public byte[] encodePairStart() {
			return encodeSystemEvent(SystemEventType.PAIR_START, null);
		};

		public byte[] encodePairComplete() {
			return encodeSystemEvent(SystemEventType.PAIR_COMPLETE, null);
		};


		public byte[] encodeSetTime() {
			ByteBuffer timeBB = ByteBuffer
				.allocate(3 + 6 * 5)
				.order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) 0x13a2)
				;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Oreo, or sdk version 26, includes instants and such
				final Instant base = Instant.ofEpochSecond(631065600);

				Instant now = Instant.now();
				ZoneRules timeZone = ZoneId.systemDefault().getRules();

				ZoneOffsetTransition firstTransition = timeZone.nextTransition(now);
				ZoneOffsetTransition secondTransition = timeZone.nextTransition(firstTransition.getInstant());

				ZoneOffsetTransition startTransition;
				ZoneOffsetTransition endTransition;

				if (firstTransition.isGap()) { // todo: handle when dst isnt a thing
					startTransition = firstTransition;
					endTransition = secondTransition;
				} else {
					startTransition = secondTransition;
					endTransition = firstTransition;
				}

				timeBB
					.put((byte) 0x05) // count of settings
					.put((byte) 0x01) // Identifier for time
					.put((byte) 0x04) // len of time (int)
					.putInt((int) ChronoUnit.SECONDS.between(base, now))
					.put((byte) 0x02) // identifier for dst offset
					.put((byte) 0x04) // len of dst offset (int)
					.putInt((int) timeZone.getDaylightSavings(now).getSeconds())
					.put((byte) 0x03) // identifier for timezone offset
					.put((byte) 0x04) // len of timezone offset (int)
					.putInt(timeZone.getOffset(now).getTotalSeconds())
					.put((byte) 0x04) // identifier for next dst start
					.put((byte) 0x04) // len of next data (int)
					.putInt((int) ChronoUnit.SECONDS.between(base, startTransition.getInstant()))
					.put((byte) 0x05) // identifier for next dst end
					.put((byte) 0x04) // len of next data (int)
					.putInt((int) ChronoUnit.SECONDS.between(base, endTransition.getInstant()))
					;
			} else {
				int base = 631065600;
				long now_ms = System.currentTimeMillis(); // will break in the year 2126
				TimeZone timeZone = TimeZone.getDefault();
				int dstOffset = timeZone.inDaylightTime(new   Date(now_ms)) ? timeZone.getDSTSavings() / 1000 : 0;
				int timeZoneOffset = timeZone.getOffset(now_ms) / 1000;
				int nextDstStart = 0x36e769f0; // may 2019
				int nextDstEnd = 0x382120e0; // nov 2019

				timeBB
					.put((byte) 0x05) // count of settings
					.put((byte) 0x01) // Identifier for time
					.put((byte) 0x04) // len of time (int)
					.putInt((int) (now_ms / 1000) - base)
					.put((byte) 0x02) // identifier for dst offset
					.put((byte) 0x04) // len of dst offset (int)
					.putInt(dstOffset)
					.put((byte) 0x03) // identifier for timezone offset
					.put((byte) 0x04) // len of timezone offset (int)
					.putInt(timeZoneOffset)
					.put((byte) 0x04) // identifier for next dst start
					.put((byte) 0x04) // len of next data (int)
					.putInt(nextDstStart)
					.put((byte) 0x05) // identifier for next dst end
					.put((byte) 0x04) // len of next data (int)
					.putInt(nextDstEnd)
					;
			}

			// TODO: double up;
			return timeBB.array();
		}

}
