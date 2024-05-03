package com.example.kidsdrawapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    // Константа для идентификатора запроса на выбор изображения из галереи
    private val PICK_IMAGE_REQUEST = 1
    // Ссылка на объект DrawingView
    private lateinit var drawingView: DrawingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        val btnColor: ImageButton = findViewById(R.id.btnColor)
        val btnEraser: ImageButton = findViewById(R.id.btnEraser)
        val seekBar: SeekBar = findViewById(R.id.penSize)

        // Устанавливаем обработчик клика на кнопку "Цвет"
        btnColor.setOnClickListener {
            showColorPickerDialog(drawingView)
        }
        // Связываем кнопки "Ластик" и "Цвет" с объектом DrawingView
        drawingView.setButtons(btnEraser, btnColor)
        // Связываем SeekBar для управления размером кисти с объектом DrawingView
        drawingView.setSeekBar(seekBar)

        // Находим кнопку меню опций
        val btnOptionsMenu: ImageButton = findViewById(R.id.btnOptionsMenu)
        // Устанавливаем обработчик клика на кнопку меню опций
        btnOptionsMenu.setOnClickListener {
            // Создаем всплывающее меню
            val popupMenu = PopupMenu(this, btnOptionsMenu)
            // Заполняем меню пунктами из файла pop.xml
            popupMenu.menuInflater.inflate(R.menu.pop, popupMenu.menu)

            // Устанавливаем обработчик для пунктов меню
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    // Пункт "Выбрать изображение"
                    R.id.action_button1 -> {
                        pickImageFromGallery()
                        true
                    }
                    // Пункт "Сохранить рисунок"
                    R.id.action_button2 -> {
                        saveDrawingToStorage(drawingView.getBitmap(), "filename")
                        true
                    }
                    // Пункт "Поделиться рисунком"
                    R.id.action_button3 -> {
                        shareDrawing(drawingView.getBitmap())
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    // Метод для открытия галереи и выбора изображения
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // Обработчик результата выбора изображения из галереи
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // Получаем URI выбранного изображения
            val imageUri: Uri? = data?.data
            Log.d("MyActivity", "imageUri: $imageUri") // Вывод URI для отладки
            // Получаем Bitmap выбранного изображения
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            Log.d("MyActivity", "bitmap: $bitmap") // Вывод Bitmap для отладки
            // Устанавливаем выбранное изображение в качестве фона DrawingView
            drawingView.setBackgroundImage(bitmap)
        }
    }

    // Метод для сохранения рисунка в файл
    private fun saveDrawingToStorage(bitmap: Bitmap, fileName: String) {
        // Генерируем уникальное имя файла с текущей датой и временем
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val uniqueFileName = "drawing_$timeStamp.png"

        // Создаем папку для сохранения рисунка (если она не существует)
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "YourFolderName")
        if (!folder.exists()) {
            folder.mkdirs()
        }

        // Создаем файл и записываем в него рисунок
        val file = File(folder, uniqueFileName)
        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            // Обновляем галерею, чтобы новое изображение было видно
            MediaScannerConnection.scanFile(
                this,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )

            // Показываем сообщение о сохранении рисунка
            Toast.makeText(this, "Рисунок сохранен", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Метод для отправки рисунка через другие приложения
    private fun shareDrawing(bitmap: Bitmap) {
        // Создаем папку в кэше приложения для временного хранения изображения
        val cachePath = File(externalCacheDir, "images")
        cachePath.mkdirs()

        // Создаем файл в этой папке и записываем в него рисунок
        val file = File(cachePath, "image.png")
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        //После этого мы получаем URI для файла с помощью FileProvider,
        // который позволяет нам создавать безопасные URI для файлов, чтобы передать их другим приложениям.

        val uri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", file)
        //Здесь мы используем FileProvider для получения безопасного URI файла.
        //Далее мы создаем интент для отправки изображения через действие Intent.ACTION_SEND.
        //Мы указываем тип данных ("image/*") и добавляем URI изображения в интент как EXTRA_STREAM.
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/*"
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(shareIntent, "Поделиться с помощью"))
    }

    private fun showColorPickerDialog(drawingView: DrawingView) {
        val initialColor = Color.BLACK
        val dialog = AmbilWarnaDialog(this, initialColor, object :
            AmbilWarnaDialog.OnAmbilWarnaListener {
            //Мы определяем методы onCancel и onOk интерфейса OnAmbilWarnaListener,
            // чтобы обработать события отмены выбора цвета и подтверждения выбранного цвета.
            override fun onCancel(dialog: AmbilWarnaDialog?) {
                // Обработка отмены выбора цвета
            }

            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                // Устанавливаем выбранный цвет в DrawingView
                drawingView.setColor(color)
                drawingView.invalidate()  // Обновляем рисунок после изменения цвета
            }
        })
        dialog.show()
    }
}
