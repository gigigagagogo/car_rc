#include <Arduino.h>
#include <WiFi.h>
#include <SparkFun_TB6612.h>

int PWMA_1 = 33, PWMB_1 = 32, PWMA_2 = 26, PWMB_2 = 25;
int BIN2_1 = 15, BIN1_1 = 16, BIN2_2 = 23, BIN1_2 = 22;
int AIN1_1 = 17, AIN2_1 = 18, AIN1_2 = 19, AIN2_2 = 21; 
int STBY_1 = 2, STBY_2 = 0; 
int offset = 1;
Motor motor1 = Motor(BIN1_1, BIN2_1, PWMB_1, offset, STBY_1);
Motor motor2 = Motor(AIN1_1, AIN2_1, PWMA_1, offset, STBY_1);

Motor motor3 = Motor(BIN1_2, BIN2_2, PWMB_2, offset, STBY_2);
Motor motor4 = Motor(AIN1_2, AIN2_2, PWMA_2, offset, STBY_2);

const char* ssid = WIFI_SSID;
const char* password = WIFI_PASS;

IPAddress local_IP(10, 87, 202, 50);
IPAddress gateway(10, 87, 202, 1);
IPAddress subnet(255, 255, 255, 0);


WiFiServer server(3333); 
void parseCommands(String &line, int &throttle, int &ster);

void setup() {
  Serial.begin(115200);
  
  // Configura IP statico
  if (!WiFi.config(local_IP, gateway, subnet)) {
    Serial.println("STA Failed to configure");
  }

  Serial.print("Connecting to: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    Serial.print(" Status: ");
    Serial.println(WiFi.status());
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

          motor3.drive(throttle);
          motor4.drive(throttle);
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