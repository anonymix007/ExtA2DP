package ru.kirddos.exta2dp.modules;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.libxposed.api.XposedContext;
import io.github.libxposed.api.XposedModule;
import ru.kirddos.exta2dp.R;
import ru.kirddos.exta2dp.SourceCodecType;

import static ru.kirddos.exta2dp.ConstUtils.*;

public class SettingsUIModule extends XposedModule {
    protected static final int[] CODEC_TYPES = {
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

    public SettingsUIModule(@NonNull XposedContext base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        log("SettingsModule: " + param.getProcessName());
    }

    @SuppressLint("DiscouragedPrivateApi")
    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        super.onPackageLoaded(param);
        log(" Exception: " + param.getPackageName());
        log("main classloader is " + this.getClassLoader());
        log("param classloader is " + param.getClassLoader());
        log("class classloader is " + SourceCodecType.class.getClassLoader());
        log("module apk path: " + this.getPackageCodePath());
        log("pid/tid: " + android.os.Process.myPid() + " " + android.os.Process.myTid());
        log("----------");

        if (!param.getPackageName().equals(SETTINGS_PACKAGE) || !param.isFirstPackage()) return;
        log("In settings!");

        try {
            Class<?> btCodecConfig = param.getClassLoader().loadClass("android.bluetooth.BluetoothCodecConfig");

            Method getCodecName = btCodecConfig.getDeclaredMethod("getCodecName", int.class);
            hookBefore(getCodecName, callback -> {

                int type = callback.getArg(0);
                String name;
                //log(TAG + " BluetoothCodecConfig: getCodecName");

                if (type == SOURCE_CODEC_TYPE_LHDCV2) {
                    name = "LHDC V2";
                } else if (type == SOURCE_CODEC_TYPE_LHDCV3) {
                    name = "LHDC V3";
                } else if (type == SOURCE_CODEC_TYPE_LHDCV5) {
                    name = "LHDC V5";
                } else {
                    return;
                }

                callback.returnAndSkip(name);
            });
        } catch (ClassNotFoundException | NoSuchMethodException | NullPointerException e) {
            log(TAG + " btCodecConfig: ", e);
        }

        try {
            Class<?> abstractBtDialogPrefController = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.AbstractBluetoothDialogPreferenceController");

            Method getCurrentCodecConfig = abstractBtDialogPrefController.getDeclaredMethod("getCurrentCodecConfig");
            Method getA2dpActiveDevice = recursiveFindMethod(abstractBtDialogPrefController, "getA2dpActiveDevice");
            getA2dpActiveDevice.setAccessible(true);

            Method getCodecStatus = BluetoothA2dp.class.getDeclaredMethod("getCodecStatus", BluetoothDevice.class);

            Field mBluetoothA2dp = abstractBtDialogPrefController.getSuperclass().getDeclaredField("mBluetoothA2dp");
            mBluetoothA2dp.setAccessible(true);

            log(TAG + " AbstractBluetoothDialogPreferenceController" + mBluetoothA2dp.getType());

            hookBefore(getCurrentCodecConfig, callback -> {
                try {
                    final BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) mBluetoothA2dp.get(callback.getThis());
                    if (bluetoothA2dp == null) {
                        callback.returnAndSkip(null);
                        return;
                    }
                    BluetoothDevice activeDevice = (BluetoothDevice) getA2dpActiveDevice.invoke(callback.getThis());
                    if (activeDevice == null) {
                        Log.d(TAG, "Unable to get current codec config. No active device.");
                        callback.returnAndSkip(null);
                        return;
                    }
                    final BluetoothCodecStatus codecStatus = (BluetoothCodecStatus) getCodecStatus.invoke(bluetoothA2dp, activeDevice);
                    if (codecStatus == null) {
                        Log.d(TAG, "Unable to get current codec config. Codec status is null");
                        callback.returnAndSkip(null);
                        return;
                    }
                    if (codecStatus.getCodecConfig().getCodecType() >= SOURCE_CODEC_TYPE_LHDCV2) {
                        Log.d(TAG, "LHDC codec type");
                    }
                    callback.returnAndSkip(codecStatus.getCodecConfig());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log(TAG + " abstractBtDialogPrefController: ", e);
                }
            });


            Method getHighestCodec = abstractBtDialogPrefController.getDeclaredMethod("getHighestCodec", BluetoothA2dp.class, BluetoothDevice.class, List.class);
            getHighestCodec.setAccessible(true);
            hookBefore(getHighestCodec, callback -> {
                List<BluetoothCodecConfig> configs = callback.getArg(2);
                for (int codecType : CODEC_TYPES) {
                    for (BluetoothCodecConfig config : configs) {
                        if (config.getCodecType() == codecType) {
                            callback.returnAndSkip(codecType);
                            return;
                        }
                    }
                }
                callback.returnAndSkip(BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID);
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

            Class<?> customPreferenceDialogFragment = recursiveFindField( baseBtCodecDialogPreference, "mFragment").getType();
            Method onCreateDialogView = recursiveFindMethod(customPreferenceDialogFragment, "onCreateDialogView", Context.class);
            Field mDialogLayoutRes = recursiveFindField(customPreferenceDialogFragment, "mDialogLayoutRes");

            hookBefore(onCreateDialogView, callback -> {
                try {
                    int resId = (int) mDialogLayoutRes.get(callback.getThis());
                    if (resId == R.layout.bluetooth_lhdc_audio_quality_dialog) {
                        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        callback.returnAndSkip(inflater.inflate(resId, null));
                    }
                } catch (IllegalAccessException e) {
                    log(TAG + " Exception: ", e);
                }
            });

            onBindDialogView = baseBtCodecDialogPreference.getDeclaredMethod("onBindDialogView", View.class);
            getRadioButtonGroupId = baseBtCodecDialogPreference.getDeclaredMethod("getRadioButtonGroupId");
            getRadioButtonGroupId.setAccessible(true);
            Method finalCallbackGetIndex = callbackGetIndex;
            Method finalCallbackGetSelectableIndex = callbackGetSelectableIndex;
            Method finalGetRadioButtonGroupId = getRadioButtonGroupId;
            hookAfter(onBindDialogView, callback -> {
                View view = (View) callback.getArgs()[0];
                log(TAG + " hookOnBindDialogView " + view);
                try {
                    ArrayList<Integer> mRadioButtonIds = (ArrayList<Integer>) radioButtonIds.get(callback.getThis());
                    ArrayList<String> mRadioButtonStrings = (ArrayList<String>) radioButtonStrings.get(callback.getThis());

                    Object mCallback = iCallback.get(callback.getThis());
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

                    int viewId = (Integer) finalGetRadioButtonGroupId.invoke(callback.getThis());
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


                    if (radioGroup == null) radioGroup = (RadioGroup) inflater.inflate(resource, null);

                    if (layout.getChildAt(0) != radioGroup) {
                        layout.removeView(layout.getChildAt(0));
                        layout.addView(radioGroup, 0);
                    }

                    radioGroup.check(mRadioButtonIds.get(currentIndex));
                    radioGroup.setOnCheckedChangeListener((RadioGroup.OnCheckedChangeListener) callback.getThis());
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

            Method initialize = null;
            getRadioButtonGroupId = null;
            /* for (Method m : btCodecDialogPreference.getDeclaredMethods()) {
                log(": Found method " + m.getName());
                if (m.getName().equals("initialize")) {
                    initialize = m;
                } else if (m.getName().equals("getRadioButtonGroupId")) {
                    getRadioButtonGroupId = m;
                }
            }  */

            //if (initialize == null)
            initialize = btCodecDialogPreference.getDeclaredMethod("initialize", Context.class);
            //if (getRadioButtonGroupId == null)
            getRadioButtonGroupId = btCodecDialogPreference.getDeclaredMethod("getRadioButtonGroupId");
            getRadioButtonGroupId.setAccessible(true);
            hookBefore(getRadioButtonGroupId, callback -> callback.returnAndSkip(R.id.bluetooth_audio_codec_radio_group));

            hookBefore(initialize, callback -> {
                try {
                    ArrayList<Integer> mRadioButtonIds = (ArrayList<Integer>) radioButtonIds.get(callback.getThis());
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
                    String[] stringArray = getResources().getStringArray(
                            R.array.bluetooth_a2dp_codec_titles);

                    log("a2dp_codec_titles array length: " + stringArray.length);

                    log("a2dp_codec_titles array: " + Arrays.toString(stringArray));

                    ArrayList<String> mRadioButtonStrings = (ArrayList<String>) radioButtonStrings.get(callback.getThis());

                    mRadioButtonStrings.addAll(Arrays.asList(stringArray));

                    stringArray = getResources().getStringArray(R.array.bluetooth_a2dp_codec_summaries);
                    log("a2dp_codec_summaries array length: " + stringArray.length);
                    log("a2dp_codec_summaries array: " + Arrays.toString(stringArray));
                    ArrayList<String> mSummaryStrings = (ArrayList<String>) summaryStrings.get(callback.getThis());
                    mSummaryStrings.addAll(Arrays.asList(stringArray));
                } catch (IllegalAccessException | NullPointerException e) {
                    log("Exception: ", e);
                }
                callback.returnAndSkip(null);
            });

            Class<?> btCodecDialogPreferenceController = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BluetoothCodecDialogPreferenceController");
            Method convertCfgToBtnIndex = btCodecDialogPreferenceController.getDeclaredMethod("convertCfgToBtnIndex", int.class);
            Method writeConfigurationValues = btCodecDialogPreferenceController.getDeclaredMethod("writeConfigurationValues", int.class);

            Field btA2dpConfigStore = btCodecDialogPreferenceController.getSuperclass().getDeclaredField("mBluetoothA2dpConfigStore");
            btA2dpConfigStore.setAccessible(true);
            Class<?> btA2dpConfigStoreClass = btA2dpConfigStore.getType();

            /*Method setSampleRate = null;
            Method setBitsPerSample = null;
            Method setChannelMode = null;
            Method setCodecType = null;
            Method setCodecPriority = null;
            Method setCodecSpecific1Value = null;
            Method setCodecSpecific2Value = null;
            Method setCodecSpecific3Value = null;
            Method setCodecSpecific4Value = null;
            for (Method m : btA2dpConfigStoreClass.getDeclaredMethods()) {
                log(": Found method " + m.getName());
                if (m.getName().equals("setSampleRate")) {
                    setSampleRate = m;
                } else if (m.getName().equals("setBitsPerSample")) {
                    setBitsPerSample = m;
                } else if (m.getName().equals("setChannelMode")) {
                    setChannelMode = m;
                } else if (m.getName().equals("setCodecPriority")) {
                    setCodecPriority = m;
                } else if (m.getName().equals("setCodecType")) {
                    setCodecType = m;
                }
            }  */

            //if (setSampleRate == null)
            Method setSampleRate = btA2dpConfigStoreClass.getDeclaredMethod("setSampleRate", int.class);
            //if (setBitsPerSample == null)
            Method setBitsPerSample = btA2dpConfigStoreClass.getDeclaredMethod("setBitsPerSample", int.class);
            //if (setChannelMode == null)
            Method setChannelMode = btA2dpConfigStoreClass.getDeclaredMethod("setChannelMode", int.class);
            //if (setCodecType == null)
            Method setCodecType = btA2dpConfigStoreClass.getDeclaredMethod("setCodecType", int.class);
            //if (setCodecPriority == null)
            Method setCodecPriority = btA2dpConfigStoreClass.getDeclaredMethod("setCodecPriority", int.class);
            Method setCodecSpecific1Value = btA2dpConfigStoreClass.getDeclaredMethod("setCodecSpecific1Value", long.class);
            //Method setCodecSpecific2Value = btA2dpConfigStoreClass.getDeclaredMethod("setCodecSpecific2Value", int.class);
            //Method setCodecSpecific3Value = btA2dpConfigStoreClass.getDeclaredMethod("setCodecSpecific3Value", int.class);
            //Method setCodecSpecific4Value = btA2dpConfigStoreClass.getDeclaredMethod("setCodecSpecific4Value", int.class);

            hookBefore(writeConfigurationValues, callback -> {
                try {
                    int index = callback.getArg(0);
                    log(TAG + " Hooked BluetoothCodecDialogPreferenceController.writeConfigurationValues: type " + index);
                    int type;
                    switch (index) {
                        case 8:
                            type = SOURCE_CODEC_TYPE_LHDCV2;
                            break;
                        case 9:
                            type = SOURCE_CODEC_TYPE_LHDCV3;
                            break;
                        case 10:
                            type = SOURCE_CODEC_TYPE_LHDCV5;
                            break;
                        default:
                            return;
                    }
                    setCodecType.invoke(btA2dpConfigStore.get(callback.getThis()), type);
                    setCodecPriority.invoke(btA2dpConfigStore.get(callback.getThis()), BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST);
                    setSampleRate.invoke(btA2dpConfigStore.get(callback.getThis()), BluetoothCodecConfig.SAMPLE_RATE_NONE);
                    setBitsPerSample.invoke(btA2dpConfigStore.get(callback.getThis()), BluetoothCodecConfig.BITS_PER_SAMPLE_NONE);
                    setChannelMode.invoke(btA2dpConfigStore.get(callback.getThis()), BluetoothCodecConfig.CHANNEL_MODE_NONE);
                    callback.returnAndSkip(null);
                } catch (NullPointerException | IllegalAccessException |
                         InvocationTargetException e) {
                    log(TAG + " Exception: ", e);
                }
            });

            hookBefore(convertCfgToBtnIndex, callback -> {
                try {
                    int type = callback.getArg(0);
                    int index = 0;

                    if (type == SOURCE_CODEC_TYPE_LHDCV2) {
                        index = 8;
                    } else if (type == SOURCE_CODEC_TYPE_LHDCV3) {
                        index = 9;
                    } else if (type == SOURCE_CODEC_TYPE_LHDCV5) {
                        index = 10;
                    }

                    log(TAG + " Hooked BluetoothCodecDialogPreferenceController.convertCfgToBtnIndex: type = " + type + ", index = " + index);

                    if (index != 0) { // Handle LHDC 2/3/4/5 only
                        callback.returnAndSkip(index);
                    }
                } catch (NullPointerException e) {
                    log(TAG + " Exception: ", e);
                }
            });

            Class<?> btCodecDialogSampleRatePreference = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BluetoothSampleRateDialogPreference");

            /*initialize = null;
            getRadioButtonGroupId = null;
            for (Method m : btCodecDialogSampleRatePreference.getDeclaredMethods()) {
                log(": Found method " + m.getName());
                if (m.getName().equals("initialize")) {
                    initialize = m;
                } else if (m.getName().equals("getRadioButtonGroupId")) {
                    getRadioButtonGroupId = m;
                }
            }*/
            //if (initialize == null)
            initialize = btCodecDialogSampleRatePreference.getDeclaredMethod("initialize", Context.class);
            //if (getRadioButtonGroupId == null)
            getRadioButtonGroupId = btCodecDialogSampleRatePreference.getDeclaredMethod("getRadioButtonGroupId");

            initialize.setAccessible(true);
            getRadioButtonGroupId.setAccessible(true);

            hookBefore(getRadioButtonGroupId, callback -> callback.returnAndSkip(R.id.bluetooth_audio_sample_rate_radio_group));
            hookBefore(initialize, callback -> {
                try {
                    ArrayList<Integer> mRadioButtonIds = (ArrayList<Integer>) radioButtonIds.get(callback.getThis());
                    ArrayList<String> mRadioButtonStrings = (ArrayList<String>) radioButtonStrings.get(callback.getThis());
                    ArrayList<String> mSummaryStrings = (ArrayList<String>) summaryStrings.get(callback.getThis());
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_default);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_441);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_480);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_882);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_960);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_1764);
                    mRadioButtonIds.add(R.id.bluetooth_audio_sample_rate_1920);
                    String[] stringArray = getResources().getStringArray(
                                R.array.bluetooth_a2dp_codec_sample_rate_titles);
                    for (int i = 0; i < stringArray.length; i++) {
                        mRadioButtonStrings.add(stringArray[i]);
                    }
                    log("a2dp_codec_sample_rate_titles array: " + Arrays.toString(stringArray));
                    stringArray = getResources().getStringArray(
                            R.array.bluetooth_a2dp_codec_sample_rate_summaries);
                    for (int i = 0; i < stringArray.length; i++) {
                        mSummaryStrings.add(stringArray[i]);
                    }
                    radioButtonIds.set(callback.getThis(), mRadioButtonIds);
                    radioButtonStrings.set(callback.getThis(), mRadioButtonStrings);
                    summaryStrings.set(callback.getThis(), mSummaryStrings);
                    log("a2dp_codec_sample_rate_summaries array: " + Arrays.toString(stringArray));
                } catch (IllegalAccessException | NullPointerException e) {
                    log("Exception: ", e);
                }
                callback.returnAndSkip(null);
            });
            Class<?> ldacQualityPref = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.BluetoothQualityDialogPreference");

