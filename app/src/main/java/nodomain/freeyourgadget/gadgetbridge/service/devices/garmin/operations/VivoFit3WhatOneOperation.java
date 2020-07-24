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

public class VivoFit3WhatOneOperation extends VivoFit3Operation {
	private static final Logger LOG = LoggerFactory.getLogger(VivoFit3WhatOneOperation.class);
	private final byte[] data;

	public VivoFit3WhatOneOperation(VivoFit3Support support, short mtype, byte[] data) {
		super(support);
		messageType = mtype;
		this.data = data;
	}

	@Override
	public void respond(TransactionBuilder builder) throws IOException {
		LOG.debug("respond()");
		new VivoFit3AckOperation(getSupport(), this, new VivoFit3BytesOperation(getSupport(), messageType, data))
			.perform(builder);
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		/* nothing... for now */
	}

	public void readExternal(ObjectInput in) throws IOException {
		/* nothing... for now */
	}
}
