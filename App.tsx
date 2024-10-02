/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React from 'react';
import {
  SafeAreaView,
  ScrollView,
  Text,
  useColorScheme,
  View,
  Button,
  NativeModules,
  Alert,
  Platform,
  PermissionsAndroid
} from 'react-native';

import { Colors } from 'react-native/Libraries/NewAppScreen';

function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };
  const openQRScanner = async () => {
    try {
      const qrCode = await NativeModules.QRModule.openQRScanner();
      Alert.alert('QR Code', qrCode);
    } catch (error) {
      if (error instanceof Error) {
        Alert.alert('Error', error.message);
      } else {
        Alert.alert('Error', 'An unknown error occurred');
      }
    }
  };

  const checkPhoneStatePermission = async () => {
    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
        {
          title: 'Phone State Permission',
          message: 'This app needs access to your phone state to manage SIM cards.',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        }
      );
      return (granted === PermissionsAndroid.RESULTS.GRANTED);
    } catch (err) {
      console.warn('Phone state permission error:', err);
      return false;
    }
  };

  const checkPhoneNumberPermission = async () => {
    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.READ_PHONE_NUMBERS,
        {
          title: 'Phone Number Permission',
          message: 'This app needs access to your phone numbers to manage SIM cards.',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        }
      );
      return (granted === PermissionsAndroid.RESULTS.GRANTED);
    } catch (err) {
      console.warn('Phone number permission error:', err);
      return false;
    }
  };

  const manageEmbeddedSubcriptions = async () => {
    console.log("SSSSưSS", NativeModules);

    const { SimManageModule } = NativeModules
    console.log("SSSSSS", SimManageModule);

    if (Platform.OS === 'android') {
      console.log('Andoird');
      
      const hasPhoneStatePermission = await checkPhoneStatePermission();
      if (!hasPhoneStatePermission) return;

      console.log(hasPhoneStatePermission);


      const hasPhoneNumberPermission = await checkPhoneNumberPermission();
      if (!hasPhoneNumberPermission) return;

      try {
        console.log("into manageEmbeddedSubcriptions");

        await SimManageModule.manageEmbeddedSubcriptions();
        // console.log("Result: ", result);

      } catch (error) {
        if (error instanceof Error) {
          Alert.alert('Error', error.message);
        } else {
          Alert.alert('Error', 'An unknown error occurred');
        }
      }
    }

  }
  return (
    <SafeAreaView style={backgroundStyle}>
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        <View
          style={{
            backgroundColor: isDarkMode ? Colors.black : Colors.white,
          }}>
          <Text>Alos</Text>
          <Button title="Open QR Scanner" onPress={openQRScanner} />
          <Text style={{ height: 10 }}></Text>
          <Button title="Action" onPress={manageEmbeddedSubcriptions} />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

export default App;
