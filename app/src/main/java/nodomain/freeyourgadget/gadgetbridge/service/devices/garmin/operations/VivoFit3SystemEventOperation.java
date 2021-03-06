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
import java.util.UUID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Externalizable;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.VivoFit3Support;

public class VivoFit3SystemEventOperation extends VivoFit3Operation {
	{
		messageType = 0x13a6;
	}

	public enum Type implements Externalizable {
		// from mormegil
		SYNC_COMPLETE(             (byte) 0x00),
		SYNC_FAIL(                 (byte) 0x01),
		FACTORY_RESET(             (byte) 0x02),
		PAIR_START(                (byte) 0x03),
		PAIR_FAIL(                 (byte) 0x04),
		HOST_DID_ENTER_FOREGROUND( (byte) 0x05),
		HOST_DID_ENTER_BACKGROUND( (byte) 0x06),
		SYNC_READY(                (byte) 0x07),
		NEW_DOWNLOAD_AVAILABLE(    (byte) 0x08),
		DEVICE_SOFTWARE_UPDATE(    (byte) 0x09),
		DEVICE_DISCONNECT(         (byte) 0x0a),
		TUTORIAL_COMPLETE(         (byte) 0x0b),
		SETUP_WIZARD_START(        (byte) 0x0c),
		SETUP_WIZARD_COMPLETE(     (byte) 0x0d),
		SETUPW_ZIARD_SKIPPED(      (byte) 0x0e),
		TIME_UPDATED(              (byte) 0x0f);


		private byte val;
		private Type(byte value) {
			this.val = val;
		}

		public void writeExternal(ObjectOutput aOutput) throws IOException {
			aOutput.writeByte(val);
		}
		public void readExternal(ObjectInput aInput) {
			/* nothing */
		}
	}

	private Type type;
	private String extraData;

	public VivoFit3SystemEventOperation(VivoFit3Support support, Type type) {
		super(support);
		this.type = type;
		this.extraData = "\0";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(type);
		out.writeByte(extraData.length());
		out.writeBytes(extraData);
	}

	public void readExternal(ObjectInput in) throws IOException {
		/* whatever */
	}
}
