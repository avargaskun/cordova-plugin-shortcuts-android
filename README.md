# Android Shortcuts Plugin for Cordova 

## DESCRIPTION

Use this plug-in to manage App shortcuts on Android from a Cordova application. Supports both pinned and dynamic shortcuts. For more information about App Shortcuts see: https://developer.android.com/guide/topics/ui/shortcuts.html

The work that went into creating this plug-in was inspired by the existing [cordova-plugin-shortcut](https://github.com/jorgecis/ShortcutPlugin) which unfortunately no longer works with Android Nougat and later.

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
	data: 'myapp://path/to/launch?param=value',
	extraSubject: 'Additional text to be included with the intent',
	shortLabel: 'Short description',
	longLabel: 'Longer string describing the shortcut',
	iconBitmap: '<Bitmap for the shortcut icon, base64 encoded>'
}
window.plugins.Shortcuts.setDynamic([shortcut], function() {
	window.alert('Shortcuts were applied successfully');
}, function(error) {
	window.alert('Error: ' + error);
})
```