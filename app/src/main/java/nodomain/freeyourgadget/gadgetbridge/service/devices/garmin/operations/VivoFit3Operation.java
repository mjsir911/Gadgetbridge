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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.VivoFit3Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.ByteBufferObjectOutputStream;

abstract public class VivoFit3Operation extends AbstractBTLEOperation<VivoFit3Support> implements Externalizable {
	private static final Logger LOG = LoggerFactory.getLogger(VivoFit3Operation.class);
	protected short messageType;
	public VivoFit3Operation(VivoFit3Support support) {
		super(support);
	}
	public short getMessageType() {
		return messageType;
	}
	protected void doPerform() throws IOException {
		LOG.debug("VivoFit3Operation.doPerform()");
		TransactionBuilder builder = createTransactionBuilder("TX");
		try {
			perform(builder);
			builder.queue(getQueue());
		} finally {
			operationFinished();
		}
	}

	public void perform(TransactionBuilder builder) throws IOException {
		LOG.debug("Sending: " + String.valueOf(this));
		try (ObjectOutput out = getSupport().getUploadStream(builder)) {
			writeHeader(out);
			LOG.debug("sending the rest");
			writeExternal(out);
		}
	}
	public void respond(TransactionBuilder builder) throws IOException {
		new VivoFit3AckOperation(getSupport(), this).perform(builder);
	}

	public void writeHeader(ObjectOutput out) throws IOException {
		LOG.debug("sending header");
		out.writeShort(getMessageType());
	}

	static public VivoFit3Operation dispatch(VivoFit3Support support, ObjectInput in) throws IOException {
		short len = in.readShort();
		LOG.debug("found len:  " + String.valueOf(len));
		short type = in.readShort();
		LOG.debug("found type: 0x" + Integer.toHexString(type));
		switch (type) {
			case 0x13a0: return new VivoFit3DeviceInfoOperation(support);
			case 0x13a3: return new VivoFit3WhatOneOperation(support, (short) 0x13a3, new byte[] {0x00});
			case 0x1393: return new VivoFit3WhatOneOperation(support, (short) 0x1393, new byte[] {0x00});
			case 0x1394: return new VivoFit3WhatOneOperation(support, (short) 0x1394, new byte[] {0x00});
		}
		return null;
	}
}
