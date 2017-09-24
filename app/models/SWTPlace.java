package models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import controllers.SWTGooglePlace;
import play.data.FormFactory;

import javax.inject.Inject;
import javax.persistence.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

/**
 * Created by TeoLenovo on 4/6/2017.
 */
@Entity
public class SWTPlace extends Model {

    public SWTPlace(String googleId) {
        this.googleId = googleId;
        SWTGooglePlace googlePlace = SWTGooglePlace.getSWTGooglePlaceById(googleId);
        this.state = googlePlace.state.toLowerCase();
        this.county = googlePlace.county.toLowerCase();
        this.city = googlePlace.city.toLowerCase();
        this.name = googlePlace.name;
        this.lng = googlePlace.lng;
        this.lat = googlePlace.lat;
    }

    @Id
    public Long id;

    @Column(unique = true)
    @JsonIgnore
    public String googleId;

    public String state;

    public String county;

    public String city;

    public String name;

    @Transient
    public Double avgRating;

    @Transient
    public Integer numRatings;

    public Double lat;

    public Double lng;

    @JsonIgnore
    public SWTGooglePlace getGooglePlace() throws IllegalArgumentException {
        SWTGooglePlace place = SWTGooglePlace.getSWTGooglePlaceById(googleId);
        if (place == null) {
            throw new IllegalArgumentException("Something went wrong while getting google place" +
                    "object for id: " + googleId);
        }
        return place;
    }

    @JsonIgnore
    public void calculateRating() {
        Integer numOfRatings = 0;
        Double sumOfRatings = 0.0;
        DecimalFormat oneDigit = new DecimalFormat("#,##0.0");
        for (SWTRating rating : ratings) {
            numOfRatings++;
            sumOfRatings += rating.rating;
        }
        this.avgRating = Double.parseDouble(oneDigit.format(sumOfRatings / numOfRatings));
        this.numRatings = numOfRatings;
    }

    @OneToMany(mappedBy = "swtPlace")
    @JsonIgnore
    public Set<SWTRating> ratings;

    //DAO
    public static Finder<Long, SWTPlace> find = new Finder<>(SWTPlace.class);
    public static SWTPlace findPlaceByGoogleId(String gid) {
        return find.where().eq("googleId", gid).findUnique();
    }
    public static SWTPlace findPlaceById(String id) {
        return find.where().eq("id", id).findUnique();
    }
    public static List<SWTPlace> findPlaceByCity(String city) {
        return find.where().eq("city", city).findList();
    }
    public static List<SWTPlace> findPlaceByCounty(String county) {
        return find.where().eq("county", county).findList();
    }
    public static List<SWTPlace> findPlaceByState(String state) {
        return find.where().eq("state", state).findList();
    }

}
