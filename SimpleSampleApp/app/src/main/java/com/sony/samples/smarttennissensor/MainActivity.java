/*
 * Copyright (C) 2016 Sony Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.sony.samples.smarttennissensor;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nifty.cloud.mb.core.DoneCallback;
import com.sony.smarttennissensor.data.RacketModel;
import com.sony.smarttennissensor.data.ShotData;
import com.sony.smarttennissensor.service.Config;
import com.sony.smarttennissensor.service.ILiveModeListener;
import com.sony.smarttennissensor.service.IOnConfigChangeListener;
import com.sony.smarttennissensor.service.IOnSensorChangeListener;
import com.sony.smarttennissensor.service.IOnSensorStateChangeListener;
import com.sony.smarttennissensor.service.IResultListener;
import com.sony.smarttennissensor.service.ISensorConnectListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import com.nifty.cloud.mb.core.NCMBPush;
import com.nifty.cloud.mb.core.NCMB;
import com.nifty.cloud.mb.core.NCMB;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBObject;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private static final int MESSAGE_BIND = 0;
    private static final int MESSAGE_CONNECT = 1;
    private static final int MESSAGE_DISCONNECT = 2;
    private static final int MESSAGE_GET_RACKET_LIST = 3;
    private static final int MESSAGE_GET_CONFIG = 4;
    private static final int MESSAGE_SET_CONFIG = 5;
    private static final int MESSAGE_START_LIVE_MODE = 6;
    private static final int MESSAGE_STOP_LIVE_MODE = 7;
    private static final int MESSAGE_GET_SENSOR_STATE = 8;

    private static final String TAG = "ClientFragment";
    private Handler handler = null;
    private HostDelegate hostDelegate = null;
    protected List<RacketModel> racketList = null;
    private View root = null;

    protected ISensorConnectListener sensorConnectListener = new ISensorConnectListener.Stub() {

        @Override
        public void onConnect(final BluetoothDevice connectedDevice) throws RemoteException {

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "onConnect " + connectedDevice.getName(), Toast.LENGTH_SHORT).show();
                    prependToLog("onConnect: " + connectedDevice.getName());
                }
            };

            MainActivity.this.runOnUiThread(r);
        }

        @Override
        public void onConnectionFailed() throws RemoteException {

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "onConnectionFailed ", Toast.LENGTH_SHORT).show();
                    prependToLog("onConnectionFailed()");
                }
            };
            MainActivity.this.runOnUiThread(r);
        }

    };

    protected IOnSensorChangeListener sensorChangeListener = new IOnSensorChangeListener.Stub() {
        @Override
        public void onSensorChanged(final BluetoothDevice device) throws RemoteException {
            Runnable r = new Runnable() {
                @Override
                public void run() {

                    prependToLog("onSensorChanged()" + device.getName());
                }
            };
            MainActivity.this.runOnUiThread(r);
        }


    };

    protected IOnSensorStateChangeListener sensorStateChangeListener = new IOnSensorStateChangeListener.Stub() {
        @Override
        public void onStateChange(final int state) throws RemoteException {

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    String stateName = "";
                    switch (state) {
                        case HostDelegate.SENSOR_STATE_DISCONNECTED:
                            stateName = "DISCONNECTED";
                            break;
                        case HostDelegate.SENSOR_STATE_CONNECTING:
                            stateName = "CONNECTING";
                            break;
                        case HostDelegate.SENSOR_STATE_CONNECTED:
                            stateName = "CONNECTED";
                            break;
                        case HostDelegate.SENSOR_STATE_LIVE_MODE:
                            stateName = "LIVE MODE";
                            break;

                    }

                    prependToLog("onSensorStateChanged()" + stateName);

                }
            };
            MainActivity.this.runOnUiThread(r);
        }

    };


    protected ILiveModeListener liveModeListener = new ILiveModeListener.Stub() {


        @Override
        public void onStarted() throws RemoteException {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "onStarted ", Toast.LENGTH_SHORT).show();
                    prependToLog("onStarted() LiveMode");
                }
            };
            MainActivity.this.runOnUiThread(r);


        }

        @Override
        public void onImpactDetected() throws RemoteException {

        }


        @Override
        public void onShotDataDetected(final ShotData data) throws RemoteException {

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "onShotDataDetected ", Toast.LENGTH_SHORT).show();
                    try {
                        /**
                         * ↓ここで、時間をとったりしている
                         */
                        prependToLog(String.format("onShotDataDetected(){Time:%s, impactPosition:%d, impactSpeed:%s, impactSpin:%s,"+
                                        "getSwingSpeed:%s, getSwingType:%d",
                                data.getTime(),
                                data.getImpactPosition(),
                                String.valueOf(data.getImpactSpeed()),
                                String.valueOf(data.getImpactSpin()),
                                String.valueOf(data.getSwingSpeed()),
                                data.getSwingType()));


                        JSONArray jsonArary = new JSONArray();

                        // jsonデータの作成
                        JSONObject jsonOneData;
                        jsonOneData = new JSONObject();
                        jsonOneData.put("Time", data.getTime());
                        jsonOneData.put("ImpactPosition", data.getImpactPosition());
                        jsonOneData.put("ImpactSpeed", data.getImpactSpeed());
                        jsonOneData.put("ImpactSpin", data.getImpactSpin());
                        jsonOneData.put("SwingSpeed", data.getSwingSpeed());
                        jsonOneData.put("SwingType", data.getSwingType());
                        jsonArary.put(jsonOneData);


                        NCMBPush push = new NCMBPush();
                        push.setAction("com.sample.pushsample.RECEIVE_PUSH");
                        push.setTitle("test title");
                        push.setMessage(String.valueOf(jsonOneData));
                        push.setTarget(new JSONArray("[android],[ios]"));
                        push.setDialog(true);
                        push.sendInBackground(new DoneCallback() {
                            @Override
                            public void done(NCMBException e) {
                                if (e != null) {
                                    // エラー処理
                                } else {
                                    // プッシュ通知登録後の操作
                                }
                            }
                        });






                        // クラスのNCMBObjectを作成
                        NCMBObject obj = new NCMBObject("TestClass");

