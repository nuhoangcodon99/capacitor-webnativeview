import { registerPlugin } from '@capacitor/core';

import type { WebNativeViewPlugin } from './definitions';

const WebNativeView = registerPlugin<WebNativeViewPlugin>('WebNativeView', {
});

export * from './definitions';
export { WebNativeView };
