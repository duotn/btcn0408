package com.example.btcn0408;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MovieDetailActivity extends AppCompatActivity {
    private ImageView ivPoster;
    private TextView tvTitle, tvGenre, tvDesc;
    private RecyclerView rvShowtimes;
    private ShowtimeAdapter adapter;
    private List<Showtime> showtimeList;
    private FirebaseFirestore db;
    private String movieId;
    private String movieTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        db = FirebaseFirestore.getInstance();
        movieId = getIntent().getStringExtra("movieId");
        movieTitle = getIntent().getStringExtra("movieTitle");

        ivPoster = findViewById(R.id.ivDetailPoster);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvGenre = findViewById(R.id.tvDetailGenre);
        tvDesc = findViewById(R.id.tvDetailDesc);
        rvShowtimes = findViewById(R.id.rvShowtimes);

        showtimeList = new ArrayList<>();
        adapter = new ShowtimeAdapter(showtimeList, this::bookTicket);
        rvShowtimes.setLayoutManager(new LinearLayoutManager(this));
        rvShowtimes.setAdapter(adapter);

        loadMovieDetails();
        loadShowtimes();
    }

    private void loadMovieDetails() {
        db.collection("movies").document(movieId).get().addOnSuccessListener(documentSnapshot -> {
            Movie movie = documentSnapshot.toObject(Movie.class);
            if (movie != null) {
                tvTitle.setText(movie.getTitle());
                tvGenre.setText(movie.getGenre());
                tvDesc.setText(movie.getDescription());
                Glide.with(this).load(movie.getImageUrl()).into(ivPoster);
            }
        });
    }

    private void loadShowtimes() {
        db.collection("showtimes")
                .whereEqualTo("movieId", movieId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showtimeList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Showtime showtime = document.toObject(Showtime.class);
                            showtime.setId(document.getId());
                            showtimeList.add(showtime);
                        }
                        adapter.notifyDataSetChanged();
                        if (showtimeList.isEmpty()) {
                            Toast.makeText(this, "Phim này hiện chưa có suất chiếu.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void bookTicket(Showtime showtime) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để đặt vé", Toast.LENGTH_SHORT).show();
            return;
        }

        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setShowtimeId(showtime.getId());
        ticket.setMovieTitle(movieTitle);
        ticket.setSeatNumber("Ghế A" + (int)(Math.random() * 10 + 1));
        ticket.setBookingTime(new Date().toString());

        db.collection("tickets").add(ticket)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Đặt vé thành công: " + movieTitle, Toast.LENGTH_LONG).show();
                    Log.d("FIREBASE_BOOKING", "Ticket ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi lưu vé: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
