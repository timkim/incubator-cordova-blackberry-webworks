/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cordova.compass;

import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;
import org.apache.cordova.json4j.JSONArray;
import org.apache.cordova.json4j.JSONException;
import org.apache.cordova.json4j.JSONObject;
import org.apache.cordova.util.Logger;

import net.rim.device.api.system.MagnetometerData;
import net.rim.device.api.system.MagnetometerListener;
import net.rim.device.api.system.MagnetometerSensor;
import net.rim.device.api.system.MagnetometerSensor.Channel;
import net.rim.device.api.system.Application;

public class Compass extends Plugin implements MagnetometerListener{
    public static final String ACTION_GET_HEADING = "getHeading";
    public static final String ACTION_SET_TIMEOUT = "setTimeout";
    public static final String ACTION_GET_TIMEOUT = "getTimeout";
    public static final String ACTION_STOP = "stop";
    
    public static int STOPPED = 0;
    public static int STARTED = 1;
    
    private Channel magChannel;
    float currentHeading = 0;
    int status;
    public float timeout = 30000;
    long lastAccessTime;
    
    public PluginResult execute(String action, JSONArray args, String calbackId){
    
		PluginResult result = null;
		if (!MagnetometerSensor.isSupported()) {
			result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION, "Magnetometer sensor not supported");
		}
		else if (ACTION_GET_HEADING.equals(action)) {
			JSONObject heading = new JSONObject();
			try {
                this.registerMagChannel();
                
				heading.put("magneticHeading", currentHeading);
				heading.put("trueHeading", currentHeading);
				heading.put("headingAccuracy", 0);
				heading.put("timestamp", 0);
			} catch (JSONException e) {
				return new PluginResult(PluginResult.Status.JSON_EXCEPTION, "JSONException:" + e.getMessage());
			}
			result = new PluginResult(PluginResult.Status.OK, heading);
		}
		else if (ACTION_GET_TIMEOUT.equals(action)) {
			float f = this.getTimeout();
			return new PluginResult(PluginResult.Status.OK, Float.toString(f));
		}
		else if (ACTION_SET_TIMEOUT.equals(action)) {
			try {
				float t = Float.parseFloat(args.getString(0));
				this.setTimeout(t);
				return new PluginResult(PluginResult.Status.OK, "");
			} catch (NumberFormatException e) {
				return new PluginResult(PluginResult.Status.ERROR, e.getMessage());
			} catch (JSONException e) {
				return new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage());
			}
		}
		else if (ACTION_STOP.equals(action)) {
			this.unRegisterMagChannel();
			return new PluginResult(PluginResult.Status.OK, "");
		}
		else {
			result = new PluginResult(PluginResult.Status.INVALID_ACTION, "Magnetometer: Invalid action:" + action);
		}

		return result;    
    }
    

	/**
	 * Identifies if action to be executed returns a value and should be run synchronously.
	 *
	 * @param action	The action to execute
	 * @return			T=returns value
	 */

	public boolean isSynch(String action) {
		return true;
	}

    /**
     * Get status of magnetometer sensor.
     *
     * @return			status
     */
   
	public int getStatus() {
		return this.status;
	}

	/**
	 * Set the status and send it to JavaScript.
	 * @param status
	 */

	private void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Set the timeout to turn off magnetometer sensor.
	 *
	 * @param timeout		Timeout in msec.
	 */

	public void setTimeout(float timeout) {
		this.timeout = timeout;
	}

	/**
	 * Get the timeout to turn off magnetometer sensor.
	 *
	 * @return timeout in msec
	 */
   
	public float getTimeout() {
		return this.timeout;
	}

	/**
	 * Opens a raw data channel to the magnetometer sensor.
	 * @return the MagnetometerSensor.Channel for the application
	 */
	private void registerMagChannel() {
        // open channel
        magChannel = MagnetometerSensor.openChannel( Application.getApplication() );
        magChannel.addMagnetometerListener( this );
	}

	/**
	 * Implements the MagnetometerListener method.  We listen for the purpose
	 * of closing the application's Magnetometer sensor channel after timeout
	 * has been exceeded.
	 */
    
	public void onData(MagnetometerData magData) {
        // get the new orientation
        float direction = magData.getHeading(0);
        long timestamp = magData.getTimestamp();
        
        currentHeading = direction;

        // If values haven't been read for length of timeout,
        // turn off magnetometer sensor to save power
		if ((timestamp - this.lastAccessTime) > this.timeout) {
			magChannel.close();
		}
	}
    
    public void unRegisterMagChannel(){
        magChannel.close();
    }
    /**
     * Called when Plugin is destroyed.
     */
     
    public void onDestroy() {
        magChannel.close();
    }
}

