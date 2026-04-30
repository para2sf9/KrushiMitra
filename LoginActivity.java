package com.example.ai_agri;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ai_agri.utils.DatabaseManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    TextView signup_txt, forgot_password;
    Button login_btn, skip_btn, google_btn;
    EditText email_edit, password_edit;
    ImageView togglePassword;
    private boolean isPasswordVisible = false;
    private GoogleSignInClient mGoogleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primaryGreen)),
                SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primaryGreen)));
        setContentView(R.layout.activity_login);

        signup_txt = findViewById(R.id.signup_txt);
        login_btn = findViewById(R.id.login_btn);
        skip_btn = findViewById(R.id.skip_btn);
        google_btn = findViewById(R.id.google_btn);
        email_edit = findViewById(R.id.email);
        password_edit = findViewById(R.id.password);
        togglePassword = findViewById(R.id.togglePassword);
        forgot_password = findViewById(R.id.forgot_password);

        // Password visibility toggle
        togglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                password_edit.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Or a "closed eye" icon if available
            } else {
                password_edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(android.R.drawable.ic_menu_view);
            }
            password_edit.setSelection(password_edit.getText().length());
        });

        // Forgot Password
        forgot_password.setOnClickListener(v -> {
            String email = email_edit.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email to recover password", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseManager.forgotPassword(email, new DatabaseManager.AuthCallback() {
                @Override
                public void onSuccess(String password) {
                    runOnUiThread(() -> {
                        // For demonstration purposes, we send an email via Intent since backend mailing is missing
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("message/rfc822");
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Password Recovery - AI AGRI");
                        intent.putExtra(Intent.EXTRA_TEXT, "Your password is: " + password);
                        try {
                            startActivity(Intent.createChooser(intent, "Send mail..."));
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(LoginActivity.this, "Your password is: " + password, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show());
                }
            });
        });

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("13392603000-kh6j1qc5okmd1pviddl1h7uchskt3d3p.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        google_btn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        signup_txt.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));

        skip_btn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        login_btn.setOnClickListener(v -> performEmailLogin());
    }

    private void performEmailLogin() {
        String email = email_edit.getText().toString().trim();
        String password = password_edit.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_edit.setError("Invalid email address");
            return;
        }

        if (password.length() < 6) {
            password_edit.setError("Password must be at least 6 characters");
            return;
        }

        login_btn.setEnabled(false);
        login_btn.setText("Logging in...");

        DatabaseManager.login(this, email, password, new DatabaseManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    login_btn.setEnabled(true);
                    login_btn.setText("Login");
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                // Email Verification check from Google ID Token
                boolean isEmailVerified = true;
                String idToken = account.getIdToken();
                if (idToken != null) {
                    try {
                        String[] parts = idToken.split("\\.");
                        if (parts.length > 1) {
                            String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE), StandardCharsets.UTF_8);
                            JsonObject jsonObject = new Gson().fromJson(payload, JsonObject.class);
                            if (jsonObject.has("email_verified")) {
                                isEmailVerified = jsonObject.get("email_verified").getAsBoolean();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing ID token for verification", e);
                    }
                }

                if (!isEmailVerified) {
                    Toast.makeText(this, "Your Google email is not verified. Please use a verified account.", Toast.LENGTH_LONG).show();
                    return;
                }

                String email = account.getEmail();
                String displayName = account.getDisplayName();
                String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";
                String id = account.getId();
                
                saveAndNavigate(email, displayName, "", id, photoUrl);
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Google Sign-In failed: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveAndNavigate(String email, String name, String phone, String id, String photoUrl) {
        DatabaseManager.saveSession(this, email, name, phone, id, photoUrl);
        navigateToMain();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
