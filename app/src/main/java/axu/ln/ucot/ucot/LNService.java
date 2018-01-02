package axu.ln.ucot.ucot;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class LNService extends Service {

    String output = "";
    String input = "";
    OutputStream os = null;
    BufferedReader br = null;
    Process process = null;
    String password = "123456";
    String tag = "Ucot";
    int delay = 10 * 1000;
    String address=null;
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        LNService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LNService.this;
        }
    }
    private ServiceCallbacks serviceCallbacks;
    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }
    @Override
    public void onCreate() {
        super.onCreate();
//        !!!be careful with this function
//                the folder existence does not mean that the client can be run
//                because the files, e.g., ucot, key, may be damged
        File folder = new File(getFilesDir() + "/.ucot/node");
        if (folder.exists()) {
            delay = 0;
        }
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                updateUI("Start client\n");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences sp=getSharedPreferences(getString(R.string.sp_name),MODE_PRIVATE);
                        address =sp.getString(getString(R.string.sp_address),null);
                        input = getFilesDir() + "/.ucot/ucot --datadir " + getFilesDir() + "/.ucot/node --networkid 15 --port 30604 --rpc --rpcport 3333 --ipcdisable --nodiscover --unlock "+address+" --syncmode light console";
                        Log.i("Ucot", input);
                        updateUI("res:" + output + "\n");
                        try {
                            Runtime runtime = Runtime.getRuntime();
                            process = runtime.exec(input);
                            br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            os = process.getOutputStream();
                            output = null;
                            while ((output = br.readLine()) != null) {
                                Log.e("Ucot", output);
                                updateUI("res:" + output + "\n");
                            }
                            try {
                                process.waitFor();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (br != null)
                                    br.close();
                                if (os != null) {
                                    os.close();
                                    os = null;
                                }
                                if (process != null)
                                    process.destroy();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }).start();
            }
        }, delay);
        Timer timerPass= new Timer();
        timerPass.schedule(new TimerTask() {
            @Override
            public void run() {
                sendCommand(password);
            }
        },delay+1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(tag, "onStartCommand() executed");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(tag, "onDestroy() executed");
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.i(tag, "onBind() executed");
        return mBinder;
    }
    private void updateUI(String content) {
        Log.i(tag, "Service update ui by callBack: " + content);
        if (serviceCallbacks!=null){
            serviceCallbacks.callBack(content);
        }
//        if (mainHandler == null) {
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    tv.append("UI is not ready to be updated.\n");
//                }
//            });
//        }
//        Message tmpMessage = mainHandler.obtainMessage();
//        tmpMessage.what = 1;
//        tmpMessage.obj = content;
//        mainHandler.sendMessage(tmpMessage);
    }
    public void sendCommand(String content){

        Log.i("Ucot", "try to send command: " + content);
        if (os != null) {
            try {
                updateUI("send Command: " + content + "\n");
                os.write((content+ "\n").getBytes());
                os.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Light client is not ready in service.", Toast.LENGTH_SHORT).show();
            return;
        }

    }
}
