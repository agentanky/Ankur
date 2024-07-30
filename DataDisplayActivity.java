package com.zybooks.tandan_project2;

// Import statements
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

public class DataDisplayActivity extends AppCompatActivity {
    private static final int PERMISSION_SEND_SMS = 123;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_display);

        // Verify JWT token
        String token = getIntent().getStringExtra("JWT_TOKEN");
        try {
            Claims claims = JWTUtils.validateToken(token);
            String username = claims.getSubject();
            Toast.makeText(this, "Welcome " + username, Toast.LENGTH_SHORT).show();
        } catch (ExpiredJwtException e) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } catch (Exception e) {
            Toast.makeText(this, "Invalid token. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DBHelper(this);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new InventoryAdapter(this, dbHelper.getAllItems(), dbHelper);
        recyclerView.setAdapter(adapter);

        Button addButton = findViewById(R.id.add_data_button);
        addButton.setOnClickListener(v -> showAddItemDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for permission each time activity is resumed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestSmsPermission();
        }
    }

    // Request SMS permission
    private void requestSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
            explainPermission();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS);
        }
    }

    // Explain to user why permission is necessary
    private void explainPermission() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Needed")
                .setMessage("This app requires SMS permission to notify you when inventory counts are low.")
                .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(DataDisplayActivity.this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SEND_SMS))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    // Handle the answer of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS Permission denied. SMS notifications will not be sent.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showAddItemDialog() {
        // Show add item dialog box (name and quantity)
        LayoutInflater inflater = LayoutInflater.from(this);
        View subView = inflater.inflate(R.layout.dialog_add_item, null);
        final EditText nameField = subView.findViewById(R.id.editTextItemName);
        final EditText quantityField = subView.findViewById(R.id.editTextItemQuantity);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Item");
        builder.setView(subView);
        builder.setPositiveButton("ADD ITEM", (dialog, which) -> {
            String name = nameField.getText().toString();
            int quantity;
            try {
                quantity = Integer.parseInt(quantityField.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(DataDisplayActivity.this, "Please enter a valid quantity", Toast.LENGTH_SHORT).show();
                return;
            }
            dbHelper.addItem(name, quantity);
            adapter.changeCursor(dbHelper.getAllItems());
            if (quantity == 0) {
                sendSmsIfNeeded(name);
            }
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendSmsIfNeeded(String itemName) {
        // Send SMS if needed
        String message;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            message = "Alert: Stock for " + itemName + " is now zero. An SMS notification has been sent.";
            smsManager.sendTextMessage("3108501988", null, "Alert: Stock for " + itemName + " is zero.", null, null);
        } else {
            message = "SMS permission not granted. Unable to send SMS notification for zero inventory of " + itemName + ".";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Inventory Alert");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
