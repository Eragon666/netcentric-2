#include "NetCentricApp.h"
#include "MbedCommand.h"
#include "Servo.h" // This is a modified version of the standard (Simon) servo library 

extern "C" void mbed_mac_address(char *mac);


Servo front(p23); // Front wheel
Servo right(p24); // Right
Servo left(p25); // Left

// MAC-addresses of the robots:
char robot1[6] = {0x00,0x02,0xF7,0xF1,0x86,0xB8};
char robot2[6] = {0x00,0x02,0xF7,0xF1,0x7F,0xDA};

// Process commands here.
MbedResponse *NetCentricApp::getResponse(MbedRequest *request)
{
    if (request->commandId == COMMAND_DRIVE) {
        return driveCommand(request);
    }

    MbedResponse *commandNotFound = new MbedResponse();
    commandNotFound->requestId = request->id;
    commandNotFound->commandId = request->commandId;
    commandNotFound->error = ERR_COMMAND_NOT_FOUND;
    commandNotFound->n = 0;
    commandNotFound->values = NULL;

    return commandNotFound;
}

void move(float front_pos, float right_pos, float left_pos, float time) {
    front = front_pos;
    right = right_pos;
    left = left_pos;
    
    wait(time);
    
    front = 0.5;
    right = 0.5;
    left = 0.5;

    return;
       
}

MbedResponse *NetCentricApp::driveCommand(MbedRequest *request)
{
    float direction = request->args[0];
 
    switch((int)direction) {
        case 1: // move forward

            move(0.5, 0.0, 1.0, 3.5);
            break;
        case 2: // move backwards

            move(0.5, 1.0, 0.0, 3.5);
            break;
        case 3: // turn left, move forward, turn right

            move(0.0, 0.0, 0.0, 1.2);
            move(0.5, 0.0, 1.0, 3.0);
            move(1.0, 1.0, 1.0, 1.2);
            break;
        case 4: // turn right, move forward, turn left

            move(1.0, 1.0, 1.0, 1.2);
            move(0.5, 0.0, 1.0, 3.0);
            move(0.0, 0.0, 0.0, 1.2);
            break;
    }


    MbedResponse *r = new MbedResponse();
    r->requestId = request->id;
    r->commandId = request->commandId;
    r->error = NO_ERROR;
    r->n = 1;
    r->values = new float[1];
    r->values[0] = 0;
    return r;
}


// Setup once a device is connected.
void NetCentricApp::setupDevice()
{
    printf("Connected to Android!\r\n");
    float range = 0.0005;
    float neutral = 0.0015; 
    // MAC address is used to determine which robot it is
    char mac[6];
    mbed_mac_address(mac);
//    for(int i=0; i<6;i++) {
//        printf("%02X", mac[i]);
//    }
//    printf("\n");
    

    if(mac[0]==robot1[0] && mac[1]==robot1[1] && mac[2]==robot1[2] && mac[3]==robot1[3] && mac[4]==robot1[4] && mac[5]==robot1[5]) {
        neutral = 0.001508; // calibrated for robot 1
    } else if(mac[0]==robot2[0] && mac[1]==robot2[1] && mac[2]==robot2[2] && mac[3]==robot2[3] && mac[4]==robot2[4] && mac[5]==robot2[5]) {
        neutral = 0.0015; // calibrated for robot 2
    }
    
    
    front.calibrate(range, 60.0, neutral);
    right.calibrate(range, 60.0, neutral);
    left.calibrate(range, 60.0, neutral);
    
    front = 0.5;
    right = 0.5;
    left = 0.5;
    
}

// Called on disconnect.
void NetCentricApp::resetDevice()
{
    printf("Disconnected\r\n");
}


// Construction of requests.
int NetCentricApp::callbackRead(u8 *buffer, int len)
{
    if (len > 0) {
        // Parse request, format:
        //  int     - request ID
        //  int     - command ID
        //  ubyte   - # args
        //  float[] -- args

        // Note len is fixed as the packet is always equally big. Don't try to use
        // packets of variable size, the smallest size of a encountered packet is
        // used.

        MbedRequest *request = new MbedRequest();

        request->id = getInt(buffer, 0, len);
        request->commandId = getInt(buffer, 4, len);
        request->n = getInt(buffer, 8, len);
        request->args = NULL;

        printf("request: %i, command: %i, n-args: %i\r\n", request->id, request->commandId, request->n);

        int n = request->n;
        if (n > 0) {
            request->args = new float[n];
            for (int i = 0; i < n; i++) {
                int offset = 12 + (i * 4);
                float f = getFloat(buffer, offset, len);
                request->args[i] = f;
            }
        }

        // Construct and send response.
        MbedResponse *response = getResponse(request);
        int responseSize = 4 + 4 + 4 + 4 + (response->n*4);
        u8 responseBuffer[responseSize];

        memcpy(responseBuffer + 0, reinterpret_cast<u8 const *>(&response->requestId), 4);
        memcpy(responseBuffer + 4, reinterpret_cast<u8 const *>(&response->commandId), 4);
        memcpy(responseBuffer + 8, reinterpret_cast<u8 const *>(&response->error), 4);
        memcpy(responseBuffer + 12, reinterpret_cast<u8 const *>(&response->n), 4);
        if (response->n > 0) {
            for (int i = 0; i < response->n; i++)  {
                float f = response->values[i];
                memcpy(responseBuffer + 16 + i*4, reinterpret_cast<u8 const *>(&f), 4);
            }

        }

        write(responseBuffer, responseSize);

        // Clean up.
        if (request->n > 0) {
            delete[] request->args;
        }
        delete request;

        if (response->n > 0) {
            delete[] response->values;
        }
        delete response;
    }

    return 0;
}

// Called to confirm a write operation.
int NetCentricApp::callbackWrite()
{
    return 0;
}


/* Unsigned byte to primitives. Little endian assumed, Java sends Big endian by default. */
float NetCentricApp::getFloat(u8 *buffer, int offset, int bufferLen)
{
    if (offset + 3 > bufferLen) {
        printf("float index out of bounds!\r\n");
        return 0.0;
    }

    float f;
    memcpy(&f, buffer + offset, sizeof(f));
    return f;
}

int NetCentricApp::getInt(u8 *buffer, int offset, int bufferLen)
{
    if (offset + 3 > bufferLen) {
        printf("int index out of bounds!\r\n");
        return 0;
    }

    int i;
    memcpy(&i, buffer + offset, sizeof(i));
    return i;
}

u8 NetCentricApp::getUByte(u8 *buffer, int offset, int bufferLen)
{
    if (offset > bufferLen) {
        printf("byte index out of bounds!\r\n");
        return 0;
    }

    u8 b;
    memcpy(&b, buffer + offset, sizeof(b));
    return b;
}