package com.example.btcn0408;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyTicketsActivity extends AppCompatActivity {
    private RecyclerView rvTickets;
    private TicketAdapter adapter;
    private List<Ticket> ticketList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_tickets);

        db = FirebaseFirestore.getInstance();
        rvTickets = findViewById(R.id.rvMyTickets);
        
        ticketList = new ArrayList<>();
        adapter = new TicketAdapter(ticketList);
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        rvTickets.setAdapter(adapter);

        loadTickets();
    }

    private void loadTickets() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        db.collection("tickets")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ticketList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Ticket ticket = document.toObject(Ticket.class);
                            ticketList.add(ticket);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Lỗi tải vé", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {
        private List<Ticket> tickets;

        TicketAdapter(List<Ticket> tickets) { this.tickets = tickets; }

        @NonNull
        @Override
        public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
            return new TicketViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
            Ticket t = tickets.get(position);
            holder.tvSeat.setText("Ghế: " + t.getSeatNumber());
            holder.tvTime.setText("Ngày đặt: " + t.getBookingTime());
            holder.tvTitle.setText("Mã suất chiếu: " + t.getShowtimeId());
        }

        @Override
        public int getItemCount() { return tickets.size(); }

        class TicketViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSeat, tvTime;
            TicketViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvTicketMovieTitle);
                tvSeat = v.findViewById(R.id.tvTicketSeat);
                tvTime = v.findViewById(R.id.tvTicketTime);
            }
        }
    }
}
