package ru.kirddos.exta2dp;

import static ru.kirddos.exta2dp.ConstUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.SharedPreferences;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class ExtA2DPActivity extends Activity {

    enum State {
        Info,
        Settings,
        None;

        int getLayout() {
            return switch (this) {
                case Info -> R.layout.info;
                case Settings -> R.layout.settings;
                case None -> R.layout.none;
            };
        }

        boolean isSettings() {
            return this == Settings;
        }

        boolean isInfo() {
            return this == Info;
        }

        boolean isNone() {
            return this == None;
        }
    }

    private static final String TAG = "ExtA2DPActivity";
    public static final String ACTION_CODEC_CONFIG_CHANGED = "android.bluetooth.a2dp.profile.action.CODEC_CONFIG_CHANGED";
    public static final String ACTIVE_DEVICE_CHANGED = "android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED";
    XposedService xposed;
    BluetoothCodecConfig currentConfig;
    List<BluetoothCodecConfig> localCapabilities;
    List<BluetoothCodecConfig> selectableCapabilities;
    BluetoothA2dp a2dp;
    BluetoothAdapter adapter;
    BluetoothDevice device;
    State tab;
    ViewGroup tabContent;
    ChipGroup codecs;
    ChipGroup sampleRates;
    ChipGroup bitsPerSamples;
    ChipGroup channelModes;
    ChipGroup bitrates;
    TextView bitrateText;

    private final BroadcastReceiver bluetoothA2dpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mBluetoothA2dpReceiver.onReceive intent=" + intent);
            String action = intent.getAction();

            if (ACTION_CODEC_CONFIG_CHANGED.equals(action)) {
                BluetoothCodecStatus codecStatus = intent.getParcelableExtra(
                        BluetoothCodecStatus.EXTRA_CODEC_STATUS, BluetoothCodecStatus.class);
                Log.d(TAG, "Received BluetoothCodecStatus=" + codecStatus);
                setConfigs(codecStatus);
                setA2dpActiveDevice();
                if (tab.isNone()) {
                    setTab(State.Info);
                }
                updateInfo();
            } else if (ACTIVE_DEVICE_CHANGED.equals(action)) {
                if (tab.isNone()) {
                    tab = State.Info;
                }
                Log.d(TAG, "Active device changed");
                processA2dp();
                updateInfo();
            } else {
                Log.d(TAG, "Action: " + action);
            }
        }
    };
    private final BluetoothProfile.ServiceListener bluetoothA2dpServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            a2dp = (BluetoothA2dp) proxy;
            processA2dp();
            updateInfo();
        }

        @Override
        public void onServiceDisconnected(int profile) {
            a2dp = null;
        }
    };


    private final XposedServiceHelper.OnServiceListener xposedListener = new XposedServiceHelper.OnServiceListener() {
        @Override
        public void onServiceBind(@NonNull XposedService xposedService) {
            xposed = xposedService;
            Toast.makeText(ExtA2DPActivity.this, "Got XposedService: " + xposedService, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onServiceDied(@NonNull XposedService xposedService) {
            xposed = null;
        }
    };

    void savePrefs() {
        if (xposed == null) {
            Toast.makeText(this, "XposedService is " + xposed, Toast.LENGTH_LONG).show();
            return;
        }
        /*SharedPreferences priorities = xposed.getRemotePreferences("codec_priority");
        var editor = priorities.edit();

        for (int i = 0; i < CODEC_NAMES.length; i++) {
            editor.putInt(CODEC_NAMES[i], 1000 * i + 1);
        }
        editor.apply();*/
    }

    void setConfigs(BluetoothCodecStatus codecStatus) {
        currentConfig = codecStatus.getCodecConfig();
        localCapabilities = codecStatus.getCodecsLocalCapabilities();
        selectableCapabilities = codecStatus.getCodecsSelectableCapabilities();
        selectableCapabilities.sort(Comparator.comparingInt(BluetoothCodecConfig::getCodecPriority));
    }

    void processA2dp() {
        setA2dpActiveDevice();
        if (device != null && a2dp != null) {
            try {
                BluetoothCodecStatus codecStatus = (BluetoothCodecStatus) HiddenApiBypass.invoke(BluetoothA2dp.class, a2dp, "getCodecStatus", device);
                setConfigs(codecStatus);
                if (tab.isNone()) {
                    setTab(State.Info);
                }
                updateInfo();
            } catch (Exception e) {
                Log.e(TAG, "processA2dp exception", e);
            }
        } else {
            currentConfig = null;
            localCapabilities = null;
            selectableCapabilities = null;
            if (tab.isInfo()) {
                setTab(State.None);
            }

        }
    }

    void setCodecConfigPreference(BluetoothCodecConfig config) {
        BluetoothDevice bluetoothDevice = (device != null) ? device : getA2dpActiveDevice();
        if (bluetoothDevice == null) {
            return;
        }
        try {
            HiddenApiBypass.invoke(a2dp.getClass(), a2dp, "setCodecConfigPreference", bluetoothDevice, config);
        } catch (Exception e) {
            Log.e(TAG, "setCodecConfigPreference exception", e);
        }
    }


    private void setA2dpActiveDevice() {
        List<BluetoothDevice> activeDevices = new ArrayList<>();
        try {
            //noinspection unchecked
            activeDevices = (List<BluetoothDevice>) HiddenApiBypass.invoke(BluetoothAdapter.class, adapter, "getActiveDevices", BluetoothProfile.A2DP);
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        }
        device = (activeDevices.size() > 0) ? activeDevices.get(0) : null;
        Log.d(TAG, "BluetoothDevice: " + device);
    }

    private BluetoothDevice getA2dpActiveDevice() {
        setA2dpActiveDevice();
        return device;
    }

    String resolveLHDC3Codec(BluetoothCodecConfig config) {
        String name = null;
        int qualityIndex = 0;
        if ((config.getCodecSpecific1() & LHDC_QUALITY_DEFAULT_TAG) == LHDC_QUALITY_DEFAULT_MAGIC) {
            qualityIndex = (int) (config.getCodecSpecific1() & 0xFF);
        }
        boolean isLLAC = (config.getCodecSpecific3() & A2DP_LHDC_LLAC_ENABLED) == A2DP_LHDC_LLAC_ENABLED;
        boolean isLHDCV4 = (config.getCodecSpecific3() & A2DP_LHDC_V4_ENABLED) == A2DP_LHDC_V4_ENABLED;
        if (isLLAC && !isLHDCV4) {
            name = "LLAC";
        } else if (!isLLAC && isLHDCV4) {
            name = "LHDC V4";
        } else if (isLLAC && isLHDCV4) {
            if (qualityIndex >= 6 && qualityIndex < 9) {
                name = "probably LHDC V4";
            } else {
                name = "probably LLAC";
            }
        }
        return name;
    }

    String resolveCodecName(BluetoothCodecConfig config, boolean handleLHDC3) {
        int type = config.getCodecType();
        if (handleLHDC3 && type == SOURCE_CODEC_TYPE_LHDCV3) return resolveLHDC3Codec(config);
        String customName = getCustomCodecName(type);
        String name = getCodecName(type);
        return customName == null ? name : customName;
    }

    String resolveCodecQuality(BluetoothCodecConfig config) {
        int type = config.getCodecType();
        if (type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC) {
            long cs1 = config.getCodecSpecific1();
            int qualityIndex = cs1 == 0 ? 3 : (int) (cs1 % 10);
            return getResources().getStringArray(R.array.bluetooth_a2dp_codec_ldac_playback_quality_summaries)[qualityIndex];
        } else if (isLHDC(type)) {
            int qualityIndex = 0;
            if ((config.getCodecSpecific1() & LHDC_QUALITY_DEFAULT_TAG) == LHDC_QUALITY_DEFAULT_MAGIC) {
                qualityIndex = (int) (config.getCodecSpecific1() & 0xFF);
            }
            return getResources().getStringArray(R.array.bluetooth_a2dp_codec_lhdc_playback_quality_summaries)[qualityIndex];
        }
        return "N/A";
    }


    String codecConfigString(BluetoothCodecConfig config) {
        String name = getCustomCodecName(config.getCodecType());
        if (name != null) {
            int qualityIndex = 0;
            if ((config.getCodecSpecific1() & LHDC_QUALITY_DEFAULT_TAG) == LHDC_QUALITY_DEFAULT_MAGIC) {
                qualityIndex = (int) (config.getCodecSpecific1() & 0xFF);
            }
            String extra = "";

            if (config.getCodecType() == SOURCE_CODEC_TYPE_LHDCV3) {
                boolean isLLAC = (config.getCodecSpecific3() & A2DP_LHDC_LLAC_ENABLED) == A2DP_LHDC_LLAC_ENABLED;
                boolean isLHDCV4 = (config.getCodecSpecific3() & A2DP_LHDC_V4_ENABLED) == A2DP_LHDC_V4_ENABLED;
                if (isLLAC && !isLHDCV4) {
                    name = "LLAC";
                } else if (!isLLAC && isLHDCV4) {
                    name = "LHDC V4";
                } else if (isLLAC && isLHDCV4) {
                    if (qualityIndex >= 6 && qualityIndex < 9) {
                        name = "probably LHDC V4";
                    } else {
                        name = "probably LLAC";
                    }
                }
                if ((config.getCodecSpecific3() & A2DP_LHDC_JAS_ENABLED) == A2DP_LHDC_JAS_ENABLED) {
                    extra += "\n" + "JAS enabled"; // WTF is this?
                }
                if ((config.getCodecSpecific3() & A2DP_LHDC_AR_ENABLED) == A2DP_LHDC_AR_ENABLED) {
                    extra += "\n" + "AR enabled";
                }
                if ((config.getCodecSpecific3() & A2DP_LHDC_META_ENABLED) == A2DP_LHDC_META_ENABLED) {
                    extra += "\n" + "META enabled";
                }
                if ((config.getCodecSpecific3() & A2DP_LHDC_MBR_ENABLED) == A2DP_LHDC_MBR_ENABLED) {
                    extra += "\n" + "Min bitrate enabled";
                }
                if ((config.getCodecSpecific3() & A2DP_LHDC_LARC_ENABLED) == A2DP_LHDC_LARC_ENABLED) {
                    extra += "\n" + "LARC enabled"; // And this?
                }
            }

            return "Current codec is " + name +
                    "\nQuality setting is " +
                    getResources().getStringArray(R.array.bluetooth_a2dp_codec_lhdc_playback_quality_summaries)[qualityIndex] +
                    " (" + qualityIndex + ")" + (extra.isBlank() ? "" : "\nExtras:") + extra;
        } else {
            return "Current codec is " + getCodecName(config);
        }
    }

    void setTab() {
        tabContent.removeAllViews();
        tabContent.addView(getLayoutInflater().inflate(tab.getLayout(), null));
        updateInfo();
    }

    void setTab(State x) {
        tab = x;
        setTab();
    }

    void setMainText(BluetoothCodecConfig current) {
        TextView codec = findViewById(R.id.codec);
        TextView quality = findViewById(R.id.quality);
        TextView name = findViewById(R.id.name);
        TextView mac = findViewById(R.id.mac);
        codec.setText(resolveCodecName(current, true));
        quality.setText(resolveCodecQuality(current));

        if (device == null) {
            return;
        }

        mac.setText(device.getAddress());
        try {
            name.setText(device.getName());
        } catch (SecurityException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    void updateInfo() {
        if (tab.isInfo()) {
            codecs = findViewById(R.id.codecs);
            sampleRates = findViewById(R.id.sample_rates);
            bitsPerSamples = findViewById(R.id.bits_per_samples);
            channelModes = findViewById(R.id.channel_modes);
            bitrates = findViewById(R.id.bitrates);
            bitrateText = findViewById(R.id.bitrateText);

            if (currentConfig != null && selectableCapabilities != null && localCapabilities != null) {
                setMainText(currentConfig);
                addCodecChips();
                addSampleRateChips();
                addBitsPerSampleChips();
                addChannelModeChips();
                addBitrateChips();
            }
        } else if (tab.isSettings()) {
            TextView companion = findViewById(R.id.generate_module);
            companion.setOnClickListener(v -> {
                Toast.makeText(this, "Clicked " + v, Toast.LENGTH_SHORT).show();
                savePrefs();
            });
        }
    }

    void addCodecChips() {
        codecs.removeAllViews();
        int id = View.NO_ID;
        if (selectableCapabilities != null) {
            for (var config : selectableCapabilities) {
                String name = resolveCodecName(config, false);
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip, null);
                chip.setId(View.generateViewId());
                chip.setText(name);
                if (config.getCodecType() == currentConfig.getCodecType()) {
                    id = chip.getId();
                }
                chip.setTag(config);
                codecs.addView(chip);
            }
        }
        if (id != View.NO_ID) codecs.check(id);
        codecs.setOnCheckedStateChangeListener((group, checkedIds) -> {
            assert checkedIds.size() == 1;
            Chip chip = findViewById(checkedIds.get(0));
            BluetoothCodecConfig config = (BluetoothCodecConfig) chip.getTag();

            if (config.getCodecType() == currentConfig.getCodecType()) {
                return;
            }

            Toast.makeText(this, "Changed to " + resolveCodecName(config, false), Toast.LENGTH_SHORT).show();
            BluetoothCodecConfig newConfig = new BluetoothCodecConfig.Builder()
                    .setCodecType(config.getCodecType())
                    .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST).build();
            setCodecConfigPreference(newConfig);
        });
    }

    void addSampleRateChips() {
        sampleRates.removeAllViews();
        int id = View.NO_ID;

        int[] rates = {
                BluetoothCodecConfig.SAMPLE_RATE_44100, BluetoothCodecConfig.SAMPLE_RATE_48000,
                BluetoothCodecConfig.SAMPLE_RATE_88200, BluetoothCodecConfig.SAMPLE_RATE_96000,
                BluetoothCodecConfig.SAMPLE_RATE_176400, BluetoothCodecConfig.SAMPLE_RATE_192000
        };

        String[] names = getResources().getStringArray(R.array.bluetooth_a2dp_codec_sample_rate_summaries);

        if (currentConfig != null) {
            BluetoothCodecConfig capability = currentConfig;

            for (var config : selectableCapabilities) {
                if (config.getCodecType() == currentConfig.getCodecType()) {
                    capability = config;
                }
            }

            for (int i = 0; i < rates.length; i++) {
                if ((capability.getSampleRate() & rates[i]) != 0) {
                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip, null);
                    chip.setId(View.generateViewId());
                    chip.setText(names[i + 1]);
                    if (currentConfig.getSampleRate() == rates[i]) {
                        id = chip.getId();
                    }
                    chip.setTag(i);
                    sampleRates.addView(chip);
                }
            }
        }

        if (id != View.NO_ID) sampleRates.check(id);

        sampleRates.setOnCheckedStateChangeListener((group, checkedIds) -> {
            assert checkedIds.size() == 1;
            Chip chip = findViewById(checkedIds.get(0));
            int rate = (int) chip.getTag();
            if (currentConfig.getSampleRate() == rates[rate]) {
                return;
            }
            Toast.makeText(this, "Changed to " + names[rate + 1], Toast.LENGTH_SHORT).show();
            BluetoothCodecConfig newConfig = new BluetoothCodecConfig.Builder()
                    .setCodecType(currentConfig.getCodecType())
                    .setBitsPerSample(currentConfig.getBitsPerSample())
                    .setChannelMode(currentConfig.getChannelMode())
                    .setSampleRate(rates[rate])
                    .setCodecSpecific1(currentConfig.getCodecSpecific1())
                    .setCodecSpecific2(currentConfig.getCodecSpecific2())
                    .setCodecSpecific3(currentConfig.getCodecSpecific3())
                    .setCodecSpecific4(currentConfig.getCodecSpecific4())
                    .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST).build();
            setCodecConfigPreference(newConfig);
        });
    }

    void addBitsPerSampleChips() {
        bitsPerSamples.removeAllViews();
        int id = View.NO_ID;

        int[] bits = {
                BluetoothCodecConfig.BITS_PER_SAMPLE_16, BluetoothCodecConfig.BITS_PER_SAMPLE_24,
                BluetoothCodecConfig.BITS_PER_SAMPLE_32,
        };

        String[] names = {
                "16", "24", "32"
        };

        if (currentConfig != null) {
            BluetoothCodecConfig capability = currentConfig;

            for (var config : selectableCapabilities) {
                if (config.getCodecType() == currentConfig.getCodecType()) {
                    capability = config;
                }
            }

            for (int i = 0; i < bits.length; i++) {
                if ((capability.getBitsPerSample() & bits[i]) != 0) {
                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip, null);
                    chip.setId(View.generateViewId());
                    chip.setText(names[i]);
                    if (currentConfig.getBitsPerSample() == bits[i]) {
                        id = chip.getId();
                    }
                    chip.setTag(i);
                    bitsPerSamples.addView(chip);
                }
            }
        }

        if (id != View.NO_ID) bitsPerSamples.check(id);

        bitsPerSamples.setOnCheckedStateChangeListener((group, checkedIds) -> {
            assert checkedIds.size() == 1;
            Chip chip = findViewById(checkedIds.get(0));
            int b = (int) chip.getTag();
            if (currentConfig.getBitsPerSample() == bits[b]) {
                return;
            }
            Toast.makeText(this, "Changed to " + names[b] + " bits per sample", Toast.LENGTH_SHORT).show();
            BluetoothCodecConfig newConfig = new BluetoothCodecConfig.Builder()
                    .setCodecType(currentConfig.getCodecType())
                    .setBitsPerSample(bits[b])
                    .setChannelMode(currentConfig.getChannelMode())
                    .setSampleRate(currentConfig.getSampleRate())
                    .setCodecSpecific1(currentConfig.getCodecSpecific1())
                    .setCodecSpecific2(currentConfig.getCodecSpecific2())
                    .setCodecSpecific3(currentConfig.getCodecSpecific3())
                    .setCodecSpecific4(currentConfig.getCodecSpecific4())
                    .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST).build();
            setCodecConfigPreference(newConfig);
        });
    }

    void addChannelModeChips() {
        channelModes.removeAllViews();
        int id = View.NO_ID;

        int[] modes = {
                BluetoothCodecConfig.CHANNEL_MODE_MONO, BluetoothCodecConfig.CHANNEL_MODE_STEREO,
        };
        int[] flac = {
                FLAC_MONO, FLAC_STEREO
        };
        String[] names = {
                "Mono", "Stereo"
        };

        if (currentConfig != null) {
            BluetoothCodecConfig capability = currentConfig;

            for (var config : selectableCapabilities) {
                if (config.getCodecType() == currentConfig.getCodecType()) {
                    capability = config;
                }
            }

            int flacChannels = BluetoothCodecConfig.CHANNEL_MODE_STEREO;

            if (currentConfig.getCodecType() == SOURCE_CODEC_TYPE_FLAC) {
                if ((currentConfig.getCodecSpecific1() & FLAC_STEREO_MONO_MASK) == FLAC_MONO) {
                    flacChannels = BluetoothCodecConfig.CHANNEL_MODE_MONO;
                }
                if ((capability.getChannelMode() & modes[0]) == 0) {
                    // FLAC actually supports it
                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip, null);
                    chip.setId(View.generateViewId());
                    chip.setText(names[0]);
                    if (flacChannels == BluetoothCodecConfig.CHANNEL_MODE_MONO) {
                        id = chip.getId();
                    }
                    chip.setTag(0);
                    channelModes.addView(chip);
                }
            }

            for (int i = 0; i < modes.length; i++) {
                if ((capability.getChannelMode() & modes[i]) != 0) {
                    Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip, null);
                    chip.setId(View.generateViewId());
                    chip.setText(names[i]);
                    if (currentConfig.getCodecType() == SOURCE_CODEC_TYPE_FLAC) {
                        if (flacChannels == modes[i]) {
                            id = chip.getId();
                        }
                    } else if (currentConfig.getChannelMode() == modes[i]) {
                        id = chip.getId();
                    }
                    chip.setTag(i);
                    channelModes.addView(chip);
                }
            }
        }

        if (id != View.NO_ID) channelModes.check(id);

        channelModes.setOnCheckedStateChangeListener((group, checkedIds) -> {
            assert checkedIds.size() == 1;
            Chip chip = findViewById(checkedIds.get(0));
            int ch = (int) chip.getTag();
            int ch2 = ch;
            if (currentConfig.getChannelMode() == modes[ch] && currentConfig.getCodecType() != SOURCE_CODEC_TYPE_FLAC) {
                return;
            }
            long cs1 = currentConfig.getCodecSpecific1();

            if (currentConfig.getCodecType() == SOURCE_CODEC_TYPE_FLAC) {
                cs1 = flac[ch];
                ch = 1;
                if ((currentConfig.getCodecSpecific1() & FLAC_STEREO_MONO_MASK) == cs1) {
                    return;
                }
            }
            Toast.makeText(this, "Changed to " + names[ch2], Toast.LENGTH_SHORT).show();
            BluetoothCodecConfig newConfig = new BluetoothCodecConfig.Builder()
                    .setCodecType(currentConfig.getCodecType())
                    .setBitsPerSample(currentConfig.getBitsPerSample())
                    .setChannelMode(modes[ch])
                    .setSampleRate(currentConfig.getSampleRate())
                    .setCodecSpecific1(cs1)
                    .setCodecSpecific2(currentConfig.getCodecSpecific2())
                    .setCodecSpecific3(currentConfig.getCodecSpecific3())
                    .setCodecSpecific4(currentConfig.getCodecSpecific4())
                    .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST).build();
            setCodecConfigPreference(newConfig);
        });
    }

    void addBitrateChips() {
        bitrates.removeAllViews();
        bitrates.setVisibility(View.VISIBLE);
        bitrateText.setVisibility(View.VISIBLE);

        int id = View.NO_ID;

        int type = currentConfig.getCodecType();
        long cs1 = currentConfig.getCodecSpecific1();
        String[] names;
        if (type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC) {
            names = getResources().getStringArray(R.array.bluetooth_a2dp_codec_ldac_playback_quality_summaries);
            for (int i = 0; i < 4; i++) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip, null);
                chip.setId(View.generateViewId());
                chip.setText(names[i]);

                if (cs1 != 0 && (int) (cs1 % 10) == i) {
                    id = chip.getId();
                } else if (cs1 == 0 && i == 3) {
                    id = chip.getId();
                }
                chip.setTag(1000 + i);
                bitrates.addView(chip);
            }

        } else if (isLHDC(type)) {
            names = getResources().getStringArray(R.array.bluetooth_a2dp_codec_lhdc_playback_quality_summaries);
            for (int i = lhdc_quality_index_adjust_offset; i <= LHDC_QUALITY_DEFAULT_MAX_INDEX; i++) {
                if ((currentConfig.getSampleRate() < BluetoothCodecConfig.SAMPLE_RATE_96000 || currentConfig.getCodecType() != SOURCE_CODEC_TYPE_LHDCV5) && i == (8 - lhdc_quality_index_adjust_offset)) {
                    continue;
                }
                if (currentConfig.getSampleRate() >= BluetoothCodecConfig.SAMPLE_RATE_96000 && i < 3) {
                    continue;
                }

                if (currentConfig.getSampleRate() >= BluetoothCodecConfig.SAMPLE_RATE_96000 && currentConfig.getCodecType() == SOURCE_CODEC_TYPE_LHDCV3 && i < 5) {
                    continue;
                }

                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip, null);
                chip.setId(View.generateViewId());
                chip.setText(names[i]);
                if ((int) (cs1 & LHDC_QUALITY_DEFAULT_TAG) == LHDC_QUALITY_DEFAULT_MAGIC && (cs1 & 0xff) == i) {
                    id = chip.getId();
                } else if ((int) (cs1 & LHDC_QUALITY_DEFAULT_TAG) != LHDC_QUALITY_DEFAULT_MAGIC && i == 0) {
                    id = chip.getId();
                }
                chip.setTag(LHDC_QUALITY_DEFAULT_MAGIC | (i + lhdc_quality_index_adjust_offset));
                bitrates.addView(chip);
                Log.d(TAG, i + " -> " + (LHDC_QUALITY_DEFAULT_MAGIC | (i + lhdc_quality_index_adjust_offset)));
            }
        } else if (type == SOURCE_CODEC_TYPE_LC3PLUS_HR) {
            Toast.makeText(this, "Not yet implemented!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            bitrates.setVisibility(View.GONE);
            bitrateText.setVisibility(View.GONE);
            return;
        }

        if (id != View.NO_ID) bitrates.check(id);

        bitrates.setOnCheckedStateChangeListener((group, checkedIds) -> {
            assert checkedIds.size() == 1;
            Chip chip = findViewById(checkedIds.get(0));
            int cs = (int) chip.getTag();

            int idx;
            int curIdx;
            if (type == BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC) {
                idx = cs >= 1000 ? cs % 10 : 3;
                curIdx = currentConfig.getCodecSpecific1() >= 1000 ? (int) (currentConfig.getCodecSpecific1() % 10) : 3;

            } else if (isLHDC(type)) {
                idx = (cs & LHDC_QUALITY_DEFAULT_TAG) == LHDC_QUALITY_DEFAULT_MAGIC ? cs & 0xff : 0;
                curIdx = (cs1 & LHDC_QUALITY_DEFAULT_TAG) == LHDC_QUALITY_DEFAULT_MAGIC ? (int) (cs1 & 0xff) : 0;
            } else if (type == SOURCE_CODEC_TYPE_LC3PLUS_HR) {
                return;
            } else {
                return;
            }

            if (curIdx == idx) {
                return;
            }

            //Toast.makeText(this, "Changed bitrate to " + names[idx], Toast.LENGTH_SHORT).show();
            BluetoothCodecConfig newConfig = new BluetoothCodecConfig.Builder()
                    .setCodecType(currentConfig.getCodecType())
                    .setBitsPerSample(currentConfig.getBitsPerSample())
                    .setChannelMode(currentConfig.getChannelMode())
                    .setSampleRate(currentConfig.getSampleRate())
                    .setCodecSpecific1(cs)
                    .setCodecSpecific2(currentConfig.getCodecSpecific2())
                    .setCodecSpecific3(currentConfig.getCodecSpecific3())
                    .setCodecSpecific4(currentConfig.getCodecSpecific4())
                    .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST).build();
            setCodecConfigPreference(newConfig);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        var windowInsetsController = getWindow().getInsetsController();
        if (windowInsetsController != null) {
            windowInsetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            windowInsetsController.hide(WindowInsets.Type.statusBars());
        }

        HiddenApiBypass.addHiddenApiExemptions("");

        if (checkSelfPermission("android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"}, 2);
            if (SOURCE_CODEC_TYPE_APTX_ADAPTIVE == -1) {
                Toast.makeText(this, "Current ROM is not compatible with ExtA2DP, it will not work!", Toast.LENGTH_LONG).show();
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CODEC_CONFIG_CHANGED);
        filter.addAction(ACTIVE_DEVICE_CHANGED);
        registerReceiver(bluetoothA2dpReceiver, filter);

        XposedServiceHelper.registerListener(xposedListener);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        adapter = bluetoothManager.getAdapter();

        if (adapter != null) {
            adapter.getProfileProxy(this, bluetoothA2dpServiceListener, BluetoothProfile.A2DP);
        }

        tabContent = findViewById(R.id.tab_container);

        BottomNavigationView navigationView = findViewById(R.id.nav);
        navigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.tab_info) {
                if (tab.isInfo()) {
                    return true;
                }
                if (device != null) {
                    tab = State.Info;
                } else {
                    tab = State.None;
                }
            } else if (item.getItemId() == R.id.tab_settings) {
                if (tab.isSettings()) {
                    return true;
                }
                tab = State.Settings;
            } else {
                return false;
            }
            setTab();
            return true;
        });

        if (device == null) {
            setTab(State.None);
        } else {
            setTab(State.Info);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        processA2dp();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putSerializable("TAB", tab);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle bundle){
        super.onRestoreInstanceState(bundle);
        processA2dp();
        setTab(bundle.getSerializable("TAB", State.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothA2dpReceiver);
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        if (adapter != null) {
            adapter.closeProfileProxy(BluetoothProfile.A2DP, a2dp);
        }
    }
}