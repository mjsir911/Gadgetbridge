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

import android.os.Build;
import android.bluetooth.BluetoothGatt;
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

public class VivoFit3AckOperation extends VivoFit3Operation {
	private static final Logger LOG = LoggerFactory.getLogger(VivoFit3AckOperation.class);
	{
		messageType = 0x1388;
	}

	private int status = 0;
	private VivoFit3Operation reply;
	private VivoFit3Operation replyTo;

	public VivoFit3AckOperation(VivoFit3Support support, VivoFit3Operation replyTo, VivoFit3Operation reply) {
		super(support);
		this.replyTo = replyTo;
		this.reply = reply;
	}
	public VivoFit3AckOperation(VivoFit3Support support, VivoFit3Operation replyTo) {
		this(support, replyTo, null);
	}

	protected void doRecieve() throws IOException {
		/* nothing */
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		replyTo.writeHeader(out);
		out.writeByte(status);
		if (reply != null) {
			out.writeObject(reply);
			out.writeByte(1);
		}
	}

	public void readExternal(ObjectInput in) throws IOException {
		super.readExternal(in);
		status = in.readByte();
		try {
			reply = (VivoFit3Operation) in.readObject();
		} catch (ClassNotFoundException e) {
			reply = null;
		}
	}
}
