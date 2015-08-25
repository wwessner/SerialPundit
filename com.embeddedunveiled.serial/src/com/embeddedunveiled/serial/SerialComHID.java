/*
 * Author : Rishi Gupta
 * 
 * This file is part of 'serial communication manager' library.
 *
 * The 'serial communication manager' is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * The 'serial communication manager' is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with serial communication manager. If not, see <http://www.gnu.org/licenses/>.
 */

package com.embeddedunveiled.serial;

import com.embeddedunveiled.serial.internal.SerialComHIDJNIBridge;

/**
 * <p></p>
 * 
 */
public class SerialComHID {

	private SerialComHIDJNIBridge mHIDJNIBridge;

	/**
	 * <p>Allocates a new SerialComHID object.</p>
	 * 
	 * @param mHIDJNIBridge
	 */
	public SerialComHID(SerialComHIDJNIBridge mHIDJNIBridge) {
		this.mHIDJNIBridge = mHIDJNIBridge;
	}

	/**
	 * <p>Returns an array of SerialComHIDdevice objects containing information about HID devices 
	 * as found by this library. The HID devices found may be USB HID or Bluetooth HID device. 
	 * Application can call various  methods on returned SerialComHIDdevice object to get specific 
	 * information like vendor id and product id etc.</p>
	 * 
	 * <p>TODO</p>
	 * 
	 * @return list of the HID devices with information about them or empty array if no device matching given criteria found.
	 * @throws SerialComException if an I/O error occurs.
	 * @throws IllegalArgumentException if vendorFilter is negative or invalid number.
	 */
	public SerialComHIDdevice[] listHIDdevicesWithInfo() throws SerialComException {
		int i = 0;
		int numOfDevices = 0;
		SerialComHIDdevice[] hidDevicesFound = null;

		String[] hidDevicesInfo = mHIDJNIBridge.listHIDdevicesWithInfo();

		if(hidDevicesInfo != null) {
			numOfDevices = hidDevicesInfo.length / 7;
			hidDevicesFound = new SerialComHIDdevice[numOfDevices];
			for(int x=0; x < numOfDevices; x++) {
				hidDevicesFound[x] = new SerialComHIDdevice(hidDevicesInfo[i], hidDevicesInfo[i+1], hidDevicesInfo[i+2], 
						hidDevicesInfo[i+3], hidDevicesInfo[i+4], hidDevicesInfo[i+5], hidDevicesInfo[i+6]);
				i = i + 7;
			}
			return hidDevicesFound;
		}else {
			return new SerialComHIDdevice[] { };
		}	
	}

	/**
	 * Converts report read from human interface device to hexadecimal string. This may be 
	 * useful when report is to be passed to next level as hex data or report is to be 
	 * feed into external HID report parser tool.
	 * 
	 * @param report report to be converted into hex string.
	 * @return constructed hex string if report.length > 0 otherwise empty string.
	 * @throws IllegalArgumentException if report is null.
	 */
	public String formatReportToHex(byte[] report) throws SerialComException {
		return SerialComUtil.byteArrayToHexString(report, " ");
	}

	/**
	 * <p>Opens a HID device for communication using its path name.</p>
	 * 
	 * @param pathName device node full path for Unix-like OS and port name for Windows.
	 * @return handle of the opened HID device.
	 * @throws SerialComException if an IO error occurs.
	 * @throws IllegalArgumentException if pathName is null or empty string.
	 */
	public long openHidDevice(final String pathName) throws SerialComException {
		if(pathName == null) {
			throw new IllegalArgumentException("Argument pathName can not be null !");
		}
		String pathNameVal = pathName.trim();
		if(pathNameVal.length() == 0) {
			throw new IllegalArgumentException("Argument pathName can not be empty string !");
		}

		long handle = mHIDJNIBridge.openHidDevice(pathNameVal);
		if(handle < 0) {
			/* JNI should have already thrown exception, this is an extra check to increase reliability of program. */
			throw new SerialComException("Could not open the HID device " + pathNameVal + ". Please retry !");
		}
		return handle;
	}

	/**
	 * <p>Closes a HID device.</p>
	 * 
	 * @param handle handle of the device to be closed.
	 * @return true if device closed successfully.
	 * @throws SerialComException if fails to close the device or an IO error occurs.
	 */
	public boolean closeHidDevice(long handle) throws SerialComException {
		int ret = mHIDJNIBridge.closeHidDevice(handle);
		if(ret < 0) {
			throw new SerialComException("Could not close the given HID device. Please retry !");
		}
		return true;
	}

	/**
	 * <p>Gives the size of the report descriptor used by HID device represented by given handle.</p>
	 * 
	 * @param handle handle of the HID device whose report descriptor size is to be determined.
	 * @return size of report descriptor in bytes.
	 * @throws SerialComException if an I/O error occurs.
	 */
	public int getReportDescriptorSize(final long handle) throws SerialComException {
		int reportDescriptorSize = mHIDJNIBridge.getReportDescriptorSize(handle);
		if(reportDescriptorSize < 0) {
			throw new SerialComException("Could not determine the report descriptor size. Please retry !");
		}
		return reportDescriptorSize;
	}

