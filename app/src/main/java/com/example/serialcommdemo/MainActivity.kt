package com.example.serialcommdemo
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.example.serialcommdemo.databinding.ActivityMainBinding
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {
    lateinit var  usbManager: UsbManager
    var device: UsbDevice? = null//contains data of device connected
    var serial: UsbSerialDevice? = null
    var connection: UsbDeviceConnection? = null

    val ACTION_USB_PERMISSION = "permission"

    private lateinit var binding: ActivityMainBinding

    private var readData: String = ""
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
                println("byee")
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
    }


}