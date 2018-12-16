# Android Shortcuts Plugin for Cordova 

## DESCRIPTION

Use this plugin to create shortcuts in Android. Use this plugin to handle Intents on your application.

For more information on Android App Shortcuts: https://developer.android.com/guide/topics/ui/shortcuts.html

For more information on Android Intents: https://developer.android.com/guide/components/intents-filters.html

The work that went into creating this plug-in was inspired by the existing plugins: [cordova-plugin-shortcut](https://github.com/jorgecis/ShortcutPlugin) and [cordova-plugin-webintent2](https://github.com/okwei2000/webintent).

## LICENSE

	The MIT License

	Copyright (c) 2013 Adobe Systems, inc.
	portions Copyright (c) 2013 Jorge Cisneros jorgecis@gmail.com

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.


## How to install it

  cordova plugins add cordova-plugin-shortcuts-android

## How to use it

### Checking if Dynamic Shortcuts are supported

Dynamic shortcuts require SDK 25 or later. Use `supportsDynamic` to check whether the current device meets those requirements.

```javascript
window.plugins.Shortcuts.supportsDynamic(function(supported) { 
	if (supported)
		window.alert('Dynamic shortcuts are supported');
	else
		window.alert('Dynamic shortcuts are NOT supported');
}, function(error) {
	window.alert('Error: ' + error);
})
```

### Checking if Pinned Shortcuts are supported

Pinned shortcuts require SDK 26 or later. Use `supportsPinned` to check whether the current device meets those requirements.

```javascript
window.plugins.Shortcuts.supportsPinned(function(supported) { 
	if (supported)
		window.alert('Pinned shortcuts are supported');
	else
		window.alert('Pinned shortcuts are NOT supported');
}, function(error) {
	window.alert('Error: ' + error);
})
```

### Setting the application Dynamic Shortcuts

Use `setDynamic` to set the Dynamic Shortcuts for the application, all at once. The shortcuts provided as a parameter will override any existing shortcut. Use an empty array to clear out existing shortcuts.

```javascript
var shortcut = {
	id: 'my_shortcut_1',
	shortLabel: 'Short description',
	longLabel: 'Longer string describing the shortcut',
	iconBitmap: '<Bitmap for the shortcut icon, base64 encoded>',
	iconFromResource: "ic_playlist_play_red", //filename w/o extension of an icon that resides on res/drawable-* (hdpi,mdpi..)
	intent: {
		action: 'android.intent.action.RUN',
		categories: [
			'android.intent.category.TEST', // Built-in Android category
			'MY_CATEGORY' // Custom categories are also supported
		],
		flags: 67108864, // FLAG_ACTIVITY_CLEAR_TOP
		data: 'myapp://path/to/launch?param=value', // Must be a well-formed URI
		extras: {
			'android.intent.extra.SUBJECT': 'Hello world!', // Built-in Android extra (string)
			'MY_BOOLEAN': true, // Custom extras are also supported (boolean, number and string only)
		}
	}
}
window.plugins.Shortcuts.setDynamic([shortcut], function() {
	window.alert('Shortcuts were applied successfully');
}, function(error) {
	window.alert('Error: ' + error);
})
```

### Adding a Pinned Shortcut to the launcher

Use `addPinned` to add a new Pinned Shortcut to the launcher.

```javascript
var shortcut = {
	id: 'my_shortcut_1',
	shortLabel: 'Short description',
	longLabel: 'Longer string describing the shortcut',
	iconBitmap: '<Bitmap for the shortcut icon, base64 encoded>', // Defaults to the main application icon
	iconFromResource: "ic_playlist_play_red", //filename w/o extension of an icon that resides on res/drawable-* (hdpi,mdpi..)
	intent: {
		action: 'android.intent.action.RUN',
		categories: [
			'android.intent.category.TEST', // Built-in Android category
			'MY_CATEGORY' // Custom categories are also supported
		],
		flags: 67108864, // FLAG_ACTIVITY_CLEAR_TOP
		data: 'myapp://path/to/launch?param=value', // Must be a well-formed URI
		extras: {
			'android.intent.extra.SUBJECT': 'Hello world!', // Built-in Android extra (string)
			'MY_BOOLEAN': true, // Custom extras are also supported (boolean, number and string only)
		}
	}
}
window.plugins.Shortcuts.addPinned(shortcut, function() {
	window.alert('Shortcut pinned successfully');
}, function(error) {
	window.alert('Error: ' + error);
})
```

### Querying current Intent

Use `getIntent` to get the Intent that was used to launch the current instance of the Cordova activity.

```javascript
window.plugins.Shortcuts.getIntent(function(intent) {
	window.alert(JSON.stringify(intent));
})
```

### Subscribe to new Intents

Use `onNewIntent` to register a callback to be executed every time a new Intent is sent to your Cordova activity. Note that in some conditions this callback may not be executed. 

For more information see the documentation for [Activity.onNewIntent(Intent)](https://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent))

```javascript
window.plugins.Shortcuts.onNewIntent(function(intent) {
	window.alert(JSON.stringify(intent));
})
```

Call with an empty callback to de-register the existing callback.

```javascript
window.plugins.Shortcuts.onNewIntent(); // De-register existing callback
```
