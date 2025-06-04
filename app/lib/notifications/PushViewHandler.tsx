// PushViewHandler.tsx
import React, { useEffect, useRef, useState } from 'react';
import { AppState, AppStateStatus, Modal, View, Button, Alert} from 'react-native';
import { WebView } from 'react-native-webview';
import UnifiedPush from './UnifiedPush'; // Make sure this matches your actual path
import { NativeEventEmitter, NativeModules } from 'react-native';

const notifyEmitter = new NativeEventEmitter(NativeModules.UnifiedPush);

const TARGET_SUBSTRING = 'UP_REGISTER'; // <-- adjust this to your trigger keyword

const PushViewHandler = () => {
  const [showWebView, setShowWebView] = useState(false);
  const [webViewUrl, setWebViewUrl] = useState<string | null>(null);
  const appState = useRef<AppStateStatus>(AppState.currentState);
  const hasTrigger = useRef(false);

  UnifiedPush.markJSReady();

  useEffect(() => {

    // Benachrichtigungen von der nativen Seite empfangen
    const notificationListener = notifyEmitter.addListener('onNotification', (event) => {
      try {
        const parsed = JSON.parse(event.message);
        console.log('Verarbeitete Push-Nachricht:', parsed);

        if (!parsed) return;
        // Prüfen, ob die Nachricht den Auslöser enthält und eine URL vorhanden ist
        if (parsed && parsed.TARGET_SUBSTRING?.equals(TARGET_SUBSTRING)) {
          setWebViewUrl(parsed.ntfy_url);/// authenticate
          hasTrigger.current = true;
        }
      } catch (err) {
        console.error('Fehler beim Parsen der Push-Nachricht:', err);
      }
    });

     // Listener für App-Zustandsänderung (z. B. Rückkehr in den Vordergrund)
    const appStateListener = AppState.addEventListener('change', (nextAppState) => {
      if (appState.current.match(/inactive|background/) && nextAppState === 'active') {
        if (hasTrigger.current) {
          setShowWebView(true);
          hasTrigger.current = false;
        }
      }
      appState.current = nextAppState;
    });

    return () => {
      // Aufräumen der Listener beim Verlassen des Components
      notificationListener.remove();
      appStateListener.remove();
    };
  }, []);

  return (
    <Modal visible={showWebView} animationType="slide">
      <View style={{ flex: 1 }}>
        {webViewUrl ? (
          <WebView source={{ uri: webViewUrl }} />
        ) : (
          <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
            <Alert title="Fehler" message="Keine URL für WebView angegeben." />
          </View>
        )}
        <Button title="WebView schließen" onPress={() => setShowWebView(false)} />
      </View>
    </Modal>
  );
};


export default PushViewHandler;
