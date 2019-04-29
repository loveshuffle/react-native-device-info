package com.learnium.RNDeviceInfo;

import android.Manifest;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.BatteryManager;
import android.provider.Settings.Secure;
import android.webkit.WebSettings;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.app.ActivityManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.lang.Runtime;
import java.net.NetworkInterface;
import java.math.BigInteger;

import javax.annotation.Nullable;
import java.io.File;


public class RNDeviceModule extends ReactContextBaseJavaModule {

  ReactApplicationContext reactContext;

  WifiInfo wifiInfo;

  public RNDeviceModule(ReactApplicationContext reactContext) {
    super(reactContext);

    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNDeviceInfo";
  }

  private WifiInfo getWifiInfo() {
    if (this.wifiInfo == null) {
      WifiManager manager = (WifiManager) reactContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
      this.wifiInfo = manager.getConnectionInfo();
    }
    return this.wifiInfo;
  }

  private String getCurrentLanguage() {
    Locale current;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      current = getReactApplicationContext().getResources().getConfiguration().getLocales().get(0);
    } else {
      current = getReactApplicationContext().getResources().getConfiguration().locale;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return current.toLanguageTag();
    } else {
      StringBuilder builder = new StringBuilder();
      builder.append(current.getLanguage());
      if (current.getCountry() != null) {
        builder.append("-");
        builder.append(current.getCountry());
      }
      return builder.toString();
    }
  }

  private ArrayList<String> getPreferredLocales() {
    Configuration configuration = getReactApplicationContext().getResources().getConfiguration();
    ArrayList<String> preferred = new ArrayList<>();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      for (int i = 0; i < configuration.getLocales().size(); i++) {
        preferred.add(configuration.getLocales().get(i).getLanguage());
      }
    } else {
      preferred.add(configuration.locale.getLanguage());
    }

    return preferred;
  }

  private String getCurrentCountry() {
    Locale current;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      current = getReactApplicationContext().getResources().getConfiguration().getLocales().get(0);
    } else {
      current = getReactApplicationContext().getResources().getConfiguration().locale;
    }

    return current.getCountry();
  }
  	private String getSimOperatorFun() {
		try {
			TelephonyManager tel = (TelephonyManager) this.reactContext.getSystemService(Context.TELEPHONY_SERVICE);
			String IMSI = tel.getSubscriberId();
			String ProvidersName = "";
			if (IMSI != null) {
				if (IMSI.startsWith("46000") || IMSI.startsWith("46002") || IMSI.startsWith("46007")) {
					ProvidersName = "cmcc";
				} else if (IMSI.startsWith("46001")  || IMSI.startsWith("46006")) {
					ProvidersName = "unicom";
				} else if (IMSI.startsWith("46003")) {
					ProvidersName = "telecom";
				}
				return ProvidersName;
			} else {
				return "unknown";
			}
		} catch (Exception e) {
			//TODO: handle exception
			return "unknown";
		}
  	}
	private String getImei() {
		try {
			TelephonyManager tel = (TelephonyManager) this.reactContext.getSystemService(Context.TELEPHONY_SERVICE);
			return tel.getDeviceId() ;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	private String getImsi() {
		try {
			TelephonyManager tel = (TelephonyManager) this.reactContext.getSystemService(Context.TELEPHONY_SERVICE);
			return tel.getSubscriberId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
    }
	private int getScreenDensityDpi() {
			try{
			WindowManager wm = (WindowManager) this.reactContext.getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics metric = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(metric);
			return metric.densityDpi;
		} catch (Exception e){
			e.printStackTrace();
		}
		return 0;
	}
  private Boolean isEmulator() {
    return Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.MODEL.contains("google_sdk")
        || Build.MODEL.contains("Emulator")
        || Build.MODEL.contains("Android SDK built for x86")
        || Build.MANUFACTURER.contains("Genymotion")
        || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
        || "google_sdk".equals(Build.PRODUCT);
  }

  private Boolean isTablet() {
    int layout = getReactApplicationContext().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
    if (layout != Configuration.SCREENLAYOUT_SIZE_LARGE && layout != Configuration.SCREENLAYOUT_SIZE_XLARGE) {
      return false;
    }

    final DisplayMetrics metrics = getReactApplicationContext().getResources().getDisplayMetrics();
    if (metrics.densityDpi == DisplayMetrics.DENSITY_DEFAULT
            || metrics.densityDpi == DisplayMetrics.DENSITY_HIGH
            || metrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM
            || metrics.densityDpi == DisplayMetrics.DENSITY_TV
            || metrics.densityDpi == DisplayMetrics.DENSITY_XHIGH) {
      return true;
    }
    return false;
  }

  private float fontScale() {
    return getReactApplicationContext().getResources().getConfiguration().fontScale;
  }

  private Boolean is24Hour() {
    return android.text.format.DateFormat.is24HourFormat(this.reactContext.getApplicationContext());
  }

  @ReactMethod
  public void isPinOrFingerprintSet(Callback callback) {
    KeyguardManager keyguardManager = (KeyguardManager) this.reactContext.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE); //api 16+
    callback.invoke(keyguardManager.isKeyguardSecure());
  }

  @ReactMethod
  public void getIpAddress(Promise p) {
    String ipAddress = Formatter.formatIpAddress(getWifiInfo().getIpAddress());
    p.resolve(ipAddress);
  }

  @ReactMethod
  public void getMacAddress(Promise p) {
    String macAddress = getWifiInfo().getMacAddress();

    String permission = "android.permission.INTERNET";
    int res = this.reactContext.checkCallingOrSelfPermission(permission);

    if (res == PackageManager.PERMISSION_GRANTED) {
      try {
        List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface nif : all) {
          if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

          byte[] macBytes = nif.getHardwareAddress();
          if (macBytes == null) {
              macAddress = "";
          } else {

            StringBuilder res1 = new StringBuilder();
            for (byte b : macBytes) {
                res1.append(String.format("%02X:",b));
            }

            if (res1.length() > 0) {
                res1.deleteCharAt(res1.length() - 1);
            }

            macAddress = res1.toString();
          }
        }
      } catch (Exception ex) {
      }
    }

    p.resolve(macAddress);    
  }

  @ReactMethod
  public String getCarrier() {
    TelephonyManager telMgr = (TelephonyManager) this.reactContext.getSystemService(Context.TELEPHONY_SERVICE);
    return telMgr.getNetworkOperatorName();
  }

  @ReactMethod
//   public Integer getTotalDiskCapacity() {
//     try {
//       StatFs root = new StatFs(Environment.getRootDirectory().getAbsolutePath());
//       return root.getBlockCount() * root.getBlockSize();
//     } catch (Exception e) {
//       e.printStackTrace();
//     }
//     return null;
//   }
    public long getTotalDiskCapacity(){
        /*获取存储卡路径*/
        File sdcardDir= Environment.getExternalStorageDirectory();
        /*StatFs 看文件系统空间使用情况*/
        StatFs statFs=new StatFs(sdcardDir.getPath());

        long blockSize = 0;
        long totalSize = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          blockSize= statFs.getBlockSizeLong();
          totalSize=statFs.getBlockCountLong();
        } else {
           blockSize= statFs.getBlockSize();
           totalSize=statFs.getBlockCount();
        }
    
    
        return blockSize*totalSize;
    }

  @ReactMethod
