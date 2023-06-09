package ru.kirddos.exta2dp.modules;

import static ru.kirddos.exta2dp.ConstUtils.*;
import ru.kirddos.exta2dp.R;
import ru.kirddos.exta2dp.SourceCodecType;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.content.Context;
import android.os.Build;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;

import io.github.libxposed.api.XposedContext;
import io.github.libxposed.api.XposedModule;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class BluetoothAppModule extends XposedModule {
    private final String TAG = "BluetoothAppModule";
    private final String BLUETOOTH_PACKAGE = "com.android.bluetooth";

    public BluetoothAppModule(@NonNull XposedContext base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        log(TAG + ": " + param.getProcessName());
    }

    @SuppressLint("DiscouragedPrivateApi")
    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        super.onPackageLoaded(param);
        log("onPackageLoaded: " + param.getPackageName());
        log("main classloader is " + this.getClassLoader());
        log("param classloader is " + param.getClassLoader());
        log("class classloader is " + SourceCodecType.class.getClassLoader());
        log("module apk path: " + this.getPackageCodePath());
        log("pid/tid: " + android.os.Process.myPid() + " " + android.os.Process.myTid());
        log("----------");

        try {
            Class<?> btCodecConfig = param.getClassLoader().loadClass("android.bluetooth.BluetoothCodecConfig");

            Method getCodecName = btCodecConfig.getDeclaredMethod("getCodecName", int.class);
            hookBefore(getCodecName, callback -> {

                int type = callback.getArg(0);
                String name;
                //log(TAG + " BluetoothCodecConfig: getCodecName");
                switch (type) {
                    case SOURCE_CODEC_TYPE_LHDCV2:
                        name = "LHDC V2";
                        break;
                    case SOURCE_CODEC_TYPE_LHDCV3:
                        name = "LHDC V3";
                        break;
                    case SOURCE_CODEC_TYPE_LHDCV5:
                        name = "LHDC V5";
                        break;
                    default:
                        return;
                }
                callback.returnAndSkip(name);
            });
        } catch (ClassNotFoundException | NoSuchMethodException | NullPointerException e) {
            log(TAG + " btCodecConfig: ", e);
        }

        if (!param.getPackageName().equals(BLUETOOTH_PACKAGE) || !param.isFirstPackage()) return;

        log("In Bluetooth!");

        System.loadLibrary("exta2dp");

        try {
            Class<?> a2dpCodecConfig = param.getClassLoader().loadClass("com.android.bluetooth.a2dp.A2dpCodecConfig");
            //Class<?> a2dpNativeInterface = param.getClassLoader().loadClass("com.android.bluetooth.a2dp.A2dpNativeInterface");
            //deoptimize(a2dpCodecConfig.getConstructor(Context.class, a2dpNativeInterface));

            Class<?> vendor = param.getClassLoader().loadClass("com.android.bluetooth.btservice.Vendor");
            Class<?> adapterServiceClass = param.getClassLoader().loadClass("com.android.bluetooth.btservice.AdapterService");

            Class<?> a2dpStateMachine = param.getClassLoader().loadClass("com.android.bluetooth.a2dp.A2dpStateMachine");

            Field splitA2dpEnabled = vendor.getDeclaredField("splitA2dpEnabled");
            splitA2dpEnabled.setAccessible(true);

            Field mVendor = adapterServiceClass.getDeclaredField("mVendor");
            mVendor.setAccessible(true);

            Method processCodecConfigEvent = a2dpStateMachine.getDeclaredMethod("processCodecConfigEvent", BluetoothCodecStatus.class);
            processCodecConfigEvent.setAccessible(true);

            Method getAdapterService = adapterServiceClass.getDeclaredMethod("getAdapterService");
            getAdapterService.setAccessible(true);

            hookBefore(processCodecConfigEvent, callback -> {
                try {
                    Object adapterService = getAdapterService.invoke(null);
                    log(TAG + " processCodecConfigEvent: " + adapterService);
                    Object vendorObj = mVendor.get(adapterService);
                    boolean save = splitA2dpEnabled.getBoolean(vendorObj);
                    log(TAG + " processCodecConfigEvent: splitA2dpEnabled = " + save);
                    splitA2dpEnabled.set(vendorObj, false);
                    log(TAG + " processCodecConfigEvent: splitA2dpEnabled = false, calling original method");
                    callback.invokeOrigin();
                    splitA2dpEnabled.set(vendorObj, save);
                    log(TAG + " processCodecConfigEvent: restore splitA2dpEnabled");
                    callback.returnAndSkip(null);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    log(TAG + " Exception: ", e);
                }
            });


            Method assignCodecConfigPriorities = a2dpCodecConfig.getDeclaredMethod("assignCodecConfigPriorities");
            Field assigned_codec_length = a2dpCodecConfig.getDeclaredField("assigned_codec_length");
            assigned_codec_length.setAccessible(true);


            hookAfter(assignCodecConfigPriorities, callback -> {
                try {
                    BluetoothCodecConfig[] mCodecConfigPriorities = (BluetoothCodecConfig[]) callback.getResult();
                    int pos = mCodecConfigPriorities.length;

                    if (pos >= 9) {
                        log(TAG + " assignCodecConfigPriorities: Your device seems to have LHDC already, handle it differently");
                        return;
                    }

                    BluetoothCodecConfig[] res = new BluetoothCodecConfig[pos + 3]; // LHDC 2, 3/4 and 5 for now


                    System.arraycopy(mCodecConfigPriorities, 0, res, 0, pos);


                    //int basePriority = res[pos - 1].getCodecPriority();

                    int basePriority = res[pos - 2].getCodecPriority();

                    for (int i = 0; i < pos; i++) {
                        if (basePriority < res[i].getCodecPriority()) {
                            basePriority = res[i].getCodecPriority();
                        }
                    }

                    Constructor<BluetoothCodecConfig> newBluetoothCodecConfig = BluetoothCodecConfig.class.getDeclaredConstructor(int.class, int.class, int.class, int.class, int.class, long.class, long.class, long.class, long.class);
                    res[pos++] = newBluetoothCodecConfig.newInstance(SOURCE_CODEC_TYPE_LHDCV2, basePriority + 1, BluetoothCodecConfig.SAMPLE_RATE_NONE, BluetoothCodecConfig.BITS_PER_SAMPLE_NONE, BluetoothCodecConfig.CHANNEL_MODE_NONE, 0 /* codecSpecific1 */, 0 /* codecSpecific2 */, 0 /* codecSpecific3 */, 0 /* codecSpecific4 */);
                    res[pos++] = newBluetoothCodecConfig.newInstance(SOURCE_CODEC_TYPE_LHDCV3, basePriority + 2, BluetoothCodecConfig.SAMPLE_RATE_NONE, BluetoothCodecConfig.BITS_PER_SAMPLE_NONE, BluetoothCodecConfig.CHANNEL_MODE_NONE, 0 /* codecSpecific1 */, 0 /* codecSpecific2 */, 0 /* codecSpecific3 */, 0 /* codecSpecific4 */);
                    res[pos++] = newBluetoothCodecConfig.newInstance(SOURCE_CODEC_TYPE_LHDCV5, basePriority + 3, BluetoothCodecConfig.SAMPLE_RATE_NONE, BluetoothCodecConfig.BITS_PER_SAMPLE_NONE, BluetoothCodecConfig.CHANNEL_MODE_NONE, 0 /* codecSpecific1 */, 0 /* codecSpecific2 */, 0 /* codecSpecific3 */, 0 /* codecSpecific4 */);

                    log(TAG + " assignCodecConfigPriorities: " + Arrays.toString(res));

                    assigned_codec_length.set(callback.getThis(), pos);
                    callback.setResult(res);
                } catch (Exception e) {
                    log(TAG + " Exception: ", e);
                }
            });
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            log(TAG + " Exception: ", e);
        }
        hookServiceStart(param.getClassLoader(), "com.android.bluetooth.cc.CCService");
        hookServiceStart(param.getClassLoader(), "com.android.bluetooth.pc.PCService");
        hookServiceStart(param.getClassLoader(), "com.android.bluetooth.mcp.McpService");
        hookServiceStart(param.getClassLoader(), "com.android.bluetooth.bc.BCService");
        hookServiceStart(param.getClassLoader(), "com.android.bluetooth.acm.AcmService");
        hookServiceStart(param.getClassLoader(), "com.android.bluetooth.ba.BATService");
        //hookServiceStart(param.getClassLoader(), "com.android.bluetooth.lebroadcast.BassClientService");
        try {
            Class<?> adapterService = param.getClassLoader().loadClass("com.android.bluetooth.btservice.AdapterService");
            Class<?> adapterState = param.getClassLoader().loadClass("com.android.bluetooth.btservice.AdapterState");
            Class<?> profileService = param.getClassLoader().loadClass("com.android.bluetooth.btservice.ProfileService");
            Class<?> abstractionLayer = param.getClassLoader().loadClass("com.android.bluetooth.btservice.AbstractionLayer");

            Field mHandler = adapterService.getDeclaredField("mHandler");
            mHandler.setAccessible(true);
            Class <?> mHandlerClass = mHandler.getType();
            Field adapterServiceThis = mHandlerClass.getDeclaredField("this$0");
            adapterServiceThis.setAccessible(true);

            Field mRegisteredProfiles = adapterService.getDeclaredField("mRegisteredProfiles");
            mRegisteredProfiles.setAccessible(true);
            Field mRunningProfiles = adapterService.getDeclaredField("mRunningProfiles");
            mRunningProfiles.setAccessible(true);

            Method startProfileServices = adapterService.getDeclaredMethod("startProfileServices");
            Method processProfileServiceStateChanged = mHandlerClass.getDeclaredMethod("processProfileServiceStateChanged", profileService, int.class);

            Field mAdapterProperties = adapterService.getDeclaredField("mAdapterProperties");
            mAdapterProperties.setAccessible(true);
            Method onBluetoothReady = mAdapterProperties.getType().getDeclaredMethod("onBluetoothReady");
            onBluetoothReady.setAccessible(true);
            Field mAdapterStateMachine = adapterService.getDeclaredField("mAdapterStateMachine");
            mAdapterStateMachine.setAccessible(true);
            Method sendMessage = mAdapterStateMachine.getType().getSuperclass().getDeclaredMethod("sendMessage", int.class);
            sendMessage.setAccessible(true);
            Method updateUuids = adapterService.getDeclaredMethod("updateUuids");
            updateUuids.setAccessible(true);

            Field mVendor = adapterService.getDeclaredField("mVendor");
            mVendor.setAccessible(true);

            Method setBluetoothClassFromConfig = adapterService.getDeclaredMethod("setBluetoothClassFromConfig");
            setBluetoothClassFromConfig.setAccessible(true);
            Method initProfileServices = adapterService.getDeclaredMethod("initProfileServices");
            initProfileServices.setAccessible(true);
            Method getAdapterPropertyNative = adapterService.getDeclaredMethod("getAdapterPropertyNative", int.class);
            getAdapterPropertyNative.setAccessible(true);
            Method fetchWifiState = adapterService.getDeclaredMethod("fetchWifiState");
            fetchWifiState.setAccessible(true);
            Method isVendorIntfEnabled = adapterService.getDeclaredMethod("isVendorIntfEnabled");
            isVendorIntfEnabled.setAccessible(true);
            Method isPowerbackRequired = adapterService.getDeclaredMethod("isPowerbackRequired");
            isPowerbackRequired.setAccessible(true);
            Method setPowerBackoff = mVendor.getType().getDeclaredMethod("setPowerBackoff", boolean.class);
            setPowerBackoff.setAccessible(true);

            Field ioCapsField = abstractionLayer.getDeclaredField("BT_PROPERTY_LOCAL_IO_CAPS");
            ioCapsField.setAccessible(true);
            int BT_PROPERTY_LOCAL_IO_CAPS = ioCapsField.getInt(null);
            Field ioCapsBleField = abstractionLayer.getDeclaredField("BT_PROPERTY_LOCAL_IO_CAPS_BLE");
            ioCapsBleField.setAccessible(true);
            int BT_PROPERTY_LOCAL_IO_CAPS_BLE = ioCapsBleField.getInt(null);

            Field dynamicAudioBufferField = abstractionLayer.getDeclaredField("BT_PROPERTY_LOCAL_IO_CAPS_BLE");
            dynamicAudioBufferField.setAccessible(true);
            int BT_PROPERTY_DYNAMIC_AUDIO_BUFFER = dynamicAudioBufferField.getInt(null);

            Field bredrStartedField = adapterState.getDeclaredField("BREDR_STARTED");
            bredrStartedField.setAccessible(true);

            int BREDR_STARTED = bredrStartedField.getInt(null);

            hookAfter(processProfileServiceStateChanged, callback -> {
                try {
                    Object adapter = adapterServiceThis.get(callback.getThis());
                    ArrayList running = (ArrayList) mRunningProfiles.get(adapter);
                    ArrayList registered = (ArrayList) mRegisteredProfiles.get(adapter);
                    log(TAG + " processProfileServiceStateChanged: state " + callback.getArgs()[1] + ", running: " + running + ", registered: " + registered);
                    log(TAG + " processProfileServiceStateChanged: registered " + registered.size() + " running " + running.size());
                    Class<?> config = param.getClassLoader().loadClass("com.android.bluetooth.btservice.Config");
                    Method getSupportedProfiles = config.getDeclaredMethod("getSupportedProfiles");
                    getSupportedProfiles.setAccessible(true);
                    int amountProfilesSupported = ((Class[])getSupportedProfiles.invoke(null)).length;
                    log(TAG + " processProfileServiceStateChanged: supported " + amountProfilesSupported);
                    if (registered.size() == amountProfilesSupported) {
                        log(TAG + " processProfileServiceStateChanged: registered all supported profiles");
                        if (registered.size() == running.size()) {
                            log(TAG + " processProfileServiceStateChanged: running all supported profiles");
                        } else if (registered.size() == running.size() + 5) {
                            log(TAG + " HOOKED onProfileServiceStateChange() - All profile services started..");


                            onBluetoothReady.invoke(mAdapterProperties.get(adapter));
                            updateUuids.invoke(adapter);
                            setBluetoothClassFromConfig.invoke(adapter);
                            initProfileServices.invoke(adapter);
                            getAdapterPropertyNative.invoke(adapter,BT_PROPERTY_LOCAL_IO_CAPS);//AbstractionLayer.BT_PROPERTY_LOCAL_IO_CAPS;
                            getAdapterPropertyNative.invoke(adapter,BT_PROPERTY_LOCAL_IO_CAPS_BLE);//AbstractionLayer.BT_PROPERTY_LOCAL_IO_CAPS_BLE;
                            getAdapterPropertyNative.invoke(adapter,BT_PROPERTY_DYNAMIC_AUDIO_BUFFER);//AbstractionLayer.BT_PROPERTY_DYNAMIC_AUDIO_BUFFER;

                            sendMessage.invoke(mAdapterStateMachine.get(adapter), BREDR_STARTED);//AdapterState.BREDR_STARTED

                            //update wifi state to lower layers
                            fetchWifiState.invoke(adapter);
                            if ((boolean) isVendorIntfEnabled.invoke(adapter)) {
                                if ((boolean) isPowerbackRequired.invoke(adapter)) {
                                    setPowerBackoff.invoke(mVendor.get(adapter), true);
                                } else {
                                    setPowerBackoff.invoke(mVendor.get(adapter), false);
                                }
                            }
                        } else{
                            log(TAG + " processProfileServiceStateChanged: SOME SUPPORTED PROFILES ARE NOT RUNNING! FIXME, BREDR will fail with timeout");
                        }
                    }
                } catch (IllegalAccessException | ClassNotFoundException |
                         InvocationTargetException | NullPointerException | NoSuchMethodException e) {
                    log(TAG + " Exception: ", e);
                }
            });
            hookAfter(startProfileServices, callback -> {
                log(TAG + " startProfileServices");
                try {
                    log(TAG + " startProfileServices: " + mRunningProfiles.get(callback.getThis()));
                    log(TAG + " startProfileServices: " + mRegisteredProfiles.get(callback.getThis()));
                } catch (IllegalAccessException e) {
                    log(TAG + " Exception: ", e);
                }
            });
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException|
                 IllegalAccessException | NullPointerException /*  | InvocationTargetException*/ e) {
            log(TAG + " Exception: ", e);
        }

    }

    void hookServiceStart(ClassLoader cl, String name) {
        try {
            Class<?> cc = cl.loadClass(name);
            Method ccStart = cc.getDeclaredMethod("start");
            hookBefore(ccStart, callback -> callback.returnAndSkip(false));
        } catch (ClassNotFoundException | NoSuchMethodException e){//| NoSuchFieldException e) {
            log(TAG + " Exception: ", e);
        }
    }

}
