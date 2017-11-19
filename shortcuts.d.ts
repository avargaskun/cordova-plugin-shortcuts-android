interface Shortcut {
  id: string;
  shortLabel?: string;
  longLabel?: string;
  iconPath?: string;
  iconBitmap?: string;
  intent?: Intent;
}

interface Intent {
  activityClass?: string; // Defaults to currently running activity
  activityPackage?: string; // Defaults to currently running package
  action?: string; // Defaults to ACTION_VIEW
  flags?: number; // Defaults to FLAG_ACTIVITY_NEW_TASK + FLAG_ACTIVITY_CLEAR_TOP
  categories?: string[];
  data?: string;
  extras?: { [key: string]: any };
}

interface Shortcuts {
  supportsDynamic(onSuccess?: (supported: boolean) => void, onError?: (error: any) => void);
  supportsPinned(onSuccess?: (supported: boolean) => void, onError?: (error: any) => void);
  setDynamic(shortcuts: Shortcut[], onSuccess?: () => void, onError?: (error: any) => void);
  addPinned(shortcut: Shortcut, onSuccess?: () => void, onError?: (error: any) => void);
  getIntent(onSuccess?: (intent: Intent) => void, onError?: (error: any) => void);
  onNewIntent(callback?: (intent: Intent) => void, onError?: (error: any) => void);
}

interface Plugins {
  Shortcuts: Shortcuts;
}

interface Window {
  plugins: Plugins;
}