#include <Arduino.h>
#include <WiFi.h>
#include <SparkFun_TB6612.h>

int PWMA = 33, PWMB = 32;
int BIN2 = 15, BIN1 = 16;
int AIN1 = 17, AIN2 = 18; 
int STBY = 2; 
int offset = 1;
Motor motor1 = Motor(BIN1, BIN2, PWMB, offset, STBY);
Motor motor2 = Motor(AIN1, AIN2, PWMA, offset, STBY);

const char* ssid = WIFI_SSID;
const char* password = WIFI_PASS;

IPAddress local_IP(192, 168, 1, 100);
IPAddress gateway(192, 168, 1, 1);
IPAddress subnet(255, 255, 255, 0);
IPAddress primaryDNS(8, 8, 8, 8);
IPAddress secondaryDNS(8, 8, 4, 4);

WiFiServer server(3333); 
void parseCommands(String &line, int &throttle, int &ster);

void setup() {
  Serial.begin(115200);
  
  // Configura IP statico
  if (!WiFi.config(local_IP, gateway, subnet, primaryDNS, secondaryDNS)) {
    Serial.println("STA Failed to configure");
  }
  
  Serial.print("Connecting to: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("");
  Serial.println("WiFi connected.");
  
  Serial.print("IP address statico: ");
  Serial.println(WiFi.localIP());
  
  server.begin();
}

void loop() {
  WiFiClient client = server.available();
  if (client) {
    Serial.println("Client connesso");
    String line = "";
    int throttle = 0, ster = 0;
    while (client.connected()) {
      while (client.available()) {
        char c = client.read();
        line += c;
        if(c == '\n') {
          parseCommands(line,throttle,ster);
          Serial.print("T="); Serial.print(throttle);
          Serial.print(" R="); Serial.println(ster);
          line = "";
          motor2.drive(throttle);
          motor1.drive(throttle);
        }
      }
      delay(1);
    }
    client.stop();
    Serial.println("Client disconnesso");
  }
}

void parseCommands(String &line, int &throttle, int &ster){
  int tIndex = line.indexOf("T:");
  int end = line.indexOf(";");

  if (end == -1 || tIndex == -1){
    Serial.println("Comando T non disponibile");
    return ;
  }

  String accs = line.substring(tIndex + 2, end);
  throttle = accs.toInt();
  String directions = line.substring(end + 3);
  ster = directions.toInt();

}