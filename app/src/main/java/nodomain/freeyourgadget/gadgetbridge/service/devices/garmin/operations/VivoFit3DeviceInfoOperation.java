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

public class VivoFit3DeviceInfoOperation extends VivoFit3Operation {
	private static final Logger LOG = LoggerFactory.getLogger(VivoFit3DeviceInfoOperation.class);
	{
		messageType = 0x13a0;
	}

	private int proto_version;
	private int product_num;
	private int unit_id;
	private int software_version;
	private int max_size;
	private String name;
	private String manufacturer;
	private String model;

	public VivoFit3DeviceInfoOperation(VivoFit3Support support) {
		super(support);
	}
	public VivoFit3DeviceInfoOperation(VivoFit3Support support, int proto_version, int product_num, int unit_id, int software_version, int max_size, String name, String manufacturer, String model) {
		this(support);
		LOG.debug("___MARCO___ DeviceInfoOperation" + String.valueOf(getMessageType()));
		this.proto_version = proto_version;
		this.product_num = product_num;
		this.unit_id = unit_id;
		this.software_version = software_version;
		this.max_size = max_size;
		this.name = name;
		this.manufacturer = manufacturer;
		this.model = model;
	}

	public static VivoFit3DeviceInfoOperation getDefault(VivoFit3Support support) {
		String name = BluetoothAdapter.getDefaultAdapter().getName();
		return new VivoFit3DeviceInfoOperation(support, 112, 0xFFFF, 0xFFFFFFFF, 5235, 0xFFFF, name, Build.MANUFACTURER, Build.MODEL);
	}

	@Override
	protected void doRecieve() throws IOException {
		LOG.debug("doing receive perform!");
		GBDevice dev = getDevice();
		dev.setName(name);
		dev.setFirmwareVersion(String.valueOf(software_version));
		dev.setFirmwareVersion2("N/A");
		dev.setModel(!model.isEmpty() ? model : String.valueOf(product_num));
		TransactionBuilder builder = getSupport().createTransactionBuilder("DeviceInfoInitialize");
		builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
		getSupport().performImmediately(builder);

		new VivoFit3AckOperation(getSupport(), this, getDefault(getSupport())).perform();
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeShort(proto_version);
		out.writeShort(product_num);
		out.writeInt(unit_id);
		out.writeShort(software_version);
		out.writeShort(max_size);
		out.writeUTF(name);
		out.writeUTF(manufacturer);
		out.writeUTF(model);
	}

	public void readExternal(ObjectInput in) throws IOException {
		super.readExternal(in);
		proto_version = in.readUnsignedShort();
		product_num = in.readUnsignedShort();
		unit_id = in.readInt();
		software_version = in.readUnsignedShort();
		max_size = in.readUnsignedShort();
		name = in.readUTF();
		manufacturer = in.readUTF();
		model = in.readUTF();

		LOG.debug("proto_version: " + String.valueOf(proto_version));
		LOG.debug("product_num: " + String.valueOf(product_num));
		LOG.debug("unit_id: " + String.valueOf(unit_id));
		LOG.debug("software_version: " + String.valueOf(software_version));
		LOG.debug("max_size: " + String.valueOf(max_size));
		LOG.debug("name" + name);
		LOG.debug("manufacturer" + manufacturer);
		LOG.debug("model" + model);
	}
}
