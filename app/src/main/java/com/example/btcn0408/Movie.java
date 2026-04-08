package com.example.btcn0408;

public class Movie {
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String genre;
    private int duration; // in minutes

    public Movie() {}

    public Movie(String id, String title, String description, String imageUrl, String genre, int duration) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.genre = genre;
        this.duration = duration;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}
