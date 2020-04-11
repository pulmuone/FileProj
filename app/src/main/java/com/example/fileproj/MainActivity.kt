package com.example.fileproj

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_main.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

class MainActivity : AppCompatActivity() {

    var permission_list = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        btnSave.setOnClickListener { view ->
            try {

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "gwise.dat")
                    //put(MediaStore.MediaColumns.MIME_TYPE, "plain/text")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    //put(MediaStore.Downloads.IS_PENDING, 1)
                }

                //val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)!!
                //#1. ContentResolver에 ContentValues를 insert
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)!!

                //#2. ContentResolver의 FD에 파일을 write
                contentResolver.openFileDescriptor(uri, "w", null)

                var output = contentResolver.openOutputStream(uri, "w")
                var dos = DataOutputStream(output)

                dos.writeInt(200)
                dos.writeDouble(55.55)
                dos.writeUTF("반갑습니다.")
                dos.flush()
                dos.close()
                textView.text = "저장 완료"

                contentValues.clear()
                //contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                //#3. write 완료 후 contentResolver update
                contentResolver.update(uri, contentValues, null, null)

            } catch(e : Exception) {
                Log.d("test1", e.printStackTrace().toString())
                e.printStackTrace()
            }
        }

        btnRead.setOnClickListener { view ->

            textView.text = ""

            val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME
            )

            val query = contentResolver.query(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    projection,
                    "${MediaStore.Downloads.DISPLAY_NAME} = 'gwise.dat'",
                    null,
                    null)

            // Scoped Storage 대응
            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), id)

                    contentUri?.let{
                        var input = contentResolver.openInputStream(contentUri)
                        var dis = DataInputStream(input)

                        var value1 = dis.readInt()
                        var value2 = dis.readDouble()
                        var value3 = dis.readUTF()
                        dis.close()

                        textView.text = "value1 : ${value1}\n"
                        textView.append("value2 : ${value2}\n")
                        textView.append("value3 : ${value3}\n")

                        Log.d("abc", contentUri.toString())
                    }
                }
            }
        }

        btnDel.setOnClickListener { view ->
            fileDelete("gwise")
        }
    }

    fun fileDelete(fileName:String)
    {
        val projection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME
        )

        val query = contentResolver.query(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                projection,
                "${MediaStore.Downloads.DISPLAY_NAME} like '${fileName}%'",
                null,
                null)

        // Scoped Storage 대응
        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL), id)

                contentUri?.let{
                    contentResolver.delete(contentUri, null, null)
                    Log.d("abc", contentUri.toString())
                }
            }
        }
    }

    fun checkPermission() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        for(permission : String in permission_list) {
            var chk = checkCallingOrSelfPermission(permission)
            if(chk == PackageManager.PERMISSION_DENIED) {
                requestPermissions(permission_list, 0)
                break
            }
        }
    }
}
