package ru.kirddos.exta2dp.modules;

import io.github.libxposed.api.XposedContext;
import io.github.libxposed.api.XposedModule;
import ru.kirddos.exta2dp.SourceCodecType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
                        callback.returnAndSkip(AUDIO_FORMAT_LHDC/*AUDIO_FORMAT_LHDC*/);
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
