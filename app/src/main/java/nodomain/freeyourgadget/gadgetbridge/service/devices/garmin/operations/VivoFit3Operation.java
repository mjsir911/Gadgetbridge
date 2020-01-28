/*  Copyright (C) 2019-2020 Andreas Böhler

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
	private boolean recieving = false;
	protected void setRecieving() {
		recieving = true;
	}
	public short getMessageType() {
		return messageType;
	}
	protected void doPerform() throws IOException {
		try {
			if (recieving) {
				doRecieve();
			} else {
				doSend();
			}
		} finally {
			operationFinished();
		}
	}
	protected void doSend() throws IOException {
		LOG.debug("Sending: " + String.valueOf(this));
		OutputStream out = getSupport().getUploadStream();
		ByteBufferObjectOutputStream bos = new ByteBufferObjectOutputStream(out);
		bos.buffer.order(ByteOrder.LITTLE_ENDIAN);
		writeHeader(bos);
		writeExternal(bos);
	}
	protected void doRecieve() throws IOException {
		new VivoFit3AckOperation(getSupport(), this).perform();
	}

	public void writeHeader(ObjectOutput out) throws IOException {
		out.writeShort(getMessageType());
	}

	public void readExternal(ObjectInput in) throws IOException {
		setRecieving();
		LOG.debug("__MARCO__ readExternal");
		int type = in.readUnsignedShort();
		assert type == getMessageType() : type;
	}
}