// オブジェクトの値を設定
                        obj.put("message", "Hello");
                        obj.put("impactPosition", data.getImpactPosition());
                        obj.put("impactSpeed", data.getImpactSpeed());
                        obj.put("impactSpin", data.getImpactSpin());
                        obj.put("swingSpeed", data.getSwingSpeed());
                        obj.put("SwingType", data.getSwingType());

// データストアへの登録
                        obj.saveInBackground(new DoneCallback() {
                            @Override
                            public void done(NCMBException e) {

                                if(e != null){
                                    Toast.makeText(MainActivity.this, "Fail", Toast.LENGTH_LONG).show();
                                    //保存に失敗した場合の処理

                                }else {
                                    //保存に成功した場合の処理
                                    Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_LONG).show();

                                }
                            }
                        });





                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            MainActivity.this.runOnUiThread(r);


        }

        @Override
        public void onEnded() throws RemoteException {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "onEnded ", Toast.LENGTH_SHORT).show();
                    prependToLog("onEnded()");
                }
            };
            MainActivity.this.runOnUiThread(r);

        }

        @Override
        public void onError(final int err) throws RemoteException {

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "onError ", Toast.LENGTH_SHORT).show();
                    prependToLog("onError()" + err);
                }
            };
            MainActivity.this.runOnUiThread(r);
        }
    };

    protected void prependToLog(String message) {

        String text = ((TextView) root.findViewById(R.id.output_text_view)).getText().toString();
        text = (message + "\n" + text);
        text = text.substring(0, Math.min(text.length(), 5000));
        ((TextView) root.findViewById(R.id.output_text_view)).setText(text);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NCMB.initialize(this.getApplicationContext(),"4b0e13dc5247b05b09983492cb9a188fb657a577268449109dd436a0cb173d1d","41d883232bbf939e45b48eea19fdc16afbfef6f435617d947337334681c0eb73");




        hostDelegate = new HostDelegate();
        hostDelegate.setContext(MainActivity.this);

        root = getLayoutInflater().inflate(R.layout.activity_service_tester, null);
        setContentView(root);

        root.findViewById(R.id.bind_button).setOnClickListener(this);
        root.findViewById(R.id.getSensorState).setOnClickListener(this);
        root.findViewById(R.id.connect_button).setOnClickListener(this);
        root.findViewById(R.id.disconnect_button).setOnClickListener(this);
        root.findViewById(R.id.get_racket_list_button).setOnClickListener(this);
        root.findViewById(R.id.get_config_button).setOnClickListener(this);
        root.findViewById(R.id.set_racket_button).setOnClickListener(this);
        root.findViewById(R.id.start_live_mode).setOnClickListener(this);
        root.findViewById(R.id.stop_live_mode).setOnClickListener(this);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MESSAGE_BIND:
                        bind();
                        break;

                    case MESSAGE_GET_SENSOR_STATE:
                        getSensorState();
                        break;

                    case MESSAGE_CONNECT:
                        connectToSensor();
                        break;

                    case MESSAGE_DISCONNECT:
                        disconnect();
                        break;

                    case MESSAGE_GET_RACKET_LIST:
                        getRacketList();
                        break;

                    case MESSAGE_GET_CONFIG:
                        getConfig();
                        break;

                    case MESSAGE_SET_CONFIG:
                        setConfig();
                        break;

                    case MESSAGE_START_LIVE_MODE:
                        startLiveMode();
                        break;

                    case MESSAGE_STOP_LIVE_MODE:
                        stopLiveMode();
                        break;


                }
            }
        };
    }


    protected void bind() {
        hostDelegate.bind();
    }

    public void getSensorState() {
        String statusMessage = hostDelegate.getSensorState();
        prependToLog(statusMessage);
    }

    public void connectToSensor() {
        hostDelegate.promptUserAndConnectToSensor(sensorConnectListener, sensorChangeListener, sensorStateChangeListener);
    }


    public void disconnect() {
        hostDelegate.disconnect();
    }

    public void getRacketList() {
        List<RacketModel> racketList = hostDelegate.getRacketList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; racketList != null && i < racketList.size(); i++) {
            RacketModel r = racketList.get(i);
            sb.append("\n" + r.getId() + "\n");
            sb.append(r.getFullName() + "\n");
            sb.append(r.getManufacturer().getName() + "\n");
            sb.append(r.getSeriesName() + "\n");

        }
        prependToLog(sb.toString());
        this.racketList = racketList;
    }


    protected void getConfig() {

        Config config = hostDelegate.getConfig();
        String message = "Null. (Not yet set.)";

        if (config != null) {
            message = "RacketId: " + config.racketId + ", DominantHand: " + (config.dominantHand == Config.HAND_LEFT ? "Left" : "Right");
        }
        final String finalMessage = message;
        prependToLog("getConfig: " + finalMessage);

    }

    public void setConfig() {

        if (racketList != null && racketList.size() > 0) {
            RacketModel racket = racketList.get(0);
            Config config = new Config(racket.getId(), Config.HAND_RIGHT);

            IResultListener.Stub resultCallback = new IResultListener.Stub() {

                @Override
                public void onResult(final int error) throws RemoteException {

                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            prependToLog("setConfig result: " + error);
                        }
                    };

                    MainActivity.this.runOnUiThread(r);
                }

            };
            IOnConfigChangeListener configChangeListener = new IOnConfigChangeListener.Stub() {
                @Override
                public void onConfigChanged(final Config config) throws RemoteException {
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            prependToLog("Config changed to racket:" + config.racketId + " , hand:" + (config.dominantHand == Config.HAND_LEFT ? "Left" : "Right"));
                        }
                    };

                    MainActivity.this.runOnUiThread(r);
                }

            };

            hostDelegate.setConfig(config, resultCallback, configChangeListener);

        }

    }


    public void startLiveMode() {
        hostDelegate.startLiveMode(liveModeListener);
    }

    public void stopLiveMode() {
        hostDelegate.stopLiveMode();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.bind_button:
                handler.obtainMessage(MESSAGE_BIND).sendToTarget();
                break;
            case R.id.getSensorState:
                handler.obtainMessage(MESSAGE_GET_SENSOR_STATE).sendToTarget();
                break;
            case R.id.connect_button:
                handler.obtainMessage(MESSAGE_CONNECT).sendToTarget();
                break;
            case R.id.disconnect_button:
                handler.obtainMessage(MESSAGE_DISCONNECT).sendToTarget();
                break;
            case R.id.get_racket_list_button:
                handler.obtainMessage(MESSAGE_GET_RACKET_LIST).sendToTarget();
                break;
            case R.id.get_config_button:
                handler.obtainMessage(MESSAGE_GET_CONFIG).sendToTarget();
                break;
            case R.id.set_racket_button:
                handler.obtainMessage(MESSAGE_SET_CONFIG).sendToTarget();
                break;
            case R.id.start_live_mode:
                handler.obtainMessage(MESSAGE_START_LIVE_MODE).sendToTarget();
                break;
            case R.id.stop_live_mode:
                handler.obtainMessage(MESSAGE_STOP_LIVE_MODE).sendToTarget();
                break;

        }
    }


}
