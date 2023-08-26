package ru.kirddos.exta2dp;

public class ConstUtils {

    public static final int SOURCE_QVA_CODEC_TYPE_MAX_EXTA2DP = 14;

    public static final int SOURCE_QVA_CODEC_TYPE_MAX = SourceCodecType.getQVA();

    private static final int DEFAULT_LHDCV3 = SOURCE_QVA_CODEC_TYPE_MAX+1;
    private static final int DEFAULT_LHDCV2 = SOURCE_QVA_CODEC_TYPE_MAX+2;
    private static final int DEFAULT_LHDCV5 = SOURCE_QVA_CODEC_TYPE_MAX+3;

    public static final int SOURCE_CODEC_TYPE_LHDCV2 = SourceCodecType.getIdByName("LHDCV2", DEFAULT_LHDCV2);
    public static final int SOURCE_CODEC_TYPE_LHDCV3 = SourceCodecType.getIdByName("LHDCV3", DEFAULT_LHDCV3);
    public static final int SOURCE_CODEC_TYPE_LHDCV5 = SourceCodecType.getIdByName("LHDCV5", DEFAULT_LHDCV5);

    public static final int SOURCE_CODEC_TYPE_LC3PLUS_HR = SOURCE_QVA_CODEC_TYPE_MAX + 4;
    public static final int SOURCE_CODEC_TYPE_FLAC = SOURCE_QVA_CODEC_TYPE_MAX + 5;

    public static final int SOURCE_CODEC_TYPE_OPUS = SourceCodecType.getIdByName("OPUS", -1);



    public static final int SOURCE_CODEC_TYPE_APTX_TWSP = SourceCodecType.getIdByName("APTX_TWSP");
    public static final int SOURCE_CODEC_TYPE_APTX_ADAPTIVE = SourceCodecType.getIdByName("APTX_ADAPTIVE");

    public static final int AUDIO_FORMAT_LDAC = 0x23000000;
    public static final int AUDIO_FORMAT_LHDC = 0x28000000;
    public static final int AUDIO_FORMAT_LHDC_LL = 0x29000000;

    public static final int AUDIO_FORMAT_FLAC = 0x1B000000;
    public static final int AUDIO_FORMAT_LC3 = 0x2B000000;


    public static final int FLAC_STEREO_MONO_MASK = 0xF;
    public static final int FLAC_STEREO = 0x2;
    public static final int FLAC_DEFAULT_VALUE = FLAC_STEREO;
    public static final int FLAC_MONO = 0x1;

    // In standard case, low0 is available
    public static final int lhdc_quality_index_adjust_offset = 0;
    // In case of low0 is removed, shift the rest indices
    //private static final int lhdc_quality_index_adjust_offset = 1;

    public static final int LHDC_QUALITY_DEFAULT_TAG = 0xC000;
    public static final int LHDC_QUALITY_DEFAULT_MAGIC = 0x8000;
    public static final int LHDC_QUALITY_DEFAULT_INDEX = (5 - lhdc_quality_index_adjust_offset);
    public static final int LHDC_QUALITY_DEFAULT_MAX_INDEX = (9 - lhdc_quality_index_adjust_offset); //0~9
}
