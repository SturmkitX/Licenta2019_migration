#include "WiFiEsp.h"
#include "ArduinoJson.h"
#include "sha256.h"
#include "MemoryFree.h"
#include "TinyGPS++.h"
#include <string.h>

// Emulate Serial1 on pins 6/7 if not present
#ifndef HAVE_HWSERIAL1
#include "SoftwareSerial.h"
SoftwareSerial Serial1(6, 7); // RX, TX
SoftwareSerial Serial2(3, 4);
#endif

#define MAX_CLIENT_BUF 15
#define MAX_BAN_SIZE 16
#define MAX_UPDATE_TIMEOUT 300000       // should be increased, but a low value is needed for testing
#define AUTO_UPDATE_INTERVAL 120000
#define SCAN_INTERVAL 20000

#define SERVER_ADDRESS "192.168.0.105"
#define SERVER_PORT 3000

typedef struct
{
    char ssid[32];
    char password[64];
} ApInfo;

char ssid[32] = "";
char pass[32] = "";
int status = WL_IDLE_STATUS; // the Wifi radio's status
byte rfId[] = {4, -118, 76, 66, -20, 76, -128};
byte rfIdSize = 7;
bool lost = false;
bool configured = false;
short reqCount = 0;           // number of attempted server requests
const short maxReqCount = 10; // max number of requests before the device is marked as lost
byte clientBuf[MAX_CLIENT_BUF];
ApInfo apInfo[8] = {{"Baietii 108", "shonstieparola"}}; // a device may have up to 8 predefined APs (for increasing the chance of finding a connectable AP)
byte apInfoSize = 1;
char bannedAp[MAX_BAN_SIZE][32] = {};
int banIndex = 1;
unsigned long lastScan;
unsigned long lastUpdate;
unsigned long autoLastUpdate;
bool gpsConnected = false;
uint8_t scannedNetworks;

bool wifiConnected = false;

WiFiEspServer server(80);
WiFiEspClass manager;

TinyGPSPlus gps;

void computeSsid(char ssid[], byte hash[])
{
    const char hexa[] = "0123456789abcdef";
    ssid[0] = hexa[(hash[0] >> 4)];
    ssid[1] = hexa[(hash[0] & 0x0F)];

    ssid[2] = hexa[(hash[1] >> 4)];
    ssid[3] = hexa[(hash[1] & 0x0F)];

    ssid[4] = hexa[(hash[2] >> 4)];
    ssid[5] = hexa[(hash[2] & 0x0F)];

    ssid[6] = hexa[(hash[3] >> 4)];
    ssid[7] = hexa[(hash[3] & 0x0F)];

    ssid[8] = hexa[(hash[8] >> 4)];
    ssid[9] = hexa[(hash[8] & 0x0F)];

    ssid[10] = hexa[(hash[9] >> 4)];
    ssid[11] = hexa[(hash[9] & 0x0F)];

    ssid[12] = hexa[(hash[10] >> 4)];
    ssid[13] = hexa[(hash[10] & 0x0F)];

    ssid[14] = hexa[(hash[11] >> 4)];
    ssid[15] = hexa[(hash[11] & 0x0F)];

    ssid[16] = hexa[(hash[16] >> 4)];
    ssid[17] = hexa[(hash[16] & 0x0F)];

    ssid[18] = hexa[(hash[17] >> 4)];
    ssid[19] = hexa[(hash[17] & 0x0F)];

    ssid[20] = hexa[(hash[18] >> 4)];
    ssid[21] = hexa[(hash[18] & 0x0F)];

    ssid[22] = hexa[(hash[19] >> 4)];
    ssid[23] = hexa[(hash[19] & 0x0F)];
}

void computePassword(char password[], byte hash[])
{
    const char hexa[] = "0123456789abcdef";
    password[0] = hexa[(hash[0] >> 4)];
    password[1] = hexa[(hash[0] & 0x0F)];

    password[2] = hexa[(hash[1] >> 4)];
    password[3] = hexa[(hash[1] & 0x0F)];

    password[4] = hexa[(hash[4] >> 4)];
    password[5] = hexa[(hash[4] & 0x0F)];

    password[6] = hexa[(hash[5] >> 4)];
    password[7] = hexa[(hash[5] & 0x0F)];

    password[8] = hexa[(hash[8] >> 4)];
    password[9] = hexa[(hash[8] & 0x0F)];

    password[10] = hexa[(hash[9] >> 4)];
    password[11] = hexa[(hash[9] & 0x0F)];

    password[12] = hexa[(hash[12] >> 4)];
    password[13] = hexa[(hash[12] & 0x0F)];

    password[14] = hexa[(hash[13] >> 4)];
    password[15] = hexa[(hash[13] & 0x0F)];

    password[16] = hexa[(hash[16] >> 4)];
    password[17] = hexa[(hash[16] & 0x0F)];

    password[18] = hexa[(hash[17] >> 4)];
    password[19] = hexa[(hash[17] & 0x0F)];

    password[20] = hexa[(hash[20] >> 4)];
    password[21] = hexa[(hash[20] & 0x0F)];

    password[22] = hexa[(hash[21] >> 4)];
    password[23] = hexa[(hash[21] & 0x0F)];
}

