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
import java.util.Map;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Externalizable;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.VivoFit3Support;

public class VivoFit3SetSettingsOperation extends VivoFit3Operation {
	private static final Logger LOG = LoggerFactory.getLogger(VivoFit3SetSettingsOperation.class);
	{
		messageType = 0x13a2;
	}

	private final Map<Setting, Object> settings;

	public enum Setting implements Externalizable {
		NAME(       (byte) 0x00),
		TIME(       (byte) 0x01),
		DST_OFFSET( (byte) 0x02),
		TZ_OFFSET(  (byte) 0x03),
		DST_START(  (byte) 0x04),
		DST_END(    (byte) 0x05);

		private byte constant;
		private Setting(byte constant) {
			this.constant = constant;
		}

		public void writeExternal(ObjectOutput aOutput) throws IOException {
			aOutput.writeByte(constant);
		}
		public void readExternal(ObjectInput aInput) {
			/* nothing */
		}
	}

	public VivoFit3SetSettingsOperation(VivoFit3Support support, Map<Setting, Object> settings) {
		super(support);
		this.settings = settings;
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeByte(settings.size());
		for (Map.Entry<Setting, Object> settingPair : settings.entrySet()) {
			out.writeObject(settingPair.getKey());
			final Object value = settingPair.getValue();
			if (value instanceof String) {
				out.writeUTF((String) value);
			} else if (value instanceof Integer) {
				out.writeByte(4);
				out.writeInt((Integer) value);
			} else if (value instanceof Boolean) {
				out.writeByte(1);
				out.writeByte(Boolean.TRUE.equals(value) ? 1 : 0);
			} else {
				throw new IllegalArgumentException("Unsupported setting value type " + value);
			}

		}
	}

	public void readExternal(ObjectInput in) throws IOException {
		/* doesn't do much */
	}
}
