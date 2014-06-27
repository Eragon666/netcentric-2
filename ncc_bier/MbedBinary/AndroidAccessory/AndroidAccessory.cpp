#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "USBHost.h"
#include "AndroidAccessory.h"
#include "mbed.h"

AndroidAccessory* _adk;

void AdkreadCallback(int device, int endpoint, int status, u8* buf, int len, void* userData);
void AdkwriteCallback(int device, int endpoint, int status, u8* buf, int len, void* userData);



AndroidAccessory::AndroidAccessory(int rbuffsize,int wbuffsize,
                                   const char* manufacturer,
                                   const char *model,
                                   const char *description,
                                   const char *version,
                                   const char *uri,
                                   const char *serial
                                  ) {

    _adk=this;

    this->manufacturer=manufacturer;
    this->model=model;
    this->description=description;
    this->version=version;
    this->uri=uri;
    this->serial=serial;

    u32 len;
    u8* p=USBGetBuffer(&len);
    if (len<(rbuffsize+wbuffsize+255)) {
        error("buff size too big.please resize max=%d. currentSize=%d\r\n",len,(rbuffsize+wbuffsize+255));
    }

    _readbuff=p;
    _readbuffsize=rbuffsize;
    p+=rbuffsize;
    _writebuff=p;
    _writebuffsize=wbuffsize;
    p+=wbuffsize;
    _strbuff=p;
    p+=255;

}



int AndroidAccessory::write(u8 *buff, int len) {
    log("AndroidAccessory::write ");
   // __disable_irq();
    int ret=USBBulkTransfer(_device,output_ep,buff,len,AdkwriteCallback,this);
   // __enable_irq();
    log("--ret=%d \r\n",ret);
    return ret;
}
int AndroidAccessory::writeNC(u8 *buff, int len) {
    log("AndroidAccessory::write ");
   // __disable_irq();
    int ret=USBBulkTransfer(_device,output_ep,buff,len);
   // __enable_irq();
    log("--ret=%d \r\n",ret);
    return ret;
}



int AndroidAccessory::read(u8 *buff, int len) {
   // if(_initok==false)return 0;
    
    log("AndroidAccessory::read ");
   // __disable_irq();
    int ret=USBBulkTransfer(_device,input_ep|0x80,buff,len);
   // __enable_irq();
    log("--ret=%d \r\n",ret);
    return ret;
}


void AndroidAccessory::init(int device, int configuration, int interfaceNumber) {

    log("AndroidAccessory::init \r\n");

//    _initok=false;
    _device = device;
    _configuration = configuration;
    _interfaceNumber = interfaceNumber;
    printf("device = %d configuration = %d interfaceNumber = %d\r\n", device, configuration, interfaceNumber);
    int err;

    u8* buffer=_strbuff;
    err = GetDescriptor(_device,DESCRIPTOR_TYPE_CONFIGURATION,0,buffer,4);

    if (err < 0) {
        log("Failed to get descriptor\r\n");
        return;
    }


    int len = buffer[2] | (buffer[3] << 8);
    if (len > 255) {
        log("config descriptor too large\n");
        /* might want to truncate here */
        return;
    }
    err = GetDescriptor(_device,DESCRIPTOR_TYPE_CONFIGURATION,0,buffer,len);
    u8* p = buffer;
    input_ep=0;
    output_ep=0;
    EndpointDescriptor *epDesc;
    while (p<(buffer+len)) {
        u8 descLen  = p[0];
        u8 descType = p[1];
        log("descLen=%d,descType=%d\r\n",descLen,descType);
        switch (descType) {
            case DESCRIPTOR_TYPE_CONFIGURATION:
                log("config desc\r\n");
                break;
            case DESCRIPTOR_TYPE_INTERFACE:
                log("interface desc\r\n");
                break;
            case DESCRIPTOR_TYPE_ENDPOINT:
                epDesc=(EndpointDescriptor*)p;
                if (!input_ep && (epDesc->bEndpointAddress& 0x80)) {
                    input_ep=epDesc->bEndpointAddress& 0x7f;
                    //PacketSize drop
                    log("input Endpoint address=%d,wMaxPacketSize=%d,bmAttributes=%d\r\n",input_ep,epDesc->wMaxPacketSize,epDesc->bmAttributes);

                } else if (!output_ep) {
                    output_ep=epDesc->bEndpointAddress& 0x7f;
                    //PacketSize drop
                    log("output Endpoint address=%d,wMaxPacketSize=%d,bmAttributes=%d\r\n",input_ep,epDesc->wMaxPacketSize,epDesc->bmAttributes);
                } else {
                    //other
                    log("non input,output Endpoint address=%d,wMaxPacketSize=%d,bmAttributes=%d\r\n",input_ep,epDesc->wMaxPacketSize,epDesc->bmAttributes);
                }
                break;
            default:
                log("unkown desc type(%d) \r\n",descType);
        }
        p+=descLen;
    }

    if (!(input_ep && output_ep)) {
        log("can't find accessory endpoints\r\n");
        return;
    }

    log("SetConfiguration\r\n");
    err = SetConfiguration(device,configuration);
    if (err < 0) {
        log("SetConfiguration error\r\n");
        return;
    }


    log("interrupt setup\r\n");
    //interrupt setup
    if (_readbuff==NULL || _readbuffsize<=0) {
        error("_readbuffer error please setup buffer call setReadBuffer function\r\n");
    }

    if (IO_PENDING!=USBBulkTransfer(_device,input_ep|0x80,_readbuff,_readbuffsize,AdkreadCallback,this))
        return;


    log("setupDevice\r\n");
    this->setupDevice();
//    _initok=true;
}