void setup()
{
    Serial.begin(115200);  // initialize serial for debugging
    Serial1.begin(115200); // initialize serial for ESP module
    Serial2.begin(9600);    // initialize serial for GPS module
    WiFi.init(&Serial1);   // initialize ESP module

    // check for the presence of the shield
    if (WiFi.status() == WL_NO_SHIELD)
    {
        Serial.println("WiFi shield not present");
        while (true)
            ; // don't continue
    }

    // at first, the device is not configured
    String hashContent = String(rfId[0]);
    for (int i = 1; i < rfIdSize; i++)
    {
        hashContent += (":" + String(rfId[i]));
    }

    Serial.println("Hash content is:");
    Serial.println(hashContent);
    Serial.println("Hash content size is:");
    Serial.println(String(hashContent.length()));

    // hash the ID using SHA-256
    Sha256 sha;
    //  sha.initHmac("abcd1234", 9);
    sha.init();
    sha.print(hashContent);
    byte *hash = sha.result();

    // extract part of the hash and use it for SSID (must be converted to HEX)
    computeSsid(ssid, hash);
    computePassword(pass, hash);

    Serial.print("Attempting to start AP ");
    Serial.println(ssid);
    Serial.println(pass);

    // sha.reset();

    // start access point
    status = WiFi.beginAP(ssid, 10, pass, ENC_TYPE_WPA2_PSK, VIS_TYPE_BROADCAST, false);
    if (espDrv.getConnectionStatus() == WL_CONNECTED)
    {
        Serial.println("Original status is connected!");
    }
    else
    {
        Serial.println("Original status DISCONNECTED");
    }
    

    Serial.println("Access point started");
    printWifiStatus();

    // start the web server on port 80
    server.begin();
    Serial.println("Server started");
}

void scanNetworks()
{
    scannedNetworks = manager.scanNetworks();
    delay(2000);
}

bool connectWifi()
{
    scanNetworks();

    // list the found networks
    // for (int i=0; i < number; i++)
    // {
    //     Serial.println(espDrv.getSSIDNetworks(i));
    // }

    Serial.println("Number of scanned networks: " + String(scannedNetworks));
    if (apInfoSize == 0)
    {
        // skip to open point connection
        goto openConnection;
    }

    // 1. search for a predefined AP
    for (int i=0; i < scannedNetworks; i++)
    {
        // espDrv is declared as extern in EspDrv.h
        char* foundSsid = espDrv.getSSIDNetworks(i);
        for (int j=0; j < apInfoSize; j++)
        {
            if (strcmp(foundSsid, apInfo[j].ssid) == 0)
            {
                // found a SSID matching the name, check if it also matches the password
                Serial.print("Found Predefined AP with BSSID: ");
                Serial.println(espDrv.getBSSIDNetworks(i));
                int status = manager.begin(apInfo[j].ssid, apInfo[j].password);
                if (status == WL_CONNECTED)
                {
                    // get the IP address
                    // delay(10000);
                    IPAddress addr = manager.localIP();
                    char addrStr[16];
                    sprintf(addrStr, "%d.%d.%d.%d", addr[0], addr[1], addr[2], addr[3]);

                    String statusLog = "Access Point IP is: ";
                    statusLog += addrStr;
                    Serial.println(statusLog);
                    // check if the AP has Internet connection
                    if (espDrv.ping("google.com"))
                    {
                        Serial.println("Successfully pinged Google!");
                        return true;
                    }
                    else
                    {
                        String status = "Connected to AP ";
                        status += apInfo[j].ssid;
                        status += ", but no Internet connection";
                        Serial.println(status);
                        manager.disconnect();
                    }
                    
                }
            }
        }        
    }

    // we will reach this point in case there is no predefined AP we can connect to
    openConnection:
    Serial.println("Searching for an open connection:");
    for (int i=0; i < scannedNetworks; i++)
    {
        uint8_t encType = espDrv.getEncTypeNetworks(i);
        int32_t rssi = espDrv.getRSSINetworks(i);
        if (encType == ENC_TYPE_NONE)
        {
            // connect to this AP
            char *foundSsid = espDrv.getSSIDNetworks(i);
            String statusLog = "Open AP: ";
            statusLog += foundSsid;
            statusLog += ", RSSI: ";
            statusLog += String(rssi);
            Serial.println(statusLog);
            int index = 0;
            for (index; index < MAX_BAN_SIZE && bannedAp[index][0] != '\0'; index++)
            {
                if (strcmp(foundSsid, bannedAp[index]) == 0)
                {
                    index = MAX_BAN_SIZE;
                    break;
                }
            }
            if (index == MAX_BAN_SIZE) continue;
            
            int status = manager.begin(foundSsid, "");
            if (status == WL_CONNECTED)
            {
                // first try only (should be increased, but let's not bother other people's APs anymore)
                // int tries = 0;
                // check if the AP has Internet connection

                // get the IP address
                IPAddress addr = manager.localIP();
                char addrStr[16];
                sprintf(addrStr, "%d.%d.%d.%d", addr[0], addr[1], addr[2], addr[3]);

                statusLog = "Access Point IP is: ";
                statusLog += addrStr;
                Serial.println(statusLog);
                if (espDrv.ping("google.com"))
                {
                    return true;
                }
                else
                {
                    statusLog = "Banned AP ";
                    statusLog += foundSsid;
                    Serial.println(statusLog);
                    manager.disconnect();
                    strcpy(bannedAp[banIndex % MAX_BAN_SIZE], foundSsid);
                    banIndex = (banIndex + 1) % MAX_BAN_SIZE;
                }
            }
        }
    }

    Serial.println("No open connection found!");
    // no connection could have been made
    return false;
}

