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
  const isMounted = useRef(true); 

  UnifiedPush.markJSReady();

  useEffect(() => {
    isMounted.current = true;
    // Benachrichtigungen von der nativen Seite empfangen
    const notificationListener = notifyEmitter.addListener('onNotification', (event) => {
    try {
      // console.log('Verarbeitete Push-Nachricht:', event);
      const parsed = JSON.parse(event.message);
      if (!parsed) return;
      // Prüfen, ob die Nachricht den Auslöser enthält und eine URL vorhanden ist
      if (parsed?.UP_REGISTER === TARGET_SUBSTRING && parsed.ntfy_url) {
        setWebViewUrl(parsed.ntfy_url);
        if (AppState.currentState === 'active') {
          // Sofort anzeigen, wenn im Vordergrund
          setShowWebView(true);
        } else {
          // Zurückstellen bis die App aktiv ist
          hasTrigger.current = true;
        }
      }else{
        console.log('event1234 -- FAILED');
      }
    } catch (err) {
      console.log('Fehler beim Parsen der Push-Nachricht: ERROR');
    }
  });

     // Listener für App-Zustandsänderung (z. B. Rückkehr in den Vordergrund)
    const appStateListener = AppState.addEventListener('change', (nextAppState) => {
      if (appState.current.match(/inactive|background/) && nextAppState === 'active') {
        if (hasTrigger.current && isMounted.current) {
          setShowWebView(true);
          hasTrigger.current = false;
        }
      }
      appState.current = nextAppState;
    });

    return () => {
      // Aufräumen der Listener beim Verlassen des Components
      isMounted.current = false; 
      notificationListener.remove();
      appStateListener.remove();
    };
  }, []);

  return (
    <Modal visible={showWebView} animationType="slide">
      <View style={{ flex: 1 }}>
        {webViewUrl ? (
          <WebView key={webViewUrl} source={{ uri: webViewUrl }} />
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
