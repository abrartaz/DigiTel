package com.digitel;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LogActivity extends AppCompatActivity {
    TableLayout tableLayout;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        tableLayout = findViewById(R.id.tableLayout);
        dbHelper = new DatabaseHelper(this);

        displayLog();
    }

    private void displayLog() {
        Cursor cursor = dbHelper.getAllEntries();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            double pricePerLiter = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRICE_PER_LITRE));
            double totalSale = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_SALE));
            String time = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIME));

            TableRow row = createTableRow(id, pricePerLiter, totalSale, time);
            tableLayout.addView(row);

            // Add horizontal line between rows
            View line = new View(this);
            line.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            line.setBackgroundColor(Color.parseColor("#000000")); // Set the color of the line as per your requirement
            tableLayout.addView(line);
        }
        cursor.close();
    }

    private TableRow createTableRow(final int id, double pricePerLiter, double totalSale, String time) {
        TableRow row = new TableRow(this);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(layoutParams);
        row.setClickable(true);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 1; i < tableLayout.getChildCount(); i += 2) {
                    TableRow tableRow = (TableRow) tableLayout.getChildAt(i);
                    if (tableRow == v) {
                        tableRow.setBackgroundColor(Color.LTGRAY);
                    } else {
                        tableRow.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
                showOptionsDialog(id);
            }
        });

        TextView idTextView = createTextView(String.valueOf(id));
        TextView pricePerLiterTextView = createTextView(String.valueOf(pricePerLiter));
        TextView totalSaleTextView = createTextView(String.valueOf(totalSale));
        TextView timeTextView = createTextView(time);

        row.addView(idTextView);
        row.addView(pricePerLiterTextView);
        row.addView(totalSaleTextView);
        row.addView(timeTextView);

        return row;
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f); // Set width to 0 and weight to 1
        layoutParams.setMargins(1, 1, 1, 1); // Add margins to simulate vertical lines
        textView.setLayoutParams(layoutParams);
        textView.setText(text);
        textView.setTextColor(getResources().getColor(android.R.color.black));
        textView.setPadding(8, 8, 8, 8);
        textView.setGravity(android.view.Gravity.CENTER);
        return textView;
    }

    private void showOptionsDialog(final int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose an option")
                .setItems(new CharSequence[]{"Update", "Delete"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // Update
                                showUpdateDialog(id);
                                break;
                            case 1:
                                // Delete
                                dbHelper.getWritableDatabase().delete(DatabaseHelper.TABLE_NAME,
                                        DatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)});
                                Toast.makeText(LogActivity.this, "Entry deleted for ID: " + id, Toast.LENGTH_SHORT).show();
                                refreshTable();
                                break;
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create().show();
    }

    private void showUpdateDialog(final int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Entry");

        // Inflate custom layout for the dialog
        View view = getLayoutInflater().inflate(R.layout.dialog_update_entry, null);

        // Find views in the custom layout
        final EditText pricePerLiterEditText = view.findViewById(R.id.editTextPricePerLiter);
        final EditText totalSaleEditText = view.findViewById(R.id.editTextTotalSale);

        // Set values of the selected entry in EditText fields
        Cursor cursor = dbHelper.getWritableDatabase().query(DatabaseHelper.TABLE_NAME,
                new String[]{DatabaseHelper.COLUMN_PRICE_PER_LITRE, DatabaseHelper.COLUMN_TOTAL_SALE},
                DatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor.moveToFirst()) {
            double pricePerLiter = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PRICE_PER_LITRE));
            double totalSale = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_SALE));
            pricePerLiterEditText.setText(String.valueOf(pricePerLiter));
            totalSaleEditText.setText(String.valueOf(totalSale));
        }
        cursor.close();

        builder.setView(view);

        // Set buttons for dialog
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Retrieve new values from EditText fields
                double newPricePerLiter = Double.parseDouble(pricePerLiterEditText.getText().toString());
                double newTotalSale = Double.parseDouble(totalSaleEditText.getText().toString());

                // Update the entry in the database
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_PRICE_PER_LITRE, newPricePerLiter);
                values.put(DatabaseHelper.COLUMN_TOTAL_SALE, newTotalSale);
                int rowsAffected = dbHelper.getWritableDatabase().update(DatabaseHelper.TABLE_NAME,
                        values, DatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)});
                if (rowsAffected > 0) {
                    Toast.makeText(LogActivity.this, "Entry updated successfully", Toast.LENGTH_SHORT).show();
                    refreshTable();
                } else {
                    Toast.makeText(LogActivity.this, "Failed to update entry", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.create().show();
    }

    private void refreshTable() {
        // Remove all rows except the header row
        tableLayout.removeViews(1, tableLayout.getChildCount() - 1);
        // Reload log entries
        displayLog();
    }
}
