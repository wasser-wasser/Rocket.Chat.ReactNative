import { NativeModules, NativeEventEmitter, Alert } from 'react-native';


const { UnifiedPush } = NativeModules;
const notifyEmitter = new NativeEventEmitter(UnifiedPush);


type UnifiedPushType = {
  getCachedNotification: () => Promise<string | null>;
  registerApp: () => Promise<string>;
  registerAppWithId: (appId: string) => Promise<string>;
  sendRegistration: (url: string, userId: string, userToken: string) => Promise<string>;
  markJSReady: () => void;
  // initialize: () => void;
};


notifyEmitter.addListener("onNotification", (event) => {
  Alert.alert('onNotification   1 '+JSON.stringify(event));
  console.log("Received native UnifiedPushType event:", event);
});

// push?.payload.ejson   --- const { rid, name, sender, type, host, messageId }: IEjson = EJSON.parse(notification.ejson);

export default UnifiedPush as UnifiedPushType;
