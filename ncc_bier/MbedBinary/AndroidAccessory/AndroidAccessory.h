/* mbed AndroidAccessory Library
 * Created by p07gbar from work by Makoto Abe
 *
 */
#ifndef ADK_H_INCLUDED
#define ADK_H_INCLUDED

#include "mbed.h"
#include "USBHost.h"


//#define  ADKLOG 1
#if ADKLOG
#define  LOG(...)       printf(__VA_ARGS__)
#define  Log(...)       printf(__VA_ARGS__)
#define  log(...)       printf(__VA_ARGS__)

#else
#define  LOG(...)       do {} while(0)
#define  Log(...)       do {} while(0)
#define  log(...)       do {} while(0)

#endif

#define ACCESSORY_STRING_MANUFACTURER   0
#define ACCESSORY_STRING_MODEL          1
#define ACCESSORY_STRING_DESCRIPTION    2
#define ACCESSORY_STRING_VERSION        3
#define ACCESSORY_STRING_URI            4
#define ACCESSORY_STRING_SERIAL         5

#define ACCESSORY_GET_PROTOCOL          51
#define ACCESSORY_SEND_STRING           52
#define ACCESSORY_START                 53



/** An AndroidAccessory control class
 * 
 * It allows easy creation of a mbed android ADK accessory, with minimal low level fussing.
 * Base code should have methods resetDevice(), setupDevice(), callbackRead(u8 *buff, int len) and callBackWrite() functions
 * 
 */

class AndroidAccessory {
public:



/** Create a AndroidAccessory object
 *
 * Create a AndroidAccessoryobject with specified buffer sizes and infomation
 *
 * @param rbuffsize The size of the read buffer
 * @param wbuffsize The size of the write buffer
 * @param manufacturer The manufacturer of the accessory
 * @param model The model of the accessory
 * @param description A short description of the accessory
 * @param version The current version of the accessory
 * @param uri Some data to go with the accessory (URL or more description)
 * @param serial The serial number of the accessory
 */
    AndroidAccessory(int rbuffsize,int wbuffsize,
                     const char* manufacturer,
                     const char *model,
                     const char *description,
                     const char *version,
                     const char *uri,
                     const char *serial
                    );
                    
 /** Init the device
 * This is meant to be implimented by the user of the class
 *
 * @param device Device number
 * @param configuration Configuration
 * @param interfaceNumber Inteface number
 */
    virtual void init(int device, int configuration, int interfaceNumber); 
    
/** Reset the device
 * This is meant to be implimented by the user of the class
 *
 */
    virtual void resetDevice()=0;
        
/** Setup the device
 * This is meant to be implimented by the user of the class. Called when the device is first intialised
 *
 */
    virtual void setupDevice()=0;
    
 /** Callback on Read
 * This is meant to be implimented by the user of the class. Called when some data has been read in.
 *
 * @param buff The buffered read in data
 * @param len The length of the packet recived
 *
 */
    virtual int callbackRead(u8 *buff, int len)=0;
 
 /** Callback after Write
 * This is meant to be implimented by the user of the class. Called when the write has been finished.
 *
 */
    virtual int callbackWrite()=0;
    
 /** Write over USB
 * This sends the data in the buffer over USB in a packet
 *
 * @param buff The buffer to write out
 * @param len The length of the packet to send
 *
 */    
    int write(u8 *buff, int len);
    
/** Write over USB
 * This sends the data in the buffer over USB in a packet, sends _writebuff and _writebuffsize
 *
 */    
    int write() {
        return write(_writebuff,_writebuffsize);
    }
    
 /** Write over USB with no callback
 * This sends the data in the buffer over USB in a packet, waits until the packet is sent, rather than doing a callback
 *
 * @param buff The buffer to write out
 * @param len The length of the packet to send
 *
 */  
    int writeNC(u8 *buff, int len);
    
 /** Write over USB
 * This sends the data in the buffer over USB in a packet, waits until the packet is sent, rather than doing a callback, sends _writebuff and _writebuffsize
 *
 */    
    int writeNC() {
        return writeNC(_writebuff,_writebuffsize);
    }

 /** Read the buffer USB
 * This sends the data in the buffer over USB in a packet, waits until the packet is sent, rather than doing a callback
 *
 * @param buff The buffer to read into
 * @param len The length of the packet to read in
 *
 * @param returns The number of bytes read
 *
 */  
    int read(u8 *buff, int len);


    void adkEnd() {
       // _initok=false;
        resetDevice();
    }; //if connection close
    bool switchDevice(int device);

    //buffer
    u8* _readbuff;
    int _readbuffsize;
    u8* _writebuff;
    int _writebuffsize;
    u8* _strbuff;//255bytes;
     void sendString(const char *str);

private:

    void sendString(int device, int index, const char *str);
    int getProtocol(int device);

    const char *manufacturer;
    const char *model;
    const char *description;
    const char *version;
    const char *uri;
    const char *serial;

    //endpoints
    int input_ep;
    int output_ep;

    int _device;
    int _configuration;
    int _interfaceNumber;

    //bool _initok;

};

extern AndroidAccessory* _adk;


#endif