	/**
	 * <p>Write the given output report to HID device. For devices which support only single report, report ID 
	 * value must be 0x00. Report ID items are used to indicate which data fields are represented in each 
	 * report structure. A Report ID item tag assigns a 1-byte identification prefix to each report transfer. 
	 * If no Report ID item tags are present in the Report descriptor, it can be assumed that only one Input, 
	 * Output, and Feature report structure exists and together they represent all of the device’s data.</p>
	 * 
	 * <p>Only Input reports are sent via the Interrupt In pipe. Feature and Output reports must be initiated 
	 * by the host via the Control pipe or an optional Interrupt Out pipe.</p>
	 * 
	 * @param handle handle of the HID device to which this report will be sent.
	 * @param reportId unique identifier for the report type.
	 * @param report report to be written to HID device.
	 * @return true if report is written to device successfully.
	 * @throws SerialComException if an I/O error occurs.
	 * @throws IllegalArgumentException if report is null or empty array. 
	 */
	public boolean writeOutputReport(long handle, byte reportId, final byte[] report) throws SerialComException {
		if(report == null) {
			throw new IllegalArgumentException("Argumenet report can not be null !");
		}
		if(report.length == 0) {
			throw new IllegalArgumentException("Argumenet report can not be of zero length !");
		}

		int ret = mHIDJNIBridge.writeOutputReport(handle, reportId, report);
		if(ret < 0) {
			throw new SerialComException("Could not write output report to the HID device. Please retry !");
		}
		return true;
	}

	/** 
	 */
	public int readInputReport(long handle, byte[] dataBuffer) throws SerialComException {
		if(dataBuffer == null) {
			throw new IllegalArgumentException("Argumenet dataBuffer can not be null !");
		}
		int ret = mHIDJNIBridge.readInputReport(handle, dataBuffer);
		if(ret < 0) {
			throw new SerialComException("Could not read input report from HID device. Please retry !");
		}
		return ret;
	}

	/** 
	 */
	public int readInputReportWithTimeout(long handle, byte[] dataBuffer, int timeoutValue) throws SerialComException {
		if(dataBuffer == null) {
			throw new IllegalArgumentException("Argumenet dataBuffer can not be null !");
		}
		int ret = mHIDJNIBridge.readInputReportWithTimeout(handle, dataBuffer, timeoutValue);
		if(ret < 0) {
			throw new SerialComException("Could not read input report from HID device. Please retry !");
		}
		return ret;
	}

	/**
	 */
	public boolean sendFeatureReport(long handle, byte reportId, final byte[] data) throws SerialComException {
		if(data == null) {
			throw new IllegalArgumentException("Argumenet data can not be null !");
		}
		if(data.length == 0) {
			throw new IllegalArgumentException("Argumenet data can not be of zero length !");
		}

		int ret = mHIDJNIBridge.sendFeatureReport(handle, reportId, data);
		if(ret < 0) {
			throw new SerialComException("Could not send feature report to HID device. Please retry !");
		}
		return true;
	}

	/** 
	 */
	public int getFeatureReport(long handle, byte[] dataBuffer) throws SerialComException {
		if(dataBuffer == null) {
			throw new IllegalArgumentException("Argumenet dataBuffer can not be null !");
		}
		int ret = mHIDJNIBridge.getFeatureReport(handle, dataBuffer);
		if(ret < 0) {
			throw new SerialComException("Could not get feature report from HID device. Please retry !");
		}
		return ret;
	}

	/**
	 */
	public String getManufacturerString(long handle) throws SerialComException {
		String ret = mHIDJNIBridge.getManufacturerString(handle);
		if(ret == null) {
			throw new SerialComException("Could not get the manufacturer string from the HID device. Please retry !");
		}
		return ret;
	}

	/**
	 */
	public String getIndexedString(int index) throws SerialComException {
		String ret = mHIDJNIBridge.getIndexedString(index);
		if(ret == null) {
			throw new SerialComException("Could not get the string at given index from the HID device. Please retry !");
		}
		return ret;
	}

	/**
	 */
	public String getProductString(long handle) throws SerialComException {
		String ret = mHIDJNIBridge.getProductString(handle);
		if(ret == null) {
			throw new SerialComException("Could not get the product string from the HID device. Please retry !");
		}
		return ret;
	}

	/**
	 */
	public String getSerialNumberString(long handle) throws SerialComException {
		String ret = mHIDJNIBridge.getSerialNumberString(handle);
		if(ret == null) {
			throw new SerialComException("Could not get the serial number string from the HID device. Please retry !");
		}
		return ret;
	}

}
