package ru.kirddos.exta2dp.modules;

import io.github.libxposed.api.XposedContext;
import io.github.libxposed.api.XposedModule;
import ru.kirddos.exta2dp.SourceCodecType;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import static ru.kirddos.exta2dp.ConstUtils.*;

public class SystemFrameworkModule extends XposedModule {
    private static final String TAG = "SystemFrameworkModule";
    public SystemFrameworkModule(@NonNull XposedContext base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        log(TAG + ": " + param.getProcessName());
    }
    @SuppressWarnings("ConstantConditions")
    @SuppressLint({"BlockedPrivateApi", "DiscouragedPrivateApi", "PrivateApi"})
    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
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
                } else if (type == SOURCE_CODEC_TYPE_LC3PLUS_HR) {
                    name = "LC3plus HR";
                } else if (type == SOURCE_CODEC_TYPE_FLAC) {
                    name = "FLAC";
                } else {
                    return;
                }

                callback.returnAndSkip(name);
            });
        } catch (ClassNotFoundException | NoSuchMethodException | NullPointerException e) {
            log(TAG + " btCodecConfig: ", e);
        }
    }


    @SuppressWarnings("ConstantConditions")
    @SuppressLint({"BlockedPrivateApi", "DiscouragedPrivateApi", "PrivateApi"})
    @Override
    public void onSystemServerLoaded(@NonNull SystemServerLoadedParam param) {
        super.onSystemServerLoaded(param);

        log("onSystemServerLoaded");
        log("main classloader is " + this.getClassLoader());
        log("param classloader is " + param.getClassLoader());
        log("class classloader is " + SourceCodecType.class.getClassLoader());
        log("module apk path: " + this.getPackageCodePath());
        log("pid/tid: " + android.os.Process.myPid() + " " + android.os.Process.myTid());
        log("----------");

        log("In android/system");

        try {
            Class<?> audioSystem = param.getClassLoader().loadClass("android.media.AudioSystem");
            Method bluetoothCodecToAudioFormat = audioSystem.getDeclaredMethod("bluetoothCodecToAudioFormat", int.class);

            hookBefore(bluetoothCodecToAudioFormat, callback -> {
                try {
                    int btCodec = callback.getArg(0);

                    log(TAG + " bluetoothCodecToAudioFormat: " + btCodec + "->" + (btCodec >= SOURCE_CODEC_TYPE_LHDCV2 ? "AUDIO_FORMAT_LHDC" : "default"));

                    if (btCodec == SOURCE_CODEC_TYPE_LHDCV2 || btCodec == SOURCE_CODEC_TYPE_LHDCV3 || btCodec == SOURCE_CODEC_TYPE_LHDCV5) {
                        callback.returnAndSkip(AUDIO_FORMAT_LHDC);
                    } else if (btCodec == SOURCE_CODEC_TYPE_LC3PLUS_HR) {
                        callback.returnAndSkip(AUDIO_FORMAT_LC3);
                    } else if (btCodec == SOURCE_CODEC_TYPE_FLAC) {
                        callback.returnAndSkip(AUDIO_FORMAT_FLAC);
                    }
                } catch (NullPointerException e) {
                    log(TAG + " Exception: ", e);
                }
            });
            Method audioFormatToBluetoothSourceCodec = audioSystem.getDeclaredMethod("audioFormatToBluetoothSourceCodec", int.class);

            hookBefore(audioFormatToBluetoothSourceCodec, callback -> {
                try {
                    int audioFormat = callback.getArg(0);
                    if (audioFormat == AUDIO_FORMAT_LHDC) {
                        callback.returnAndSkip(SOURCE_CODEC_TYPE_LHDCV3);
                    } else if (audioFormat == AUDIO_FORMAT_LC3) {
                        callback.returnAndSkip(SOURCE_CODEC_TYPE_LC3PLUS_HR);
                    } else if (audioFormat == AUDIO_FORMAT_FLAC) {
                        callback.returnAndSkip(SOURCE_CODEC_TYPE_FLAC);
                    }
                } catch (NullPointerException e) {
                    log(TAG + " Exception: ", e);
                }
            });

        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log(TAG + " Exception: ", e);
        }
    }
}
