package controllers;

import akka.io.Inet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.SWTPlace;
import models.SWTRating;
import models.SWTUser;
import play.Logger;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CompletionStage;

/**
 * Created by TeoLenovo on 4/13/2017.
 */
public class SWTPlaceController extends Controller {


    private static final Logger.ALogger logger = Logger.of(SWTPlaceController.class);
    private @Inject WSClient ws;

    @Inject
    private FormFactory formFactory;

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
        return ok(views.html.swtPlace.render(place,gplace));
    }

    public Result searchBox() {
        final int latestRatingsScope = 3;
        return ok(views.html.rateSearch.render(new LinkedHashSet<>(SWTRating.latestRatings(latestRatingsScope))));
    }

    public Result findSearch() {
        return ok(views.html.findSearch.render());
    }

    /**
     * Used for ajax calls from user register form
     */
    public Result getSWTPlaces() {
        DynamicForm form = formFactory.form().bindFromRequest();
        String text = form.get("text").toLowerCase();
        String type = form.get("type");
        List<SWTPlace> places = new LinkedList<>();
        if (type.equals("city")) {
            places = SWTPlace.findPlaceByCity(text);
        }
        for (SWTPlace place : places) {
            place.calculateRating();
        }
        return ok(Json.toJson(places));
    }

    public Result findSearchDashBoard(String text) {

        return ok(views.html.findSearchDash.render());
    }

    public Result searchFor(String text) {
        Optional<JsonNode> jsonResponse = makeGooglePlacesRequest(text);
        if (jsonResponse.isPresent()) {
            JsonNode rootJson = jsonResponse.get();
            JsonNode resultsNode = rootJson.path("results");
            if (resultsNode.size() > 1) {
                List<SWTGooglePlace> places = new LinkedList<>();
                for (JsonNode googlePlaceNode : resultsNode) {
                    places.add(new SWTGooglePlace(googlePlaceNode));
                }
                return ok(views.html.swtPlaces.render(places));
            } else if(resultsNode.size() == 1) {
                return redirect(routes.SWTPlaceController.place(resultsNode.findPath("place_id").textValue()));
            } else {
                flash("info", "No establishment found for that search!");
                return redirect(request().getHeader("referer"));
            }
        } else {
            return ok("error");
        }
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
