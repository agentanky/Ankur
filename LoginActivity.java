package com.zybooks.tandan_project2;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

/** This class handles authentication such as logging in and creating an account.
 * It also includes the UI, and database interaction **/
public class LoginActivity extends AppCompatActivity {
    EditText usernameEditText, passwordEditText;
    Button loginButton, createAccountButton;

    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DBHelper(this);
        // Bind UI components
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        createAccountButton = findViewById(R.id.createAccountButton);

        // Set up click listener for login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        // Set up click listener for account creation (calling createAccount)
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAccount();
            }
        });
    }

    // Handles logging by checking credentials against the DB
    private void login() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=? AND password=?", new String[] {username, password});
        if (cursor.moveToFirst()) {
            // Generate JWT token upon successful login
            String token = JWTUtils.generateToken(username);
            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
            // Navigate to DataDisplayActivity with JWT token
            Intent intent = new Intent(this, DataDisplayActivity.class);
            intent.putExtra("JWT_TOKEN", token);
            startActivity(intent);
            finish(); // Optionally finish LoginActivity if you don't want it on the back stack
        } else {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
    }

    // Inserts data for new account creation
    private void createAccount() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", usernameEditText.getText().toString());
        values.put("password", passwordEditText.getText().toString());

        long id = db.insert("users", null, values);
        if (id != -1)
            Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
        else {
            Toast.makeText(this, "Failed to create account", Toast.LENGTH_SHORT).show();
        }
    }
}
