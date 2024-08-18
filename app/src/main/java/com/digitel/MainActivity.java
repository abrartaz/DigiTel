package com.digitel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.dhaval2404.imagepicker.ImagePicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    Button scanBtn, saveBtn,logBtn;
    Uri imageUri;
    TextView total_price_value,price_per_liter_value;

    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        scanBtn = findViewById(R.id.scan_btn);
        saveBtn = findViewById(R.id.save_button);
        saveBtn.setVisibility(View.GONE);
        logBtn = findViewById(R.id.log_button);
        total_price_value=findViewById(R.id.total_price_value);
        price_per_liter_value=findViewById(R.id.price_per_liter_value);

        dbHelper = new DatabaseHelper(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        scanBtn.setOnClickListener(v -> {
//            Log.d("MainActivity", "Button Clicked!");
            ImagePicker.with(MainActivity.this)
                    .compress(1024)
                    .maxResultSize(1080, 1080)
                    .start();
        });

        logBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the LogActivity
                Intent intent = new Intent(MainActivity.this, LogActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Log.d("MainActivity", "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            if (imageUri != null) {
                // Resolve image orientation and rotate bitmap if necessary
                Bitmap rotatedBitmap = rotateBitmap(imageUri);

                // Set rotated bitmap to ImageView
                imageView.setImageBitmap(rotatedBitmap);

                // Send rotated image to API
                total_price_value.setText("");
                price_per_liter_value.setText("");
                scanBtn.setClickable(false);
                sendImageToAPI(rotatedBitmap);


//                Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap rotateBitmap(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Get orientation info from Exif data
            int orientation = getOrientation(imageUri);
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            inputStream.close();
            return rotatedBitmap;
        } catch (IOException e) {
            return null;
        }
    }

    private int getOrientation(Uri photoUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(photoUri);
            if (inputStream != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();

                return getOrientationFromExif(photoUri.getPath());
            }
        } catch (IOException ignored) {

        }
        return 0;
    }

    private int getOrientationFromExif(String imagePath) {
        int orientation = 0;
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    orientation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    orientation = 270;
                    break;
                default:
                    orientation = 0;
            }
        } catch (IOException ignored) {
        }
        return orientation;
    }

    private void sendImageToAPI(Bitmap bitmap) {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.0.224:5000/ocr");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                String boundary = UUID.randomUUID().toString();
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundaryPrefix = "--" + boundary;
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                byte[] jpegData = outputStream.toByteArray();
                OutputStream requestOutputStream = connection.getOutputStream();

                requestOutputStream.write((boundaryPrefix + lineEnd).getBytes());
                requestOutputStream.write(("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"" + lineEnd).getBytes());
                requestOutputStream.write(("Content-Type: image/jpeg" + lineEnd + lineEnd).getBytes());
                requestOutputStream.write(jpegData);
                requestOutputStream.write((lineEnd + twoHyphens + boundary + twoHyphens + lineEnd).getBytes());

                requestOutputStream.flush();
                requestOutputStream.close();

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream responseStream = connection.getInputStream();
                    String jsonResponse = convertInputStreamToString(responseStream);
                    String test="\"x\"";
                    String anomaly_display="\"x1\"";
                    if (test.equals(jsonResponse)){
                        showInvalidImagePopup(this);

                    } else if (anomaly_display.equals(jsonResponse)) {
                        // Add code here
                        showInvalidImagePopup2(this);

                    } else{
                        // Parse the JSON response
                        try {
                            JSONArray jsonArray = new JSONArray(jsonResponse);

                            // Extract the first (and only) JSONObject from the JSONArray
                            JSONObject jsonObject = jsonArray.getJSONObject(0);

                            // Extract values from the JSON object
                            double pricePerLiter = jsonObject.getDouble("Price per Litre");
                            double totalSale = jsonObject.getDouble("Total Sale");

                            // Log the Price per Litre value

                            // Update UI elements with extracted values
                            runOnUiThread(() -> {
                                total_price_value.setText(String.format("%.2f", totalSale));
                                price_per_liter_value.setText(String.format("%.2f", pricePerLiter));
                                scanBtn.setText("Retake");
                                scanBtn.setBackgroundColor(Color.rgb(143, 74, 111));
                                scanBtn.setClickable(true);

                                saveBtn.setVisibility(View.VISIBLE);
                                saveBtn.setOnClickListener(v -> {
                                    dbHelper.insertFuelEntry(this,pricePerLiter, totalSale);
                                    saveBtn.setVisibility(View.GONE);
                                });
                            });

                        } catch (JSONException ignored) {
                        }
                    }
                } else {
                    InputStream errorStream = connection.getErrorStream();
                    // Process the error response from the API
                }

                connection.disconnect();

            } catch (IOException ignored) {
            }
        }).start();
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }

        bufferedReader.close();
        return stringBuilder.toString();
    }


    // Function to show the "Invalid Image" popup

    private void showInvalidImagePopup(Context context) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Invalid Image");
            builder.setMessage("The selected image is invalid or contains multiple displays.");
            builder.setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss(); // Dismiss the dialog when OK is clicked
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            total_price_value.setText("");
            price_per_liter_value.setText("");
            scanBtn.setClickable(true);
            saveBtn.setVisibility(View.GONE);
        });
    }
    private void showInvalidImagePopup2(Context context) {
        runOnUiThread(() -> {
            // Create a LinearLayout as the root view for the dialog
            LinearLayout layout = new LinearLayout(context);
            layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 40, 40, 40); // Add padding as needed
            layout.setGravity(Gravity.CENTER); // Center the content within the LinearLayout

            // Create Title TextView
            TextView titleView = new TextView(context);
            titleView.setText("Cropped Display Detected");
            titleView.setTextSize(20);
            titleView.setTextColor(Color.BLACK);
            titleView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            titleView.setGravity(Gravity.CENTER); // Center the text horizontally
            layout.addView(titleView);

            // Create Message TextView
            TextView messageView = new TextView(context);
            messageView.setText("Please take an image of the entire display!");
            messageView.setTextColor(Color.GRAY);
            messageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            messageView.setGravity(Gravity.CENTER); // Center the text horizontally
            layout.addView(messageView);

            // Create OK Button
            Button okButton = new Button(context);
            okButton.setText("OK");
            okButton.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            okButton.setGravity(Gravity.CENTER); // Center the button text horizontally
            layout.addView(okButton);

            // Create AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(layout);
            AlertDialog dialog = builder.create();

            // Set button click listener to dismiss dialog
            okButton.setOnClickListener(v -> {
                dialog.dismiss(); // Dismiss the dialog when OK is clicked
            });

            // Show the dialog
            dialog.show();
            total_price_value.setText("");
            price_per_liter_value.setText("");
            scanBtn.setClickable(true);
            saveBtn.setVisibility(View.GONE);


            // Optionally adjust dialog appearance
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE)); // Set dialog background color
        });
    }

}
