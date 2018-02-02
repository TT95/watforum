package controllers;

import models.SWTUser;
import models.SWTYear;
import org.apache.commons.lang3.time.DateUtils;
import org.h2.mvstore.DataUtils;
import play.Configuration;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by teo on 2/2/18.
 */
public class SWTAdminContoller extends Controller {

    @Inject
    private Configuration configuration;

    public Result connect() {
        // auth check
        List<Long> stringIds =
                Arrays.stream(configuration.getString("adminIds").split(","))
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
        if(!stringIds.contains(SWTUserController.currentUser().id)) {
            flash("error", "No admin privileges!");
            return redirect(routes.Public.landing());
        }
        List<SWTUser> users = SWTUser.findAll();
        Integer usersNum = users.size();
        Integer participantsNum = 0;
        Integer oldParticipantsNum;
        Integer todayRegistered = 0;
        for(SWTUser user : users) {
            // number of participants
            for(SWTYear year : user.swtYears) {
                if (year.year.equals(Calendar.getInstance().get(Calendar.YEAR))) {
                    participantsNum ++;
                }
            }
            // registered today?
            if (DateUtils.isSameDay(user.createdAt, new Date())) {
                todayRegistered ++;
            }
        }
        oldParticipantsNum = usersNum - participantsNum;
        return ok(views.html.admin.render(
                usersNum,
                participantsNum,
                oldParticipantsNum,
                todayRegistered));
    }
}