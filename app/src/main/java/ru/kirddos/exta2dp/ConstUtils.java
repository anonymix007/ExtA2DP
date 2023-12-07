package ru.kirddos.exta2dp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothCodecConfig;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

public class ConstUtils {

    //public static final int SOURCE_QVA_CODEC_TYPE_MAX_EXTA2DP = 14;

    public static final int SOURCE_QVA_CODEC_TYPE_MAX = SourceCodecType.getQVA();

    private static final int DEFAULT_LHDCV3 = SOURCE_QVA_CODEC_TYPE_MAX+1;
    private static final int DEFAULT_LHDCV2 = SOURCE_QVA_CODEC_TYPE_MAX+2;
    private static final int DEFAULT_LHDCV5 = SOURCE_QVA_CODEC_TYPE_MAX+3;

    public static final int SOURCE_CODEC_TYPE_LHDCV2 = SourceCodecType.getIdByName("LHDCV2", DEFAULT_LHDCV2);
    public static final int SOURCE_CODEC_TYPE_LHDCV3 = SourceCodecType.getIdByName("LHDCV3", DEFAULT_LHDCV3);
    public static final int SOURCE_CODEC_TYPE_LHDCV5 = SourceCodecType.getIdByName("LHDCV5", DEFAULT_LHDCV5);

    public static final int SOURCE_CODEC_TYPE_LC3PLUS_HR = SOURCE_QVA_CODEC_TYPE_MAX + 4;
    public static final int SOURCE_CODEC_TYPE_FLAC = SOURCE_QVA_CODEC_TYPE_MAX + 5;

    public static final int SOURCE_CODEC_TYPE_OPUS = SourceCodecType.getIdByName("OPUS");

    public static final int SOURCE_CODEC_TYPE_APTX_TWSP = SourceCodecType.getIdByName("APTX_TWSP");
    public static final int SOURCE_CODEC_TYPE_APTX_ADAPTIVE = SourceCodecType.getIdByName("APTX_ADAPTIVE");


    @SuppressLint("NewApi")
    public static final int[] CODEC_IDS = {
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

    public static final String[] CODEC_NAMES = {
            "SBC", "AAC", "aptX", "aptX HD", "LDAC", "LC3",
            "Opus", "aptX Adaptive", "aptX TWSP", "LHDC V3",
            "LHDC V2", "LHDC V5", "LC3plus HR", "FLAC"
    };

    //public static final int AUDIO_FORMAT_LHDC = 0x28000000;
    //public static final int AUDIO_FORMAT_LHDC_LL = 0x29000000;
    //public static final int AUDIO_FORMAT_FLAC = 0x1B000000;
    //public static final int AUDIO_FORMAT_LC3 = 0x2B000000;

    public static final int FLAC_STEREO_MONO_MASK = 0xF;
    public static final int FLAC_STEREO = 0x2;
    public static final int FLAC_MONO = 0x1;

    // In standard case, low0 is available
    public static final int lhdc_quality_index_adjust_offset = 0;
    // In case of low0 is removed, shift the rest indices
    //private static final int lhdc_quality_index_adjust_offset = 1;

    public static final int LHDC_QUALITY_DEFAULT_TAG = 0xC000;
    public static final int LHDC_QUALITY_DEFAULT_MAGIC = 0x8000;
    public static final int LHDC_QUALITY_DEFAULT_INDEX = (5 - lhdc_quality_index_adjust_offset);
    public static final int LHDC_QUALITY_DEFAULT_MAX_INDEX = (9 - lhdc_quality_index_adjust_offset); //0~9


    public static final int A2DP_LHDC_JAS_ENABLED = 0x1;
    public static final int A2DP_LHDC_AR_ENABLED = 0x2;
    public static final int A2DP_LHDC_META_ENABLED = 0x4;
    public static final int A2DP_LHDC_LLAC_ENABLED = 0x8;
    public static final int A2DP_LHDC_MBR_ENABLED = 0x10;
    public static final int A2DP_LHDC_LARC_ENABLED = 0x20;
    public static final int A2DP_LHDC_V4_ENABLED = 0x40;


    public static String getCustomCodecName(int type) {
        if (type == SOURCE_CODEC_TYPE_LHDCV2) {
            return "LHDC V2";
        } else if (type == SOURCE_CODEC_TYPE_LHDCV3) {
            return "LHDC V3";
        } else if (type == SOURCE_CODEC_TYPE_LHDCV5) {
           return "LHDC V5";
        } else if (type == SOURCE_CODEC_TYPE_LC3PLUS_HR) {
            return "LC3plus HR";
        } else if (type == SOURCE_CODEC_TYPE_FLAC) {
            return "FLAC";
        } else {
            return null;
        }
    }
    public static String getCodecName(int type) {
        return (String) HiddenApiBypass.invoke(BluetoothCodecConfig.class, null, "getCodecName", type);
    }
    public static String getCodecName(BluetoothCodecConfig config) {
        return getCodecName(config.getCodecType());
    }

    public static boolean isCustomCodec(int type) {
        return isLHDC(type) ||
               type == SOURCE_CODEC_TYPE_LC3PLUS_HR ||
               type == SOURCE_CODEC_TYPE_FLAC;

    }

    public static boolean isLHDC(int type) {
        return type == SOURCE_CODEC_TYPE_LHDCV2 ||
               type == SOURCE_CODEC_TYPE_LHDCV3 ||
               type == SOURCE_CODEC_TYPE_LHDCV5;
    }

}
