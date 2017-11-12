interface KeyboardShowEvent {
    keyboardHeight: number;
  }
  
  interface Shortcut {
    id: string;
    activityClass?: string;
    activityPackage?: string;
    data?: string;
    extraSubject?: string;
    shortLabel?: string;
    longLabel?: string;
    iconPath?: string;
    iconBitmap?: string;
  }
  
  interface Shortcuts {
    supportsDynamic(onSuccess?: (supported: boolean) => void, onError?: (error: any) => void);
    supportsPinned(onSuccess?: (supported: boolean) => void, onError?: (error: any) => void);
    setDynamic(shortcuts: Shortcut[], onSuccess?: () => void, onError?: (error: any) => void);
    removeDynamic(onSuccess?: () => void, onError?: (error: any) => void);
  }
  
  interface Plugins {
    Shortcuts: Shortcuts;
  }

  interface Window {
      plugins: Plugins
  }