package ru.kirddos.exta2dp.modules;

import io.github.libxposed.api.XposedInterface.*;

interface BeforeCallback {
    Hooker before(BeforeHookCallback callback);
}

interface AfterCallback {
    void after(AfterHookCallback callback, Hooker state);
}
