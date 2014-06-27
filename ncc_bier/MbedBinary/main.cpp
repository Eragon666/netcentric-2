
#include "NetCentricApp.h"
#include "mbed.h"
#include "Servo.h" // This is a modified version of the standard (Simon) servo library 
#include "stdio.h"

extern "C" void mbed_mac_address(char *mac);
 
// mbed: 10, robot 1: 0002F7F186B8 
// mbed: 14, robot 2: 0002F7F17FDA

int main() {
    printf("Started NetCentric App\r\n");
    
    NetCentricApp app;
    
    USBInit();
    while (true) {
        USBLoop();
    }
}