void prepareJSON(JsonDocument& data)
{
    JsonArray rfIdArray = data.createNestedArray("rfId");
    for (int i=0; i < rfIdSize; i++)
    {
        rfIdArray.add((char)rfId[i]);
    }
    JsonArray posArray = data.createNestedArray("positions");
    if (espDrv.getConnectionStatus() != WL_NO_SHIELD)
    {
        JsonObject wifiObj = posArray.createNestedObject();
        wifiObj["source"] = "WIFI";
        JsonArray macs = wifiObj.createNestedArray("macs");

        // send max 5 MAC addresses, for now
        for (int i=0; i < 5; i++)
        {
            JsonObject macInfo = macs.createNestedObject();
            macInfo["mac"] = espDrv.getBSSIDNetworks(i);
            macInfo["rssi"] = espDrv.getRSSINetworks(i);
        }
    }

    if (gps.location.isUpdated())
    {
        JsonObject gpsObj = posArray.createNestedObject();
        gpsObj["source"] = "GPS";

        gpsObj["latitude"] = gps.location.lat();
        gpsObj["longitude"] = gps.location.lng();
        gpsObj["range"] = gps.hdop.value() * 2.5f;
    }
}

void loop()
{
    // if (!wifiConnected && millis() - lastScan >= 10000)
    if (millis() - lastScan >= SCAN_INTERVAL)
    {
        if (espDrv.getConnectionStatus() != WL_CONNECTED)
            wifiConnected = connectWifi();
        lastScan = millis();
    }

    while (Serial2.available() > 0)
    {
        gps.encode(Serial2.read());
    }

    // only perform updates if we are connected and periodically
    if (millis() - autoLastUpdate >= AUTO_UPDATE_INTERVAL)
    {
        if (espDrv.getConnectionStatus() == WL_CONNECTED)
        {
            WiFiEspClient client;
            StaticJsonDocument<400> doc;
            prepareJSON(doc);
            client.connect(SERVER_ADDRESS, SERVER_PORT);
            if (client.connected())
            {
                delay(500);
                Serial.println("JSON Object to send: ");
                serializeJsonPretty(doc, Serial);
                Serial.println();
                client.print("POST /public/history HTTP/1.1\r\n");
                client.print("Host: ");
                client.print(SERVER_ADDRESS);
                client.print("\r\n");
                client.print("Content-Type: application/json\r\n");
                client.print("Content-Length: ");
                client.print(measureJson(doc));
                client.print("\r\n");
                client.print("\r\n");
                serializeJson(doc, client);
                client.print("\r\n");

                delay(2000);
                client.stop();
                lastUpdate = millis();
            }
            else
            {
                Serial.print("Failed to connect to server: ");
                Serial.println(client.status());
            }
        }

        autoLastUpdate = millis();
    }

    if (configured && millis() - lastUpdate >= MAX_UPDATE_TIMEOUT)
    {
        lost = true;
        Serial.println("The AP is now lost and visible");

        // set AP as visible
        // status = WiFi.beginAP(ssid, 10, pass, ENC_TYPE_WPA_PSK, VIS_TYPE_BROADCAST, false);
    }

    // delay(2000);

    // Serial.println("Waiting for a client...");
    // a client connected
    // Serial.print("Free memory before accepting a client: ");
    // Serial.println(freeMemory());
    // Serial.print("Available bytes before awaiting client: ");
    // Serial.println(Serial1.available());
    WiFiEspClient client = server.available(); // listen for incoming clients
    // Serial.print("Client status: ");
    // Serial.println(client.status());

    if (client)
    {
        StaticJsonDocument<300> doc;
        String msg = "";
        Serial.println("New client");
        // while (client.connected())
        // {
        //     if (client.available())
        //     {
        //         msg += (char)client.read();
        //     }
        //     else
        //     {
        //         break;
        //     }
        // }
        while (client.available())
        {
            msg += (char)client.read();
        }

        Serial.println(msg);
        // deserialize JSON
        deserializeJson(doc, msg);

        // process the JSON (must contain action, device RFID and other action-dependent fields)
        // actions can be of 2 types:
        // 1. AP_UPDATE - updates the device's list of known APs (can immediately close connection)
        // 2. POS_UPDATE - queries the last position measurements (must be sent back to the phone and the device must be marked as found)
        if (doc["action"] == "AP_UPDATE")
        {
            JsonArray arr = doc["id"];
            Serial.println("Local size: " + String(rfIdSize) + " Received size: " + String(arr.size()));
            if (arr.size() != rfIdSize)
            {
                Serial.println("Incorrect RFID size");
                // should notice the client
                goto endProcessing;
            }

            for (int i = 0; i < rfIdSize; i++)
            {
                if (arr[i] != rfId[i])
                {
                    // incorrect RF-ID
                    Serial.println("Correct RFID size, but incorrect content");
                    goto endProcessing;
                }
            }

            // the ID is correct at this point
            // update the AP list
            arr = doc["apList"];
            byte arrSize = arr.size();
            for (int i = 0; i < arrSize; i++)
            {
                strcpy(apInfo[i].ssid, arr[i]["ssid"]);
                strcpy(apInfo[i].password, arr[i]["password"]);
            }
            apInfoSize = arr.size();
            Serial.println("APs successfully updated");
            Serial.println("AP list new size: " + String(apInfoSize));

            // send an ACK back to the client
            if (client.connected())
                client.print("ACK\n");

            // disconnect from the current AP, needed for testing purposes
            // manager.disconnect();
            // wifiConnected = false;
            configured = true;
            lastUpdate = millis();
        }
        else
        if (doc["action"] == "POS_UPDATE")
        {
            Serial.println("Received action POS_UPDATE");
            JsonArray arr = doc["id"];
            Serial.println("Local size: " + String(rfIdSize) + " Received size: " + String(arr.size()));
            if (arr.size() != rfIdSize)
            {
                Serial.println("Incorrect RFID size");
                // should notice the client
                goto endProcessing;
            }

            for (int i = 0; i < rfIdSize; i++)
            {
                if (arr[i] != rfId[i])
                {
                    // incorrect RF-ID
                    Serial.println("Correct RFID size, but incorrect content");
                    goto endProcessing;
                }
            }

            // the ID is correct at this point
            scanNetworks();
            StaticJsonDocument<400> info;
            prepareJSON(info);
            serializeJson(info, client);
            lost = false;

            // set AP back to hidden
            // status = WiFi.beginAP(ssid, 10, pass, ENC_TYPE_WPA_PSK, VIS_TYPE_HIDDEN, false);
        }
        

    endProcessing:
        // give the web browser time to receive the data
        delay(10);

        // close the connection
        client.stop();
        Serial.println("Client disconnected");
    }
    // else
    // {
    //     String status = "No client connected! ";
    //     status += (wifiConnected ? "CONNECTED" : "NOT CONNECTED");
    //     Serial.println(status);
    // }
    
}

void sendHttpResponse(WiFiEspClient client)
{
    client.print(
        "HTTP/1.1 200 OK\r\n"
        "Content-Type: text/html\r\n"
        "Connection: close\r\n" // the connection will be closed after completion of the response
        "Refresh: 20\r\n"       // refresh the page automatically every 20 sec
        "\r\n");
    client.print("<!DOCTYPE HTML>\r\n");
    client.print("<html>\r\n");
    client.print("<h1>Hello World!</h1>\r\n");
    client.print("Requests received: ");
    client.print(++reqCount);
    client.print("<br>\r\n");
    client.print("Analog input A0: ");
    client.print(analogRead(0));
    client.print("<br>\r\n");
    client.print("</html>\r\n");
}

void printWifiStatus()
{
    // print your WiFi shield's IP address
    IPAddress ip = WiFi.localIP();
    Serial.print("IP Address: ");
    Serial.println(ip);

    // print where to go in the browser
    Serial.println();
    Serial.print("To see this page in action, connect to ");
    Serial.print(ssid);
    Serial.print(" and open a browser to http://");
    Serial.println(ip);
    Serial.println();
}
