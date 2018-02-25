package axu.ln.ucot.ucot;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

public class ActivitySecurePhoto extends AppCompatActivity implements ServiceCallbacks {
    public void callBack(String content) {
        updateUI(content);
    }

    public interface updateUI {
        void updateUI(String content);
    }

    private String photoAddress = null;
    private ListView lv;
    private String tag = "Ucot";
    private Handler mainHandler = null;
    private String[] Taglist = {"FNumber", "ApertureValue", "Artist", "BitsPerSample", "BrightnessValue", "CFAPattern", "ColorSpace", "ComponentsConfiguration", "CompressedBitsPerPixel", "Compression", "Contrast", "Copyright", "CustomRendered", "DateTime", "DateTimeDigitized", "DateTimeOriginal", "DefaultCropSize", "DeviceSettingDescription", "DigitalZoomRatio", "DNGVersion", "ExifVersion", "ExposureBiasValue", "ExposureIndex", "ExposureMode", "ExposureProgram", "ExposureTime", "FileSource", "Flash", "FlashpixVersion", "FlashEnergy", "FocalLength", "FocalLengthIn35mmFilm", "FocalPlaneResolutionUnit", "FocalPlaneXResolution", "FocalPlaneYResolution", "GainControl", "GPSAltitude", "GPSAltitudeRef", "GPSAreaInformation", "GPSDateStamp", "GPSDestBearing", "GPSDestBearingRef", "GPSDestDistance", "GPSDestDistanceRef", "GPSDestLatitude", "GPSDestLatitudeRef", "GPSDestLongitude", "GPSDestLongitudeRef", "GPSDifferential", "GPSDOP", "GPSImgDirection", "GPSImgDirectionRef", "GPSLatitude", "GPSLatitudeRef", "GPSLongitude", "GPSLongitudeRef", "GPSMapDatum", "GPSMeasureMode", "GPSProcessingMethod", "GPSSatellites", "GPSSpeed", "GPSSpeedRef", "GPSStatus", "GPSTimeStamp", "GPSTrack", "GPSTrackRef", "GPSVersionID", "ImageDescription", "ImageLength", "ImageUniqueID", "ImageWidth", "InteroperabilityIndex", "ISOSpeedRatings", "ISOSpeedRatings", "JPEGInterchangeFormat", "JPEGInterchangeFormatLength", "LightSource", "Make", "MakerNote", "MaxApertureValue", "MeteringMode", "Model", "NewSubfileType", "OECF", "AspectFrame", "PreviewImageLength", "PreviewImageStart", "ThumbnailImage", "Orientation", "PhotometricInterpretation", "PixelXDimension", "PixelYDimension", "PlanarConfiguration", "PrimaryChromaticities", "ReferenceBlackWhite", "RelatedSoundFile", "ResolutionUnit", "RowsPerStrip", "ISO", "JpgFromRaw", "SensorBottomBorder", "SensorLeftBorder", "SensorRightBorder", "SensorTopBorder", "SamplesPerPixel", "Saturation", "SceneCaptureType", "SceneType", "SensingMethod", "Sharpness", "ShutterSpeedValue", "Software", "SpatialFrequencyResponse", "SpectralSensitivity", "StripByteCounts", "StripOffsets", "SubfileType", "SubjectArea", "SubjectDistance", "SubjectDistanceRange", "SubjectLocation", "SubSecTime", "SubSecTimeDigitized", "SubSecTimeDigitized", "SubSecTimeOriginal", "SubSecTimeOriginal", "ThumbnailImageLength", "ThumbnailImageWidth", "TransferFunction", "UserComment", "WhiteBalance", "WhitePoint", "XResolution", "YCbCrCoefficients", "YCbCrPositioning", "YCbCrSubSampling", "YResolution"};
    private String InfoTobeSave = null;
    private boolean sentTX = false;
    private boolean readTX = false;
    private ImageView iv;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    lv.setVisibility(View.INVISIBLE);
                    iv.setVisibility(View.VISIBLE);
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, 1);
                    return true;
                case R.id.navigation_dashboard:
