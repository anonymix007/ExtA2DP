package ru.kirddos.exta2dp.modules;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import ru.kirddos.exta2dp.SourceCodecType;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import static ru.kirddos.exta2dp.ConstUtils.*;

public class SystemFrameworkModule extends XposedModule {
    private static final String TAG = "SystemFrameworkModule";

    public SystemFrameworkModule(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        log(TAG + ": " + param.getProcessName());
    }

    @XposedHooker
    static class FrameworkHooker implements Hooker {
        static ConcurrentHashMap<Member, BeforeCallback> beforeCallbacks = new ConcurrentHashMap<>();
        static ConcurrentHashMap<Member, AfterCallback> afterCallbacks = new ConcurrentHashMap<>();

        @BeforeInvocation
        public static Hooker before(BeforeHookCallback callback) {
            BeforeCallback bc =  beforeCallbacks.get(callback.getMember());
            return bc == null ? null : bc.before(callback);
        }

        @AfterInvocation
        public static void after(AfterHookCallback callback, Hooker state) {
            AfterCallback ac = afterCallbacks.get(callback.getMember());
            if (ac != null) ac.after(callback, state);
        }
    }

    void hookBefore(Method method, BeforeCallback callback) {
        assert !FrameworkHooker.beforeCallbacks.containsKey(method);
        FrameworkHooker.beforeCallbacks.put(method, callback);
        hook(method, FrameworkHooker.class);
    }

    void hookAfter(Method method, AfterCallback callback) {
        assert !FrameworkHooker.afterCallbacks.containsKey(method);
        FrameworkHooker.afterCallbacks.put(method, callback);
        hook(method, FrameworkHooker.class);
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint({"BlockedPrivateApi", "DiscouragedPrivateApi", "PrivateApi"})
    @Override
    public void onPackageLoaded(@NonNull PackageLoadedParam param) {
        try {
            Class<?> btCodecConfig = param.getClassLoader().loadClass("android.bluetooth.BluetoothCodecConfig");

            Method getCodecName = btCodecConfig.getDeclaredMethod("getCodecName", int.class);
            hookBefore(getCodecName, callback -> {

                int type = (int) callback.getArgs()[0];
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
                    return null;
                }

                callback.returnAndSkip(name);
                return null;
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
        log("main classloader is " + this.getClass().getClassLoader());
        log("param classloader is " + param.getClassLoader());
        log("class classloader is " + SourceCodecType.class.getClassLoader());
        log("pid/tid: " + android.os.Process.myPid() + " " + android.os.Process.myTid());
        log("----------");

        log("In android/system");

        try {
            Class<?> audioSystem = param.getClassLoader().loadClass("android.media.AudioSystem");
            Method bluetoothCodecToAudioFormat = audioSystem.getDeclaredMethod("bluetoothCodecToAudioFormat", int.class);

            hookBefore(bluetoothCodecToAudioFormat, callback -> {
                try {
                    int btCodec = (int) callback.getArgs()[0];

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
                return null;
            });
            Method audioFormatToBluetoothSourceCodec = audioSystem.getDeclaredMethod("audioFormatToBluetoothSourceCodec", int.class);

            hookBefore(audioFormatToBluetoothSourceCodec, callback -> {
                try {
                    int audioFormat = (int) callback.getArgs()[0];
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
                return null;
            });

        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log(TAG + " Exception: ", e);
        }
    }


}
