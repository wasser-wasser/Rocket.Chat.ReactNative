import { NativeModules, NativeEventEmitter, Alert } from 'react-native';


const { UnifiedPush } = NativeModules;
const notifyEmitter = new NativeEventEmitter(UnifiedPush);


interface PushNotificationEvent {
  message: string;
}

type UnifiedPushType = {
  getCachedNotification: () => Promise<string | null>;
  registerApp: () => Promise<string>;
  registerAppWithId: (appId: string) => Promise<string>;
  sendRegistration: (url: string, userId: string, userToken: string) => Promise<string>;
  removeRegistration: (url: string, userId: string, userToken: string) => Promise<string>;
  markJSReady: () => void;
  sendNotification: (appId: string) => void;
  // initialize: () => void;
};


// notifyEmitter.addListener("onNotification", (event: PushNotificationEvent) => {
//  //
// });

// push?.payload.ejson   --- const { rid, name, sender, type, host, messageId }: IEjson = EJSON.parse(notification.ejson);

export default UnifiedPush as UnifiedPushType;
