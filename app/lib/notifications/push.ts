import { Platform, PermissionsAndroid } from 'react-native';
// import { initUnifiedPush } from 'react-native-unifiedpush-connector';
import { store as reduxStore } from '../store/auxStore';
import { INotification } from '../../definitions';
import React from 'react';
import { DeviceEventEmitter } from 'react-native';
export let deviceToken = '';

export const removeAllNotifications = (): void => {
	// Notifications.removeAllDeliveredNotifications();
  console.log(`app/lib/notifications/push.ts  removeAllNotifications() fired`)
};

export const pushNotificationConfigure = async (
  onNotification: (notification: INotification) => void
): Promise<void> => {
  if (Platform.OS === 'android') {
    const permission = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
    );

    if (permission !== PermissionsAndroid.RESULTS.GRANTED) {
      console.warn('Notification permission not granted');
      return;
    }
  }

  try {
    // const headers = {
    //       'Content-Type': 'application/json',
    //       'X-Auth-Token': token,
    //       'X-User-Id': id
    //     };
    // const register_resp = await fetch(`${server}/register`, {
		// 	method: 'POST',
		// 	headers: headers,
		// 	body: JSON.stringify({})
		// });
    // console.log(register_resp);

    // await initUnifiedPush({
    //   onMessage: (message: string) => {
    //     const notification: INotification = JSON.parse(message);
    //     onNotification(notification);
    //   },
    //   onRegistration: (endpoint: string) => {
    //     console.log('UnifiedPush registered with endpoint:', endpoint);
    //     // You can send this endpoint to your server if needed
    //   },
    //   onUnregistered: () => {
    //     console.log('UnifiedPush unregistered');
    //   },
    //   onRegistrationFailed: (error: string) => {
    //     console.error('UnifiedPush registration failed:', error);
    //   },
    // });
  } catch (error) {
    console.error('Error initializing UnifiedPush:', error);
  }
};
