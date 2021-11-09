package com.example.blescanner

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.TRANSPORT_AUTO
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.example.blescanner.DeviceProfile.Companion.CHARACTERISTIC_UUID
import com.example.blescanner.DeviceProfile.Companion.SERVICE_UUID
import com.example.blescanner.databinding.ActivityMainBinding
import java.util.*
import android.bluetooth.BluetoothGattDescriptor




class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothGatt : BluetoothGatt

    companion object{
        private val TAG = "TWESBTSCANNER"
        val BLUETOOTH_REQUESTCODE = 1

    }

    private val bluetoothAdapter : BluetoothAdapter by lazy {
        (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        Log.v(TAG, "onCreate")

        if(!bluetoothAdapter.isEnabled){
            binding.btInfoTextView.text = "BT is disabled"
            openBtActivity()
        }else{
            binding.btInfoTextView.text = "BT is ready"
        }

        binding.startScanButton.setOnClickListener{
            if(bluetoothAdapter.isEnabled) {
                startBLEScan()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        startBLEScan()
    }

    var btRequestActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            Log.v(TAG, "btRequestActivity RESULT OK")
            binding.btInfoTextView.text = "BT is ready"
        } else {
            Log.v(TAG, "btRequestActivity RESULT NOT OK")
            binding.btInfoTextView.text = "BT is disabled"
        }
    }

    fun openBtActivity() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        btRequestActivity.launch(intent)
    }

    fun startBLEScan(){
        Log.v(TAG, "StartBLEScan")

        var scanFilter = ScanFilter.Builder().setDeviceName("TONIESP").build()

        var scanFilters : MutableList<ScanFilter> = mutableListOf()
        scanFilters.add(scanFilter)

        var scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        Log.v(TAG, "Start Scan")
        bluetoothAdapter!!.bluetoothLeScanner.startScan(scanFilters,scanSettings,bleScanCallback)
        Log.v(TAG, "Start Scan -> Waiting results")


    }

    private fun connectToDevice(device : BluetoothDevice){

        bluetoothGatt = device.connectGatt(this,false,bleGattCallback, TRANSPORT_LE)
    }

    var connected = false
    private val bleScanCallback : ScanCallback by lazy {
        object : ScanCallback() {

            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                Log.v(TAG, "onScanResult")

                val bluetoothDevice = result?.device

                if (bluetoothDevice != null && connected == false) {
                    Log.v(TAG, "Device Name ${bluetoothDevice.name} Device Address ${bluetoothDevice.uuids}"
                    )
                    connected = true

                    connectToDevice(bluetoothDevice)

                }
            }

        }
    }

    private fun sendMessage(){
        var service : BluetoothGattService? = bluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
        val message : String = "Ready"
        var messageBytes = ByteArray(0)
        messageBytes = message.toByteArray(charset("UTF-8"))
        characteristic!!.value = messageBytes
        bluetoothGatt?.writeCharacteristic(characteristic)

    }

    private val bleGattCallback : BluetoothGattCallback by lazy {
        object : BluetoothGattCallback(){

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                Log.v(TAG, "onCharacteristicRead")
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                super.onCharacteristicChanged(gatt, characteristic)
                Log.v(TAG, "onCharacteristicChanged")
                Log.v(TAG, "onCharacteristicChanged ${characteristic?.getStringValue(0)}")
            }



            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                Log.v(TAG, "onConnectionStateChange")

                Log.v(TAG, "onConnectionStateChange $newState")
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    Log.v(TAG, "discoverServices")
                    gatt?.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                Log.v(TAG, "onServicesDiscovered")

                var service = gatt!!.getService(SERVICE_UUID)
                var characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                //characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                var value =  gatt.setCharacteristicNotification(characteristic, true)
                //gatt.readCharacteristic(characteristic)
                Log.v(TAG, "onServicesDiscovered $value")

                //sendMessage()
            }

        }
    }


}
class DeviceProfile{
    companion object{
        var SERVICE_UUID  = UUID.fromString("4fa4c201-1fb5-459e-8fcc-c5c9c331914b")
        var CHARACTERISTIC_UUID  = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    }
}
