package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.SWTGooglePlace;
import models.SWTPlace;
import models.SWTRating;
import play.Logger;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import security.Secured;

import javax.inject.Inject;
import java.util.*;

/**
 * Created by TeoLenovo on 4/13/2017.
 */
public class SWTPlaceController extends Controller {


    private static final Logger.ALogger logger = Logger.of(SWTPlaceController.class);
    private @Inject WSClient ws;

    @Inject
    private FormFactory formFactory;

    @Security.Authenticated(Secured.class)
    public Result place(String id) {

        SWTPlace place = SWTPlace.findPlaceByGoogleId(id);
        if (place == null) {
             place = new SWTPlace(id);
        }
        SWTGooglePlace gplace;
        try {
            gplace = place.getGooglePlace();
        } catch (IllegalArgumentException ex) {
            flash("error", "Internal error occurred");
            logger.error("Error while fetching json for google place");
            return redirect(routes.SWTPlaceController.searchBox());
        }
        place.calculateRating();
        return ok(views.html.swtPlace.render(place,gplace));
    }

    @Security.Authenticated(Secured.class)
    public Result searchBox() {
        final int latestRatingsScope = 3;
        return ok(views.html.rateSearch.render(new LinkedHashSet<>(SWTRating.latestRatings(latestRatingsScope))));
    }

    @Security.Authenticated(Secured.class)
    public Result findSearch() {
        return ok(views.html.findSearch.render());
    }

    /**
     * Used for ajax calls from search box
     */
    public Result getSWTPlaces() {
        DynamicForm form = formFactory.form().bindFromRequest();
        Double lngFrom = Double.parseDouble(form.get("lngFrom"));
        Double lngTo = Double.parseDouble(form.get("lngTo"));
        Double latFrom = Double.parseDouble(form.get("latFrom"));
        Double latTo = Double.parseDouble(form.get("latTo"));
        List<SWTPlace> placesList =  new LinkedList<>();
        placesList.addAll(SWTPlace.findPlaceByViewPort(lngFrom, lngTo, latFrom, latTo));
        for (SWTPlace place : placesList) {
            place.calculateRating();
        }
        return ok(Json.toJson(placesList));
    }

    /* ajax requests */
    public Result searchFor(String text) {
        Optional<JsonNode> jsonResponse = makeGooglePlacesRequest(text);
        if (jsonResponse.isPresent()) {
            JsonNode rootJson = jsonResponse.get();
            JsonNode resultsNode = rootJson.path("results");
            if (resultsNode.size() > 1) {
                List<SWTGooglePlace> places = new LinkedList<>();
                for (JsonNode googlePlaceNode : resultsNode) {
                    SWTGooglePlace gplace = new SWTGooglePlace(googlePlaceNode);
                    // must be in USA
                    if(gplace.isInUSA()) {
                        places.add(new SWTGooglePlace(googlePlaceNode));
                    }
                }
                if (places.isEmpty()) {
                    return noSearchMatch();
                }
                return ok(views.html.swtPlaces.render(places));
            } else if(resultsNode.size() == 1) {
                SWTGooglePlace gplace = new SWTGooglePlace(resultsNode);
                if (gplace.isInUSA()) {
                    return redirect(routes.SWTPlaceController.place(
                            resultsNode.findPath("place_id").textValue()));
                } else {
                    return noSearchMatch();
                }
            } else {
                return noSearchMatch();
            }
        } else {
            return ok("error");
        }
    }

    private Result noSearchMatch() {
        flash("info", "No establishment found for that search!");
        return redirect(request().getHeader("referer"));
    }

    private Optional<JsonNode> makeGooglePlacesRequest(String searchText) {
        String formattedText = searchText.replaceAll("\\s+", "+");
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + formattedText
                + "&key=AIzaSyBOVsLLDx5MQmY4CUaD9-kt5Dqw5tPjJV4&type=establishment";
        // make sure it is injected (problems in past)
        if (ws == null) {
            ws  = play.api.Play.current().injector().instanceOf(WSClient.class);
        }
        try {
            return Optional.of(ws.url(url).get().thenApply(WSResponse::asJson).toCompletableFuture().get());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

}
