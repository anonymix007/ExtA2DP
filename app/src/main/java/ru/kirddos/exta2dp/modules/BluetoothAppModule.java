package ru.kirddos.exta2dp.modules;

import static ru.kirddos.exta2dp.ConstUtils.*;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import io.github.libxposed.api.XposedModule;

import ru.kirddos.exta2dp.SourceCodecType;

import androidx.annotation.NonNull;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;


public class BluetoothAppModule extends XposedModule {
    private static final String TAG = "BluetoothAppModule";
    private static final String BLUETOOTH_PACKAGE = "com.android.bluetooth";

    public BluetoothAppModule(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        log(TAG + " : " + param.getProcessName());
    }

    @XposedHooker
    static class BluetoothHooker implements Hooker {
        static ConcurrentHashMap<Member, ArrayList<BeforeCallback>> beforeCallbacks = new ConcurrentHashMap<>();
        static ConcurrentHashMap<Member, ArrayList<AfterCallback>> afterCallbacks = new ConcurrentHashMap<>();

        @BeforeInvocation
        public static Hooker before(BeforeHookCallback callback) {
            ArrayList<BeforeCallback> abc =  beforeCallbacks.get(callback.getMember());
            if (abc == null) return null;
            Hooker result = null;
            for (var bc: abc) {
                result = bc.before(callback);
            }
            return result;
        }

        @AfterInvocation
        public static void after(AfterHookCallback callback, Hooker state) {
            ArrayList<AfterCallback> aac = afterCallbacks.get(callback.getMember());
            if (aac == null) return;
            for (var ac: aac) {
                ac.after(callback, state);
            }
        }
    }

    void hookBefore(Method method, BeforeCallback callback) {
        var abc = BluetoothHooker.beforeCallbacks.get(method);
        if (abc == null) {
            hook(method, BluetoothHooker.class);
            abc = new ArrayList<>();
            abc.add(callback);
            BluetoothHooker.beforeCallbacks.put(method, abc);
        } else {
            abc.add(callback);
        }
    }

    void hookAfter(Method method, AfterCallback callback) {
        var aac = BluetoothHooker.afterCallbacks.get(method);
        if (aac == null) {
            hook(method, BluetoothHooker.class);
            aac = new ArrayList<>();
            aac.add(callback);
            BluetoothHooker.afterCallbacks.put(method, aac);
        } else {
            aac.add(callback);
        }
    }


    @SuppressLint("NewApi")
    protected static final int[] CODEC_IDS = {
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3,
            SOURCE_CODEC_TYPE_OPUS,
            SOURCE_CODEC_TYPE_APTX_ADAPTIVE,
            SOURCE_CODEC_TYPE_APTX_TWSP,
            SOURCE_CODEC_TYPE_LHDCV3,
            SOURCE_CODEC_TYPE_LHDCV2,
            SOURCE_CODEC_TYPE_LHDCV5,
            SOURCE_CODEC_TYPE_LC3PLUS_HR,
            SOURCE_CODEC_TYPE_FLAC
    };

    @SuppressLint({"DiscouragedPrivateApi", "BlockedPrivateApi", "PrivateApi", "NewApi"})
    @SuppressWarnings({"NullableProblems", "ConstantConditions"})
    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        super.onPackageLoaded(param);
        log("onPackageLoaded: " + param.getPackageName());
        log("main classloader is " + this.getClass().getClassLoader());
        log("param classloader is " + param.getClassLoader());
        log("class classloader is " + SourceCodecType.class.getClassLoader());
        log("pid/tid: " + android.os.Process.myPid() + " " + android.os.Process.myTid());
        log("----------");


        if (!param.getPackageName().equals(BLUETOOTH_PACKAGE) || !param.isFirstPackage()) return;

        log("In Bluetooth!");

        System.loadLibrary("exta2dp");

        setCodecIds(CODEC_IDS);

