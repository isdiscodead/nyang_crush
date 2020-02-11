package com.pro.nyangcrush;



public class User {

    private String UserId;
    private String Name;
    private String Email;
    private int Score;

    public User() {
    }

    public User(String UserId, String Name, String Email, int Score) {

        this.UserId = UserId;

        this.Name = Name;

        this.Email = Email;

        this.Score = Score;

    }
    public String getUserId() {

        return UserId;

    }


    public void setUserId(String UserId) {
        this.UserId = UserId;
    }

    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.Name = Name;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String Email) {
        this.Email = Email;
    }


    public int getScore() {
        return Score;
    }

    public void setScore(int Score) {
        this.Score = Score;
    }

}


