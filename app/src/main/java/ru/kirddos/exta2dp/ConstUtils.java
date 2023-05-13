package ru.kirddos.exta2dp;

public class ConstUtils {
    public static final int SOURCE_CODEC_TYPE_LHDCV2 = 9;//42;
    public static final int SOURCE_CODEC_TYPE_LHDCV3 = 10;//43;
    public static final int SOURCE_CODEC_TYPE_LHDCV5 = 11;//44;

    public static final int SOURCE_CODEC_TYPE_APTX_TWSP = SourceCodecType.getIdByName("APTX_TWSP");
    public static final int SOURCE_CODEC_TYPE_APTX_ADAPTIVE = SourceCodecType.getIdByName("APTX_ADAPTIVE");

    public static final int AUDIO_FORMAT_LDAC = 0x23000000;
    public static final int AUDIO_FORMAT_LHDC = 0x28000000;
    public static final int AUDIO_FORMAT_LHDC_LL = 0x29000000;


    // In standard case, low0 is available
    public static final int lhdc_quality_index_adjust_offset = 0;
    // In case of low0 is removed, shift the rest indices
    //private static final int lhdc_quality_index_adjust_offset = 1;

    public static final int LHDC_QUALITY_DEFAULT_TAG = 0xC000;
    public static final int LHDC_QUALITY_DEFAULT_MAGIC = 0x8000;
    public static final int LHDC_QUALITY_DEFAULT_INDEX = (5 - lhdc_quality_index_adjust_offset);
    public static final int LHDC_QUALITY_DEFAULT_MAX_INDEX = (9 - lhdc_quality_index_adjust_offset); //0~9
}
