package com.sharesmile.share.v9;

// THIS CODE IS GENERATED BY greenDAO, EDIT ONLY INSIDE THE "KEEP"-SECTIONS

// KEEP INCLUDES - put your custom includes here
// KEEP INCLUDES END
/**
 * Entity mapped to table "USER".
 */
public class User {

    private Long id;
    private String name;
    /** Not-null value. */
    private String emailId;
    private String birthday;
    private String mobileNO;
    private String gender;
    private String profileImageUrl;

    // KEEP FIELDS - put your custom fields here
    // KEEP FIELDS END

    public User() {
    }

    public User(Long id) {
        this.id = id;
    }

    public User(Long id, String name, String emailId, String birthday, String mobileNO, String gender, String profileImageUrl) {
        this.id = id;
        this.name = name;
        this.emailId = emailId;
        this.birthday = birthday;
        this.mobileNO = mobileNO;
        this.gender = gender;
        this.profileImageUrl = profileImageUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** Not-null value. */
    public String getEmailId() {
        return emailId;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getMobileNO() {
        return mobileNO;
    }

    public void setMobileNO(String mobileNO) {
        this.mobileNO = mobileNO;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // KEEP METHODS - put your custom methods here
    // KEEP METHODS END

}
