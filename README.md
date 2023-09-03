## Extended A2DP
This is the [LSPosed](https://github.com/LSPosed/LSPosed) module to add A2DP codecs into Android ROMs.
It uses [Modern Xposed API](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API) and is actively being developed.

### Current State
Works on Red Magic 8 Pro and on few other devices.

### Supported Versions
Android 13 (maybe 14?)

LSPosed v1.9.1 (6991)

### Hardware requirements
Relatively fresh Snapdragon SoC and CLO ROM.

As a rule of thumb: if aptX Adaptive if present, it's most likely work (though for the best experience 6/7/8 Gen 1 or newer are recommended)
If you're using older device, you'll most likely need a custom Magisk companion module. See examples [here](https://github.com/anonymix007/ExtA2DP/issues/4#issuecomment-1694643330) and [here](https://github.com/anonymix007/ExtA2DP/issues/2#issuecomment-1646835100) in addition to one provided on the release page.

#### TODO
- [x] UI in com.android.settings
- [x] Logic in com.android.bluetooth
- [x] Logic in native code
- [x] Fix codec quality selection
- [x] bt_adv_audio-related problems (done, but see next)
- [ ] Fix LE Audio (depends on bt_adv_audio, if someone knows where to get it, please contact me)
- [ ] More codecs? Opus, LC3plus HR, ...

### License
ExtA2DP is licensed under the terms of **GNU General Public License v3 (GPL-3)** (http://www.gnu.org/copyleft/gpl.html).
