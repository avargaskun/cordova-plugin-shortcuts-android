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

Shortcuts.prototype.addPinned = function (shortcut, successCallback, errorCallback) {
  cordova.exec(
    successCallback,
    errorCallback,
    'ShortcutsPlugin',
    'addPinned',
    [shortcut]
  );
};

Shortcuts.prototype.getIntent = function (successCallback, errorCallback) {
  cordova.exec(
    successCallback,
    errorCallback,
    'ShortcutsPlugin',
    'getIntent',
    []
  )
}

Shortcuts.prototype.onNewIntent = function (callback, errorCallback) {
  cordova.exec(
    callback,
    errorCallback,
    'ShortcutsPlugin',
    'onNewIntent',
    [typeof (callback) !== 'function']
  )
}

if (!window.plugins) {
  window.plugins = {};
}

if (!window.plugins.Shortcuts) {
  window.plugins.Shortcuts = new Shortcuts();
}