            getRadioButtonGroupId = ldacQualityPref.getDeclaredMethod("getRadioButtonGroupId");
            initialize = ldacQualityPref.getDeclaredMethod("initialize", Context.class);;

            Method getKey = recursiveFindMethod(ldacQualityPref,"getKey");

            Method finalGetRadioButtonGroupId1 = getRadioButtonGroupId;
            getRadioButtonGroupId.setAccessible(true);
            hookBefore(initialize, callback -> {
                try {
                    //int resId = (int) mDialogLayoutResId.get(callback.getThis());

                    //int resId = (int) finalGetRadioButtonGroupId1.invoke(callback.getThis());

                    String key = (String) getKey.invoke(callback.getThis());

                    if (key == null || key.equals("ldac")) { //Dirty hack: if no key is present, this must be LHDC
                        //resId = R.id.bluetooth_lhdc_audio_quality_scroll_view;
                        log(TAG + " LHDC Quality: initialize");
                        ArrayList<Integer> mRadioButtonIds = (ArrayList<Integer>) radioButtonIds.get(callback.getThis());
                        ArrayList<String> mRadioButtonStrings = (ArrayList<String>) radioButtonStrings.get(callback.getThis());
                        ArrayList<String> mSummaryStrings = (ArrayList<String>) summaryStrings.get(callback.getThis());
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
                        String[] stringArray= getResources().getStringArray(
                                    R.array.bluetooth_a2dp_codec_lhdc_playback_quality_titles);

                        for (int i = 0; i < stringArray.length; i++) {
                            mRadioButtonStrings.add(stringArray[i]);
                        }
                        log("a2dp_codec_lhdc_quality_titles array: " + Arrays.toString(stringArray));
                        stringArray = getResources().getStringArray(
                                R.array.bluetooth_a2dp_codec_lhdc_playback_quality_summaries);
                        for (int i = 0; i < stringArray.length; i++) {
                            mSummaryStrings.add(stringArray[i]);
                        }
                        log("a2dp_codec_lhdc_quality_summaries array: " + Arrays.toString(stringArray));
                        radioButtonIds.set(callback.getThis(), mRadioButtonIds);
                        radioButtonStrings.set(callback.getThis(), mRadioButtonStrings);
                        summaryStrings.set(callback.getThis(), mSummaryStrings);
                        callback.returnAndSkip(null);
                    }
                } catch (IllegalAccessException | NullPointerException | InvocationTargetException e) {
                    log("Exception: ", e);
                }
            });

        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException | NullPointerException /*|
                 IllegalAccessException*/ e) {
            log(TAG + " Exception: ", e);
        }

        try{
            Class<?> devSettingsDashboardFragment = param.getClassLoader().loadClass("com.android.settings.development.DevelopmentSettingsDashboardFragment");

            Class<?> lifecycleClass = param.getClassLoader().loadClass("com.android.settingslib.core.lifecycle.Lifecycle");

            Class<?> btCodecDialogPreferenceController = param.getClassLoader().loadClass("com.android.settings.development.bluetooth.AbstractBluetoothDialogPreferenceController");
            Field mBluetoothA2dpConfigStore = btCodecDialogPreferenceController.getDeclaredField("mBluetoothA2dpConfigStore");
            Field mPreferenceControllers = devSettingsDashboardFragment.getDeclaredField("mPreferenceControllers");
            mBluetoothA2dpConfigStore.setAccessible(true);
            mPreferenceControllers.setAccessible(true);
            Class<?> btA2dpConfigStore = mBluetoothA2dpConfigStore.getType();
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
            Method displayPreference = ldacPrefControl.getDeclaredMethod("displayPreference", preferenceScreenClass);
            Method getPreferenceKey = ldacPrefControl.getDeclaredMethod("getPreferenceKey");
            Method refreshSummary = recursiveFindMethodByName(ldacPrefControl, "refreshSummary"); //  Preference once again
            //Method getKey = recursiveFindMethod(mPreference.getType(),"getKey");


            hookAfter(buildPreferenceControllers, callback -> {
                try {
                    ArrayList<Object> controllers = (ArrayList<Object>) callback.getResult();
                    Object context = callback.getArgs()[0];
                    Object activity = callback.getArgs()[1];
                    Object lifecycle = callback.getArgs()[2];
                    Object fragment = callback.getArgs()[3];
                    Object store = callback.getArgs()[4];

                    Object controller = ldacPrefControl.getDeclaredConstructor(Context.class, lifecycleClass, btA2dpConfigStore).newInstance(context, lifecycle, store);

                    hookBefore(getPreferenceKey, x -> {
                        if (x.getThis() == controller) x.returnAndSkip("bluetooth_select_a2dp_lhdc_playback_quality");
                    });

                    //controllers.add(new BluetoothLHDCQualityDialogPreferenceController((Context) context, (Lifecycle) lifecycle, (BluetoothA2dpConfigStore) store));

                    log(TAG + " LHDC Controller: " + controller);

                    controllers.add(controller);
                    callback.setResult(controllers);
                } catch (NullPointerException | InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException e) {
                    log(TAG + " Exception: ", e);
                }
            });
            hookBefore(displayPreference, callback -> {
                try {
                    //Object preferenceScreen = callback.getArg(0);
                    //Object lhdcPreference = findPreference.invoke(preferenceScreen, "bluetooth_select_a2dp_lhdc_playback_quality");

                    //log(TAG + " LHDC Preference: " + lhdcPreference);

                    //final BluetoothCodecConfig currentConfig = (BluetoothCodecConfig) getCurrentCodecConfig.invoke(callback.getThis());
                    //if (currentConfig != null && currentConfig.getCodecType() == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC) {
                    //    return;
                    //}
                    String key = (String) getPreferenceKey.invoke(callback.getThis());
                    if (key.contains("bluetooth_select_a2dp_lhdc_playback_quality")) {
                        log(TAG + " We're now LHDC controller!");
                        if (mPreference.get(callback.getThis()) == null) {
                            callback.returnAndSkip(null);
                        }
                    } else {
                        log(TAG + " We're still LDAC controller!");
                    }
                    //mPreference.set(callback.getThis(), lhdcPreference);
                    //hookBefore(getPreferenceKey, x -> x.returnAndSkip("bluetooth_select_a2dp_lhdc_playback_quality"));
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException e) {
                    log(TAG + "Exception: ", e);
                }
            });

            hookBefore(writeConfigurationValues, callback -> {
                try {
                    //Object pref = mPreference.get(callback.getThis());
                    //Class<?> prefClass = pref.getClass();
                    //Method getRadioButtonGroupId = prefClass.getDeclaredMethod("getRadioButtonGroupId");
                    //getRadioButtonGroupId.setAccessible(true);
                    //int resId = (int) getRadioButtonGroupId.invoke(pref);
                    final int index = callback.getArg(0);

                    String key = (String) getPreferenceKey.invoke(callback.getThis());
                    if (key.contains("bluetooth_select_a2dp_lhdc_playback_quality")) {
                        log(TAG + " in LHDC controller: writeConfigurationValues");
                        long codecSpecific1Value;
                        if (index <= LHDC_QUALITY_DEFAULT_MAX_INDEX) {
                            codecSpecific1Value = LHDC_QUALITY_DEFAULT_MAGIC | (index + lhdc_quality_index_adjust_offset);
                        } else {
                            codecSpecific1Value = LHDC_QUALITY_DEFAULT_MAGIC | LHDC_QUALITY_DEFAULT_INDEX;
                        }
                        setCodecSpecific1Value.invoke(mBluetoothA2dpConfigStore.get(callback.getThis()),  codecSpecific1Value);
                        callback.returnAndSkip(null);
                    }
                    //pref.getClass().getDeclaredMethod("");
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException e) {
                    log(TAG + "Exception: ", e);
                }
            });
            /*hookBefore(getCurrentIndexByConfig, callback -> {
                try {
                    Object pref = mPreference.get(callback.getThis());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });*/
            hookBefore(getSelectableIndex, callback -> {
                try {
                    //Object pref = mPreference.get(callback.getThis());
                    //Class<?> prefClass = pref.getClass();
                    //Method getRadioButtonGroupId = prefClass.getDeclaredMethod("getRadioButtonGroupId");
                    //getRadioButtonGroupId.setAccessible(true);
                    //int resId = (int) getRadioButtonGroupId.invoke(pref);
                    String key = (String) getPreferenceKey.invoke(callback.getThis());

                    if (key.contains("bluetooth_select_a2dp_lhdc_playback_quality")) {
                        log(TAG + " in LHDC controller: getSelectableIndex");
                        List<Integer> selectableIndex = new ArrayList<>();
                        final BluetoothCodecConfig currentConfig = (BluetoothCodecConfig) getCurrentCodecConfig.invoke(callback.getThis());
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
                                // All items of LHDCV5 are available.
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
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException e) {
                    log(TAG + "Exception: ", e);
                }
            });
            hookAfter(updateState, callback -> {
                try {
                    Object pref = mPreference.get(callback.getThis());
                    Class<?> prefClass = pref.getClass();

                    String key = (String) getPreferenceKey.invoke(callback.getThis());
                    if (!key.contains("bluetooth_select_a2dp_lhdc_playback_quality")) {
                        return;
                    }

                    Object preference = callback.getArgs()[0];
                    Method setEnabled = recursiveFindMethod(prefClass, "setEnabled", boolean.class);



                    Field summaryStrings = recursiveFindField(prefClass,"mSummaryStrings");
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
                        final BluetoothCodecConfig currentConfig = (BluetoothCodecConfig) getCurrentCodecConfig.invoke(callback.getThis());
                        if (currentConfig != null) {
                            int type = currentConfig.getCodecType();
                            if (type == SOURCE_CODEC_TYPE_LHDCV2 || type == SOURCE_CODEC_TYPE_LHDCV3 || type == SOURCE_CODEC_TYPE_LHDCV5) {
                                setEnabled.invoke(preference,true);
                                refreshSummary.invoke(callback.getThis(), preference);
                                //setSummary.invoke(preference, getResources().getString(R.string.bluetooth_select_a2dp_codec_lhdc_playback_quality));
                            } else {
                                setEnabled.invoke(preference,false);
                            }
                        }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException e) {
                    log(TAG + "Exception: ", e);
                }
            });
            hookBefore(convertCfgToBtnIndex, callback -> {
                try {
                    Object pref = mPreference.get(callback.getThis());
                    Class<?> prefClass = pref.getClass();
                    Method getRadioButtonGroupId = prefClass.getDeclaredMethod("getRadioButtonGroupId");
                    getRadioButtonGroupId.setAccessible(true);
                    final int config = (int) callback.getArg(0);
                    String key = (String) getPreferenceKey.invoke(callback.getThis());
                    if (!key.contains("bluetooth_select_a2dp_lhdc_playback_quality")) {
                        return;
                    }

                    log(TAG + " in LHDC controller: convertCfgToBtnIndex");
                    int index = config;
                    int tmp = config & LHDC_QUALITY_DEFAULT_TAG;  //0xC000
                    if (tmp != LHDC_QUALITY_DEFAULT_MAGIC) {  //0x8000
                        index = 0;
                    } else {
                        index &= 0xff;
                    }
                    callback.returnAndSkip(index);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException | NoSuchMethodException e) {
                    log(TAG + "Exception: ", e);
                }
            });
            hookAfter(onCreateView, callback -> {
                try {
                    //Bundle savedInstanceState = (Bundle) callback.getArgs()[0];
                    Object preferenceScreen = getPreferenceScreen.invoke(callback.getThis());
                    //Class<?> preferenceScreenClass = preferenceScreen.getClass();
                    //PreferenceScreen x;

                    //p

                    //Method findPreference = preferenceScreenClass.getDeclaredMethod("findPreference", CharSequence.class);
                    //findPreference.setAccessible(true);

                    Object ldacQualityPreference = findPreference.invoke(preferenceScreen, "bluetooth_a2dp_ldac_playback_quality");

                    Class<?> ldacPrefClass = ldacQualityPreference.getClass();

                    Method getRadioButtonGroupId = ldacPrefClass.getDeclaredMethod("getRadioButtonGroupId");
                    getRadioButtonGroupId.setAccessible(true);
                    Method setKey = recursiveFindMethod(ldacPrefClass,"setKey", String.class);
                    Method getKey = recursiveFindMethod(ldacPrefClass,"getKey");
                    Method setEnabled = recursiveFindMethod(ldacPrefClass,"setEnabled", boolean.class);
                    Method setDialogTitle = recursiveFindMethod(ldacPrefClass,"setDialogTitle", CharSequence.class);
                    Method setSummary = recursiveFindMethod(ldacPrefClass, "setSummary", CharSequence.class);

                    int prefCount = (int) getPreferenceCount.invoke(preferenceScreen);

                    //log(TAG + " : Preference Count: " + prefCount);

                    Object networking = null;

                    for (int i = 0; i < prefCount; i++) {
                        Object currPref = getPreference.invoke(preferenceScreen, i);
                        String title = String.valueOf((CharSequence) getTitle.invoke(currPref));
                        //log(TAG + " : Pref " + i + " -> " + title);

                        if (title.toLowerCase().contains("networking")) {
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
                                if (x.getThis() == lhdcQualityPreference) x.returnAndSkip(R.id.bluetooth_lhdc_audio_quality_radio_group);
                    });

                    ArrayList<Object> controllers = (ArrayList<Object>) mPreferenceControllers.get(callback.getThis());

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
                    }
                } catch (NullPointerException | InvocationTargetException | IllegalAccessException | /*NoSuchFieldException |*/ InstantiationException | NoSuchMethodException e) {
                   log(TAG + " Exception: ", e);
                }

            });

        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException /*| IllegalAccessException*/ e) {
            log(TAG + " Exception: ", e);
        }
        
    }
    public void recursivePrintMethods(Class<?> clazz) { recursivePrintMethods(clazz, 0); }

    public void recursivePrintMethods(Class<?> clazz, int level) {
        if (clazz == null) {
            log(TAG+ " : Done!");
            return;
        }
        log(TAG + " " + " ".repeat(level) + ": Class " + clazz.getCanonicalName());
        for (Method m : clazz.getDeclaredMethods()) {
            log(TAG + " " + " ".repeat(level+1) + ": Found method " + m.getName());
        }
        recursivePrintMethods(clazz.getSuperclass(), level + 1);
    }
    public void recursivePrintFields(Class<?> clazz) { recursivePrintFields(clazz, 0); }
    public void recursivePrintFields(Class<?> clazz, int level) {
        if (clazz == null) {
            log(TAG+ " : Done!");
            return;
        }
        log(TAG + " " + " ".repeat(level) + ": Class " + clazz.getCanonicalName());
        for (Field f : clazz.getDeclaredFields()) {
            log(TAG + " " + " ".repeat(level+1) + ": Found field " + f.getName());
        }
        recursivePrintFields(clazz.getSuperclass(), level + 1);
    }

    public Field recursiveFindField(Class<?> clazz, String name) {
        if (clazz == null) {
            log(TAG+ " : Done!");
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

    /* Sometimes we don't now exact parameters, because the classes might be renamed by OEM. Method names are usually still the same though */
    public Method recursiveFindMethodByName(Class<?> clazz, String name) {
        if (clazz == null) {
            log(TAG+ " : Not found: " + name);
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

    public Class<?> recursiveFindSuperClassByName(Class<?> clazz, String name) {
        if (clazz == null) {
            log(TAG+ " : Not found: " + name);
            return null;
        }
        if(clazz.getCanonicalName().endsWith(name)) return clazz;
        return recursiveFindSuperClassByName(clazz.getSuperclass(), name);
    }
    public void recursivePrintSuperClass(Class<?> clazz) {
        if (clazz == null) {
            log(TAG+ " : Done!");
            return;
        }
        log(TAG+ " : Class " + clazz.getCanonicalName());
        recursivePrintSuperClass(clazz.getSuperclass());
    }

    public Method recursiveFindMethod(Class<?> clazz, String name, Class<?> ... params) {
        if (clazz == null) {
            log(TAG+ " : Not found: " + name);
            return null;
        }
        try {
            Method res = clazz.getDeclaredMethod(name, params);
            res.setAccessible(true);
            return res;
        } catch (NoSuchMethodException e) { // Maybe it's in superclass?
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
