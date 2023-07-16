## Extended A2DP
This is the [LSPosed](https://github.com/LSPosed/LSPosed) module to add A2DP codecs into existing Android ROMs. 
It uses [Modern Xposed API](https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API) and is being actively developed.

### Current state
Works on Red Magic 8 Pro, changing codecs in development settings is a bit problematic. It is recommended to change through SBC (or any initially supported codec) and not between added by this module directly.

Other devices might work, but no guarantees. Pull requests are welcome. 

### Supported Versions
Android 13

LSPosed v1.8.6 (6940)

### Hardware requirements
Relatively fresh Snapdragon SoC, I've only tested on 8 Gen 2.

#### TODO
- [x] UI in com.android.settings
- [x] Logic in com.android.bluetooth
- [x] Logic in native code
- [x] Fix codec quality selection
- [x] bt_adv_audio-related problems (done, but see next)
- [ ] Fix LE Audio (depends on bt_adv_audio)

### License
ExtA2DP is licensed under the **GNU General Public License v3 (GPL-3)** (http://www.gnu.org/copyleft/gpl.html).
