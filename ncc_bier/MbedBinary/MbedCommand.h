#ifndef __MBED_COMMAND_H__
#define __MBED_COMMAND_H__

#include "USBHost.h"

struct MbedRequest {
    MbedRequest() { }
    int id;
    int commandId;
    int n;
    float *args;
};

struct MbedResponse {
    MbedResponse() { }
    int requestId;
    int commandId;
    int error;
    int n;
    float *values;
};

#endif