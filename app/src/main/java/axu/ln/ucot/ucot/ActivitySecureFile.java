package axu.ln.ucot.ucot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.necer.ndialog.NDialog;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;

import axu.ln.ucot.ucot.recycler.AdapterVerifyResult;
import axu.ln.ucot.ucot.recycler.VerifiedFile;
import axu.ln.ucot.ucot.ui.InputDialog;
import axu.ln.ucot.ucot.utils.FileUtils;
import axu.ln.ucot.ucot.utils.HexUtils;
import axu.ln.ucot.ucot.utils.PreferenceUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static axu.ln.ucot.ucot.utils.FileUtils.*;

public class ActivitySecureFile extends AppCompatActivity implements ServiceCallbacks {

    private static final String SAVED_DIRECTORY = "com.calintat.explorer.SAVED_DIRECTORY";

    private static final String SAVED_SELECTION = "com.calintat.explorer.SAVED_SELECTION";

    private static final String EXTRA_NAME = "com.calintat.explorer.EXTRA_NAME";

    private static final String EXTRA_TYPE = "com.calintat.explorer.EXTRA_TYPE";
    private static final String tag = "ucot";

    private CollapsingToolbarLayout toolbarLayout;

    private CoordinatorLayout coordinatorLayout;

    private DrawerLayout drawerLayout;

    private NavigationView navigationView;

    private Toolbar toolbar;

    private File currentDirectory;

    private AdapterVerifyResult adapter;

    private String name;

    private String type;
    private boolean sentTX = false;
    private boolean readTX = false;
    private Handler mainHandler = null;

