package com.example.btcn0408;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {
    private ImageView ivAvatar;
    private EditText etName;
    private Button btnChangeAvatar, btnSave, btnMyTickets;
    private Uri selectedImageUri;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        ivAvatar = findViewById(R.id.ivAvatar);
        etName = findViewById(R.id.etProfileName);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnMyTickets = findViewById(R.id.btnMyTickets);

        if (user != null) {
            etName.setText(user.getDisplayName());
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).into(ivAvatar);
            }
        }

        ActivityResultLauncher<Intent> pickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        ivAvatar.setImageURI(selectedImageUri);
                    }
                }
        );

        btnChangeAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> uploadData());

        btnMyTickets.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, MyTicketsActivity.class));
        });
    }

    private void uploadData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        if (selectedImageUri != null) {
            StorageReference ref = storage.getReference().child("avatars/" + user.getUid() + ".jpg");
            ref.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot -> {
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateProfile(user, etName.getText().toString(), uri);
                });
            }).addOnFailureListener(e -> Toast.makeText(this, "Upload ảnh lỗi", Toast.LENGTH_SHORT).show());
        } else {
            updateProfile(user, etName.getText().toString(), user.getPhotoUrl());
        }
    }

    private void updateProfile(FirebaseUser user, String name, Uri photoUri) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(photoUri)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
