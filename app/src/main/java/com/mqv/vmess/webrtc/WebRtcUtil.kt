package com.mqv.vmess.webrtc

import org.webrtc.CameraEnumerator

object WebRtcUtil {
    fun availableCameraDevices(enumerator: CameraEnumerator): List<String>  =
        enumerator.deviceNames.asList()

    fun isDeviceHasFrontAndRearCamera(enumerator: CameraEnumerator): Boolean {
        var hasFrontCamera = false
        var hasRearCamera = false

        for (deviceName in availableCameraDevices(enumerator)) {
            if (enumerator.isFrontFacing(deviceName)) {
                hasFrontCamera = true
            }

            if (enumerator.isBackFacing(deviceName)) {
                hasRearCamera = true
            }
        }

        return hasRearCamera && hasFrontCamera
    }

    fun String.formatSdpString(): String =
        this.replace("\"", "")
            .trim()
            .split("\n")
            .map { s -> s.trim() }
            .joinToString(separator = "") { s -> "$s\r\n" }
}