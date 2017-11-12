var Shortcuts = function () {
};

Shortcuts.prototype.supportsDynamic = function (successCallback, errorCallback) {
  cordova.exec(
    successCallback,
    errorCallback,
    'ShortcutsPlugin',
    'supportsDynamic',
    []
  );
};

Shortcuts.prototype.supportsPinned = function (successCallback, errorCallback) {
  cordova.exec(
    successCallback,
    errorCallback,
    'ShortcutsPlugin',
    'supportsPinned',
    []
  );
};

Shortcuts.prototype.setDynamic = function (shortcuts, successCallback, errorCallback) {
  cordova.exec(
    successCallback,
    errorCallback,
    'ShortcutsPlugin',
    'setDynamic',
    shortcuts
  );
};

if (!window.plugins) {
  window.plugins = {};
}

if (!window.plugins.Shortcuts) {
  window.plugins.Shortcuts = new Shortcuts();
}
