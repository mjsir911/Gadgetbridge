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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.operations;

import android.os.Build;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Externalizable;
import java.util.List;
import java.util.ArrayList;

import java.util.TimeZone;
import java.util.Date;
import java.time.ZoneId;
import java.time.zone.ZoneRules;
import java.util.Calendar;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Externalizable;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.VivoFit3Support;

public class VivoFit3TimeSetOperation extends VivoFit3Operation {
	private static final Logger LOG = LoggerFactory.getLogger(VivoFit3TimeSetOperation.class);
	{
		messageType = 0x13a2;
	}

	private final List<SubTimeSetMessage> settings;
	static class SubTimeSetMessage implements Externalizable {
		enum Identifier implements Externalizable {
			TIME(       (byte) 0x01),
			DST_OFFSET( (byte) 0x02),
			TZ_OFFSET(  (byte) 0x03),
			DST_START(  (byte) 0x04),
			DST_END(    (byte) 0x05);

			private byte identifier;
			private Identifier(byte identifier) {
				this.identifier = identifier;
			}

			public void writeExternal(ObjectOutput aOutput) throws IOException {
				aOutput.writeByte(identifier);
			}
			public void readExternal(ObjectInput aInput) {
				/* nothing */
			}
		}
		Identifier identifier;
		int data;

		SubTimeSetMessage(Identifier identifier, int data) {
			this.identifier = identifier;
			this.data = data;
		}

		public void writeExternal(ObjectOutput aOutputStream) throws IOException {
			aOutputStream.writeObject(identifier);
			aOutputStream.writeByte(4);
			aOutputStream.writeInt(data);
		}
		public void readExternal(ObjectInput aInput) {
			/* nothing */
		}
	}

	public VivoFit3TimeSetOperation(VivoFit3Support support) {
		super(support);
		final int base = 631065600; // jan 1 1990
		long now_ms = System.currentTimeMillis(); // will break in the year 2126
		TimeZone timeZone = TimeZone.getDefault();
		int dstOffset = timeZone.inDaylightTime(new   Date(now_ms)) ? timeZone.getDSTSavings() / 1000 : 0;
		int timeZoneOffset = timeZone.getOffset(now_ms) / 1000;
		settings = new ArrayList<>();
		settings.add(new SubTimeSetMessage(SubTimeSetMessage.Identifier.TIME, ((int) now_ms / 1000) - base));
		settings.add(new SubTimeSetMessage(SubTimeSetMessage.Identifier.DST_OFFSET, dstOffset));
		settings.add(new SubTimeSetMessage(SubTimeSetMessage.Identifier.TZ_OFFSET, timeZoneOffset));
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(settings.size());
		for (SubTimeSetMessage setting: settings) {
			out.writeObject(setting);
		}
	}

	public void readExternal(ObjectInput in) throws IOException {
		/* doesn't do much */
	}
}
