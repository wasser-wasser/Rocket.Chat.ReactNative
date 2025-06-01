import { deepLinkingClickCallPush } from '../../../actions/deepLinking';
import { isAndroid } from '../../methods/helpers';
import { store } from '../../store/auxStore';

// import ExpoUnifiedPush, {
//   checkPermissions,
//   Distributor,
//   requestPermissions,
//   showLocalNotification,
// } from "expo-unified-push";
// import { subscribeDistributorMessages } from "expo-unified-push/ExpoUnifiedPushModule";


export const getInitialNotification = async (): Promise<void> => {
	if (isAndroid) {
		console.log(`ADD INITIAL NOTIFCATION STUFF HERE`)
		// const notifee = require('@notifee/react-native').default;
		// const initialNotification = await notifee.getInitialNotification();
		// if (initialNotification?.notification?.data?.notificationType === 'videoconf') {
		// 	store.dispatch(
		// 		deepLinkingClickCallPush({ ...initialNotification?.notification?.data, event: initialNotification?.pressAction?.id })
		// 	);
		// }
	}
};
