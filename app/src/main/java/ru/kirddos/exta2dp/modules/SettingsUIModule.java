package ru.kirddos.exta2dp.modules;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.ContextParams;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import ru.kirddos.exta2dp.R;
import ru.kirddos.exta2dp.SourceCodecType;

import static ru.kirddos.exta2dp.ConstUtils.*;

public class SettingsUIModule extends XposedModule {
    @SuppressLint("NewApi")
    protected static final int[] CODEC_TYPES = {
            SOURCE_CODEC_TYPE_FLAC,
            SOURCE_CODEC_TYPE_LC3PLUS_HR,
            SOURCE_CODEC_TYPE_LHDCV5,
            SOURCE_CODEC_TYPE_LHDCV3,
            SOURCE_CODEC_TYPE_LHDCV2,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC,
            SOURCE_CODEC_TYPE_APTX_TWSP,
            SOURCE_CODEC_TYPE_APTX_ADAPTIVE,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC,
            BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC};
    private static final String TAG = "SettingsUIModule";

    private static final String SETTINGS_PACKAGE = "com.android.settings";
    @SuppressLint({"DiscouragedPrivateApi", "PrivateApi", "NewApi", "SoonBlockedPrivateApi", "SdCardPath"})
    @SuppressWarnings("deprecation")
    public SettingsUIModule(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        log(TAG + " : " + param.getProcessName());
        ApplicationInfo ai = base.getApplicationInfo();
        log(TAG + " " + ai);
        if (!param.getProcessName().equals(SETTINGS_PACKAGE)) {
            return;
        }
        // This is dirty hack to support new LSPosed
        // It should've been done inside the framework, but here we are
        try {
            var cl = getClass().getClassLoader();
            assert cl != null;
            var ci = cl.loadClass("android.app.ContextImpl");
            var ctor = ci.getDeclaredConstructors()[0];
            log(TAG + " Constructor " + ctor);
            var atc = cl.loadClass("android.app.ActivityThread");
            Method currentActivityThread = atc.getDeclaredMethod("currentActivityThread");
            var cic = cl.loadClass("android.content.res.CompatibilityInfo");
            Method getPackageInfoNoCheck = atc.getDeclaredMethod("getPackageInfoNoCheck", ApplicationInfo.class, cic);
            var at = currentActivityThread.invoke(null);
            log(TAG + " ActivityThread: " + at);

            // This is dirty hack, not sure if it's needed, but it failed previously
            String apkPath = ai.sourceDir == null ? ai.publicSourceDir : ai.sourceDir;

            log(TAG + " Apk path: " + apkPath);

            {
                StringBuilder sb = new StringBuilder();
                StringBuilderPrinter sbp = new StringBuilderPrinter(sb);
                ai.dump(sbp, "");
                log(TAG + " ApplicationInfo: " + sb);

                log(TAG + " sourceDir: " + ai.sourceDir);
                log(TAG + " publicSourceDir: " + ai.publicSourceDir);
                log(TAG + " nativeLibraryDir: " + ai.nativeLibraryDir);
            }

            // This is even more dirty hack than previous one

            String libDir = ai.nativeLibraryDir;
            try {
                Field apkField = cl.getClass().getDeclaredField("apk");
                apkField.setAccessible(true);
                if (apkField.get(cl) instanceof String apk) {
                    log(TAG + " apk path from classloader " + apk);
                    if (apkPath == null) {
                        apkPath = apk;
                    }
                }
                Field nativeLibraryDirsField = cl.getClass().getDeclaredField("nativeLibraryDirs");
                nativeLibraryDirsField.setAccessible(true);
                //noinspection unchecked
                List<File> nativeLibraryDirs = new ArrayList<>();
                Object v = nativeLibraryDirsField.get(cl);
                if (v != null && nativeLibraryDirs.getClass().isAssignableFrom(v.getClass())) {
                    nativeLibraryDirs = (List<File>) v;
                    log(TAG + " native lib dirs from classloader: " + nativeLibraryDirs);
                    for (var dir : nativeLibraryDirs) {
                        if (libDir == null && dir.getAbsolutePath().contains(ai.packageName)) {
                            libDir = dir.getAbsolutePath();
                        }
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException | NullPointerException | ClassCastException e) {
                log(TAG + " Exception", e);
            }

            if (apkPath == null || libDir == null) {
                // It's probably release version of LSPosed, let's try relying on the types
                log(TAG + " : Release LSPosed detected, consider using debug version");
                try {
                    for (Field f : cl.getClass().getDeclaredFields()) {
                        f.setAccessible(true);
                        log(TAG + "ClassLoader field: " + f);
                        Object v = f.get(cl);
                        List<File> nativeLibraryDirs = new ArrayList<>();
                        if (v instanceof String apk) {
                            log(TAG + " apk path from classloader " + apk);
                            if (apkPath == null) {
                                apkPath = apk;
                            }
                        } else if (v != null && nativeLibraryDirs.getClass().isAssignableFrom(v.getClass())) {
                            //noinspection unchecked
                            nativeLibraryDirs = (List<File>) v;
                            log(TAG + " native lib dirs from classloader: " + nativeLibraryDirs);
                            for (var dir : nativeLibraryDirs) {
                                if (libDir == null && dir.getAbsolutePath().contains(ai.packageName)) {
                                    libDir = dir.getAbsolutePath();
                                }
                            }
                        }
                    }
                } catch (IllegalAccessException | NullPointerException | ClassCastException e) {
                    log(TAG + " Exception", e);
                }
            }

            assert apkPath != null;
            assert libDir != null;

            // And few other dirty hacks
            if (ai.sourceDir == null) {
                log(TAG + " : Applied hack for sourceDir: " + apkPath);
                ai.sourceDir = apkPath;
            }
            if (ai.publicSourceDir == null) {
                ai.publicSourceDir = apkPath;
                log(TAG + " : Applied hack for publicSourceDir: " + apkPath);
            }
            if (ai.dataDir == null) {
                ai.dataDir = "/data/user/0/" + ai.packageName;
                log(TAG + " : Applied hack for dataDir: " + ai.dataDir);
            }
            if (ai.nativeLibraryDir == null) {
                ai.nativeLibraryDir = libDir;
                log(TAG + " : Applied hack for nativeLibraryDir: " + libDir);
            }

            var lac = cl.loadClass("android.app.LoadedApk");
            ctor.setAccessible(true);
            var loadedApk = getPackageInfoNoCheck.invoke(at, ai, null);

            var args = new Object[ctor.getParameterTypes().length];
            for (int i = 0; i < ctor.getParameterTypes().length; ++i) {
                if (ctor.getParameterTypes()[i] == lac) {
                    args[i] = loadedApk;
                    continue;
                }
                if (ctor.getParameterTypes()[i] == ContextParams.class) {
                    args[i] = new ContextParams.Builder().build();
                    continue;
                }
                if (ctor.getParameterTypes()[i] == atc) {
                    args[i] = at;
                    continue;
                }
                if (ctor.getParameterTypes()[i] == int.class) {
                    args[i] = 0;
                    continue;
                }
                args[i] = null;
            }
            mBase = (Context) ctor.newInstance(args);
            log(TAG + " Context: " + mBase);
            var setOuterContext = ci.getDeclaredMethod("setOuterContext", Context.class);
            setOuterContext.setAccessible(true);
            setOuterContext.invoke(mBase, new ContextWrapper(mBase) {
                @Override
                public Resources getResources() {
                    return SettingsUIModule.this.getResources();
                }

                @Override
                public Resources.Theme getTheme() {
                    return SettingsUIModule.this.getTheme();
                }

                @Override
                public void setTheme(int resid) {
                    SettingsUIModule.this.setTheme(resid);
                }
            });

            AssetManager am = AssetManager.class.getDeclaredConstructor().newInstance();
            //noinspection JavaReflectionMemberAccess
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(am, apkPath);
            moduleResources = new Resources(am, null, null);

            Field mResources = ci.getDeclaredField("mResources");
            mResources.setAccessible(true);
            mResources.set(mBase, moduleResources);
            log(TAG + " Resources: " + moduleResources);
        } catch (Exception e) {
            log(TAG + " Exception: ", e);
        }
    }

    private Context mBase = null;
    private Resources moduleResources = null;

    Resources getResources() {
        if (moduleResources != null) return moduleResources;
        throw new RuntimeException("moduleResources should not be null by now");
    }

    Context getBaseContext() {
        if (mBase != null) return mBase;
        throw new RuntimeException("mBase should not be null by now");
    }
    public void setTheme(int resid) {
        if (mBase == null) return;
        mBase.setTheme(resid);
    }

    public Resources.Theme getTheme() {
        return mBase == null ? null : mBase.getTheme();
    }
    @XposedHooker
    static class SettingsHooker implements Hooker {
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
        var abc = SettingsHooker.beforeCallbacks.get(method);
        if (abc == null) {
            hook(method, SettingsHooker.class);
            abc = new ArrayList<>();
            abc.add(callback);
            SettingsHooker.beforeCallbacks.put(method, abc);
        } else {
            abc.add(callback);
        }
    }

    void hookAfter(Method method, AfterCallback callback) {
        var aac = SettingsHooker.afterCallbacks.get(method);
        if (aac == null) {
            hook(method, SettingsHooker.class);
            aac = new ArrayList<>();
            aac.add(callback);
            SettingsHooker.afterCallbacks.put(method, aac);
        } else {
            aac.add(callback);
        }
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @SuppressLint({"DiscouragedPrivateApi", "PrivateApi", "NewApi", "SoonBlockedPrivateApi"})
    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        log("Package: " + param.getPackageName());
        log("main classloader is " + this.getClass().getClassLoader());
        log("param classloader is " + param.getClassLoader());
        log("class classloader is " + SourceCodecType.class.getClassLoader());
        log("pid/tid: " + android.os.Process.myPid() + " " + android.os.Process.myTid());
        log("----------");

        if (!param.getPackageName().equals(SETTINGS_PACKAGE) || !param.isFirstPackage()) return;
        log("In settings!");

        try {
            Class<?> abstractBtDialogPrefController = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.AbstractBluetoothDialogPreferenceController");

            Method getCurrentCodecConfig = abstractBtDialogPrefController.getDeclaredMethod("getCurrentCodecConfig");
            Method getA2dpActiveDevice = recursiveFindMethod(abstractBtDialogPrefController, "getA2dpActiveDevice");
            getA2dpActiveDevice.setAccessible(true);

            //noinspection JavaReflectionMemberAccess
            Method getCodecStatus = BluetoothA2dp.class.getDeclaredMethod("getCodecStatus", BluetoothDevice.class);

            Field mBluetoothA2dp = abstractBtDialogPrefController.getSuperclass().getDeclaredField("mBluetoothA2dp");
            mBluetoothA2dp.setAccessible(true);

            log(TAG + " AbstractBluetoothDialogPreferenceController" + mBluetoothA2dp.getType());

            hookBefore(getCurrentCodecConfig, callback -> {
                try {
                    final BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) mBluetoothA2dp.get(callback.getThisObject());
                    if (bluetoothA2dp == null) {
                        callback.returnAndSkip(null);
                        return null;
                    }
                    BluetoothDevice activeDevice = (BluetoothDevice) getA2dpActiveDevice.invoke(callback.getThisObject());
                    if (activeDevice == null) {
                        Log.d(TAG, "Unable to get current codec config. No active device.");
                        callback.returnAndSkip(null);
                        return null;
                    }
                    final BluetoothCodecStatus codecStatus = (BluetoothCodecStatus) getCodecStatus.invoke(bluetoothA2dp, activeDevice);
                    if (codecStatus == null) {
                        Log.d(TAG, "Unable to get current codec config. Codec status is null");
                        callback.returnAndSkip(null);
                        return null;
                    }
                    if (codecStatus.getCodecConfig().getCodecType() >= SOURCE_CODEC_TYPE_LHDCV2) {
                        Log.d(TAG, "LHDC/LC3plus/FLAC codec type");
                    }
                    callback.returnAndSkip(codecStatus.getCodecConfig());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log(TAG + " abstractBtDialogPrefController: ", e);
                }
                return null;
            });


            Method getHighestCodec = abstractBtDialogPrefController.getDeclaredMethod("getHighestCodec", BluetoothA2dp.class, BluetoothDevice.class, List.class);
            getHighestCodec.setAccessible(true);
            hookBefore(getHighestCodec, callback -> {
                List<BluetoothCodecConfig> configs = (List<BluetoothCodecConfig>) callback.getArgs()[2];
                for (int codecType : CODEC_TYPES) {
                    for (BluetoothCodecConfig config : configs) {
                        if (config.getCodecType() == codecType) {
                            callback.returnAndSkip(codecType);
                            return null;
                        }
                    }
                }
                callback.returnAndSkip(BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID);
                return null;
            });
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            log(TAG + " abstractBtDialogPrefController: ", e);
        }

        try {
            Class<?> baseBtCodecDialogPreference = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BaseBluetoothDialogPreference");

            Field summaryStrings = baseBtCodecDialogPreference.getDeclaredField("mSummaryStrings");
            Field radioButtonStrings = baseBtCodecDialogPreference.getDeclaredField("mRadioButtonStrings");
            Field radioButtonIds = baseBtCodecDialogPreference.getDeclaredField("mRadioButtonIds");
            Field iCallback = baseBtCodecDialogPreference.getDeclaredField("mCallback");
            iCallback.setAccessible(true);
            Class<?> classCallback = iCallback.getType();
            Method callbackGetIndex;
            Method callbackGetSelectableIndex;
            callbackGetIndex = classCallback.getDeclaredMethod("getCurrentConfigIndex");
            callbackGetSelectableIndex = classCallback.getDeclaredMethod("getSelectableIndex");
            summaryStrings.setAccessible(true);
            radioButtonStrings.setAccessible(true);
            radioButtonIds.setAccessible(true);

            Method onBindDialogView;
            Method getRadioButtonGroupId;

            Class<?> customPreferenceDialogFragment = recursiveFindField(baseBtCodecDialogPreference, "mFragment").getType();
            Method onCreateDialogView = recursiveFindMethod(customPreferenceDialogFragment, "onCreateDialogView", Context.class);
            Field mDialogLayoutRes = recursiveFindField(customPreferenceDialogFragment, "mDialogLayoutRes");

            hookBefore(onCreateDialogView, callback -> {
                try {
                    int resId = (int) mDialogLayoutRes.get(callback.getThisObject());
                    if (resId == R.layout.bluetooth_lhdc_audio_quality_dialog) {
                        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        callback.returnAndSkip(inflater.inflate(resId, null));
                    }
                } catch (IllegalAccessException e) {
                    log(TAG + " Exception: ", e);
                }
                return null;
            });

            onBindDialogView = baseBtCodecDialogPreference.getDeclaredMethod("onBindDialogView", View.class);
            getRadioButtonGroupId = baseBtCodecDialogPreference.getDeclaredMethod("getRadioButtonGroupId");
            getRadioButtonGroupId.setAccessible(true);
            Method finalCallbackGetIndex = callbackGetIndex;
            Method finalCallbackGetSelectableIndex = callbackGetSelectableIndex;
            Method finalGetRadioButtonGroupId = getRadioButtonGroupId;
            hookAfter(onBindDialogView, (callback, state) -> {
                View view = (View) callback.getArgs()[0];
                log(TAG + " hookOnBindDialogView " + view);
                try {
                    ArrayList<Integer> mRadioButtonIds = (ArrayList<Integer>) radioButtonIds.get(callback.getThisObject());
                    ArrayList<String> mRadioButtonStrings = (ArrayList<String>) radioButtonStrings.get(callback.getThisObject());

                    Object mCallback = iCallback.get(callback.getThisObject());
                    if (mCallback == null) {
                        Log.e(TAG, "Unable to show dialog by the callback is null");
                        return;
                    }
                    if (mRadioButtonStrings.size() != mRadioButtonIds.size()) {
                        Log.e(TAG, "Unable to show dialog by the view and string size are not matched");
                        return;
                    }
                    final int currentIndex = (Integer) finalCallbackGetIndex.invoke(mCallback);
                    if (currentIndex < 0 || currentIndex >= mRadioButtonIds.size()) {
                        Log.e(TAG, "Unable to show dialog by the incorrect index: " + currentIndex);
                        Log.e(TAG, "mRadioButtonIds.size(): " + mRadioButtonIds.size() + ", callback is " + mCallback);
                        return;
                    }

                    // Initial radio button group

                    int viewId = (Integer) finalGetRadioButtonGroupId.invoke(callback.getThisObject());
                    int resource;


                    LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    ScrollView scrollView = (ScrollView) view;
                    RadioGroup radioGroup = null;
                    if (viewId == R.id.bluetooth_audio_codec_radio_group) {
                        resource = R.layout.bluetooth_audio_codec_dialog;
                    } else if (viewId == R.id.bluetooth_audio_sample_rate_radio_group) {
                        resource = R.layout.bluetooth_audio_sample_rate_dialog;
                    } else if (viewId == R.id.bluetooth_lhdc_audio_quality_radio_group) {
                        callback.setThrowable(null);
                        log(TAG + " Processing LHDC Quality!");
                        resource = R.layout.bluetooth_lhdc_audio_quality_dialog;
                        radioGroup = scrollView.findViewById(R.id.bluetooth_lhdc_audio_quality_radio_group);
                    } else {
                        return;
                    }

                    LinearLayout layout = ((LinearLayout) scrollView.getChildAt(0));


                    if (radioGroup == null)
                        radioGroup = (RadioGroup) inflater.inflate(resource, null);

                    if (layout.getChildAt(0) != radioGroup) {
                        layout.removeView(layout.getChildAt(0));
                        layout.addView(radioGroup, 0);
                    }

                    radioGroup.check(mRadioButtonIds.get(currentIndex));
                    radioGroup.setOnCheckedChangeListener((RadioGroup.OnCheckedChangeListener) callback.getThisObject());
                    // Initial radio button
                    final List<Integer> selectableIndex = (List<Integer>) finalCallbackGetSelectableIndex.invoke(mCallback);
                    RadioButton radioButton;
                    for (int i = 0; i < mRadioButtonStrings.size(); i++) {
                        radioButton = view.findViewById(mRadioButtonIds.get(i));
                        if (radioButton == null) {
                            Log.e(TAG, "Unable to show dialog by no radio button:" + mRadioButtonIds.get(i));
                            return;
                        }
                        radioButton.setText(mRadioButtonStrings.get(i));
                        radioButton.setEnabled(selectableIndex.contains(i));
                        log("getText: " + radioButton.getCurrentTextColor());

                        if (radioButton.isEnabled()) {
                            radioButton.setTextColor(0xFFE6E1E5);
                        }
                    }
                    // Initial help information text view
                    final TextView helpTextView;
                    if (layout.getChildAt(0) instanceof TextView) {
                        helpTextView = (TextView) layout.getChildAt(0);
                    } else {
                        helpTextView = (TextView) layout.getChildAt(1);
                    }

                    if (selectableIndex.size() == mRadioButtonIds.size()) {
                        // View will be invisible when all options are enabled.
                        helpTextView.setVisibility(View.GONE);
                    } else {
                        helpTextView.setText(getResources().getString(R.string.bluetooth_select_a2dp_codec_type_help_info));
                        helpTextView.setVisibility(View.VISIBLE);
                    }

                } catch (IllegalAccessException | InvocationTargetException |
                         NullPointerException e) {
                    log("Exception ", e);
                }

            });


            Class<?> btCodecDialogPreference = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BluetoothCodecDialogPreference");

            Method initialize = btCodecDialogPreference.getDeclaredMethod("initialize", Context.class);
            getRadioButtonGroupId = btCodecDialogPreference.getDeclaredMethod("getRadioButtonGroupId");
            getRadioButtonGroupId.setAccessible(true);
            hookBefore(getRadioButtonGroupId, callback -> {
                callback.returnAndSkip(R.id.bluetooth_audio_codec_radio_group);
                return null;
            });

            hookBefore(initialize, callback -> {
                try {
                    ArrayList<Integer> mRadioButtonIds = (ArrayList<Integer>) radioButtonIds.get(callback.getThisObject());
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_default);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_sbc);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_aac);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_aptx);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_aptx_hd);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_aptx_adaptive);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_ldac);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_aptx_twsp);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_lhdcv2);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_lhdcv3);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_lhdcv5);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_lc3plus_hr);
                    mRadioButtonIds.add(R.id.bluetooth_audio_codec_flac);
                    String[] stringArray = getResources().getStringArray(
                            R.array.bluetooth_a2dp_codec_titles);

                    log("a2dp_codec_titles array length: " + stringArray.length);

                    log("a2dp_codec_titles array: " + Arrays.toString(stringArray));

                    ArrayList<String> mRadioButtonStrings = (ArrayList<String>) radioButtonStrings.get(callback.getThisObject());
                    mRadioButtonStrings.addAll(Arrays.asList(stringArray));

                    stringArray = getResources().getStringArray(R.array.bluetooth_a2dp_codec_summaries);
                    log("a2dp_codec_summaries array length: " + stringArray.length);
                    log("a2dp_codec_summaries array: " + Arrays.toString(stringArray));
                    ArrayList<String> mSummaryStrings = (ArrayList<String>) summaryStrings.get(callback.getThisObject());
                    mSummaryStrings.addAll(Arrays.asList(stringArray));
                } catch (IllegalAccessException | NullPointerException e) {
                    log("Exception: ", e);
                }
                callback.returnAndSkip(null);
                return null;
            });

            Class<?> btCodecDialogPreferenceController = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BluetoothCodecDialogPreferenceController");
            Method convertCfgToBtnIndex = btCodecDialogPreferenceController.getDeclaredMethod("convertCfgToBtnIndex", int.class);
            Method writeConfigurationValues = btCodecDialogPreferenceController.getDeclaredMethod("writeConfigurationValues", int.class);

            Field mBluetoothA2dpConfigStore = btCodecDialogPreferenceController.getSuperclass().getDeclaredField("mBluetoothA2dpConfigStore");
            mBluetoothA2dpConfigStore.setAccessible(true);
            Class<?> btA2dpConfigStoreClass = mBluetoothA2dpConfigStore.getType();

            Method setSampleRate = btA2dpConfigStoreClass.getDeclaredMethod("setSampleRate", int.class);
            Method setBitsPerSample = btA2dpConfigStoreClass.getDeclaredMethod("setBitsPerSample", int.class);
            Method setChannelMode = btA2dpConfigStoreClass.getDeclaredMethod("setChannelMode", int.class);
            Method setCodecType = btA2dpConfigStoreClass.getDeclaredMethod("setCodecType", int.class);
            Method setCodecPriority = btA2dpConfigStoreClass.getDeclaredMethod("setCodecPriority", int.class);

            Field mCodecSpecific1Value = btA2dpConfigStoreClass.getDeclaredField("mCodecSpecific1Value");
            mCodecSpecific1Value.setAccessible(true);

            //Method setCodecSpecific1Value = btA2dpConfigStoreClass.getDeclaredMethod("setCodecSpecific1Value", long.class);
            //Method setCodecSpecific2Value = btA2dpConfigStoreClass.getDeclaredMethod("setCodecSpecific2Value", int.class);
            //Method setCodecSpecific3Value = btA2dpConfigStoreClass.getDeclaredMethod("setCodecSpecific3Value", int.class);
            //Method setCodecSpecific4Value = btA2dpConfigStoreClass.getDeclaredMethod("setCodecSpecific4Value", int.class);

            hookBefore(writeConfigurationValues, callback -> {
                try {
                    int index = (int) callback.getArgs()[0];
                    log(TAG + " Hooked BluetoothCodecDialogPreferenceController.writeConfigurationValues: type " + index);
                    int type;
                    switch (index) {
                        case 8 -> type = SOURCE_CODEC_TYPE_LHDCV2;
                        case 9 -> type = SOURCE_CODEC_TYPE_LHDCV3;
                        case 10 -> type = SOURCE_CODEC_TYPE_LHDCV5;
                        case 11 -> type = SOURCE_CODEC_TYPE_LC3PLUS_HR;
                        case 12 -> type = SOURCE_CODEC_TYPE_FLAC;
                        default -> {
                            return null;
                        }
                    }
                    Object store = mBluetoothA2dpConfigStore.get(callback.getThisObject());
                    setCodecType.invoke(store, type);
                    setCodecPriority.invoke(store, BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST);
                    setSampleRate.invoke(store, BluetoothCodecConfig.SAMPLE_RATE_NONE);
                    setBitsPerSample.invoke(store, BluetoothCodecConfig.BITS_PER_SAMPLE_NONE);
                    setChannelMode.invoke(store, BluetoothCodecConfig.CHANNEL_MODE_NONE);
                    callback.returnAndSkip(null);
                } catch (NullPointerException | IllegalAccessException |
                         InvocationTargetException e) {
                    log(TAG + " Exception: ", e);
                }
                return null;
            });

            hookBefore(convertCfgToBtnIndex, callback -> {
                try {
                    int type = (int) callback.getArgs()[0];
                    int index = 0;

                    if (type == SOURCE_CODEC_TYPE_LHDCV2) {
                        index = 8;
                    } else if (type == SOURCE_CODEC_TYPE_LHDCV3) {
                        index = 9;
                    } else if (type == SOURCE_CODEC_TYPE_LHDCV5) {
                        index = 10;
                    } else if (type == SOURCE_CODEC_TYPE_LC3PLUS_HR) {
                        index = 11;
                    } else if (type == SOURCE_CODEC_TYPE_FLAC) {
                        index = 12;
                    }

                    log(TAG + " Hooked BluetoothCodecDialogPreferenceController.convertCfgToBtnIndex: type = " + type + ", index = " + index);

                    if (index != 0) { // Handle LHDC 2/3/4/5 or LC3plus HR/FLAC only
                        callback.returnAndSkip(index);
                    }
                } catch (NullPointerException e) {
                    log(TAG + " Exception: ", e);
                }
                return null;
            });

            /*Class<?> btCodecDialogSampleRatePreference = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BluetoothSampleRateDialogPreference");

            initialize = btCodecDialogSampleRatePreference.getDeclaredMethod("initialize", Context.class);
            getRadioButtonGroupId = btCodecDialogSampleRatePreference.getDeclaredMethod("getRadioButtonGroupId");

            initialize.setAccessible(true);
            getRadioButtonGroupId.setAccessible(true);

            hookBefore(getRadioButtonGroupId, callback -> callback.returnAndSkip(R.id.bluetooth_audio_sample_rate_radio_group));
            hookBefore(initialize, callback -> {
                try {
                    ArrayList<Integer> mRadioButtonIds = (ArrayList<Integer>) radioButtonIds.get(callback.getThisObject());
                    ArrayList<String> mRadioButtonStrings = (ArrayList<String>) radioButtonStrings.get(callback.getThisObject());
                    ArrayList<String> mSummaryStrings = (ArrayList<String>) summaryStrings.get(callback.getThisObject());
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_default);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_441);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_480);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_882);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_960);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_1764);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_1920);
                    String[] stringArray = getResources().getStringArray(
                                R.array.bluetooth_a2dp_codec_sample_rate_titles);
                    mRadioButtonStrings.addAll(Arrays.asList(stringArray));
                    log("a2dp_codec_sample_rate_titles array: " + Arrays.toString(stringArray));
                    stringArray = getResources().getStringArray(
                            R.array.bluetooth_a2dp_codec_sample_rate_summaries);
                    mSummaryStrings.addAll(Arrays.asList(stringArray));
                    radioButtonIds.set(callback.getThisObject(), mRadioButtonIds);
                    radioButtonStrings.set(callback.getThisObject(), mRadioButtonStrings);
                    summaryStrings.set(callback.getThisObject(), mSummaryStrings);
                    log("a2dp_codec_sample_rate_summaries array: " + Arrays.toString(stringArray));
                } catch (IllegalAccessException | NullPointerException e) {
                    log("Exception: ", e);
                }
                callback.returnAndSkip(null);
            });*/

            Class<?> ldacQualityPref = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BluetoothQualityDialogPreference");

            getRadioButtonGroupId = ldacQualityPref.getDeclaredMethod("getRadioButtonGroupId");
            initialize = ldacQualityPref.getDeclaredMethod("initialize", Context.class);

            Method getKey = recursiveFindMethod(ldacQualityPref, "getKey");

            getRadioButtonGroupId.setAccessible(true);
            hookBefore(initialize, callback -> {
                try {
                    //int resId = (int) mDialogLayoutResId.get(callback.getThisObject());

                    //int resId = (int) getRadioButtonGroupId1.invoke(callback.getThisObject());

                    String key = (String) getKey.invoke(callback.getThisObject());

                    if (key == null || key.equals("bluetooth_select_a2dp_lhdc_playback_quality")) { //Dirty hack: if no key is present, this must be LHDC
                        //resId = R.id.bluetooth_lhdc_audio_quality_scroll_view;
                        log(TAG + " LHDC Quality: initialize");
                        ArrayList<Integer> mRadioButtonIds = (ArrayList<Integer>) radioButtonIds.get(callback.getThisObject());
                        ArrayList<String> mRadioButtonStrings = (ArrayList<String>) radioButtonStrings.get(callback.getThisObject());
                        ArrayList<String> mSummaryStrings = (ArrayList<String>) summaryStrings.get(callback.getThisObject());
                        mRadioButtonIds.add(R.id.bluetooth_lhdc_audio_quality_low0);
                        mRadioButtonIds.add(R.id.bluetooth_lhdc_audio_quality_low1);
                        mRadioButtonIds.add(R.id.bluetooth_lhdc_audio_quality_low2);
                        mRadioButtonIds.add(R.id.bluetooth_lhdc_audio_quality_low3);
                        mRadioButtonIds.add(R.id.bluetooth_lhdc_audio_quality_low4);
                        mRadioButtonIds.add(R.id.bluetooth_lhdc_audio_quality_low);
                        mRadioButtonIds.add(R.id.bluetooth_lhdc_audio_quality_mid);
                        mRadioButtonIds.add(R.id.bluetooth_lhdc_audio_quality_high);
                        mRadioButtonIds.add(R.id.bluetooth_lhdc_audio_quality_high1);
                        mRadioButtonIds.add(R.id.bluetooth_lhdc_audio_quality_best_effort);
                        String[] stringArray = getResources().getStringArray(
                                R.array.bluetooth_a2dp_codec_lhdc_playback_quality_titles);
                        mRadioButtonStrings.addAll(Arrays.asList(stringArray));
                        log("a2dp_codec_lhdc_quality_titles array: " + Arrays.toString(stringArray));
                        stringArray = getResources().getStringArray(
                                R.array.bluetooth_a2dp_codec_lhdc_playback_quality_summaries);
                        mSummaryStrings.addAll(Arrays.asList(stringArray));
                        log("a2dp_codec_lhdc_quality_summaries array: " + Arrays.toString(stringArray));
                        radioButtonIds.set(callback.getThisObject(), mRadioButtonIds);
                        radioButtonStrings.set(callback.getThisObject(), mRadioButtonStrings);
                        summaryStrings.set(callback.getThisObject(), mSummaryStrings);
                        callback.returnAndSkip(null);
                    }
                } catch (IllegalAccessException | NullPointerException |
                         InvocationTargetException e) {
                    log(TAG + " Exception: ", e);
                }
                return null;
            });

            Class<?> btChannelModePreferenceController = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BluetoothChannelModeDialogPreferenceController");

            writeConfigurationValues = btChannelModePreferenceController.getDeclaredMethod("writeConfigurationValues", int.class);
            convertCfgToBtnIndex = btChannelModePreferenceController.getDeclaredMethod("convertCfgToBtnIndex", int.class);
            Method getCurrentIndexByConfig = btChannelModePreferenceController.getDeclaredMethod("getCurrentIndexByConfig", BluetoothCodecConfig.class);
            Method getSelectableIndex = btChannelModePreferenceController.getDeclaredMethod("getSelectableIndex");
            Method getCurrentCodecConfig = recursiveFindMethod(btChannelModePreferenceController, "getCurrentCodecConfig");
            getCurrentCodecConfig.setAccessible(true);

            hookBefore(writeConfigurationValues, callback -> {
                try {
                    final int index = (int) callback.getArgs()[0];
                    Object store = mBluetoothA2dpConfigStore.get(callback.getThisObject());
                    BluetoothCodecConfig currentConfig = (BluetoothCodecConfig) getCurrentCodecConfig.invoke(callback.getThisObject());
                    log(TAG + " writeConfigurationValues: current config: " + currentConfig);
                    log(TAG + " writeConfigurationValues: current index: " + index);
                    if (currentConfig.getCodecType() == SOURCE_CODEC_TYPE_FLAC) {
                        // Android doesn't really seem to support mono with anything other than 16/44100, so apply hack
                        if (index == 1) {
                            setChannelMode.invoke(store, BluetoothCodecConfig.CHANNEL_MODE_STEREO);
                            mCodecSpecific1Value.set(store, FLAC_MONO);
                            callback.returnAndSkip(null);
                        } else if (index == 2) {
                            setChannelMode.invoke(store, BluetoothCodecConfig.CHANNEL_MODE_STEREO);
                            mCodecSpecific1Value.set(store, FLAC_STEREO);
                            callback.returnAndSkip(null);
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log(TAG + " Exception: ", e);
                }
                return null;
            });

            hookBefore(getCurrentIndexByConfig, callback -> {
                BluetoothCodecConfig config = (BluetoothCodecConfig) callback.getArgs()[0];
                log(TAG + " getCurrentIndexByConfig: current config: " + config);
                if (config == null) return null;
                if (config.getCodecType() == SOURCE_CODEC_TYPE_FLAC && (config.getCodecSpecific1() & FLAC_STEREO_MONO_MASK) == FLAC_MONO) {
                    callback.returnAndSkip(1);
                }
                return null;
            });

            hookAfter(getSelectableIndex, (callback, state) -> {
                try {
                    BluetoothCodecConfig currentConfig = (BluetoothCodecConfig) getCurrentCodecConfig.invoke(callback.getThisObject());
                    log(TAG + " getSelectableIndex: current config: " + currentConfig);
                    if (currentConfig == null) return;
                    if (currentConfig.getCodecType() == SOURCE_CODEC_TYPE_FLAC) {
                        List<Integer> selectableIndex = (List<Integer>) callback.getResult();
                        selectableIndex.add(1);
                        selectableIndex.add(2);
                        callback.setResult(selectableIndex);
                    }
                } catch (InvocationTargetException | IllegalAccessException e) {
                    log(TAG + " Exception: ", e);
                }
            });

            hookBefore(convertCfgToBtnIndex, callback -> {
                try {
                    int config = (int) callback.getArgs()[0];
                    BluetoothCodecConfig currentConfig = (BluetoothCodecConfig) getCurrentCodecConfig.invoke(callback.getThisObject());
                    if (currentConfig.getCodecType() == SOURCE_CODEC_TYPE_FLAC) {
                        // Android doesn't really seem to support mono with anything other than 16/44100, so apply hack
                        if (config == BluetoothCodecConfig.CHANNEL_MODE_STEREO) {
                            if ((currentConfig.getCodecSpecific1() & FLAC_STEREO_MONO_MASK) == FLAC_MONO) {
                                callback.returnAndSkip(1);
                            } else {
                                callback.returnAndSkip(2);
                            }
                        } else {
                            callback.returnAndSkip(0);
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log(TAG + " Exception: ", e);
                }
                return null;
            });

        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException |
                 NullPointerException /*|
                 IllegalAccessException*/ e) {
            log(TAG + " Exception: ", e);
        }

        try {
            Class<?> devSettingsDashboardFragment = param.getClassLoader().loadClass("com.android.settings.development.DevelopmentSettingsDashboardFragment");

            Class<?> lifecycleClass = param.getClassLoader().loadClass("com.android.settingslib.core.lifecycle.Lifecycle");

            Class<?> btCodecDialogPreferenceController = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.AbstractBluetoothDialogPreferenceController");
            Field mBluetoothA2dpConfigStore = btCodecDialogPreferenceController.getDeclaredField("mBluetoothA2dpConfigStore");
            Field mPreferenceControllers = devSettingsDashboardFragment.getDeclaredField("mPreferenceControllers");
            mBluetoothA2dpConfigStore.setAccessible(true);
            mPreferenceControllers.setAccessible(true);
            Class<?> btA2dpConfigStore = mBluetoothA2dpConfigStore.getType();
            Method setSampleRate = btA2dpConfigStore.getDeclaredMethod("setSampleRate", int.class);
            setSampleRate.setAccessible(true);
            Method onCreateView = devSettingsDashboardFragment.getDeclaredMethod("onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class);
            Method buildPreferenceControllers = devSettingsDashboardFragment.getDeclaredMethod("buildPreferenceControllers", Context.class, Activity.class, lifecycleClass, devSettingsDashboardFragment, btA2dpConfigStore);
            buildPreferenceControllers.setAccessible(true);
            onCreateView.setAccessible(true);

            Method getPreferenceScreen = recursiveFindMethod(devSettingsDashboardFragment, "getPreferenceScreen");
            Class<?> preferenceScreenClass = getPreferenceScreen.getReturnType();
            Method findPreference = recursiveFindMethod(preferenceScreenClass, "findPreference", CharSequence.class);
            Method addPreference = recursiveFindMethodByName(preferenceScreenClass, "addPreference"); // androidx.preference.Preference is not suitable
            Method getOrder = recursiveFindMethod(preferenceScreenClass, "getOrder");
            Method setOrder = recursiveFindMethod(preferenceScreenClass, "setOrder", int.class);
            Method getContext = recursiveFindMethod(preferenceScreenClass, "getContext");
            Method getPreference = recursiveFindMethod(preferenceScreenClass, "getPreference", int.class);
            Method getPreferenceCount = recursiveFindMethod(preferenceScreenClass, "getPreferenceCount");
            Method getTitle = recursiveFindMethod(preferenceScreenClass, "getTitle");
            Method setTitle = recursiveFindMethod(preferenceScreenClass, "setTitle", CharSequence.class);

            //Class<?> abstractBtPrefController = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.AbstractBluetoothDialogPreferenceController");
            //Class<?> btLHDCQualityPrefController = coolLoader.loadClass("ru.kirddos.exta2dp.modules.ui.BluetoothLHDCQualityDialogPreferenceController");

            Class<?> ldacPrefControl = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BluetoothQualityDialogPreferenceController");


            Field mPreference = recursiveFindField(ldacPrefControl, "mPreference");
            Method writeConfigurationValues = ldacPrefControl.getDeclaredMethod("writeConfigurationValues", int.class);
            //Method getCurrentIndexByConfig = ldacPrefControl.getDeclaredMethod("getCurrentIndexByConfig", BluetoothCodecConfig.class);
            Method getCurrentCodecConfig = recursiveFindMethod(ldacPrefControl, "getCurrentCodecConfig");
            Method getSelectableIndex = ldacPrefControl.getDeclaredMethod("getSelectableIndex");
            Method updateState = recursiveFindMethodByName(ldacPrefControl, "updateState"); // Preference once again
            Method convertCfgToBtnIndex = ldacPrefControl.getDeclaredMethod("convertCfgToBtnIndex", int.class);

            Method setCodecSpecific1Value = btA2dpConfigStore.getDeclaredMethod("setCodecSpecific1Value", long.class);
            Method setCodecType = btA2dpConfigStore.getDeclaredMethod("setCodecType", int.class);
            setCodecSpecific1Value.setAccessible(true);
            setCodecType.setAccessible(true);
            Method displayPreference = ldacPrefControl.getDeclaredMethod("displayPreference", preferenceScreenClass);
            Method getPreferenceKey = ldacPrefControl.getDeclaredMethod("getPreferenceKey");
            Method refreshSummary = recursiveFindMethodByName(ldacPrefControl, "refreshSummary"); //  Preference once again
            //Method getKey = recursiveFindMethod(mPreference.getType(),"getKey");


            hookAfter(buildPreferenceControllers, (callback, state) -> {
                try {
                    ArrayList<Object> controllers = (ArrayList<Object>) callback.getResult();
                    Context context = (Context) callback.getArgs()[0];
                    //Object activity = callback.getArgs()[1];
                    Object lifecycle = callback.getArgs()[2];
                    //Object fragment = callback.getArgs()[3];
                    Object store = callback.getArgs()[4];

                    Object controller = ldacPrefControl.getDeclaredConstructor(Context.class, lifecycleClass, btA2dpConfigStore).newInstance(context, lifecycle, store);

                    hookBefore(getPreferenceKey, x -> {
                        if (x.getThisObject() == controller)
                            x.returnAndSkip("bluetooth_select_a2dp_lhdc_playback_quality");
                        return null;
                    });

                    //controllers.add(new BluetoothLHDCQualityDialogPreferenceController((Context) context, (Lifecycle) lifecycle, (BluetoothA2dpConfigStore) store));

                    log(TAG + " LHDC Controller: " + controller);

                    controllers.add(controller);
                    callback.setResult(controllers);
                } catch (NullPointerException | InvocationTargetException | IllegalAccessException |
                         InstantiationException | NoSuchMethodException e) {
                    log(TAG + " Exception: ", e);
                }
            });
            hookBefore(displayPreference, callback -> {
                try {
                    String key = (String) getPreferenceKey.invoke(callback.getThisObject());
                    if (key.contains("bluetooth_select_a2dp_lhdc_playback_quality")) {
                        log(TAG + " We're now LHDC controller!");
                        if (mPreference.get(callback.getThisObject()) == null) {
                            callback.returnAndSkip(null);
                        }
                    } else {
                        log(TAG + " We're still LDAC controller!");
                    }
                    //mPreference.set(callback.getThisObject(), lhdcPreference);
                    //hookBefore(getPreferenceKey, x -> x.returnAndSkip("bluetooth_select_a2dp_lhdc_playback_quality"));
                } catch (IllegalAccessException | IllegalArgumentException |
                         InvocationTargetException | NullPointerException e) {
                    log(TAG + "Exception: ", e);
                }
                return null;
            });

            hookBefore(writeConfigurationValues, callback -> {
                try {
                    //Object pref = mPreference.get(callback.getThisObject());
                    //Class<?> prefClass = pref.getClass();
                    //Method getRadioButtonGroupId = prefClass.getDeclaredMethod("getRadioButtonGroupId");
                    //getRadioButtonGroupId.setAccessible(true);
                    //int resId = (int) getRadioButtonGroupId.invoke(pref);
                    final int index = (int) callback.getArgs()[0];

                    String key = (String) getPreferenceKey.invoke(callback.getThisObject());
                    if (key.contains("bluetooth_select_a2dp_lhdc_playback_quality")) {
                        log(TAG + " in LHDC controller: writeConfigurationValues");
                        final BluetoothCodecConfig currentConfig = (BluetoothCodecConfig) getCurrentCodecConfig.invoke(callback.getThisObject());
                        int type = currentConfig == null ? -1 : currentConfig.getCodecType();
                        log(TAG + " writeConfigurationValues: Codec type " + type + " (" + getCustomCodecName(type) + ")");
                        long codecSpecific1Value;
                        if (index <= LHDC_QUALITY_DEFAULT_MAX_INDEX) {
                            codecSpecific1Value = LHDC_QUALITY_DEFAULT_MAGIC | (index + lhdc_quality_index_adjust_offset);
                        } else {
                            codecSpecific1Value = LHDC_QUALITY_DEFAULT_MAGIC | LHDC_QUALITY_DEFAULT_INDEX;
                        }
                        if (type != SOURCE_CODEC_TYPE_LHDCV2 && type != SOURCE_CODEC_TYPE_LHDCV3 && type != SOURCE_CODEC_TYPE_LHDCV5) {
                            // Assume LHDC V3
                            // This probably needs a better fix, but here we are
                            log(TAG + " writeConfigurationValues: Set codec type to LHDC V3");
                            type = SOURCE_CODEC_TYPE_LHDCV3;
                        }

                        setCodecType.invoke(mBluetoothA2dpConfigStore.get(callback.getThisObject()), type);
                        setCodecSpecific1Value.invoke(mBluetoothA2dpConfigStore.get(callback.getThisObject()), codecSpecific1Value);
                        callback.returnAndSkip(null);
                    }
                    //pref.getClass().getDeclaredMethod("");
                } catch (IllegalAccessException | IllegalArgumentException |
                         InvocationTargetException | NullPointerException e) {
                    log(TAG + "Exception: ", e);
                }
                return null;
            });
            /*hookBefore(getCurrentIndexByConfig, callback -> {
                try {
                    Object pref = mPreference.get(callback.getThisObject());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });*/
            hookBefore(getSelectableIndex, callback -> {
                try {
                    //Object pref = mPreference.get(callback.getThisObject());
                    //Class<?> prefClass = pref.getClass();
                    //Method getRadioButtonGroupId = prefClass.getDeclaredMethod("getRadioButtonGroupId");
                    //getRadioButtonGroupId.setAccessible(true);
                    //int resId = (int) getRadioButtonGroupId.invoke(pref);
                    String key = (String) getPreferenceKey.invoke(callback.getThisObject());

                    if (key.contains("bluetooth_select_a2dp_lhdc_playback_quality")) {
                        log(TAG + " in LHDC controller: getSelectableIndex");
                        List<Integer> selectableIndex = new ArrayList<>();
                        final BluetoothCodecConfig currentConfig = (BluetoothCodecConfig) getCurrentCodecConfig.invoke(callback.getThisObject());
                        if (currentConfig != null) {
                            int type = currentConfig.getCodecType();
                            if (type == SOURCE_CODEC_TYPE_LHDCV2 || type == SOURCE_CODEC_TYPE_LHDCV3) {
                                // excluding 1000Kbps
                                for (int i = 0; i <= LHDC_QUALITY_DEFAULT_MAX_INDEX; i++) {
                                    if (i != (8 - lhdc_quality_index_adjust_offset)) {
                                        selectableIndex.add(i);
                                    }
                                }
                            }
                            if (type == SOURCE_CODEC_TYPE_LHDCV5) {
                                // All items are available for LHDC V5.
                                for (int i = 0; i <= LHDC_QUALITY_DEFAULT_MAX_INDEX; i++) {
                                    selectableIndex.add(i);
                                }
                            }
                        }
                        // All items are available to set from UI but be filtered at native layer.
                        // Nope
                        //for (int i = 0; i <= LHDC_QUALITY_DEFAULT_MAX_INDEX; i++) {
                        //    selectableIndex.add(i);
                        //}
                        callback.returnAndSkip(selectableIndex);
                    }
                } catch (IllegalAccessException | IllegalArgumentException |
                         InvocationTargetException | NullPointerException e) {
                    log(TAG + "Exception: ", e);
                }
                return null;
            });
            hookAfter(updateState, (callback, state) -> {
                try {
                    Object pref = mPreference.get(callback.getThisObject());

                    if (pref == null) {
                        callback.setThrowable(null);
                        return;
                    }

                    Class<?> prefClass = pref.getClass();

                    String key = (String) getPreferenceKey.invoke(callback.getThisObject());
                    if (!key.contains("bluetooth_select_a2dp_lhdc_playback_quality")) {
                        return;
                    }

                    Object preference = callback.getArgs()[0];
                    Method setEnabled = recursiveFindMethod(prefClass, "setEnabled", boolean.class);


                    Field summaryStrings = recursiveFindField(prefClass, "mSummaryStrings");
                    //Field radioButtonStrings = recursiveFindField(prefClass, "mRadioButtonStrings");
                    //Field radioButtonIds = recursiveFindField(prefClass, "mRadioButtonIds");
                    //Field iCallback = recursiveFindField(prefClass, "mCallback");

                    summaryStrings.setAccessible(true);

                    ArrayList<String> mSummaryStrings = (ArrayList<String>) summaryStrings.get(pref);


                    log(TAG + " LHDC Summaries: " + mSummaryStrings);

                    if (mSummaryStrings.size() == 0) {
                        mSummaryStrings.addAll(Arrays.asList(getResources().getStringArray(
                                R.array.bluetooth_a2dp_codec_lhdc_playback_quality_summaries)));
                    }

                    callback.setThrowable(null);
                    log(TAG + " in LHDC controller: updateState");
                    final BluetoothCodecConfig currentConfig = (BluetoothCodecConfig) getCurrentCodecConfig.invoke(callback.getThisObject());
                    if (currentConfig != null) {
                        int type = currentConfig.getCodecType();
                        log(TAG + " updateState: Codec type " + type + " (" + getCustomCodecName(type) + ")");
                        if (type == SOURCE_CODEC_TYPE_LHDCV2 || type == SOURCE_CODEC_TYPE_LHDCV3 || type == SOURCE_CODEC_TYPE_LHDCV5) {
                            setEnabled.invoke(preference, true);
                            refreshSummary.invoke(callback.getThisObject(), preference);
                            //setSummary.invoke(preference, getResources().getString(R.string.bluetooth_select_a2dp_codec_lhdc_playback_quality));
                        } else {
                            setEnabled.invoke(preference, false);
                        }
                    } else {
                        setEnabled.invoke(preference, false);
                    }
                } catch (IllegalAccessException | IllegalArgumentException |
                         InvocationTargetException | NullPointerException e) {
                    log(TAG + " Exception: ", e);
                }
            });
            hookBefore(convertCfgToBtnIndex, callback -> {
                try {
                    Object pref = mPreference.get(callback.getThisObject());
                    Class<?> prefClass = pref.getClass();
                    Method getRadioButtonGroupId = prefClass.getDeclaredMethod("getRadioButtonGroupId");
                    getRadioButtonGroupId.setAccessible(true);
                    int config = (int) callback.getArgs()[0];
                    String key = (String) getPreferenceKey.invoke(callback.getThisObject());
                    if (key.contains("bluetooth_a2dp_ldac_playback_quality")) {
                        log(TAG + " in LDAC controller: convertCfgToBtnIndex: " + config);
                        if (config >= 1004) {
                            //TODO: Come up with a way to alter default bitrate in module settings
                            //callback.setArg(0, 1003);
                            callback.getArgs()[0] = 1003;
                        }
                        return null;
                    }

                    log(TAG + " in LHDC controller: convertCfgToBtnIndex");
                    int index = config;
                    if ((config & LHDC_QUALITY_DEFAULT_TAG) != LHDC_QUALITY_DEFAULT_MAGIC) {
                        index = 0;
                    } else {
                        index &= 0xff;
                    }
                    callback.returnAndSkip(index);
                } catch (IllegalAccessException | IllegalArgumentException |
                         InvocationTargetException | NullPointerException |
                         NoSuchMethodException e) {
                    log(TAG + "Exception: ", e);
                }
                return null;
            });
            hookAfter(onCreateView, (callback, state) -> {
                try {
                    //Bundle savedInstanceState = (Bundle) callback.getArgs()[0];
                    Object preferenceScreen = getPreferenceScreen.invoke(callback.getThisObject());

                    Object ldacQualityPreference = findPreference.invoke(preferenceScreen, "bluetooth_a2dp_ldac_playback_quality");

                    Class<?> ldacPrefClass = ldacQualityPreference.getClass();

                    Method getRadioButtonGroupId = ldacPrefClass.getDeclaredMethod("getRadioButtonGroupId");
                    getRadioButtonGroupId.setAccessible(true);
                    Method setKey = recursiveFindMethod(ldacPrefClass, "setKey", String.class);
                    Method getKey = recursiveFindMethod(ldacPrefClass, "getKey");
                    Method setEnabled = recursiveFindMethod(ldacPrefClass, "setEnabled", boolean.class);
                    Method setDialogTitle = recursiveFindMethod(ldacPrefClass, "setDialogTitle", CharSequence.class);
                    //Method setSummary = recursiveFindMethod(ldacPrefClass, "setSummary", CharSequence.class);

                    int prefCount = (int) getPreferenceCount.invoke(preferenceScreen);

                    log(TAG + " : Preference Count: " + prefCount);

                    Object networking = null;

                    for (int i = 0; i < prefCount; i++) {
                        Object currPref = getPreference.invoke(preferenceScreen, i);
                        String title = String.valueOf(getTitle.invoke(currPref));

                        String key = String.valueOf(getKey.invoke(currPref));

                        log(TAG + " : Pref " + i + " -> " + title + " (" + key + ")");

                        if (title.toLowerCase().contains("networking")) {
                            networking = currPref;
                            break;
                        } else if (key.toLowerCase().contains("networking")) {
                            networking = currPref;
                            break;
                        }
                    }

                    if (networking == null) {
                        log(TAG + " Dev Settings layout structure has changed, fix me");
                        return;
                    }

                    prefCount = (int) getPreferenceCount.invoke(networking);
                    for (int i = 0; i < prefCount; i++) {
                        Object currPref = getPreference.invoke(networking, i);
                        String title = String.valueOf(getTitle.invoke(currPref));
                        log(TAG + " :   Pref " + i + " -> " + title);
                    }

                    int ldacOrder = (Integer) getOrder.invoke(ldacQualityPreference);

                    log(TAG + " : LDAC order is " + ldacOrder);

                    Context context = (Context) getContext.invoke(ldacQualityPreference);
                    deoptimize(ldacQualityPreference.getClass().getDeclaredConstructor(Context.class));
                    Object lhdcQualityPreference = ldacQualityPreference.getClass().getDeclaredConstructor(Context.class).newInstance(context);

                    Field mDialogLayoutResId = recursiveFindField(ldacPrefClass, "mDialogLayoutResId");
                    mDialogLayoutResId.set(lhdcQualityPreference, R.layout.bluetooth_lhdc_audio_quality_dialog);

                    setKey.invoke(lhdcQualityPreference, "bluetooth_select_a2dp_lhdc_playback_quality");
                    setOrder.invoke(lhdcQualityPreference, ldacOrder + 1);
                    setTitle.invoke(lhdcQualityPreference, getResources().getString(R.string.bluetooth_select_a2dp_codec_lhdc_playback_quality));
                    setDialogTitle.invoke(lhdcQualityPreference, getResources().getString(R.string.bluetooth_select_a2dp_codec_lhdc_playback_quality_dialog_title));
                    setEnabled.invoke(lhdcQualityPreference, true);

                    String key = (String) getKey.invoke(lhdcQualityPreference);
                    log(TAG + " LHDC Preference key: " + key);

                    boolean result = (Boolean) addPreference.invoke(networking, lhdcQualityPreference);

                    if (result) {
                        log(TAG + " LHDC preference added successfully!");
                    } else {
                        log(TAG + " LHDC preference WAS NOT ADDED!");
                    }
                    log(TAG + " LHDC Preference search: " + findPreference.invoke(preferenceScreen, key));


                    hookBefore(getRadioButtonGroupId, x -> {
                        if (x.getThisObject() == lhdcQualityPreference)
                            x.returnAndSkip(R.id.bluetooth_lhdc_audio_quality_radio_group);
                        return null;
                    });

                    ArrayList<?> controllers = (ArrayList<?>) mPreferenceControllers.get(callback.getThisObject());

                    Object lhdcQualityController = null;

                    for (Object c : controllers) {
                        if (c.getClass().isAssignableFrom(ldacPrefControl)) {
                            String controlKey = (String) getPreferenceKey.invoke(c);
                            //log(TAG + " LDAC/LHDC Controller key is " + controlKey);
                            if (controlKey.equals("bluetooth_select_a2dp_lhdc_playback_quality")) {
                                lhdcQualityController = c;
                                break;
                            }
                        }
                    }

                    if (lhdcQualityController != null) {
                        mPreference.set(lhdcQualityController, lhdcQualityPreference);
                        displayPreference.invoke(lhdcQualityController, preferenceScreen);
                        updateState.invoke(lhdcQualityController, lhdcQualityPreference);
                    }
                } catch (NullPointerException | InvocationTargetException |
                         IllegalAccessException | /*NoSuchFieldException |*/
                         InstantiationException | NoSuchMethodException e) {
                    log(TAG + " Exception: ", e);
                }
            });

            /*Class<?> btCodecDialogSampleRatePreferenceController = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BluetoothSampleRateDialogPreferenceController");
            writeConfigurationValues = btCodecDialogSampleRatePreferenceController.getDeclaredMethod("writeConfigurationValues", int.class);

            convertCfgToBtnIndex = btCodecDialogSampleRatePreferenceController.getDeclaredMethod("convertCfgToBtnIndex");

            hookBefore(convertCfgToBtnIndex, callback -> {
                int config = callback.getArgs()[0];
                int index = 0;
                switch (config) {
                    case BluetoothCodecConfig.SAMPLE_RATE_44100:
                        index = 1;
                        break;
                    case BluetoothCodecConfig.SAMPLE_RATE_48000:
                        index = 2;
                        break;
                    case BluetoothCodecConfig.SAMPLE_RATE_88200:
                        index = 3;
                        break;
                    case BluetoothCodecConfig.SAMPLE_RATE_96000:
                        index = 4;
                        break;
                    case BluetoothCodecConfig.SAMPLE_RATE_176400:
                        index = 5;
                        break;
                    case BluetoothCodecConfig.SAMPLE_RATE_192000:
                        index = 6;
                        break;
                    default:
                        Log.e(TAG, "Unsupported config:" + config);
                        break;
                }
                callback.returnAndSkip(index);
            });

            hookBefore(writeConfigurationValues, callback -> {
                try {
                    int index = callback.getArgs()[0];
                    int sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_NONE; // default
                    switch (index) {
                        case 0:
                            final BluetoothCodecConfig currentConfig = (BluetoothCodecConfig) getCurrentCodecConfig.invoke(callback.getThisObject());
                            if (currentConfig != null) {
                                int type = currentConfig.getCodecType();
                                if (type != SOURCE_CODEC_TYPE_LHDCV2 &&
                                        type != SOURCE_CODEC_TYPE_LHDCV3 &&
                                        type != SOURCE_CODEC_TYPE_LHDCV5) {
                                    return;
                                }
                            }
                            break;
                        case 1:
                            sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_44100;
                            break;
                        case 2:
                            sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_48000;
                            break;
                        case 3:
                            sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_88200;
                            break;
                        case 4:
                            sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_96000;
                            break;
                        case 5:
                            sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_176400;
                            break;
                        case 6:
                            sampleRateValue = BluetoothCodecConfig.SAMPLE_RATE_192000;
                            break;

                        default:
                            break;
                    }
                    setSampleRate.invoke(mBluetoothA2dpConfigStore.get(callback.getThisObject()), sampleRateValue);
                    callback.returnAndSkip(null);
                } catch (NullPointerException | InvocationTargetException | IllegalAccessException e) {
                    log(TAG + " Exception: ", e);
                }
            });*/

            //BluetoothCodecConfig currentConfig = getCurrentCodecConfig();

        } catch (ClassNotFoundException | NoSuchFieldException |
                 NoSuchMethodException /*| IllegalAccessException*/ e) {
            log(TAG + " Exception: ", e);
        }

    }

    @SuppressWarnings("unused")
    public void recursivePrintMethods(Class<?> clazz) {
        recursivePrintMethods(clazz, 0);
    }

    public void recursivePrintMethods(Class<?> clazz, int level) {
        if (clazz == null) {
            log(TAG + " : Done!");
            return;
        }
        log(TAG + " ".repeat(level) + " : Class " + clazz.getCanonicalName());
        for (Method m : clazz.getDeclaredMethods()) {
            log(TAG + " ".repeat(level + 1) + " : Found method " + m.getName());
        }
        recursivePrintMethods(clazz.getSuperclass(), level + 1);
    }

    @SuppressWarnings("unused")
    public void recursivePrintFields(Class<?> clazz) {
        recursivePrintFields(clazz, 0);
    }

    public void recursivePrintFields(Class<?> clazz, int level) {
        if (clazz == null) {
            log(TAG + " : Done!");
            return;
        }
        log(TAG + " ".repeat(level) + " : Class " + clazz.getCanonicalName());
        for (Field f : clazz.getDeclaredFields()) {
            log(TAG + " ".repeat(level + 1) + " : Found field " + f.getName());
        }
        recursivePrintFields(clazz.getSuperclass(), level + 1);
    }

    public Field recursiveFindField(Class<?> clazz, String name) {
        if (clazz == null) {
            log(TAG + " : Done!");
            return null;
        }
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getName().contains(name)) {
                f.setAccessible(true);
                return f;
            }
        }
        return recursiveFindField(clazz.getSuperclass(), name);
    }

    /* Sometimes we don't know exact parameters, because the classes might be renamed by OEM. Method names are usually still the same though */
    public Method recursiveFindMethodByName(Class<?> clazz, String name) {
        if (clazz == null) {
            log(TAG + " : Not found: " + name);
            return null;
        }
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                m.setAccessible(true);
                return m;
            }
        }
        return recursiveFindMethodByName(clazz.getSuperclass(), name);
    }

    @SuppressWarnings("unused")
    public Class<?> recursiveFindSuperClassByName(Class<?> clazz, String name) {
        if (clazz == null) {
            log(TAG + " : Not found: " + name);
            return null;
        }
        //noinspection ConstantConditions
        if (clazz.getCanonicalName().endsWith(name)) return clazz;
        return recursiveFindSuperClassByName(clazz.getSuperclass(), name);
    }

    @SuppressWarnings("unused")
    public void recursivePrintSuperClass(Class<?> clazz) {
        if (clazz == null) {
            log(TAG + " : Done!");
            return;
        }
        log(TAG + " : Class " + clazz.getCanonicalName());
        recursivePrintSuperClass(clazz.getSuperclass());
    }

    public Method recursiveFindMethod(Class<?> clazz, String name, Class<?>... params) {
        if (clazz == null) {
            log(TAG + " : Not found: " + name);
            return null;
        }
        try {
            Method res = clazz.getDeclaredMethod(name, params);
            res.setAccessible(true);
            return res;
        } catch (NoSuchMethodException e) {
            // Check superclass
            return recursiveFindMethod(clazz.getSuperclass(), name, params);
        }
    }

    /*public void recursiveLogView(View v) {
        recursiveLogView(v, 0);
    }

    public void recursiveLogView(View v, int level) {
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;

            int count = vg.getChildCount();
            log(TAG + " ".repeat(level+1) + vg + " -> " + count + " children");
            for (int i = 0; i < count; i++) {
                recursiveLogView(vg.getChildAt(i), level + 1);
            }
        } else {
            log(TAG + " ".repeat(level+1) + v);
        }
    }*/
}