//                    sendCommand("eth.accounts[0]");
                    if (photoAddress == null) {
                        Toast.makeText(getApplicationContext(), "Please select the photo first.", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    ShowExif(photoAddress, lv);
                    return true;
                case R.id.navigation_notifications:
                    if (photoAddress == null) {
                        Toast.makeText(getApplicationContext(), "Please select the photo first.", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    SharedPreferences sp = getSharedPreferences(getString(R.string.sp_name), MODE_PRIVATE);
                    String txHash = sp.getString(photoAddress, null);
                    if (txHash == null) {
                        Toast.makeText(getApplicationContext(), "Please secure the photo first.", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    validation(photoAddress);
                    return true;
            }
            return false;
        }
    };
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
            lnService.setCallbacks(ActivitySecurePhoto.this);
            Log.i(tag, "Bound true");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.i(tag, "Bound false");
        }
    };

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
            Intent startIntent = new Intent(ActivitySecurePhoto.this, LNService.class);
            ActivitySecurePhoto.this.startService(startIntent);
            bindService(startIntent, mConnection, BIND_AUTO_CREATE);
            return;
        }

    }

    protected void onResume() {
        super.onResume();
        Log.i(tag, "Activity sp resume");
        Intent startIntent = new Intent(ActivitySecurePhoto.this, LNService.class);
        ActivitySecurePhoto.this.startService(startIntent);
        bindService(startIntent, mConnection, BIND_AUTO_CREATE);
    }


    protected void onStop() {
        super.onStop();
        Log.i(tag, "Activity sp onStop");
//        if (mBound) {
//            Log.i(tag, "Unbind in Stop");
//            lnService.setCallbacks(null);
//            unbindService(mConnection);
//            mBound = false;
//        }
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

    protected void onDestroy() {
        super.onDestroy();
        Log.i(tag, "Activity sp onDestroy");
//        if (mBound) {
//            Log.i(tag, "Unbind in destroy");
//            lnService.setCallbacks(null);
//            unbindService(mConnection);
//            mBound = false;
//        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secure_photo);
        lv = (ListView) findViewById(R.id.lv);
        iv = (ImageView) findViewById(R.id.imageView);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 2: {
                        {
                            SharedPreferences sp = getSharedPreferences(getString(R.string.sp_name), MODE_PRIVATE);
                            SharedPreferences.Editor sped = sp.edit();
                            sped.putString(photoAddress, msg.obj.toString());
                            sped.commit();
                            Toast.makeText(getApplicationContext(),"TX hash:"+msg.obj.toString(),Toast.LENGTH_SHORT).show();

                        }
                        break;
                    }
                    case 3: {
//                        String tmp="P80205-131204.jpg;5159c4b0b313c36686ceea2e91370123;dbfa5ad43dbc36d829d925da1c1bfb3d;2.2;;;12;;;;;;;;;;2018:02:05 13:12:05;2018:02:05 13:12:05;;;;;;;;;;;0.0026;;0;;;473/100;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;5248;;3936;;110;110;;;255;Meizu;;;2;MX5;;;;;;;1;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;13;13;13;13;13;;;;;0;;;;;;;";
                        String tmp = msg.obj.toString();
                        String[] tmpL = tmp.split(";");
                        try {
                            String rawHash = md5(photoAddress);
                            String imageHash = removeEXIF(photoAddress);
                            InfoTobeSave = photoAddress.substring(photoAddress.lastIndexOf("/") + 1) + ";";
                            InfoTobeSave = InfoTobeSave + rawHash + ";";
                            InfoTobeSave = InfoTobeSave + imageHash + ";";
                            ExifInterface exif = new ExifInterface(photoAddress);
                            ArrayList<String> listings = new ArrayList<String>();
                            String[] tpL = {photoAddress.substring(photoAddress.lastIndexOf("/") + 1), rawHash, imageHash};
                            String[] nameL = {"Name", "File Hash", "Image Hash"};
                            for (int i = 0; i < tpL.length; i++) {
                                if (tpL[i].equals(tmpL[i])) {
                                    listings.add(nameL[i] + " :\nL:" + tpL[i] + "\nB:" + tmpL[i]);
                                } else {
                                    listings.add("---------" + nameL[i] + "---------:\nL:" + tpL[i] + "\nB:" + tmpL[i]);
                                }
                            }
//                            listings.add("Name:\nL:" + photoAddress.substring(photoAddress.lastIndexOf("/") + 1) + "\nB:" + tmpL[0]);
//                            listings.add("File hash:\nL:" + rawHash + "\nB:" + tmpL[1]);
//                            listings.add("Image hash:\nL:" + imageHash + "\nB:" + tmpL[2]);
                            for (int i = 0; i < Taglist.length; i++) {
                                if (exif.getAttribute(Taglist[i]) != null) {
                                    String exifTag = exif.getAttribute(Taglist[i]);
                                    String bcTag = tmpL[i + 3];
                                    if (exifTag.equals(bcTag)) {
                                        listings.add(Taglist[i] + " :\nL:" + exif.getAttribute(Taglist[i]) + "\nB:" + tmpL[i + 3]);
                                    } else {
                                        listings.add("---------" + Taglist[i] + "---------:\nL:" + exif.getAttribute(Taglist[i]) + "\nB:" + tmpL[i + 3]);
                                    }
                                    InfoTobeSave = InfoTobeSave + exif.getAttribute(Taglist[i]) + ";";
                                    continue;
                                } else if (i + 3 < tmpL.length) {
                                    if (!tmpL[i + 3].equals("")) {
                                        listings.add("---------" + Taglist[i] + "---------:\nL:\nB:" + tmpL[i + 3]);
                                    }
                                } else {
                                    InfoTobeSave = InfoTobeSave + ";";
                                }
                            }
                            Log.i(tag, InfoTobeSave);
                            lv.setVisibility(View.VISIBLE);
                            iv.setVisibility(View.INVISIBLE);
                            lv.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, listings));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;
                    }

                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (data != null) {
                Uri uri = data.getData();
                photoAddress = UriToPathUtil.getImageAbsolutePath(getApplicationContext(), uri);
                iv.setImageURI(data.getData());
                setTitle(photoAddress.substring(photoAddress.lastIndexOf("/") + 1));
//                mTextMessage.setText(photoAddress.substring(photoAddress.lastIndexOf("/") + 1));
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void validation(String filepath) {
        SharedPreferences sp = getSharedPreferences(getString(R.string.sp_name), MODE_PRIVATE);
        String txHash = sp.getString(photoAddress, null);
        if (txHash == null) {
            return;
        }
        sendCommand(" eth.getTransaction(" + txHash.trim() + ")");
        readTX = true;
    }

    public void ShowExif(String filepath, ListView lv) {
        try {
            String rawHash = md5(filepath);
            String imageHash = removeEXIF(filepath);
            InfoTobeSave = filepath.substring(filepath.lastIndexOf("/") + 1) + ";";
            InfoTobeSave = InfoTobeSave + rawHash + ";";
            InfoTobeSave = InfoTobeSave + imageHash + ";";
            ExifInterface exif = new ExifInterface(filepath);
            ArrayList<String> listings = new ArrayList<String>();
            listings.add("Name:\n" + filepath);
            listings.add("File hash:\n" + rawHash);
            listings.add("Image hash:\n" + imageHash);
            for (int i = 0; i < Taglist.length; i++) {
                if (exif.getAttribute(Taglist[i]) != null) {
                    listings.add(getTagString(Taglist[i], exif));
                    InfoTobeSave = InfoTobeSave + exif.getAttribute(Taglist[i]) + ";";
                } else {
                    InfoTobeSave = InfoTobeSave + ";";
                }
            }
            Log.i(tag, InfoTobeSave);
            SharedPreferences sp = getSharedPreferences(getString(R.string.sp_name), MODE_PRIVATE);
            String address = sp.getString(getString(R.string.sp_address), null);
            String tx = "eth.sendTransaction({from:\"0x" + address + "\",to:\"0x" + getString(R.string.bc_server_account) + "\",value:web3.toWei(1,\"ether\"),data:web3.toHex(\"" + InfoTobeSave + "\")})";
//            String tp="tst";
//            String tx = "eth.sendTransaction({from:\"0x" + address + "\",to:\"0x" + getString(R.string.bc_server_account) + "\",value:web3.toWei(1,\"ether\"),data:web3.toHex(\"" + tp + "\")})";
            Log.i(tag, "sendtx");
//            sendCommand(tx);
            sentTX = true;
            lv.setVisibility(View.VISIBLE);
            iv.setVisibility(View.INVISIBLE);
            lv.setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, listings));
            showSimpleDialog(tx);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String getTagString(String tag, ExifInterface exif) {

        return (tag + " :\n" + exif.getAttribute(tag));
    }

    private String getSecuredInformation(String txHash) {
        return null;
    }

    public static String md5(String path) {
        File file = new File(path);
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

    public String removeEXIF(String filepath) {
        String newMD5 = null;
        try {
            String newfilename = filepath.substring(0, filepath.lastIndexOf(".")) + "_SCRUBBED.jpg";
            byte FileStart[] = new byte[]{(byte) 0xff, (byte) 0xd8};
            byte ExifStart[] = new byte[]{(byte) 0xff, (byte) 0xe1};
            InputStream in = new FileInputStream(new File(filepath));
            byte[] buf = new byte[6];
            int len, offset = 0;
            if ((len = in.read(buf)) == 6) {

                byte[] tmp = Arrays.copyOfRange(buf, 0, 2);
                if (!Arrays.equals(tmp, FileStart)) {
                    Log.i("tag", "unsupport formate");
                    Toast.makeText(this, "Unsupport format", Toast.LENGTH_SHORT).show();
                    return null;
                }
                tmp = Arrays.copyOfRange(buf, 2, 4);
                if (Arrays.equals(tmp, ExifStart)) {
                    offset = byte2ArrayToInt(Arrays.copyOfRange(buf, 4, 6)) + 4;
                    Log.i("tag", offset + "");
                }
                if (offset == 0) {
                    Log.i("tag", "no Exif");
                    Toast.makeText(this, "No Exif", Toast.LENGTH_SHORT).show();
                    return null;
                } else {
                    File newFile = new File(newfilename);
                    OutputStream out = new FileOutputStream(newFile);
                    buf = new byte[1024 * 5];
                    Log.i("tag", "image start from " + offset);
                    out.write(FileStart);
                    in.skip(offset - 6);
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf);
                    }
                    out.close();
                    newMD5 = md5(newfilename);
                    newFile.delete();
                }
            }
            in.close();
//            Toast.makeText(this, "Exif removed", Toast.LENGTH_SHORT).show();
            return newMD5;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public int byte2ArrayToInt(byte[] b) {
        return b[1] & 0xFF |
                (b[0] & 0xFF) << 8;
    }

    private String HextoString(String hexString) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hexString.length(); i += 2) {
            String str = hexString.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }
    private void showSimpleDialog(final String content) {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle("Secure photo");
        builder.setMessage("Ready to secure the photo?");

        builder.setPositiveButton("Ready", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sendCommand(content);
//                Toast.makeText(getApplicationContext(),"red",Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hold on", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//                Toast.makeText(getApplicationContext(), "hold", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setCancelable(true);
        AlertDialog dialog=builder.create();
        dialog.show();
    }

}

