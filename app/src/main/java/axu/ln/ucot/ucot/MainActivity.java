package axu.ln.ucot.ucot;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks {
    @Override
    public void callBack(String content) {
        updateUI(content);
    }

    public interface updateUI {
        void updateUI(String content);
    }

    TextView tv;
    String output = "";
    String input = "";
    OutputStream os = null;
    BufferedReader br = null;
    Process process = null;
    String password = "123456";
    EditText ed;
    Handler mainHandler = null;
    String tag = "Ucot";
    int delay = 15 * 1000;
    String address = null;
    LNService lnService = null;
    boolean mBound = false;
    boolean init = false;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LNService.LocalBinder binder = (LNService.LocalBinder) service;
            lnService = binder.getService();
            mBound = true;
            lnService.setCallbacks(MainActivity.this);
            Log.i(tag,"Bound true");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.i(tag,"Bound false");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(tag, "Activity Creat");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tv = (TextView) findViewById(R.id.textbox);
        ed = (EditText) findViewById(R.id.editText);
        mainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:

                    {
                        tv.append(msg.obj.toString() + "\n");
                        if (msg.obj.toString() == "Init finish") {
                            SharedPreferences sp = getSharedPreferences(getString(R.string.sp_name), MODE_PRIVATE);
                            address = sp.getString(getString(R.string.sp_address), null);
                            init = true;
                            Intent startIntent = new Intent(MainActivity.this, LNService.class);
                            MainActivity.this.startService(startIntent);
                            bindService(startIntent, mConnection, BIND_AUTO_CREATE);

                        }
                        break;
                    }

                }
            }
        };
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand(ed.getText().toString());
//                Snackbar.make(view, "Light client is not ready.", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
        Init(MainActivity.this);

    }

    protected void onResume() {
        super.onResume();
        Log.i(tag, "Activity resume");
        if (!mBound && init)
        {
            Intent startIntent = new Intent(MainActivity.this, LNService.class);
            MainActivity.this.startService(startIntent);
            bindService(startIntent, mConnection, BIND_AUTO_CREATE);
        }

    }

    protected void onStart() {
        super.onStart();
        Log.i(tag, "Activity Start");

    }
    protected void onPause() {
        super.onPause();
        Log.i(tag, "Activity pause");
        if (mBound) {
            unbindService(mConnection);
            mBound=false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(tag, "Activity stop");
        if (mBound) {
            unbindService(mConnection);
            mBound=false;
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.i(tag, "Activity onDestroy");
        if (mBound) {
            lnService.setCallbacks(null);
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_pic: {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
                break;
            }
            case R.id.action_nfc: {
                startActivity(new Intent(MainActivity.this,ActivitySecurePhoto.class));
                break;

            }
            case R.id.action_settings: {
//                Toast.makeText(getApplicationContext(), "setting", Toast.LENGTH_SHORT).show();
                break;
            }

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(tag, "onresult " + requestCode + "," + resultCode + data.getData() + "\n");
        if (requestCode == 1) {
            if (data != null && address != null) {
                Uri uri = data.getData();
                tv.append(uri.getPath() + "\n");
                tv.append(UriToPathUtil.getImageAbsolutePath(getApplicationContext(), uri) + "\n");

//                    Toast.makeText(getApplicationContext(), uri.toString(), Toast.LENGTH_SHORT).show();
                File picFile = new File(UriToPathUtil.getImageAbsolutePath(getApplicationContext(), uri));
                String md5 = md5(picFile);
                tv.append(md5 + "\n");
                String cmd = "eth.sendTransaction({from:\"0x" + address + "\",to:\"0x" + getString(R.string.bc_server_account) + "\",value:web3.toWei(1,\"ether\"),data:web3.toHex('" + md5 + "')})";
//                String cmd = "eth.sendTransaction({from:eth.accounts[0],to:\"" + getString(R.string.bc_server_account) + "\",value:web3.toWei(0.1,\"ether\"),data:web3.toHex('" + md5 + "')})";
                sendCommand(cmd);
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    public static String md5(File file) {
        String result = "";
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            byte[] bytes = md5.digest();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public void Init(Context ctx) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//            generate folder
                    File folder = new File(getFilesDir() + "/.ucot/node");
                    if (!folder.exists()) {
                        folder.mkdirs();
                        updateUI("Generate Folder\n");
                    }
//            copy ucot
                    File ucot = new File(getFilesDir() + "/.ucot/ucot");
                    AssetManager am = getApplication().getAssets();
                    if (!ucot.exists()) {
                        updateUI("Copy ucot\n");
                        InputStream is = am.open("ucot");
                        FileOutputStream fos = new FileOutputStream(ucot);
                        byte[] buffer = new byte[1024];
                        int byteCount = 0;
                        while ((byteCount = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, byteCount);
                        }
                        fos.flush();
                        is.close();
                        fos.close();
                        ucot.setExecutable(true, true);
                    }
//            copy genesis
                    File genesis = new File(getFilesDir() + "/.ucot/genesis.json");
                    if (!genesis.exists()) {
                        updateUI("Copy Genesis\n");
                        InputStream is = am.open("genesis.json");
                        FileOutputStream fos = new FileOutputStream(genesis);
                        byte[] buffer = new byte[1024];
                        int byteCount = 0;
                        while ((byteCount = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, byteCount);
                        }
                        fos.flush();
                        is.close();
                        fos.close();
                    }
//            init Blockchain

                    File ucotFile = new File(getFilesDir() + "/.ucot/node/ucot");
                    if (!ucotFile.exists())
                        ucotFile.mkdirs();
                    if (ucotFile.exists() && ucotFile.isDirectory()) {
//                empty folder, generate key
                        if (ucotFile.list().length == 0) {
                            try {
                                updateUI("Init client\n");
                                String rootPath = getFilesDir() + "/.ucot/";
                                Runtime runtime = Runtime.getRuntime();
                                process = runtime.exec(rootPath + "ucot --datadir " + rootPath + "node init " + rootPath + "genesis.json");
                                InputStream is = process.getInputStream();
                                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                                os = process.getOutputStream();
                                output = null;
                                while ((output = br.readLine()) != null) {
                                    Log.e("Ucot", output);
                                    updateUI("res:" + output + "\n");
                                    if (output.contains("database=lightchaindata")) {
                                        Log.i(tag, "init database");
                                        try {
                                            if (br != null)
                                                br.close();
                                            if (os != null) {
                                                os.close();
                                                os = null;
                                            }
                                            if (process != null)
                                                process.destroy();
                                            break;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
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
                    }
//            copy static-nodes list
                    File staticNode = new File(getFilesDir() + "/.ucot/node/geth/static-nodes.json");
                    if (!staticNode.exists()) {
                        updateUI("Copy static node \n");
                        InputStream is = am.open("static-nodes.json");
                        FileOutputStream fos = new FileOutputStream(staticNode);
                        byte[] buffer = new byte[1024];
                        int byteCount = 0;
                        while ((byteCount = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, byteCount);
                        }
                        fos.flush();
                        is.close();
                        fos.close();
                    }
//              init account
                    File keyFile = new File(getFilesDir() + "/.ucot/node/keystore");
                    if (!keyFile.exists())
                        keyFile.mkdirs();
                    if (keyFile.exists() && keyFile.isDirectory()) {
//                empty folder, generate key
                        if (keyFile.list().length == 0) {
                            try {
                                updateUI("Init account\n");
                                String rootPath = getFilesDir() + "/.ucot/";
                                Runtime runtime = Runtime.getRuntime();
                                process = runtime.exec(rootPath + "ucot --datadir " + rootPath + "node account new");
                                InputStream is = process.getInputStream();
                                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                                os = process.getOutputStream();
                                output = null;
                                while ((output = br.readLine()) != null) {
                                    Log.e("Ucot", output);
                                    updateUI("res:" + output + "\n");
                                    if (output.startsWith("!! Unsupported terminal")) {
                                        try {
                                            os.write((password + "\n").getBytes());
                                            os.flush();
                                            Log.e("Ucot", "input pass");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (output.startsWith("Passphrase")) {
                                        try {
                                            os.write((password + "\n").getBytes());
                                            os.flush();
                                            Log.e("Ucot", "input pass");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (output.startsWith("Address")) {
                                        String address = output.substring(output.indexOf("{") + 1, output.indexOf("}"));
                                        SharedPreferences.Editor spe = getSharedPreferences(getString(R.string.sp_name), MODE_PRIVATE).edit();
                                        spe.putString(getString(R.string.sp_address), address);
                                        spe.apply();

                                        Log.i(tag, "obtain address");
                                        try {
                                            if (br != null)
                                                br.close();
                                            if (os != null) {
                                                os.close();
                                                os = null;
                                            }
                                            if (process != null)
                                                process.destroy();
                                            break;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }


                                    }
                                }
//                    try {
//                        process.waitFor();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
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
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                updateUI("Init finish");

            }
        }).start();
    }

    private void updateUI(String content) {
        Log.i(tag, "Activity update ui with: " + content);
        if (mainHandler == null) {
            runOnUiThread(new Runnable() {
                public void run() {
                    tv.append("UI is not ready to be updated.\n");
                }
            });
        }
        Message tmpMessage = mainHandler.obtainMessage();
        tmpMessage.what = 1;
        tmpMessage.obj = content;
        mainHandler.sendMessage(tmpMessage);
    }

    private void sendCommand(String content) {

        Log.i("Ucot", "try to send command: " + content);
        if (mBound) {
            lnService.sendCommand(content);
        }
        else {
            Toast.makeText(getApplicationContext(), "Light client is not ready,  please try again later.", Toast.LENGTH_SHORT).show();
            Intent startIntent = new Intent(MainActivity.this, LNService.class);
            MainActivity.this.startService(startIntent);
            bindService(startIntent, mConnection, BIND_AUTO_CREATE);
            return;
        }

    }


}
