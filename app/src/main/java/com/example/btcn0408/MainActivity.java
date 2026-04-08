package com.example.btcn0408;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView rvMovies;
    private MovieAdapter adapter;
    private List<Movie> movieList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseAnalytics mFirebaseAnalytics;
    private ListenerRegistration movieListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        rvMovies = findViewById(R.id.rvMovies);
        FloatingActionButton btnLogout = findViewById(R.id.btnLogout);
        FloatingActionButton btnProfile = findViewById(R.id.btnProfile);

        movieList = new ArrayList<>();
        adapter = new MovieAdapter(movieList, movie -> {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, movie.getId());
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, movie.getTitle());
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            Intent intent = new Intent(MainActivity.this, MovieDetailActivity.class);
            intent.putExtra("movieId", movie.getId());
            intent.putExtra("movieTitle", movie.getTitle());
            startActivity(intent);
        });

        rvMovies.setLayoutManager(new GridLayoutManager(this, 2));
        rvMovies.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        btnProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        // 1. Xin quyền thông báo (Android 13+)
        requestNotificationPermission();

        // 2. Lấy FCM Token để test Push
        getFCMToken();

        listenToMoviesRealtime();
        checkAndSeedInitialData();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("FCM_TOKEN", "Lấy token thất bại", task.getException());
                return;
            }
            String token = task.getResult();
            Log.d("FCM_TOKEN", "Token của máy bạn là: " + token);
            // Bạn có thể dùng token này trong Firebase Console để gửi test push
        });
    }

    private void listenToMoviesRealtime() {
        movieListener = db.collection("movies").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                movieList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Movie movie = doc.toObject(Movie.class);
                    movie.setId(doc.getId());
                    movieList.add(movie);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void checkAndSeedInitialData() {
        db.collection("movies").limit(1).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                resetAndSeedData();
            }
        });
    }

    private void resetAndSeedData() {
        Toast.makeText(this, "Đang khởi tạo dữ liệu chuẩn...", Toast.LENGTH_SHORT).show();
        Theater t1 = new Theater("theater_1", "CGV Vincom", "72 Lê Thánh Tôn, Quận 1");
        db.collection("theaters").document(t1.getId()).set(t1);
        addMovieWithShowtimes("Spider-Man: No Way Home", "Người nhện đa vũ trụ cực hấp dẫn.", "https://image.tmdb.org/t/p/w500/1g0dhYtWySMRZAp2o9YfyqlYpZ7.jpg", "Action");
        addMovieWithShowtimes("Avengers: Endgame", "Trận chiến cuối cùng với Thanos.", "https://image.tmdb.org/t/p/w500/or06vSneywvPi2npZ9eoPmZp9Gq.jpg", "Action");
        addMovieWithShowtimes("Joker", "Nguồn gốc gã hề điên loạn.", "https://image.tmdb.org/t/p/w500/udDcl7slbuxZogbu0C3p1wkthvn.jpg", "Drama");
    }

    private void addMovieWithShowtimes(String title, String desc, String url, String genre) {
        Movie movie = new Movie(null, title, desc, url, genre, 120);
        db.collection("movies").add(movie).addOnSuccessListener(doc -> {
            String mid = doc.getId();
            db.collection("showtimes").add(new Showtime(null, mid, "theater_1", "Hôm nay - 19:00", 85000));
            db.collection("showtimes").add(new Showtime(null, mid, "theater_1", "Ngày mai - 21:00", 85000));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Làm mới dữ liệu mẫu (Reset)");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            Toast.makeText(this, "Đang dọn dẹp và reset...", Toast.LENGTH_SHORT).show();
            resetAllCollections();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetAllCollections() {
        db.collection("movies").get().addOnSuccessListener(snapshot -> {
            WriteBatch batch = db.batch();
            for (QueryDocumentSnapshot doc : snapshot) batch.delete(doc.getReference());
            batch.commit().addOnSuccessListener(aVoid -> resetAndSeedData());
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (movieListener != null) movieListener.remove();
    }
}
