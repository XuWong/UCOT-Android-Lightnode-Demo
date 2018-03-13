package axu.ln.ucot.ucot;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.spongycastle.util.encoders.Hex;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.admin.methods.response.PersonalUnlockAccount;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;

public class Main2Activity extends AppCompatActivity implements ServiceCallbacks {
    String TAG = "ucot";
    String endS = "11";

    public void callBack(String content) {
        updateUI(content);
    }

    public interface updateUI {
        void updateUI(String content);
    }

    Handler mainHandler = null;
    LNService lnService = null;
    boolean mBound = false;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LNService.LocalBinder binder = (LNService.LocalBinder) service;
            lnService = binder.getService();
            mBound = true;
            lnService.setCallbacks(Main2Activity.this);
            Log.i(TAG, "Bound true");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.i(TAG, "Bound false");
        }
    };

    private int functionIndex = 0;
    private String ConsoleResult = "";
    private String account = "1520e73a14ea17f13306319c9c787260445c09ea\"";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                functionIndex = 1;
                sendCommand("personal.unlockAccount(eth.accounts[0], \"123456\", 30)\neth.sendTransaction({from:eth.accounts[0],to:\"5b16e210676d51f8746ffbcd41e2f14975a320b1\",value:web3.toWei(0,\"ether\"),data:web3.toHex('test')})\npersonal.lockAccount(eth.accounts[0])\n" + endS);
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });
        mainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:

                    {
                        Log.i(TAG, msg.obj.toString() + "\n");
                        switch (functionIndex) {
                            case 0:
                                break;
                            case 1:
                                ConsoleResult = ConsoleResult + msg.obj.toString() + "|";
//                                Log.i(TAG,"consoleResult:"+ConsoleResult);
                                if (ConsoleResult.endsWith(endS + "|")) {
                                    Log.i(TAG, "consoleResult:" + ConsoleResult);
                                    String[] ResultList = ConsoleResult.split("\\|");
                                    Log.i(TAG, "solve:" + ResultList[1] + " " + ResultList[3]);
                                    if (ResultList[1].equals("true")) {
                                        Log.i(TAG, "unlock success");
                                        String txHash = ResultList[3];
                                        if (txHash.startsWith("\"")) {
                                            Log.i(TAG, "txHash:" + txHash);
                                        } else {
                                            Log.i(TAG, "txError:" + txHash);
                                        }
                                    }
                                    ConsoleResult = "";
                                    functionIndex = 0;

                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    }

                }
            }
        };
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    Web3j web3 = Web3jFactory.build(new HttpService());
                    EthBlockNumber ethBlockNumber = web3.ethBlockNumber().send();
                    Log.i(TAG, "blockNumber:" + ethBlockNumber.getBlockNumber());
                    Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().send();
                    String clientVersion = web3ClientVersion.getWeb3ClientVersion();
                    Log.i(TAG, "version" + clientVersion);
                    Log.i(TAG, getFilesDir() + "/.ucot/node/keystore");
//                    File keyFolder = new File(getFilesDir() + "/.ucot/node/keystore");
//                    File[] listofFiles = keyFolder.listFiles();
//                    String keyFile = getFilesDir() + "/.ucot/node/keystore/" + listofFiles[0].getName();
//                    Log.i(TAG, keyFile);
//                    Credentials credentials = WalletUtils.loadCredentials("123456", keyFile);
//                    EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(
//                            "0x1520e73a14ea17f13306319c9c787260445c09ea", DefaultBlockParameterName.LATEST).sendAsync().get();
//                    BigInteger nonce = ethGetTransactionCount.getTransactionCount();
//                    Log.i(TAG,"nonce is "+nonce);
//                    BigInteger gasPrice=BigInteger.valueOf(100);
//                    BigInteger gasLimit=BigInteger.valueOf(10000);
//                    BigInteger value=BigInteger.valueOf(0);
//                    RawTransaction rawTransaction  = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, "0x8c9c2121ce7a17e3bfa74dab35b453ea662a38ea",value,"test");
////                    RawTransaction rawTransaction  = RawTransaction.createEtherTransaction(
////                            nonce, gasPrice, gasLimit, "0x8c9c2121ce7a17e3bfa74dab35b453ea662a38ea",value);
//                    byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
//                    String hexValue = Hex.toHexString(signedMessage);
//                    Log.i(TAG,"hexValue is "+hexValue);
//                    EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction("0x"+hexValue).send();
//                    Log.i(TAG,"error message "+ethSendTransaction.getError().getMessage());
//                    String txHash=ethSendTransaction.getTransactionHash();
//                    Log.i(TAG,"TX hash is "+txHash);


                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

    }

    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Activity resume");
        if (!mBound) {
            Intent startIntent = new Intent(Main2Activity.this, LNService.class);
            Main2Activity.this.startService(startIntent);
            bindService(startIntent, mConnection, BIND_AUTO_CREATE);
        }

    }

    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Activity Start");

    }

    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Activity pause");
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Activity stop");
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Activity onDestroy");
        if (mBound) {
            lnService.setCallbacks(null);
            unbindService(mConnection);
            mBound = false;
        }
    }

    private void updateUI(String content) {
        Log.i(TAG, "Activity update ui with: " + content);
        if (mainHandler == null) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "UI is not ready,  please try again later.", Toast.LENGTH_SHORT).show();
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
        } else {
            Toast.makeText(getApplicationContext(), "Light client is not ready,  please try again later.", Toast.LENGTH_SHORT).show();
            Intent startIntent = new Intent(Main2Activity.this, LNService.class);
            Main2Activity.this.startService(startIntent);
            bindService(startIntent, mConnection, BIND_AUTO_CREATE);
            return;
        }

    }
}
