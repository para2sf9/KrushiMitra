package com.example.ai_agri;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.util.Patterns;
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

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";
    Button signup_btn, google_btn;
    TextView login_txt;
    EditText name_edit, email_edit, phone_edit, password_edit, confirm_password_edit;
    ImageView togglePassword, toggleConfirmPassword;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
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
        setContentView(R.layout.activity_sign_up);
        
        signup_btn = findViewById(R.id.signup_btn);
        google_btn = findViewById(R.id.google_btn);
        login_txt = findViewById(R.id.login_txt);
        name_edit = findViewById(R.id.name);
        email_edit = findViewById(R.id.email);
        phone_edit = findViewById(R.id.phone);
        password_edit = findViewById(R.id.password);
        confirm_password_edit = findViewById(R.id.confirm_password);
        togglePassword = findViewById(R.id.togglePassword);
        toggleConfirmPassword = findViewById(R.id.toggleConfirmPassword);

        // Password visibility toggles
        togglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                password_edit.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                togglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                password_edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                togglePassword.setImageResource(android.R.drawable.ic_menu_view);
            }
            password_edit.setSelection(password_edit.getText().length());
        });

        toggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            if (isConfirmPasswordVisible) {
                confirm_password_edit.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleConfirmPassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                confirm_password_edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleConfirmPassword.setImageResource(android.R.drawable.ic_menu_view);
            }
            confirm_password_edit.setSelection(confirm_password_edit.getText().length());
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

        login_txt.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        signup_btn.setOnClickListener(v -> {
            String name = name_edit.getText().toString().trim();
            String email = email_edit.getText().toString().trim();
            String phone = phone_edit.getText().toString().trim();
            String password = password_edit.getText().toString().trim();
            String confirmPassword = confirm_password_edit.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                email_edit.setError("Invalid email address");
                return;
            }

            if (phone.length() < 10) {
                phone_edit.setError("Invalid phone number");
                return;
            }

            if (password.length() < 6) {
                password_edit.setError("Password must be at least 6 characters");
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirm_password_edit.setError("Passwords do not match");
                return;
            }

            signup_btn.setEnabled(false);
            signup_btn.setText("Creating account...");

            DatabaseManager.signUp(this, email, password, name, phone, new DatabaseManager.AuthCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_LONG).show();
                        // For now, after signUp we go to login. 
                        // If we want to save session, we'd need the ID from the database response.
                        // Or we can let user login normally.
                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        signup_btn.setEnabled(true);
                        signup_btn.setText("Create Account");
                        if (error != null && (error.toLowerCase().contains("duplicate") || error.toLowerCase().contains("already exists") || error.toLowerCase().contains("unique constraint"))) {
                             Toast.makeText(SignUpActivity.this, "Account already exists. Please login.", Toast.LENGTH_LONG).show();
                             startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                             finish();
                        } else {
                             Toast.makeText(SignUpActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
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

                DatabaseManager.saveSession(this, email, displayName, "", id, photoUrl);
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_LONG).show();
        }
    }
}
