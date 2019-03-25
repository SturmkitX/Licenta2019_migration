#include "WiFiEsp.h"
#include "ArduinoJson.h"
#include "sha256.h"
#include <string.h>

// Emulate Serial1 on pins 6/7 if not present
#ifndef HAVE_HWSERIAL1
#include "SoftwareSerial.h"
SoftwareSerial Serial1(6, 7); // RX, TX
#endif

#define MAX_CLIENT_BUF 15
#define MAX_BAN_SIZE 16
#define UPDATE_INTERVAL 30000       // should be increased, but a low value is needed for testing

#define SERVER_ADDRESS "192.168.0.107"
#define SERVER_PORT 80

typedef struct
{
    char ssid[32];
    char password[64];
} ApInfo;

char ssid[32];
const char pass[] = "";
int status = WL_IDLE_STATUS; // the Wifi radio's status
byte rfId[] = {4, -118, 76, 66, -20, 76, -128};
byte rfIdSize = 7;
bool lost = false;
bool configured = false;
short reqCount = 0;           // number of attempted server requests
const short maxReqCount = 10; // max number of requests before the device is marked as lost
byte clientBuf[MAX_CLIENT_BUF];
ApInfo apInfo[8] = {{"Sala 5 lectura", "sala5#lectura"}}; // a device may have up to 8 predefined APs (for increasing the chance of finding a connectable AP)
byte apInfoSize = 1;
char bannedAp[MAX_BAN_SIZE][32] = {};
int banIndex = 1;
unsigned long lastScan;
unsigned long lastUpdate;
int updateFails = 0;

bool wifiConnected = false;

WiFiEspServer server(80);
WiFiEspClass manager;

// hash is 6073954540343
// ssid is 0b8c07cc48c4e1c2e32eef22

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

void setup()
{
    Serial.begin(115200);  // initialize serial for debugging
    Serial1.begin(115200); // initialize serial for ESP module
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

    Serial.print("Attempting to start AP ");
    Serial.println(ssid);

    // start access point
    status = WiFi.beginAP(ssid, 10, pass, ENC_TYPE_NONE, VIS_TYPE_HIDDEN, false);
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

bool connectWifi()
{
    uint8_t number = manager.scanNetworks();
    delay(2000);

    // list the found networks
    // for (int i=0; i < number; i++)
    // {
    //     Serial.println(espDrv.getSSIDNetworks(i));
    // }

    Serial.println("Number of scanned networks: " + String(number));
    if (apInfoSize == 0)
    {
        // skip to open point connection
        goto openConnection;
    }

    // 1. search for a predefined AP
    for (int i=0; i < number; i++)
    {
        // espDrv is declared as extern in EspDrv.h
        char* foundSsid = espDrv.getSSIDNetworks(i);
        for (int j=0; j < apInfoSize; j++)
        {
            if (strcmp(foundSsid, apInfo[j].ssid) == 0)
            {
                // found a SSID matching the name, check if it also matches the password
                String bssidLog = "Found Predefined AP with BSSID: ";
                bssidLog += espDrv.getBSSIDNetworks(i);
                Serial.println(bssidLog);
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
    for (int i=0; i < number; i++)
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

void loop()
{
    // if (!wifiConnected && millis() - lastScan >= 10000)
    if (!espDrv.getConnectionStatus() && millis() - lastScan >= 10000)
    {
        wifiConnected = connectWifi();
        lastScan = millis();
    }

    // only perform updates if we are connected and periodically
    // if (espDrv.getConnectionStatus() && millis() - lastUpdate >= UPDATE_INTERVAL)
    // {
    //     WiFiEspClient client;
    //     client.connect(SERVER_ADDRESS, SERVER_PORT);
    //     if (client.connected())
    //     {

    //         client.stop();
    //     }
    //     else
    //     {
    //         updateFails++;
    //     }
        
    // }

    // a client connected for
    WiFiEspClient client = server.available(); // listen for incoming clients

    if (client)
    {
        StaticJsonDocument<300> doc;
        String msg = "";
        int recv = 0;
        Serial.println("New client");
        while (client.connected())
        {
            if (client.available() || recv == 0)
            {
                msg += (char)client.read();
            }
            else
            {
                break;
            }
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

            // disconnect from the current AP, needed for testing purposes
            manager.disconnect();
            wifiConnected = false;
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