//   public Integer getFreeDiskStorage() {
//     try {
//       StatFs external = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
//       return external.getAvailableBlocks() * external.getBlockSize();
//     } catch (Exception e) {
//       e.printStackTrace();
//     }
//     return null;
//   }
    public long getFreeDiskStorage(){
        File path=Environment.getExternalStorageDirectory();
        StatFs statFs=new StatFs(path.getPath());
        long blockSize=0;
        long availableBlocks = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          blockSize= statFs.getBlockSizeLong();
           availableBlocks=statFs.getAvailableBlocksLong();
        } else {
           blockSize= statFs.getBlockSize();
          availableBlocks=statFs.getAvailableBlocks();
        }
        return blockSize*availableBlocks;
    }
  @ReactMethod
  public void isBatteryCharging(Promise p){
    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    Intent batteryStatus = this.reactContext.getApplicationContext().registerReceiver(null, ifilter);
    int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
    boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
    p.resolve(isCharging);
  }

  @ReactMethod
  public void getBatteryLevel(Promise p) {
    Intent batteryIntent = this.reactContext.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
    float batteryLevel = level / (float) scale;
    p.resolve(batteryLevel);
  }

  @ReactMethod
  public void isAirPlaneMode(Promise p) {
    boolean isAirPlaneMode;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
        isAirPlaneMode = Settings.System.getInt(this.reactContext.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    } else {
        isAirPlaneMode = Settings.Global.getInt(this.reactContext.getContentResolver(),Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }
    p.resolve(isAirPlaneMode);
  }

  @ReactMethod
  public void isAutoDateAndTime(Promise p) {
    boolean isAutoDateAndTime;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      isAutoDateAndTime = Settings.System.getInt(this.reactContext.getContentResolver(),Settings.System.AUTO_TIME, 0) != 0;
    } else {
      isAutoDateAndTime = Settings.Global.getInt(this.reactContext.getContentResolver(),Settings.Global.AUTO_TIME, 0) != 0;
    }
    p.resolve(isAutoDateAndTime);
  }

  @ReactMethod
  public void isAutoTimeZone(Promise p) {
    boolean isAutoTimeZone;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      isAutoTimeZone = Settings.System.getInt(this.reactContext.getContentResolver(),Settings.System.AUTO_TIME_ZONE, 0) != 0;
    } else {
      isAutoTimeZone = Settings.Global.getInt(this.reactContext.getContentResolver(),Settings.Global.AUTO_TIME_ZONE, 0) != 0;
    }
    p.resolve(isAutoTimeZone);
  }

  public String getInstallReferrer() {
    SharedPreferences sharedPref = getReactApplicationContext().getSharedPreferences("react-native-device-info", Context.MODE_PRIVATE);
    return sharedPref.getString("installReferrer", null);
  }

  @Override
  public @Nullable
  Map<String, Object> getConstants() {
    HashMap<String, Object> constants = new HashMap<String, Object>();

    PackageManager packageManager = this.reactContext.getPackageManager();
    String packageName = this.reactContext.getPackageName();

    constants.put("appVersion", "not available");
    constants.put("appName", "not available");
    constants.put("buildVersion", "not available");
    constants.put("buildNumber", 0);

    try {
      PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
      PackageInfo info = packageManager.getPackageInfo(packageName, 0);
      String applicationName = this.reactContext.getApplicationInfo().loadLabel(this.reactContext.getPackageManager()).toString();
      constants.put("appVersion", info.versionName);
      constants.put("buildNumber", info.versionCode);
      constants.put("firstInstallTime", info.firstInstallTime);
      constants.put("lastUpdateTime", info.lastUpdateTime);
      constants.put("appName", applicationName);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    String deviceName = "Unknown";

    String permission = "android.permission.BLUETOOTH";
    int res = this.reactContext.checkCallingOrSelfPermission(permission);
    if (res == PackageManager.PERMISSION_GRANTED) {
      try {
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        if (myDevice != null) {
          deviceName = myDevice.getName();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }


    try {
      if (Class.forName("com.google.android.gms.iid.InstanceID") != null) {
        constants.put("instanceId", com.google.android.gms.iid.InstanceID.getInstance(this.reactContext).getId());
      }
    } catch (ClassNotFoundException e) {
      constants.put("instanceId", "N/A: Add com.google.android.gms:play-services-gcm to your project.");
    }
    constants.put("serialNumber", Build.SERIAL);
    constants.put("deviceName", deviceName);
    constants.put("systemName", "Android");
    constants.put("systemVersion", Build.VERSION.RELEASE);
    constants.put("model", Build.MODEL);
    constants.put("brand", Build.BRAND);
    constants.put("buildId", Build.ID);
    constants.put("deviceId", Build.BOARD);
    constants.put("apiLevel", Build.VERSION.SDK_INT);
    constants.put("deviceLocale", this.getCurrentLanguage());
    constants.put("preferredLocales", this.getPreferredLocales());
    constants.put("deviceCountry", this.getCurrentCountry());
    constants.put("uniqueId", Settings.Secure.getString(this.reactContext.getContentResolver(), Settings.Secure.ANDROID_ID));
    constants.put("systemManufacturer", Build.MANUFACTURER);
    constants.put("bundleId", packageName);
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        constants.put("userAgent", WebSettings.getDefaultUserAgent(this.reactContext));
      } else {
        constants.put("userAgent", System.getProperty("http.agent"));
      }
    } catch (RuntimeException e) {
      constants.put("userAgent", System.getProperty("http.agent"));
    }
    constants.put("timezone", TimeZone.getDefault().getID());
    constants.put("isEmulator", this.isEmulator());
    constants.put("isTablet", this.isTablet());
    constants.put("fontScale", this.fontScale());
    constants.put("is24Hour", this.is24Hour());
    constants.put("carrier", this.getCarrier());
    constants.put("totalDiskCapacity", this.getTotalDiskCapacity());
    constants.put("freeDiskStorage", this.getFreeDiskStorage());
    constants.put("installReferrer", this.getInstallReferrer());

    if (reactContext != null &&
         (reactContext.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
           (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && reactContext.checkCallingOrSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) ||
           (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && reactContext.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED))) {
      TelephonyManager telMgr = (TelephonyManager) this.reactContext.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
      constants.put("phoneNumber", telMgr.getLine1Number());
    } else {
      constants.put("phoneNumber", null);
    }

    Runtime rt = Runtime.getRuntime();
    constants.put("maxMemory", rt.maxMemory());
    ActivityManager actMgr = (ActivityManager) this.reactContext.getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
    actMgr.getMemoryInfo(memInfo);
    constants.put("totalMemory", memInfo.totalMem);
    constants.put("deviceType", deviceType.getValue());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      constants.put("supportedABIs", Build.SUPPORTED_ABIS);
    } else {
      constants.put("supportedABIs", new String[]{ Build.CPU_ABI });
    }
    return constants;
  }
}
