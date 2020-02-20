package com.intrax.ozonics

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.util.ArrayList

class DeviceListAdapter(private val context: Context, val resourceId: Int, private val deviceList: ArrayList<BluetoothDevice>): BaseAdapter() {
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater


    override fun getCount(): Int {
        return deviceList.size
    }


    override fun getItem(position: Int): Any {
        return deviceList[position]
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get view for row item
        val convertView = inflater.inflate(resourceId, parent, false)
        val device: BluetoothDevice = deviceList[position]
        if(device!=null){
            val deviceName = convertView.findViewById<TextView>(R.id.deviceName)
            val deviceAddress = convertView.findViewById<TextView>(R.id.deviceAddress)
            deviceName.text = device.name
            deviceAddress.text = device.address
        }
        return convertView
    }

}