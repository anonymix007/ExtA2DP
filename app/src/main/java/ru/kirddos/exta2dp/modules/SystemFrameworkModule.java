package ru.kirddos.exta2dp.modules;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.BeforeInvocation;
import io.github.libxposed.api.annotations.XposedHooker;
import ru.kirddos.exta2dp.SourceCodecType;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import static ru.kirddos.exta2dp.ConstUtils.*;

public class SystemFrameworkModule extends XposedModule {
    private static final String TAG = "SystemFrameworkModule";

    public SystemFrameworkModule(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
        log(TAG + " : " + param.getProcessName());
    }

    @XposedHooker
    static class FrameworkHooker implements Hooker {
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
        var abc = FrameworkHooker.beforeCallbacks.get(method);
        if (abc == null) {
            hook(method, FrameworkHooker.class);
            abc = new ArrayList<>();
            abc.add(callback);
            FrameworkHooker.beforeCallbacks.put(method, abc);
        } else {
            abc.add(callback);
        }
    }

    @SuppressWarnings("unused")
    void hookAfter(Method method, AfterCallback callback) {
        var aac = FrameworkHooker.afterCallbacks.get(method);
        if (aac == null) {
            hook(method, FrameworkHooker.class);
            aac = new ArrayList<>();
            aac.add(callback);
            FrameworkHooker.afterCallbacks.put(method, aac);
        } else {
            aac.add(callback);
        }
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
                //log(TAG + " BluetoothCodecConfig: getCodecName");
                String name = getCustomCodecName(type);
                if (name != null) {
                    callback.returnAndSkip(name);
                }
                return null;
            });
        } catch (ClassNotFoundException | NoSuchMethodException | NullPointerException e) {
            log(TAG + " btCodecConfig: ", e);
        }
    }

    /*
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

                    if (btCodec == SOURCE_CODEC_TYPE_LHDCV2 || btCodec == SOURCE_CODEC_TYPE_LHDCV3 || btCodec == SOURCE_CODEC_TYPE_LHDCV5) {
                        callback.returnAndSkip(AUDIO_FORMAT_FLAC);
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
                        //callback.returnAndSkip(SOURCE_CODEC_TYPE_LHDCV3);
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
    }*/
}