bool AndroidAccessory::switchDevice(int device) {

    if (1==getProtocol(device)) {
        log("device supports protocol 1\r\n");

    } else {
        log("could not read device protocol version\r\n");
        return false;
    }


    sendString(device,ACCESSORY_STRING_MANUFACTURER,manufacturer);
    sendString(device,ACCESSORY_STRING_MODEL,model);
    sendString(device,ACCESSORY_STRING_DESCRIPTION,description);
    sendString(device,ACCESSORY_STRING_VERSION,version);
    sendString(device,ACCESSORY_STRING_URI,uri);
    sendString(device,ACCESSORY_STRING_SERIAL,serial);
    USBControlTransfer(device,
                       HOST_TO_DEVICE |REQUEST_TYPE_VENDOR|RECIPIENT_DEVICE,
                       ACCESSORY_START,
                       0,//value
                       0, //index
                       0,
                       0,
                       0,
                       0 );

    wait_ms(4);
    //reset usb host
    USBInit();

    return true;

}


int AndroidAccessory::getProtocol(int device) {
    s16 data=-1;
    USBControlTransfer(device,
                       DEVICE_TO_HOST|REQUEST_TYPE_VENDOR|RECIPIENT_DEVICE,
                       ACCESSORY_GET_PROTOCOL,
                       0,//value
                       0, //index
                       (u8*)&data,
                       2,
                       0,
                       0 );
    return data;

}

void AndroidAccessory::sendString(const char *str) {
    sendString(_device,1,str);

}

void AndroidAccessory::sendString(int device, int index, const char *str) {

    LOG("send_string start(%d,%d,%s)  %d \r\n",device,index,str,strlen(str)+1);
    strcpy((char*)_strbuff,str);
    //thankyou curryman san
    USBControlTransfer(device,
                       HOST_TO_DEVICE|REQUEST_TYPE_VENDOR|RECIPIENT_DEVICE,
                       ACCESSORY_SEND_STRING,
                       0,//value
                       index,
                       _strbuff,
                       strlen(str)+1
                      );

    LOG("send_string end(%d,%d,%s)\r\n",device,index,str);

}


/** from USBHost load function. initialize Android device**/
void OnLoadDevice(int device, DeviceDescriptor* deviceDesc, InterfaceDescriptor* interfaceDesc) {
    printf("LoadDevice %d %02X:%02X:%02X\r\n",device,interfaceDesc->bInterfaceClass,interfaceDesc->bInterfaceSubClass,interfaceDesc->bInterfaceProtocol);
    char s[128];

    for (int i = 1; i < 3; i++) {
        if (GetString(device,i,s,sizeof(s)) < 0)
            break;
        printf("%d: %s\r\n",i,s);
    }

    //for android ADK
    if ( ( deviceDesc->idVendor != 0x18D1 ||
            ( deviceDesc->idProduct != 0x2D00 && deviceDesc->idProduct != 0x2D01))
            &&_adk->switchDevice(device)) {

        printf("  try to change accmode.interfaceDesc->bInterfaceClass=%d\r\n",interfaceDesc->bInterfaceClass);
        //1th root
        //accmode_support=true;
        printf("accessory mode ok.\r\n");
        return;
    }

    if (deviceDesc->idVendor == 0x18D1 &&
            (deviceDesc->idProduct == 0x2D00 || deviceDesc->idProduct == 0x2D01)) {
        //2th root
        printf("connecting Android.\r\n");
        printf("idVender=%x  idProduct=%x  interfaceDesc->bInterfaceClass=%d\r\n",deviceDesc->idVendor,deviceDesc->idProduct,interfaceDesc->bInterfaceClass);
        _adk->init(device,1,0);
        //_AdkUSB.loop();
        return;
    }
}

void AdkreadCallback(int device, int endpoint, int status, u8* buf, int len, void* userData) {
    log("AdkreadCallback(int device=%d, int endpoint=%x, int status=%d, u8* buf=%p, int len=%d, void* userData=%p)\r\n",
        device,endpoint,status,buf,len,userData);
//    __disable_irq();
    AndroidAccessory* t = (AndroidAccessory*)userData;
    if (status!=0 && status!=8) {
        log("adk end.\r\n");
        t->adkEnd();
//        __enable_irq();
        USBInit();
        return;
    }


    //virtual method run
    t->callbackRead(buf,len);

    USBBulkTransfer(device, endpoint , buf, len, AdkreadCallback, userData);

//    wait_ms(4);
//    __enable_irq();
}




void AdkwriteCallback(int device, int endpoint, int status, u8* buf, int len, void* userData) {

    log("AdkwriteCallback(int device=%d, int endpoint=%x, int status=%d, u8* buf=%p, int len=%d, void* userData=%p)\r\n",
        device,endpoint,status,buf,len,userData);
    
    AndroidAccessory* t = (AndroidAccessory*)userData;
    t->callbackWrite();
    //wait_ms(4);
    //USBBulkTransfer(device, endpoint , buf, len, AdkwriteCallback, userData);
}

