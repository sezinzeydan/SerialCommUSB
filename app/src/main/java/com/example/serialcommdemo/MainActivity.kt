package com.example.serialcommdemo
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.serialcommdemo.databinding.ActivityMainBinding
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class MainActivity :  BaseActivity<ActivityMainBinding>() {
    lateinit var  usbManager: UsbManager
    var device: UsbDevice? = null//contains data of device connected
    var serial: UsbSerialDevice? = null
    var connection: UsbDeviceConnection? = null
    lateinit var accessory: UsbAccessory

    /*Communication vars*/
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    var usbDetected: Boolean? = null
    val ACTION_USB_PERMISSION = "permission"


    override val bindingInflater: (LayoutInflater) -> ActivityMainBinding =
        ActivityMainBinding::inflate

    override fun onViewBindingCreated(savedInstanceState: Bundle?) {
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        var filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver , filter);
        /*filter = IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);*/
        /*views.button.setOnClickListener{ sendData(("o")) }
        views.button2.setOnClickListener{disconnect()}*/
    }

    override fun onResume() {
        super.onResume()
        var accessoryList: Array<UsbAccessory>? = usbManager.accessoryList;
        if (!accessoryList?.isEmpty()!!) {
            if(!usbDetected!!){
                Log.d(TAG, "Resume, usb not started")
                accessory = accessoryList[0]

                Log.d(TAG, "Manufacturer " + accessory!!.manufacturer)
                val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
                val filter = IntentFilter(ACTION_USB_PERMISSION)
                registerReceiver(usbReceiver, filter)
                usbManager.requestPermission(accessory, permissionIntent);
            }
        }


    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    /*accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)*/

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        accessory?.apply {
                            //call method to set up accessory communication
                            openAccessory()
                        }
                    } else {
                        Log.d(TAG, "permission denied for accessory $accessory")
                    }
                }
            }
            else if(intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED){
                usbDetected = false
            }
        }
    }

    private fun openAccessory(){
        Log.d(TAG, "openAccessory: $accessory")

        fileDescriptor = usbManager.openAccessory(accessory)

        fileDescriptor?.fileDescriptor?.also { fd ->
            inputStream = FileInputStream(fd)
            outputStream = FileOutputStream(fd)
            /*val thread = Thread(null, this, "AccessoryThread")
            thread.start()*/
        }

    }



    /*
    private var readData: String = ""
     private lateinit var binding: ActivityMainBinding
    private var readCallback: UsbSerialInterface.UsbReadCallback = object: UsbSerialInterface.UsbReadCallback {
        override fun onReceivedData(arg0: ByteArray?) {
            try {
                var data: String? = null
                try {
                    data = String(arg0 as ByteArray, Charset.forName("UTF-8"))
                    "$data/n"
                    //tvReadDataText.setText(tvReadDataText.text.toString() + "\n" + data)
                    readData += data
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                Log.i("exception", e.toString())
            }
        }
    }

    /*is for setting up serial connection to device using USB serial interface
*checks if app has permission to use the USB
* look for what devices are connected
*  then create a serial device from what is connected
* then set up diff connection protocols
* what handles sending data*/
    private val broadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            println("HELLO")
            if(intent?.action!! == ACTION_USB_PERMISSION){
                val granted: Boolean = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if(granted){
                    connection = usbManager.openDevice(device)
                    serial = UsbSerialDevice.createUsbSerialDevice(device, connection)
                    if(serial != null){
                        if(serial!!.open()){
                            serial!!.setBaudRate(9600)
                            serial!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            serial!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                            serial!!.setParity(UsbSerialInterface.PARITY_NONE)
                            serial!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                            serial!!.read(readCallback)
                        }
                        else{
                            Log.i("USB", "port is not open")
                        }
                    }else{
                        Log.i("USB", "port is null")
                    }
                }else{
                    Log.i("USB","permission not granted")
                }
            }else if(intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED){
                startUsbConnection()
            }else if(intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED){
                disconnect()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        var filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction((UsbManager.ACTION_USB_ACCESSORY_ATTACHED))
        filter.addAction((UsbManager.ACTION_USB_DEVICE_ATTACHED))
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        registerReceiver(broadcastReceiver,filter)

        binding.button.setOnClickListener{ sendData(("o")) }
        binding.button2.setOnClickListener{disconnect()}
    }



    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadcastReceiver)
    }

    private fun startUsbConnection(){
        val usbDevices: HashMap<String, UsbDevice>? = usbManager.deviceList
        if(!usbDevices?.isEmpty()!!){
            usbDevices.forEach{entry->
                device = entry.value
                val vendorId: Int? = device?.vendorId
                val intent: PendingIntent = PendingIntent.getBroadcast(this,0,Intent(ACTION_USB_PERMISSION),0)
                usbManager.requestPermission(device,intent)
                Log.i("USB","connection succesful")
                return
            }
        }else{
            Log.i("USB","no usb device connected")
        }

    }

    private fun sendData(input: String){
        serial?.write(input.toByteArray())
        Log.i("USB", "sending data: " + input.toByteArray())
    }

    private fun disconnect(){
        serial?.close()
    }*/
    /* override fun onResume(){
           super.onResume()
           var filter = IntentFilter()
           filter.addAction(ACTION_USB_PERMISSION)
           filter.addAction((UsbManager.ACTION_USB_ACCESSORY_ATTACHED))
           filter.addAction((UsbManager.ACTION_USB_DEVICE_ATTACHED))

           if(!usbDetected!!){
               registerReceiver(broadcastReceiver,filter)
           }

       }*/

    /*override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED == intent?.action!!) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }*/

}