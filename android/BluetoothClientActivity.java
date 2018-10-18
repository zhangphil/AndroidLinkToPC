package zhangphil.test;

import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

/**
 * Android手机客户端通过蓝牙发送数据到部署在Windows PC电脑上。
 * 如果运行失败，请打开手机的设置，检查是否赋予该App权限：
 *
 * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
 * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
 *
 * <uses-permission android:name="android.permission.BLUETOOTH" />
 * <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
 *
 * Android手机的蓝牙客户端。
 * 代码启动后检查当前手机是否已经和蓝牙名称为  TARGET_DEVICE_NAME 的配对成功。
 * 如果配对成功，直接发起对服务器的连接并发送数据到服务器端。
 * 如果当前手机蓝牙和服务器端没有配对成功，则先启动蓝牙扫描，去搜索目标蓝牙设备。发现找到目标设备后连接目标设备并发送数据。
 *
 */

public class BluetoothClientActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;

    //要连接的目标蓝牙设备。
    private final String TARGET_DEVICE_NAME = "PHIL-PC";

    private final String TAG = "蓝牙调试";
    private final String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    // 广播接收发现蓝牙设备。
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String name = device.getName();
                if (name != null)
                    Log.d(TAG, "发现设备:" + name);

                if (name != null && name.equals("PHIL-PC")) {
                    Log.d(TAG, "发现目标设备，开始线程连接!");

                    // 蓝牙搜索是非常消耗系统资源开销的过程，一旦发现了目标感兴趣的设备，可以考虑关闭扫描。
                    mBluetoothAdapter.cancelDiscovery();

                    new Thread(new ClientThread(device)).start();
                }
            }
        }
    };

    private class ClientThread extends Thread {
        private BluetoothDevice device;

        public ClientThread(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            BluetoothSocket socket;

            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));

                Log.d(TAG, "连接服务端...");
                socket.connect();
                Log.d(TAG, "连接建立.");

                // 开始往服务器端发送数据。
                sendDataToServer(socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sendDataToServer(BluetoothSocket socket) {
            try {
                OutputStream os = socket.getOutputStream();
                os.write(new String("hello,world!").getBytes());
                os.flush();
                os.close();
                Log.d(TAG, "发送成功");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private BluetoothDevice getPairedDevices() {
        // 获得和当前Android已经配对的蓝牙设备。
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            // 遍历
            for (BluetoothDevice device : pairedDevices) {
                // 把已经取得配对的蓝牙设备名字和地址打印出来。
                Log.d(TAG, device.getName() + " : " + device.getAddress());
                if (TextUtils.equals(TARGET_DEVICE_NAME, device.getName())) {
                    Log.d(TAG, "已配对目标设备 -> " + TARGET_DEVICE_NAME);
                    return device;
                }
            }
        }

        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = getPairedDevices();
        if (device == null) {
            // 注册广播接收器。
            // 接收蓝牙发现讯息。
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver, filter);

            if (mBluetoothAdapter.startDiscovery()) {
                Log.d(TAG, "启动蓝牙扫描设备...");
            }
        } else {
            new ClientThread(device).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}