        try {
            Class<?> btCodecConfig = param.getClassLoader().loadClass("android.bluetooth.BluetoothCodecConfig");

            Method sameCodecSpecificParameters = btCodecConfig.getDeclaredMethod("sameCodecSpecificParameters", btCodecConfig);

            hookAfter(sameCodecSpecificParameters, (callback, state) -> {
                try {
                    boolean same = (boolean) callback.getResult();
                    if (same) {
                        BluetoothCodecConfig thiz = (BluetoothCodecConfig) callback.getThisObject();
                        BluetoothCodecConfig other = (BluetoothCodecConfig) callback.getArgs()[0];
                        int type = thiz.getCodecType();
                        if (type == SOURCE_CODEC_TYPE_LHDCV2 || type == SOURCE_CODEC_TYPE_LHDCV3 || type == SOURCE_CODEC_TYPE_LHDCV5 || type == SOURCE_CODEC_TYPE_LC3PLUS_HR || type == SOURCE_CODEC_TYPE_FLAC) {
                            if (thiz.getCodecSpecific1() != other.getCodecSpecific1() ||
                                    thiz.getCodecSpecific2() != other.getCodecSpecific2() ||
                                    thiz.getCodecSpecific3() != other.getCodecSpecific3() ||
                                    thiz.getCodecSpecific4() != other.getCodecSpecific4()) {
                                callback.setResult(false);
                            }
                        }
                    }
                } catch (NullPointerException e) {
                    log(TAG + " Exception: ", e);
                }
            });
        } catch (ClassNotFoundException | NoSuchMethodException | NullPointerException e) {
            log(TAG + " btCodecConfig: ", e);
        }

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
                    BluetoothCodecStatus newCodecStatus = (BluetoothCodecStatus) callback.getArgs()[0];
                    int new_codec_type = newCodecStatus.getCodecConfig().getCodecType();
                    if (new_codec_type >= SOURCE_CODEC_TYPE_LHDCV2) {
                        Object adapterService = getAdapterService.invoke(null);
                        log(TAG + " processCodecConfigEvent: " + adapterService);
                        Object vendorObj = mVendor.get(adapterService);
                        boolean save = splitA2dpEnabled.getBoolean(vendorObj);
                        log(TAG + " processCodecConfigEvent: splitA2dpEnabled = " + save);
                        splitA2dpEnabled.set(vendorObj, false);
                        log(TAG + " processCodecConfigEvent: splitA2dpEnabled = false, calling original method");
                        Method origin = (Method) callback.getMember();
                        invokeOrigin(origin, callback.getThisObject(), newCodecStatus);
                        splitA2dpEnabled.set(vendorObj, save);
                        log(TAG + " processCodecConfigEvent: restore splitA2dpEnabled");
                        callback.returnAndSkip(null);
                    }
                } catch (InvocationTargetException | IllegalAccessException e) {
                    log(TAG + " Exception: ", e);
                }
                return null;
            });


            Method assignCodecConfigPriorities = a2dpCodecConfig.getDeclaredMethod("assignCodecConfigPriorities");
            Field assigned_codec_length = a2dpCodecConfig.getDeclaredField("assigned_codec_length");
            assigned_codec_length.setAccessible(true);


            hookAfter(assignCodecConfigPriorities, (callback, state) -> {
                try {
                    BluetoothCodecConfig[] mCodecConfigPriorities = (BluetoothCodecConfig[]) callback.getResult();
                    int pos = mCodecConfigPriorities.length;

                    if (pos >= 9) {
                        log(TAG + " assignCodecConfigPriorities: Your device seems to have LHDC already, maybe we should handle it differently?");
                        //return;
                    }

                    BluetoothCodecConfig[] res = new BluetoothCodecConfig[pos + 5]; // LHDC 2, 3/4, 5 and LC3plus HR/FLAC for now

                    System.arraycopy(mCodecConfigPriorities, 0, res, 0, pos);


                    //int basePriority = res[pos - 1].getCodecPriority();

                    int basePriority = res[pos - 2].getCodecPriority();

                    for (int i = 0; i < pos; i++) {
                        if (basePriority < res[i].getCodecPriority()) {
                            basePriority = res[i].getCodecPriority();
                        }
                    }

                    //noinspection JavaReflectionMemberAccess
                    Constructor<BluetoothCodecConfig> newBluetoothCodecConfig = BluetoothCodecConfig.class.getDeclaredConstructor(int.class, int.class, int.class, int.class, int.class, long.class, long.class, long.class, long.class);
                    res[pos++] = newBluetoothCodecConfig.newInstance(SOURCE_CODEC_TYPE_LHDCV2, basePriority + 1, BluetoothCodecConfig.SAMPLE_RATE_NONE, BluetoothCodecConfig.BITS_PER_SAMPLE_NONE, BluetoothCodecConfig.CHANNEL_MODE_NONE, 0 /* codecSpecific1 */, 0 /* codecSpecific2 */, 0 /* codecSpecific3 */, 0 /* codecSpecific4 */);
                    res[pos++] = newBluetoothCodecConfig.newInstance(SOURCE_CODEC_TYPE_LHDCV3, basePriority + 2, BluetoothCodecConfig.SAMPLE_RATE_NONE, BluetoothCodecConfig.BITS_PER_SAMPLE_NONE, BluetoothCodecConfig.CHANNEL_MODE_NONE, 0 /* codecSpecific1 */, 0 /* codecSpecific2 */, 0 /* codecSpecific3 */, 0 /* codecSpecific4 */);
                    res[pos++] = newBluetoothCodecConfig.newInstance(SOURCE_CODEC_TYPE_LHDCV5, basePriority + 3, BluetoothCodecConfig.SAMPLE_RATE_NONE, BluetoothCodecConfig.BITS_PER_SAMPLE_NONE, BluetoothCodecConfig.CHANNEL_MODE_NONE, 0 /* codecSpecific1 */, 0 /* codecSpecific2 */, 0 /* codecSpecific3 */, 0 /* codecSpecific4 */);
                    res[pos++] = newBluetoothCodecConfig.newInstance(SOURCE_CODEC_TYPE_LC3PLUS_HR, basePriority + 4 /* -1 */, BluetoothCodecConfig.SAMPLE_RATE_NONE, BluetoothCodecConfig.BITS_PER_SAMPLE_NONE, BluetoothCodecConfig.CHANNEL_MODE_NONE, 0 /* codecSpecific1 */, 0 /* codecSpecific2 */, 0 /* codecSpecific3 */, 0 /* codecSpecific4 */);
                    res[pos++] = newBluetoothCodecConfig.newInstance(SOURCE_CODEC_TYPE_FLAC, basePriority + 5, BluetoothCodecConfig.SAMPLE_RATE_NONE, BluetoothCodecConfig.BITS_PER_SAMPLE_NONE, BluetoothCodecConfig.CHANNEL_MODE_NONE, 0 /* codecSpecific1 */, 0 /* codecSpecific2 */, 0 /* codecSpecific3 */, 0 /* codecSpecific4 */);

                    log(TAG + " assignCodecConfigPriorities: " + Arrays.toString(res));

                    assigned_codec_length.set(callback.getThisObject(), pos);
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
            Class<?> mHandlerClass = mHandler.getType();
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

            hookAfter(processProfileServiceStateChanged, (callback, state) -> {
                try {
                    Object adapter = adapterServiceThis.get(callback.getThisObject());
                    ArrayList<?> running = (ArrayList<?>) mRunningProfiles.get(adapter);
                    ArrayList<?> registered = (ArrayList<?>) mRegisteredProfiles.get(adapter);
                    log(TAG + " processProfileServiceStateChanged: state " + callback.getArgs()[1] + ", running: " + running + ", registered: " + registered);
                    log(TAG + " processProfileServiceStateChanged: registered " + registered.size() + " running " + running.size());
                    Class<?> config = param.getClassLoader().loadClass("com.android.bluetooth.btservice.Config");
                    Method getSupportedProfiles = config.getDeclaredMethod("getSupportedProfiles");
                    getSupportedProfiles.setAccessible(true);
                    int amountProfilesSupported = ((Class<?>[]) getSupportedProfiles.invoke(null)).length;
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
                            getAdapterPropertyNative.invoke(adapter, BT_PROPERTY_LOCAL_IO_CAPS);//AbstractionLayer.BT_PROPERTY_LOCAL_IO_CAPS;
                            getAdapterPropertyNative.invoke(adapter, BT_PROPERTY_LOCAL_IO_CAPS_BLE);//AbstractionLayer.BT_PROPERTY_LOCAL_IO_CAPS_BLE;
                            getAdapterPropertyNative.invoke(adapter, BT_PROPERTY_DYNAMIC_AUDIO_BUFFER);//AbstractionLayer.BT_PROPERTY_DYNAMIC_AUDIO_BUFFER;

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
                        } else {
                            log(TAG + " processProfileServiceStateChanged: SOME SUPPORTED PROFILES ARE NOT RUNNING! FIXME, BREDR will fail with timeout");
                        }
                    }
                } catch (IllegalAccessException | ClassNotFoundException |
                         InvocationTargetException | NullPointerException |
                         NoSuchMethodException e) {
                    log(TAG + " Exception: ", e);
                }
            });
            hookAfter(startProfileServices, (callback, state) -> {
                log(TAG + " startProfileServices");
                try {
                    log(TAG + " startProfileServices: " + mRunningProfiles.get(callback.getThisObject()));
                    log(TAG + " startProfileServices: " + mRegisteredProfiles.get(callback.getThisObject()));
                } catch (IllegalAccessException e) {
                    log(TAG + " Exception: ", e);
                }
            });
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException |
                 IllegalAccessException |
                 NullPointerException /*  | InvocationTargetException*/ e) {
            log(TAG + " Exception: ", e);
        }

    }

    void hookServiceStart(ClassLoader cl, String name) {
        try {
            Class<?> cc = cl.loadClass(name);
            Method ccStart = cc.getDeclaredMethod("start");
            hookBefore(ccStart, callback -> {
                callback.returnAndSkip(false);
                return null;
            });
        } catch (ClassNotFoundException | NoSuchMethodException e) {//| NoSuchFieldException e) {
            log(TAG + " Exception: ", e);
        }
    }

    native void setCodecIds(int[] codecIds);
}
