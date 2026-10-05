import { Ionicons } from "@expo/vector-icons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { useLocalSearchParams } from "expo-router";
import React, { useEffect, useRef, useState } from "react";
import {
    ActivityIndicator,
    Alert,
    BackHandler,
    Button,
    Modal,
    NativeModules,
    SafeAreaView,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from "react-native";
import ThermalPrinter from "react-native-thermal-printer";
import { WebView } from "react-native-webview";

// build: 
// $ cd android
// $ ./gradlew assembleRelease
//npx expo run:android
const { PrinterModule } = NativeModules;

export default function WebScreen() {
    const { url } = useLocalSearchParams<{ url: string }>();
    const webviewRef = useRef<WebView>(null);

    const [currentUrl, setCurrentUrl] = useState(
        url || "https://devkct.facenet.vn/aps-v2/mobile"
        // url || "https://anlap.facenet.vn/aps"
        // url || "https://solarnest.facenet.vn/mobile"

    );
    // const [inputUrl, setInputUrl] = useState(currentUrl);
    const [canGoBack, setCanGoBack] = useState(false);

    // Loading state
    const [loading, setLoading] = useState(false);

    // Đồng bộ khi param url đổi
    useEffect(() => {
        if (url && url !== currentUrl) {
            setCurrentUrl(url);
            // setInputUrl(url);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [url]);

    // Back cứng Android
    useEffect(() => {
        const onBackPress = () => {
            if (canGoBack) {
                webviewRef.current?.goBack();
                return true;
            }
            return false;
        };
        const subscription = BackHandler.addEventListener(
            "hardwareBackPress",
            onBackPress
        );
        return () => subscription.remove();
    }, [canGoBack]);

    // State Bluetooth
    const [devices, setDevices] = useState<any[]>([]);
    const [selectedMac, setSelectedMac] = useState<string>("");
    const [scanResult, setScanResult] = useState<string>("");

    // State WiFi
    const [ip, setIp] = useState("");
    const [port, setPort] = useState("9100");
    const [wifiResult, setWifiResult] = useState("");
    const [bluetoothResult, setBlueToothResult] = useState("");
    const [inputUrl, setInputUrl] = useState(currentUrl);

    // Modal scan
    const [modalVisible, setModalVisible] = useState(false);

    const [isBluetoothConnected, setIsBluetoothConnected] = useState(false);
    const [isWifiConnected, setIsWifiConnected] = useState(false);

    // Tab state
    const [tab, setTab] = useState<"wifi" | "bluetooth">("wifi");

    // Radio print mode
    const [printMode, setPrintMode] = useState<"ZPL" | "ESC">("ESC");

    useEffect(() => {
        loadPrinterConfig();
        loadSelectedTab();
    }, []);

    // ASYNC STORAGE
    // Lưu ip và port
    const savePrinterConfig = async (ip: string, port: string) => {
        try {
            await AsyncStorage.setItem("printerConfig", JSON.stringify({ ip, port }));
            console.log("Saved printer config:", { ip, port });
        } catch (err) {
            console.error("Error saving printer config:", err);
        }
    };
    // Load ip và port khi mở app
    const loadPrinterConfig = async () => {
        try {
            const data = await AsyncStorage.getItem("printerConfig");
            if (data) {
                const { ip, port } = JSON.parse(data);
                setIp(ip);
                setPort(port);
                console.log("Loaded printer config:", { ip, port });
            }
        } catch (err) {
            console.error("Error loading printer config:", err);
        }
    };


    // MODAL TAB
    // Hàm lưu tab hiện tại
    const saveSelectedTab = async (tabName: string) => {
        await AsyncStorage.setItem("lastSelectedTab", tabName);
    };
    // Hàm load tab đã chọn lần trước
    const loadSelectedTab = async () => {
        const tab = await AsyncStorage.getItem("lastSelectedTab");
        if (tab === "ZPL" || tab === "ESC") {
            setPrintMode(tab);
        }
    };


    // BLUETOOTH: 
    const scanDevices = async () => {
        try {
            const list = await ThermalPrinter.getBluetoothDeviceList();
            setDevices(Array.isArray(list) ? list : []);
            setScanResult("Found " + (list?.length || 0) + " devices");
        } catch (e) {
            setScanResult("Scan failed: " + JSON.stringify(e));
        }
    };
    const checkConnectBlueTooth = async () => {
        await PrinterModule.checkConnected()
            .then((res: boolean) => {
                if (res === true) {
                    setIsBluetoothConnected(true)
                }
                else {
                    setIsBluetoothConnected(false)
                }
            })
            .catch((err: any) => console.error("Close Error:", err));
    }
    const testPrintBluetooth = async (mac: string, base64Code: string) => {
        // const getImageBase64 = await imageUrlToBase64("https://scontent.fhan17-1.fna.fbcdn.net/v/t39.30808-6/547510501_1470022137568143_3622071673894331338_n.jpg?_nc_cat=105&ccb=1-7&_nc_sid=127cfc&_nc_eui2=AeHidI6OO_lCM22ZTu-RdvSt96XEQ_EKg5L3pcRD8QqDkkqntLJ-jS2jfw2_OunGjk-ZEtUhAK3Sh3gzyRpP-RuN&_nc_ohc=x9ZkHqAueCIQ7kNvwHTkUfp&_nc_oc=Adl66uFDLawvaiqi6xprLJdyQSUOzggTGIifx4Cv8IzzKxMXv70ADWHLKgKZom4iuyw&_nc_zt=23&_nc_ht=scontent.fhan17-1.fna&_nc_gid=GcjUoewKPUBLUERN1jehPQ&oh=00_AfZ3S46Nt5LSTPfT58VLz87ep0ozeztn8PKMt9VTE1X2Gw&oe=68C9C26C");
        setLoading(true);
        try {
            // const responseConnect = await PrinterModule.portOpen(mac);
            await PrinterModule.printBase64Bitmap(base64Code, 0, 0, 370, 510, 1, 0);
        } catch (e) {
            console.error("❌ Print failed:", e);
        } finally {
            setLoading(false);
        }
    };
    const handleBluetoothToggle = async () => {
        if (!selectedMac || printMode !== "ESC") {
            Alert.alert("Please select a Bluetooth device first and change Print Mode");
            return;
        }

        try {
            if (!isBluetoothConnected) {
                await PrinterModule.portOpen(selectedMac)
                    .then((res: boolean) => {
                        console.log("Success:", res)
                        if (res === true) {
                            setBlueToothResult("✅ Connected to Bluetooth printer: " + selectedMac);
                            setIsBluetoothConnected(true);
                        }
                        else {
                            setBlueToothResult("🔴 Failed to Bluetooth printer: " + selectedMac);
                            setIsBluetoothConnected(false);
                        }
                    })
                    .catch((err: any) => {
                        console.log("Error:", err)
                        setBlueToothResult("🔴 Failed to Bluetooth printer");
                        setIsBluetoothConnected(false);
                    });
                await PrinterModule.setStartPosition(60, 10)
            } else {
                await PrinterModule.portClose();
                setBlueToothResult("🔴 Disconnected Bluetooth printer");
                setIsBluetoothConnected(false);
            }
        } catch (err) {
            setBlueToothResult("❌ Bluetooth connection failed: " + JSON.stringify(err));
        }
    };

    // WIFI: 
    const testPrintWifi_Pdf = async (testBase64: string) => {
        // const testBase64 = await pdfUrlToBase64("https://drive.google.com/uc?export=download&id=1Ntx4BTnhm9D22J5egIvZNgo1Q9CHwiXW")

        // console.log("testBase64: ", testBase64);
        setLoading(true);

        try {
            if (ip && port) {
                const responseConnect = await PrinterModule.portOpenZTL(ip, port)
                const responsePrint = await PrinterModule.printPdfBase64ZTL(testBase64, "40", "10")
                // await PrinterModule.portClose()
                //     .then((res: string) => console.log("Close Success:", res))
                //     .catch((err: any) => console.error("Close Error:", err));
                if (responseConnect as any === -1) Alert.alert("Connection Failed")
                else if (responsePrint as any === -1) Alert.alert("Printed Failed")
                else { }
                setWifiResult(`✅ Connected to ${ip}:${port}`);
            } else {
                Alert.alert("WiFi Print failed");
            }
        } catch (e) {
            setWifiResult("WiFi Print failed: " + JSON.stringify(e));
        } finally {
            setLoading(false);
        }
    };
    const testPrintWifi_Img = async (base64Code: string) => {
        // const getImageBase64 = await imageUrlToBase64("https://scontent.fhan17-1.fna.fbcdn.net/v/t39.30808-6/547510501_1470022137568143_3622071673894331338_n.jpg?_nc_cat=105&ccb=1-7&_nc_sid=127cfc&_nc_eui2=AeHidI6OO_lCM22ZTu-RdvSt96XEQ_EKg5L3pcRD8QqDkkqntLJ-jS2jfw2_OunGjk-ZEtUhAK3Sh3gzyRpP-RuN&_nc_ohc=x9ZkHqAueCIQ7kNvwHTkUfp&_nc_oc=Adl66uFDLawvaiqi6xprLJdyQSUOzggTGIifx4Cv8IzzKxMXv70ADWHLKgKZom4iuyw&_nc_zt=23&_nc_ht=scontent.fhan17-1.fna&_nc_gid=GcjUoewKPUBLUERN1jehPQ&oh=00_AfZ3S46Nt5LSTPfT58VLz87ep0ozeztn8PKMt9VTE1X2Gw&oe=68C9C26C");

        try {
            if (ip && port) {
                PrinterModule.portOpenZTL(ip, port)
                    .then((res: string) => console.log("✅ Success:", res))
                    .catch((err: any) => console.error("❌ Error:", err));

                PrinterModule.printBase64BitmapZTL(base64Code, "0", "0")
                    .then((res: string) => console.log("✅ Success:", res))
                    .catch((err: any) => console.error("❌ Error:", err));

                setWifiResult(`✅ Connected to ${ip}:${port}`);
            } else {
                Alert.alert("WiFi Print failed");
            }
        } catch (e) {
            setWifiResult("❌ WiFi Print failed: " + JSON.stringify(e));
        }
    };
    // --- HANDLE WIFI CONNECT/DISCONNECT ---
    const handleWifiToggle = async () => {
        if (!ip || !port || printMode !== "ZPL") {
            Alert.alert("Please enter IP and Port and change Print Mode");
            return;
        }

        try {
            if (!isWifiConnected) {
                await PrinterModule.portOpenZTL(ip, port);
                setWifiResult(`✅ Connected to ${ip}:${port}`);
                setIsWifiConnected(true);
            } else {
                await PrinterModule.portCloseZTL();
                setWifiResult(`🔴 Disconnected WiFi printer`);
                setIsWifiConnected(false);
            }
        } catch (err) {
            setWifiResult("❌ WiFi connection failed: " + JSON.stringify(err));
        }
    };



    //HELPER:

    // async function pdfUrlToBase64(url: string) {
    //     try {
    //         const response = await fetch(url);
    //         const arrayBuffer = await response.arrayBuffer();

    //         let binary = '';
    //         const bytes = new Uint8Array(arrayBuffer);
    //         const chunkSize = 0x8000;

    //         for (let i = 0; i < bytes.length; i += chunkSize) {
    //             let chunk = bytes.subarray(i, i + chunkSize);
    //             binary += String.fromCharCode.apply(null, chunk);
    //         }

    //         // chỉ trả về base64 thuần, không có prefix
    //         return btoa(binary);
    //     } catch (error) {
    //         console.error("Error fetching or converting PDF:", error);
    //         throw error;
    //     }
    // }
    async function imageUrlToBase64(url: string | Request) {
        try {
            const response = await fetch(url);
            const blob = await response.blob();

            return new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onloadend = () => {
                    resolve(reader.result.split(",")[1]); // bỏ prefix data:image/...
                };
                reader.onerror = reject;
                reader.readAsDataURL(blob);
            });
        } catch (error) {
            console.error("Error converting image:", error);
            throw error;
        }
    }


    // HANDLE WEB VIEW
    const handleMessage = (event: any) => {
        try {
            const data = JSON.parse(event.nativeEvent.data);
            console.log("datda", data);

            // for kct
            // if ((data && data.base64) && selectedMac.length > 0) {
            //     const pureBase64 = data.base64.replace(
            //         /^data:image\/\w+;base64,/,
            //         ""
            //     );
            //     if (printMode === "ESC" && tab === "bluetooth" && selectedMac.length > 0) testPrintBluetooth(selectedMac, pureBase64);
            //     else if (printMode === "ZPL" && tab === "wifi" && ip.length > 0 && port.length > 0) testPrintWifi_Img(pureBase64);
            //     else Alert.alert("Check the connections in the popup "); //All the information is complete: (ESC + Bluetooth + Mac or ZPL + Wifi + Ip + Port)"
            // }


            // // for anlap 
            // if (data && data.base64) {
            //     // const pureBase64 = data.base64.replace(
            //     //     /^data:image\/\w+;base64,/,
            //     //     ""
            //     // );
            //     if (printMode === "ESC" && tab === "bluetooth" && selectedMac) testPrintBluetooth(selectedMac, data.base64);
            //     else if (printMode === "ZPL" && tab === "wifi" && ip && port) testPrintWifi_Pdf(data.base64);
            //     else Alert.alert(
            //         "Check the connection settings in the popup.\nMake sure all information is filled in correctly:\n- ESC + Bluetooth + MAC\n- or ZPL + Wi-Fi + IP + Port"
            //     );
            // }
            // else {
            //     Alert.alert("Error retrieving print data or the print data format is invalid");
            // }



            if (data && data.base64) {
                if (data.type && data.type === "pdf") {
                    if (printMode === "ESC" && tab === "bluetooth" && selectedMac) testPrintBluetooth(selectedMac, data.base64);
                    else
                        if (printMode === "ZPL" && tab === "wifi" && ip.length > 0 && port.length > 0) testPrintWifi_Pdf(data.base64);
                        else Alert.alert(
                            "Check the connection settings in the popup.\nMake sure all information is filled in correctly:\n- ZPL + Wi-Fi + IP + Port"
                        );
                }
                else if (data.type && data.type === "image") {
                    const pureBase64 = data.base64.replace(
                        /^data:image\/\w+;base64,/,
                        ""
                    );
                    if (printMode === "ESC" && tab === "bluetooth" && selectedMac) testPrintBluetooth(selectedMac, pureBase64);
                    else if (printMode === "ZPL" && tab === "wifi" && ip.length > 0 && port.length > 0) testPrintWifi_Img(pureBase64)
                    else Alert.alert(
                        "Check the connection settings in the popup.\nMake sure all information is filled in correctly:\n- ESC + Bluetooth + MAC\n- or ZPL + Wi-Fi + IP + Port"
                    );
                }
                else Alert.alert(
                    "Printed Failed"
                );
            }
            else {
                Alert.alert("Error retrieving print data or the print data format is invalid");
            }
        } catch (e) {
            console.log("Raw message:", event.nativeEvent.data);
            console.log(e);
        }
    };
    const handleGo = () => {
        let formattedUrl = inputUrl.trim();
        if (!formattedUrl.startsWith("http")) {
            formattedUrl = "https://" + formattedUrl;
        }
        setCurrentUrl(formattedUrl);
    };






    return (
        <SafeAreaView style={styles.container}>
            {/* Thanh nhập URL */}
            <View style={styles.header}>
                <TextInput
                    style={styles.inputHeader}
                    value={inputUrl}
                    onChangeText={setInputUrl}
                    placeholder="Nhập URL..."
                    autoCapitalize="none"
                    autoCorrect={false}
                />
                <TouchableOpacity style={styles.goButton} onPress={handleGo}>
                    <Text style={{ color: "#fff", fontWeight: "bold" }}>Go</Text>
                </TouchableOpacity>

                {/* Nút Reload */}
                <TouchableOpacity
                    style={[styles.goButton, { marginLeft: 6, backgroundColor: "#28a745" }]}
                    onPress={() => webviewRef.current?.reload()}
                >
                    <Ionicons name="refresh" size={20} color="#fff" />
                </TouchableOpacity>
            </View>

            {/* WebView */}
            <View style={{ flex: 1 }}>
                <WebView
                    ref={webviewRef}
                    source={{ uri: currentUrl }}
                    style={{ flex: 1 }}
                    onMessage={handleMessage}

                    onNavigationStateChange={(navState) => {
                        setCanGoBack(navState.canGoBack);
                        if (navState.url !== currentUrl) {
                            setCurrentUrl(navState.url);
                            setInputUrl(navState.url);
                        }
                    }}
                />
            </View>

            <TouchableOpacity
                style={styles.fab}
                onPress={() => {
                    setModalVisible(true);
                    checkConnectBlueTooth();
                    setBlueToothResult("");
                    scanDevices();
                }}
            >
                <Ionicons name="print" size={28} color="#fff" />
            </TouchableOpacity>
            {/* Nút máy in ở góc */}


            {/* Modal chọn máy in */}
            <Modal visible={modalVisible} animationType="slide" transparent>
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        {/* Tabbar */}
                        <View style={styles.tabBar}>
                            <TouchableOpacity
                                style={[styles.tabItem, tab === "wifi" && styles.activeTab]}
                                onPress={() => setTab("wifi")}
                            >
                                <Text
                                    style={tab === "wifi" ? styles.activeTabText : styles.tabText}
                                >
                                    Wifi
                                </Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[styles.tabItem, tab === "bluetooth" && styles.activeTab]}
                                onPress={() => setTab("bluetooth")}
                            >
                                <Text
                                    style={
                                        tab === "bluetooth" ? styles.activeTabText : styles.tabText
                                    }
                                >
                                    Bluetooth
                                </Text>
                            </TouchableOpacity>
                        </View>

                        {/* Radio chọn print mode */}
                        <View style={{ flexDirection: "row", marginVertical: 10 }}>
                            <TouchableOpacity
                                style={styles.radioOption}
                                onPress={() => {
                                    setPrintMode("ESC")
                                    saveSelectedTab("ESC")
                                }}
                            >
                                <View style={styles.radioCircle}>
                                    {printMode === "ESC" && <View style={styles.radioSelected} />}
                                </View>
                                <Text style={styles.radioLabel}>ESC</Text>
                            </TouchableOpacity>

                            <TouchableOpacity
                                style={styles.radioOption}
                                onPress={() => {
                                    setPrintMode("ZPL")
                                    saveSelectedTab("ZPL")
                                }}
                            >
                                <View style={styles.radioCircle}>
                                    {printMode === "ZPL" && <View style={styles.radioSelected} />}
                                </View>
                                <Text style={styles.radioLabel}>ZPL</Text>
                            </TouchableOpacity>
                        </View>

                        {/* Nội dung tab */}
                        {tab === "bluetooth" ? (
                            <>
                                <Text>{scanResult}</Text>
                                <ScrollView style={{ maxHeight: 200 }}>
                                    {devices.map((item, index) => (
                                        <TouchableOpacity
                                            key={item.macAddress || index}
                                            style={[
                                                styles.deviceItem,
                                                item.macAddress === selectedMac && {
                                                    backgroundColor: "#007AFF",
                                                },
                                            ]}
                                            onPress={() => {
                                                setSelectedMac(item.macAddress);
                                                // setModalVisible(false);
                                            }}
                                        >
                                            <Text
                                                style={{
                                                    color:
                                                        item.macAddress === selectedMac ? "#fff" : "#000",
                                                }}
                                            >
                                                {item.deviceName || "Unknown"} {item.macAddress || "Unknown"}
                                            </Text>
                                        </TouchableOpacity>
                                    ))}
                                </ScrollView>
                                <Button onPress={async () => {
                                    const getImageBase64 = await imageUrlToBase64("https://devkct.facenet.vn/scale/image.jpg");
                                    setLoading(true);
                                    try {
                                        await PrinterModule.printBase64Bitmap(getImageBase64, 0, 0, 370, 530, 1, 0);
                                    } catch (e) {
                                        console.error("❌ Print failed:", e);
                                    } finally {
                                        setLoading(false);
                                    }
                                }} title="Test print ESC" />

                                <TouchableOpacity
                                    style={[
                                        styles.testButton,
                                        { backgroundColor: isBluetoothConnected ? "red" : "#34C759" },
                                    ]}
                                    onPress={handleBluetoothToggle}
                                >
                                    <Text style={{ color: "#fff", fontWeight: "bold" }}>
                                        {isBluetoothConnected ? "Disconnect" : "Connect"}
                                    </Text>
                                </TouchableOpacity>

                                <Text style={{ marginTop: 15 }}>Result: {bluetoothResult}</Text>
                            </>
                        ) : (
                            <>
                                <TextInput
                                    style={[styles.input, { marginTop: 15 }]}
                                    onChangeText={(val) => {
                                        setIp(val);
                                        savePrinterConfig(val, port);
                                    }}
                                    value={ip}
                                    placeholder="IP of the printer"
                                    keyboardType="numeric"
                                />
                                <TextInput
                                    style={[styles.input, { marginTop: 15 }]}
                                    onChangeText={(val) => {
                                        setPort(val);
                                        savePrinterConfig(ip, val);
                                    }}
                                    value={port}
                                    placeholder="Port of the printer"
                                    keyboardType="numeric"
                                />
                                {/* <Button onPress={() => testPrintWifi_Pdf()} title="Test print" /> */}
                                {/* Nút test connection WiFi */}
                                <TouchableOpacity
                                    style={[
                                        styles.testButton,
                                        { backgroundColor: isWifiConnected ? "red" : "#34C759" },
                                    ]}
                                    onPress={handleWifiToggle}
                                >
                                    <Text style={{ color: "#fff", fontWeight: "bold" }}>
                                        {isWifiConnected ? "Disconnect" : "Connect"}
                                    </Text>
                                </TouchableOpacity>

                                <Text style={{ marginTop: 15 }}>Result: {wifiResult}</Text>
                            </>
                        )}

                        <Text style={{ marginTop: 10, fontStyle: "italic", fontSize: 12 }}>
                            Mode: {printMode} |{" "}
                            {tab === "wifi"
                                ? `WiFi: ${ip}:${port || "N/A"}`
                                : `Bluetooth: ${selectedMac || "None"}`}
                        </Text>

                        <TouchableOpacity
                            style={styles.closeButton}
                            onPress={() => setModalVisible(false)}
                        >
                            <Text style={{ color: "#fff" }}>Đóng</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
            {/* Loading overlay */}
            <Modal visible={loading} transparent animationType="fade">
                <View style={styles.loadingOverlay}>
                    <ActivityIndicator size="large" color="#fff" />
                    <Text style={{ color: "#fff", marginTop: 10, fontSize: 20 }}>Printing in progress...</Text>
                </View>
            </Modal>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, paddingTop: 30, paddingBottom: 50 },
    loadingOverlay: {
        flex: 1,
        backgroundColor: "rgba(0,0,0,0.6)",
        justifyContent: "center",
        alignItems: "center",
    },
    header: {
        // flex: 1,

        flexDirection: "row",
        alignItems: "center",
        paddingHorizontal: 8,
        paddingVertical: 6,
        backgroundColor: "#f0f0f0",
        borderBottomWidth: 1,
        borderBottomColor: "#ccc",
    },
    input: {
        backgroundColor: "#fff",
        paddingHorizontal: 10,
        paddingVertical: 6,
        borderRadius: 6,
        borderWidth: 1,
        borderColor: "#ccc",
        fontSize: 14,
    },
    inputHeader: {
        flex: 1,
        backgroundColor: "#fff",
        paddingHorizontal: 10,
        paddingVertical: 6,
        borderRadius: 6,
        borderWidth: 1,
        borderColor: "#ccc",
        fontSize: 14,
    },
    testButton: {
        marginTop: 12,
        backgroundColor: "#34C759",
        paddingVertical: 10,
        borderRadius: 6,
        alignItems: "center",
    },
    goButton: {
        marginLeft: 8,
        backgroundColor: "#007AFF",
        paddingHorizontal: 16,
        paddingVertical: 8,
        borderRadius: 6,
    },
    fab: {
        position: "absolute",
        bottom: 50,
        right: 20,
        backgroundColor: "#007AFF",
        width: 56,
        height: 56,
        borderRadius: 28,
        justifyContent: "center",
        alignItems: "center",
        elevation: 5,
    },
    modalOverlay: {
        flex: 1,
        backgroundColor: "rgba(0,0,0,0.4)",
        justifyContent: "center",
        alignItems: "center",
    },
    modalContent: {
        width: "85%",
        backgroundColor: "#fff",
        padding: 16,
        borderRadius: 10,
        elevation: 5,
    },
    tabBar: {
        flexDirection: "row",
        borderBottomWidth: 1,
        borderColor: "#ccc",
        marginBottom: 10,
    },
    tabItem: {
        flex: 1,
        paddingVertical: 8,
        alignItems: "center",
    },
    tabText: { color: "#555" },
    activeTab: {
        borderBottomWidth: 2,
        borderBottomColor: "#007AFF",
    },
    activeTabText: { color: "#007AFF", fontWeight: "bold" },
    radioOption: {
        flexDirection: "row",
        alignItems: "center",
        marginRight: 16,

    },
    radioCircle: {
        width: 22,
        height: 22,
        borderRadius: 11,
        borderWidth: 2,
        borderColor: "#007AFF", // màu viền
        alignItems: "center",
        justifyContent: "center",
        marginRight: 8,
    },
    radioSelected: {
        width: 12,
        height: 12,
        borderRadius: 6,
        backgroundColor: "#007AFF", // chấm xanh bên trong
    },
    radioLabel: { fontSize: 14 },
    deviceItem: {
        padding: 10,
        borderWidth: 1,
        borderColor: "#ccc",
        borderRadius: 6,
        marginVertical: 4,
    },
    closeButton: {
        marginTop: 12,
        backgroundColor: "#007AFF",
        paddingVertical: 8,
        borderRadius: 6,
        alignItems: "center",
    },
});
