#include "WiFiEsp.h"
#include "ArduinoJson.h"
#include <string.h>

// Emulate Serial1 on pins 6/7 if not present
#ifndef HAVE_HWSERIAL1
#include "SoftwareSerial.h"
SoftwareSerial Serial1(6, 7); // RX, TX
#endif

#define MAX_CLIENT_BUF 15

typedef struct
{
  char ssid[32];
  char password[64];
} ApInfo;

String ssid = "";
const char pass[] = "";
int status = WL_IDLE_STATUS;     // the Wifi radio's status
byte rfId[] = {4, -118, 76, 66, -20, 76, -128};
byte rfIdSize = 7;
bool lost = false;
bool configured = false;
short reqCount = 0;       // number of attempted server requests
const short maxReqCount = 10;   // max number of requests before the device is marked as lost
byte clientBuf[MAX_CLIENT_BUF];
ApInfo apInfo[8];   // a device may have up to 8 predefined APs (for increasing the chance of finding a connectable AP)

WiFiEspServer server(80);

void setup()
{
  Serial.begin(115200);   // initialize serial for debugging
  Serial1.begin(115200);    // initialize serial for ESP module
  WiFi.init(&Serial1);    // initialize ESP module

  // check for the presence of the shield
  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("WiFi shield not present");
    while (true); // don't continue
  }

  // at first, the device is not configured
  ssid = "UNCONFIG";
  for (int i=0; i < rfIdSize; i++)
  {
    ssid += (":" + rfId[i]);
  }

  Serial.print("Attempting to start AP ");
  Serial.println(ssid);

  // start access point
  status = WiFi.beginAP(ssid.c_str(), 10, pass, ENC_TYPE_WPA2_PSK, HIDDEN, false);

  Serial.println("Access point started");
  printWifiStatus();

  // start the web server on port 80
  server.begin();
  Serial.println("Server started");
}


void loop()
{
  // a client connected for 
  WiFiEspClient client = server.available();  // listen for incoming clients

  if (client) {
    StaticJsonDocument<32> doc;
    String msg = "";
    int recv = 0;
    Serial.println("New client");
    while (client.connected()) {
      if (client.available() || recv == 0) {
        msg += (char)client.read();
      } else {
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
      if (arr.size() != rfIdSize)
      {
        Serial.println("Incorrect RFID size");
        // should notice the client
        goto endProcessing;
      }

      for (int i=0; i < rfIdSize; i++)
      {
        if (arr[i] != rfId[i])
        {
          // incorrect RF-ID
          goto endProcessing;
        }
      }

      // the ID is correct at this point
      // update the AP list
      arr = doc["apList"];
      byte arrSize = arr.size();
      for (int i=0; i < arrSize; i++)
      {
        strcpy(apInfo[i].ssid, arr[i]["ssid"]);
        strcpy(apInfo[i].password, arr[i]["password"]);
        Serial.println("APs successfully updated");
      }
    }

    endProcessing:
    // give the web browser time to receive the data
    delay(10);

    // close the connection
    client.stop();
    Serial.println("Client disconnected");
  }
}

void sendHttpResponse(WiFiEspClient client)
{
  client.print(
    "HTTP/1.1 200 OK\r\n"
    "Content-Type: text/html\r\n"
    "Connection: close\r\n"  // the connection will be closed after completion of the response
    "Refresh: 20\r\n"        // refresh the page automatically every 20 sec
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