    LNService lnService = null;
    boolean mBound = false;
    private String fileSecureInfo = "";
    String TAG = "ucot";
    boolean disPlayResult = false;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LNService.LocalBinder binder = (LNService.LocalBinder) service;
            lnService = binder.getService();
            mBound = true;
            lnService.setCallbacks(ActivitySecureFile.this);
            Log.i(tag, "Bound true");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.i(tag, "Bound false");
        }
    };

    public void callBack(String content) {
        updateUI(content);
    }

    public interface updateUI {
        void updateUI(String content);
    }

    private void updateUI(String content) {
        if (mainHandler == null) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Give me a second.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        Log.i(tag, content.length() + content);
        if (sentTX) {
            if (content.length() > 64 && content.length() < 75) {
                sentTX = false;
                String receipt = content.substring(content.indexOf("\""));
                Log.i(tag, receipt);
                Message tmpMessage = mainHandler.obtainMessage();
                tmpMessage.what = 2;
                tmpMessage.obj = receipt;
                mainHandler.sendMessage(tmpMessage);
                return;
            }
        }
        if (readTX) {
            if (content.indexOf("input") >= 0) {
                Log.i(tag, "get input:" + content.substring(content.indexOf("0x") + 2, content.lastIndexOf("\"")));
                String input = HextoString(content.substring(content.indexOf("0x") + 2, content.lastIndexOf("\"")));
                Log.i(tag, input);
                readTX = false;
                Message tmpMessage = mainHandler.obtainMessage();
                tmpMessage.what = 3;
                tmpMessage.obj = input;
                mainHandler.sendMessage(tmpMessage);
                return;
            }
        }
        Message tmpMessage = mainHandler.obtainMessage();
        tmpMessage.what = 1;
        tmpMessage.obj = content;
        mainHandler.sendMessage(tmpMessage);
    }

    private void sendCommand(String content) {
        Log.i(tag, "try to send command: " + content);
        if (mBound) {
            lnService.sendCommand(content);
        } else {
            Toast.makeText(getApplicationContext(), "Light client is not ready,  please try again later.", Toast.LENGTH_SHORT).show();
            Intent startIntent = new Intent(ActivitySecureFile.this, LNService.class);
            ActivitySecureFile.this.startService(startIntent);
            bindService(startIntent, mConnection, BIND_AUTO_CREATE);
            return;
        }

    }


    protected void onPause() {
        super.onPause();
        Log.i(tag, "Activity sp onPause");
        if (mBound) {
            Log.i(tag, "Unbind in pause");
            unbindService(mConnection);
            mBound = false;
        }
    }

    private void actionShowMd5() {

        List<File> selectedItems = adapter.getSelectedItems();
        adapter.clearSelection();
        String InfoTobeSave = "";
        for (File it : selectedItems) {
            try {
                String result = md5(it);
                InfoTobeSave = InfoTobeSave + it.getName() + ":" + result + ";";
                if (result != null)
                    Log.i(tag, result);
                else
                    Log.i(tag, "get null");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        fileSecureInfo = InfoTobeSave;
        SharedPreferences sp = getSharedPreferences(getString(R.string.sp_name), MODE_PRIVATE);
        String address = sp.getString(getString(R.string.sp_address), null);
        String sendTxCommand = "eth.sendTransaction({from:\"0x" + address + "\",to:\"0x" + getString(R.string.bc_server_account) + "\",value:web3.toWei(0,\"ether\"),data:web3.toHex(\"" + InfoTobeSave + "\")})";
        sentTX = true;
        Log.i(tag, sendTxCommand);
        sendCommand(sendTxCommand);

    }

    private void verifyMd5() {
        AsyncTask<Void, Void, Map<String, Integer>> execute = new AsyncTask<Void, Void, Map<String, Integer>>() {
            @Override
            protected Map<String, Integer> doInBackground(Void... voids) {
                Map<String, Integer> verifiedFiles = new HashMap<String, Integer>();
                try {
                    int defaultState = 0;
                    Web3j web3 = Web3jFactory.build(new HttpService());
//                    File[] files = currentDirectory.listFiles();
                    File[] files = FileUtils.getChildren(currentDirectory);
                    for (File f : files) {
                        if (f.isDirectory()) {
                            continue;
                        }
                        SharedPreferences sp = getSharedPreferences(getString(R.string.sp_name), MODE_PRIVATE);
                        String txHash = sp.getString(f.getName() + "txHash", null);
                        if (txHash == null) {
                            verifiedFiles.put(f.getName(), defaultState);
                            continue;
                        } else {
                            Log.i(tag, f.getName() + ":" + txHash);
//                            EthTransaction ethTransaction = web3.ethGetTransactionByHash("0x4dafe6da356b3d58e88c562fa62631d4ba5d2fb94b6137f04f61a7ed468f57da").send();
                            EthTransaction ethTransaction = web3.ethGetTransactionByHash(txHash.replace("\"", "")).send();
                            Transaction transaction = ethTransaction.getResult();
                            if (transaction == null) {
                                verifiedFiles.put(f.getName(), defaultState);
                                continue;
                            }
                            String hex = transaction.getInput();
                            if (hex == null) {
                                verifiedFiles.put(f.getName(), defaultState);
                                continue;
                            }
                            String info = HexUtils.decode(hex);
                            String[] InfoList = info.split(";");
                            String bcMD5 = null;
                            for (String it : InfoList) {
                                if (it.startsWith(f.getName())) {
                                    bcMD5 = it.substring(it.indexOf(":") + 1);
                                    break;
                                }
                            }
                            if (bcMD5 == null) {
                                verifiedFiles.put(f.getName(), defaultState);
                                continue;
                            }
                            String fileMD5 = md5(f);
                            if (bcMD5.equals(fileMD5)) {
                                verifiedFiles.put(f.getName(), 1);
                            } else {
                                verifiedFiles.put(f.getName(), 2);
                            }

                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return verifiedFiles;
            }

            @Override
            protected void onPostExecute(Map<String, Integer> verifiedFiles) {
                adapter.clear();
                adapter.clearSelection();
                disPlayResult = true;
                toolbarLayout.setTitle("Authentication   result");
                adapter.addVerifiedFiles(verifiedFiles);
                File[] FileList = FileUtils.getChildren(currentDirectory);
                for (File vf : FileList) {
                    if (vf.isFile()) {
                        adapter.add(vf);
                        Log.i(tag, vf.getName() + ":" + verifiedFiles.get(vf.getName()));
                    }
                }
//                adapter.addAll();
            }
        }.execute();
    }
    //----------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {

//        initActivityFromIntent();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sfile);
        initAppBarLayout();
        initCoordinatorLayout();
        initDrawerLayout();
//        initFloatingActionButton();
        initNavigationView();
        initRecyclerView();
        loadIntoRecyclerView();
        invalidateToolbar();
        invalidateTitle();
        mainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 2: {
                        SharedPreferences sp = getSharedPreferences(getString(R.string.sp_name), MODE_PRIVATE);
                        SharedPreferences.Editor sped = sp.edit();
                        String[] InfoList = fileSecureInfo.split(";");
                        for (String it : InfoList) {
                            try {
                                if (it.indexOf(":") > 0) {
                                    String[] nameMd5 = it.split(":");
                                    sped.putString(nameMd5[0] + "lpmd5", nameMd5[1]);
                                    sped.putString(nameMd5[0] + "txHash", msg.obj.toString());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        sped.commit();
                        Toast.makeText(getApplicationContext(), "Selected files are secured with TX hash:" + msg.obj.toString(), Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case 3: {

                        break;
                    }

                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        if (disPlayResult) {
            setPath(currentDirectory);
            disPlayResult = false;
            return;
        }

        if (drawerLayout.isDrawerOpen(navigationView)) {

            drawerLayout.closeDrawers();

            return;
        }

        if (adapter.anySelected()) {

            adapter.clearSelection();

            return;
        }

        if (!FileUtils.isStorage(currentDirectory)) {

            setPath(currentDirectory.getParentFile());

            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 0) {

            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                Snackbar.make(coordinatorLayout, "Permission required", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Settings", v -> gotoApplicationSettings())
                        .show();
            } else {

                loadIntoRecyclerView();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {

        if (adapter != null) adapter.refresh();

        Log.i(tag, "Activity sp resume");
        Intent startIntent = new Intent(ActivitySecureFile.this, LNService.class);
        ActivitySecureFile.this.startService(startIntent);
        bindService(startIntent, mConnection, BIND_AUTO_CREATE);
        super.onResume();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

        adapter.select(savedInstanceState.getIntegerArrayList(SAVED_SELECTION));

        String path = savedInstanceState.getString(SAVED_DIRECTORY, getInternalStorage().getPath());

        if (currentDirectory != null) setPath(new File(path));

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putIntegerArrayList(SAVED_SELECTION, adapter.getSelectedPositions());

        outState.putString(SAVED_DIRECTORY, getPath(currentDirectory));

        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.action, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.action_delete:
//                actionDelete();
//                actionShowMd5();
                return true;

            case R.id.action_rename:
//                actionRename();
                actionShowMd5();
//                verifyMd5();
                return true;

            case R.id.action_search:
//                actionSearch();
                verifyMd5();
                return true;

            case R.id.action_copy:
//                actionCopy();
                actionShowMd5();
                return true;

            case R.id.action_move:
                actionMove();
                return true;

            case R.id.action_send:
                actionSend();
                return true;

            case R.id.action_sort:
                actionSort();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (adapter != null) {

            int count = adapter.getSelectedItemCount();

            menu.findItem(R.id.action_delete).setVisible(false);

            menu.findItem(R.id.action_rename).setVisible(count >= 1);

            menu.findItem(R.id.action_search).setVisible(count == 0);

            menu.findItem(R.id.action_copy).setVisible(false);
            menu.findItem(R.id.action_move).setVisible(false);
            menu.findItem(R.id.action_send).setVisible(false);
            menu.findItem(R.id.action_sort).setVisible(false);
//            menu.findItem(R.id.action_copy).setVisible(count >= 1 && name == null && type == null);
//
//            menu.findItem(R.id.action_move).setVisible(count >= 1 && name == null && type == null);
//
//            menu.findItem(R.id.action_send).setVisible(count >= 1);
//
//            menu.findItem(R.id.action_sort).setVisible(count == 0);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    //----------------------------------------------------------------------------------------------

    private void initActivityFromIntent() {

        name = getIntent().getStringExtra(EXTRA_NAME);

        type = getIntent().getStringExtra(EXTRA_TYPE);

        if (type != null) {

            switch (type) {

                case "audio":
                    setTheme(R.style.app_theme_Audio);
                    break;

                case "image":
                    setTheme(R.style.app_theme_Image);
                    break;

                case "video":
                    setTheme(R.style.app_theme_Video);
                    break;
            }
        }
    }

    private void loadIntoRecyclerView() {

        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;

        if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, permission)) {

            ActivityCompat.requestPermissions(this, new String[]{permission}, 0);

            return;
        }

        final Context context = this;

        if (name != null) {

            adapter.addAll(FileUtils.searchFilesName(context, name));

            return;
        }

        if (type != null) {

            switch (type) {

                case "audio":
                    adapter.addAll(FileUtils.getAudioLibrary(context));
                    break;

                case "image":
                    adapter.addAll(FileUtils.getImageLibrary(context));
                    break;

                case "video":
                    adapter.addAll(FileUtils.getVideoLibrary(context));
                    break;
            }

            return;
        }

        setPath(getInternalStorage());
    }

    //----------------------------------------------------------------------------------------------

    private void initAppBarLayout() {

        toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_layout);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_more));

        setSupportActionBar(toolbar);
    }

    private void initCoordinatorLayout() {

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
    }

    private void initDrawerLayout() {

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawerLayout == null) return;

        if (name != null || type != null) {

            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

//    private void initFloatingActionButton() {
//
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floating_action_button);
//
//        if (fab == null) return;
//
//        fab.setOnClickListener(v -> actionCreate());
//
//        if (name != null || type != null) {
//
//            ViewGroup.LayoutParams layoutParams = fab.getLayoutParams();
//
//            ((CoordinatorLayout.LayoutParams) layoutParams).setAnchorId(View.NO_ID);
//
//            fab.setLayoutParams(layoutParams);
//
//            fab.hide();
//        }
//    }

    private void initNavigationView() {

        navigationView = (NavigationView) findViewById(R.id.navigation_view);

        if (navigationView == null) return;

        MenuItem menuItem = navigationView.getMenu().findItem(R.id.navigation_external);

        menuItem.setVisible(getExternalStorage() != null);

        navigationView.setNavigationItemSelectedListener(item ->
        {
            switch (item.getItemId()) {
                case R.id.navigation_audio:
                    setType("audio");
                    return true;

                case R.id.navigation_image:
                    setType("image");
                    return true;

                case R.id.navigation_video:
                    setType("video");
                    return true;

                case R.id.navigation_feedback:
                    gotoFeedback();
                    return true;

                case R.id.navigation_settings:
                    gotoSettings();
                    return true;
            }

            drawerLayout.closeDrawers();

            switch (item.getItemId()) {

                case R.id.navigation_directory_0:
                    setPath(getPublicDirectory("DCIM"));
                    return true;

                case R.id.navigation_directory_1:
                    setPath(getPublicDirectory("Download"));
                    return true;

                case R.id.navigation_directory_2:
                    setPath(getPublicDirectory("Movies"));
                    return true;

                case R.id.navigation_directory_3:
                    setPath(getPublicDirectory("Music"));
                    return true;

                case R.id.navigation_directory_4:
                    setPath(getPublicDirectory("Pictures"));
                    return true;

                default:
                    return true;
            }
        });

        TextView textView = (TextView) navigationView.getHeaderView(0).findViewById(R.id.header);

        textView.setText(getStorageUsage(this));

        textView.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)));
    }

    private void initRecyclerView() {

        adapter = new AdapterVerifyResult(this);

        adapter.setOnItemClickListener(new OnItemClickListener(this));

        adapter.setOnItemSelectedListener(() -> {

            invalidateOptionsMenu();

            invalidateTitle();

            invalidateToolbar();
        });

        if (type != null) {

            switch (type) {

                case "audio":
                    adapter.setItemLayout(R.layout.list_item_1);
                    adapter.setSpanCount(getResources().getInteger(R.integer.span_count1));
                    break;

                case "image":
                    adapter.setItemLayout(R.layout.list_item_2);
                    adapter.setSpanCount(getResources().getInteger(R.integer.span_count2));
                    break;

                case "video":
                    adapter.setItemLayout(R.layout.list_item_3);
                    adapter.setSpanCount(getResources().getInteger(R.integer.span_count3));
                    break;
            }
        } else {

            adapter.setItemLayout(R.layout.list_item_0);

            adapter.setSpanCount(getResources().getInteger(R.integer.span_count0));
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        if (recyclerView != null) recyclerView.setAdapter(adapter);
    }

    //----------------------------------------------------------------------------------------------

    private void invalidateTitle() {

        if (adapter.anySelected()) {

            int selectedItemCount = adapter.getSelectedItemCount();

            toolbarLayout.setTitle(String.format("%s selected", selectedItemCount));
        } else if (name != null) {

            toolbarLayout.setTitle(String.format("Search for %s", name));
        } else if (type != null) {

            switch (type) {

                case "image":
                    toolbarLayout.setTitle("Images");
                    break;

                case "audio":
                    toolbarLayout.setTitle("Music");
                    break;

                case "video":
                    toolbarLayout.setTitle("Videos");
                    break;
            }
        } else if (currentDirectory != null && !currentDirectory.equals(getInternalStorage())) {

            toolbarLayout.setTitle(getName(currentDirectory));
        } else {

            toolbarLayout.setTitle("File secure");
        }
    }

    private void invalidateToolbar() {

        if (adapter.anySelected()) {

            toolbar.setNavigationIcon(R.drawable.ic_clear);

            toolbar.setNavigationOnClickListener(v -> adapter.clearSelection());
        } else if (name == null && type == null) {

            toolbar.setNavigationIcon(R.drawable.ic_menu);

            toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(navigationView));
        } else {

            toolbar.setNavigationIcon(R.drawable.ic_back);

            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    //----------------------------------------------------------------------------------------------

    private void actionCreate() {

        InputDialog inputDialog = new InputDialog(this, "Create", "Create directory") {

            @Override
            public void onActionClick(String text) {

                try {
                    File directory = FileUtils.createDirectory(currentDirectory, text);

                    adapter.clearSelection();

                    adapter.add(directory);
                } catch (Exception e) {

                    showMessage(e);
                }
            }
        };

        inputDialog.show();
    }

    private void actionDelete() {

        actionDelete(adapter.getSelectedItems());

        adapter.clearSelection();
    }

    private void actionDelete(final List<File> files) {

        final File sourceDirectory = currentDirectory;

        adapter.removeAll(files);

        String message = String.format("%s files deleted", files.size());

        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setAction("Undo", v -> {

                    if (currentDirectory == null || currentDirectory.equals(sourceDirectory)) {

                        adapter.addAll(files);
                    }
                })
                .addCallback(new Snackbar.Callback() {

                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {

                        if (event != DISMISS_EVENT_ACTION) {

                            try {

                                for (File file : files) FileUtils.deleteFile(file);
                            } catch (Exception e) {

                                showMessage(e);
                            }
                        }

                        super.onDismissed(snackbar, event);
                    }
                })
                .show();
    }

    private void actionRename() {

        final List<File> selectedItems = adapter.getSelectedItems();

        InputDialog inputDialog = new InputDialog(this, "Rename", "Rename") {

            @Override
            public void onActionClick(String text) {

                adapter.clearSelection();

                try {

                    if (selectedItems.size() == 1) {

                        File file = selectedItems.get(0);

                        int index = adapter.indexOf(file);

                        adapter.updateItemAt(index, FileUtils.renameFile(file, text));
                    } else {

                        int size = String.valueOf(selectedItems.size()).length();

                        String format = " (%0" + size + "d)";

                        for (int i = 0; i < selectedItems.size(); i++) {

                            File file = selectedItems.get(i);

                            int index = adapter.indexOf(file);

                            File newFile = FileUtils.renameFile(file, text + String.format(format, i + 1));

                            adapter.updateItemAt(index, newFile);
                        }
                    }
                } catch (Exception e) {

                    showMessage(e);
                }
            }
        };

        if (selectedItems.size() == 1) {

            inputDialog.setDefault(removeExtension(selectedItems.get(0).getName()));
        }

        inputDialog.show();
    }

    private void actionSearch() {

        InputDialog inputDialog = new InputDialog(this, "Search", "Search") {

            @Override
            public void onActionClick(String text) {

                setName(text);
            }
        };

        inputDialog.show();
    }

    private void actionCopy() {

        List<File> selectedItems = adapter.getSelectedItems();

        adapter.clearSelection();

        transferFiles(selectedItems, false);
    }

    private void actionMove() {

        List<File> selectedItems = adapter.getSelectedItems();

        adapter.clearSelection();

        transferFiles(selectedItems, true);
    }

    private void actionSend() {

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);

        intent.setType("*/*");

        ArrayList<Uri> uris = new ArrayList<>();

        for (File file : adapter.getSelectedItems()) {

            if (file.isFile()) uris.add(Uri.fromFile(file));
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        startActivity(intent);
    }

    private void actionSort() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        int checkedItem = PreferenceUtils.getInteger(this, "pref_sort", 0);

        String sorting[] = {"Name", "Last modified", "Size (high to low)"};

        final Context context = this;

        builder.setSingleChoiceItems(sorting, checkedItem, (dialog, which) -> {

            adapter.update(which);

            PreferenceUtils.putInt(context, "pref_sort", which);

            dialog.dismiss();
        });

        builder.setTitle("Sort by");

        builder.show();
    }

    //----------------------------------------------------------------------------------------------

    private void transferFiles(final List<File> files, final Boolean delete) {

        String paste = delete ? "moved" : "copied";

        String message = String.format(Locale.getDefault(), "%d items waiting to be %s", files.size(), paste);

        View.OnClickListener onClickListener = v -> {

            try {

                for (File file : files) {

                    adapter.addAll(FileUtils.copyFile(file, currentDirectory));

                    if (delete) FileUtils.deleteFile(file);
                }
            } catch (Exception e) {

                showMessage(e);
            }
        };

        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_INDEFINITE)
                .setAction("Paste", onClickListener)
                .show();
    }

    private void showMessage(Exception e) {

        showMessage(e.getMessage());
    }

    private void showMessage(String message) {

        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show();
    }

    //----------------------------------------------------------------------------------------------

    private void gotoFeedback() {

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

        builder.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary0));

        builder.build().launchUrl(this, Uri.parse("https://github.com/calintat/Explorer/issues"));
    }

    private void gotoSettings() {

//        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void gotoApplicationSettings() {

        Intent intent = new Intent();

        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);

        intent.setData(Uri.fromParts("package", "com.calintat.explorer", null));

        startActivity(intent);
    }

    private void setPath(File directory) {

        if (!directory.exists()) {

            Toast.makeText(this, "Directory doesn't exist", Toast.LENGTH_SHORT).show();

            return;
        }

        currentDirectory = directory;

        adapter.clear();

        adapter.clearSelection();

        adapter.addAll(FileUtils.getChildren(directory));

        invalidateTitle();
    }

    private void setName(String name) {

        Intent intent = new Intent(this, MainActivity.class);

        intent.putExtra(EXTRA_NAME, name);

        startActivity(intent);
    }

    private void setType(String type) {

        Intent intent = new Intent(this, MainActivity.class);

        intent.putExtra(EXTRA_TYPE, type);

        if (Build.VERSION.SDK_INT >= 21) {

            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }

        startActivity(intent);
    }

    //----------------------------------------------------------------------------------------------

    private final class OnItemClickListener implements axu.ln.ucot.ucot.recycler.OnItemClickListener {

        private final Context context;

        private OnItemClickListener(Context context) {

            this.context = context;
        }

        @Override
        public void onItemClick(int position) {

            final File file = adapter.get(position);

            if (adapter.anySelected()) {

                adapter.toggle(position);

                return;
            }

            if (file.isDirectory()) {

                if (file.canRead()) {

                    setPath(file);
                } else {

                    showMessage("Cannot open directory");
                }
            } else {
                AsyncTask<File, Void, String> execute = new AsyncTask<File, Void, String>() {
                    protected String doInBackground(File... files) {
                        File f = files[0];
                        String result = f.getName();
                        try {
                            int defaultState = 0;
                            Web3j web3 = Web3jFactory.build(new HttpService());
                            if (f.isDirectory()) {
                                return result;
                            }
                            result = md5(f);
                            SharedPreferences sp = getSharedPreferences(getString(R.string.sp_name), MODE_PRIVATE);
                            String txHash = sp.getString(f.getName() + "txHash", null);
                            if (txHash == null) {
                                return result;
                            } else {
                                EthTransaction ethTransaction = web3.ethGetTransactionByHash(txHash.replace("\"", "")).send();
                                Transaction transaction = ethTransaction.getResult();
                                if (transaction == null) {
                                    return result;
                                }
                                String hex = transaction.getInput();
                                if (hex == null) {
                                    return result;
                                }
                                String info = HexUtils.decode(hex);
                                String[] InfoList = info.split(";");
                                String bcMD5 = "";
                                for (String it : InfoList) {
                                    if (it.startsWith(f.getName())) {
                                        bcMD5 = it.substring(it.indexOf(":") + 1);
                                        break;
                                    }
                                }
                                if (bcMD5 == null) {
                                    return result;
                                }
                                return result + ":" + bcMD5;

                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        return result;
                    }


                    protected void onPostExecute(String result) {
                        String LCmd5 = "";
                        String BCmd5 = "";
                        String message="";
                        if (result.indexOf(":")>0)
                        {
                            String [] md5s=result.split(":");
                            LCmd5=md5s[0];
                            BCmd5=md5s[1];
                            message= "md5 of local file:\n" + LCmd5 + "\nmd5 stored in Blockchain:\n" + BCmd5;
                        }
                        else{

                            message= "md5 of local file:\n" + result + "\nUnregistered in Blockchain";
                        }
                        new NDialog(ActivitySecureFile.this)
                                .setTitle("md5 of " + file.getName())
                                .setTitleCenter(false)
                                .setMessageCenter(false)
                                .setMessage(message)
                                .setMessageSize(15)
                                .setButtonCenter(false)
                                .setButtonSize(14)
                                .setCancleable(true)
                                .setPositiveButtonText("Open file")
                                .setNegativeButtonText("Cancle")
                                .setOnConfirmListener(new NDialog.OnConfirmListener() {
                                    @Override
                                    public void onClick(int which) {
                                        if (which == 1) {
                                            if (Intent.ACTION_GET_CONTENT.equals(getIntent().getAction())) {
                                                Intent intent = new Intent();
                                                intent.setDataAndType(Uri.fromFile(file), getMimeType(file));
                                                setResult(Activity.RESULT_OK, intent);
                                                finish();
                                            } else {
                                                try {
                                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                                    intent.setDataAndType(Uri.fromFile(file), getMimeType(file));
                                                    startActivity(intent);
                                                } catch (Exception e) {
                                                    showMessage(String.format("Cannot open %s", getName(file)));
                                                }
                                            }
                                        }
                                    }
                                }).create(NDialog.CONFIRM).show();

                    }
                }.execute(file);


//                if (Intent.ACTION_GET_CONTENT.equals(getIntent().getAction())) {
//
//                    Intent intent = new Intent();
//
//                    intent.setDataAndType(Uri.fromFile(file), getMimeType(file));
//
//                    setResult(Activity.RESULT_OK, intent);
//
//                    finish();
//                }
//                else if (FileType.getFileType(file) == FileType.ZIP) {
//
//                    final ProgressDialog dialog = ProgressDialog.show(context, "", "Unzipping", true);
//
//                    Thread thread = new Thread(() -> {
//
//                        try {
//
//                            setPath(unzip(file));
//
//                            runOnUiThread(dialog::dismiss);
//                        }
//                        catch (Exception e) {
//
//                            showMessage(e);
//                        }
//                    });
//
//                    thread.run();
//                }
//                else {
//
//                    try {
//
//                        Intent intent = new Intent(Intent.ACTION_VIEW);
//
//                        intent.setDataAndType(Uri.fromFile(file), getMimeType(file));
//
//                        startActivity(intent);
//                    }
//                    catch (Exception e) {
//
//                        showMessage(String.format("Cannot open %s", getName(file)));
//                    }
//                }
            }
        }

        @Override
        public boolean onItemLongClick(int position) {

            adapter.toggle(position);

            return true;
        }
    }
}