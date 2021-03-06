package Models

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.sharesmile.share.LeaderBoard
import com.sharesmile.share.core.UnObfuscable
import org.json.JSONObject
import java.io.Serializable
import java.util.*

/**
 * Created by Shine on 19/12/16.
 */
class TeamLeaderBoard : UnObfuscable, Serializable {

    @SerializedName("count")
    var count: Long = 0

    @SerializedName("next")
    var nextUrl: String = ""

    @SerializedName("previous")
    var previousUrl: String = ""

    @SerializedName("results")
    var teamList: ArrayList<UserDetails>? = null;

    public class UserDetails : Serializable {
        @SerializedName("user")
        var user: User? = null;

        @SerializedName("league_total_distance")
        public val leagueTotalDistance: User.TotalDistance? = null

    }


    public class User : Serializable {

        @SerializedName("user_id")
        var id: Long? = 0;

        @SerializedName("first_name")
        private var firstName: String? = null

        @SerializedName("last_name")
        private val lastName: String? = null

        @SerializedName("social_thumb")
        private val imageUrl: String? = null


        public fun convertToLeaderBoard(distance: Float): LeaderBoard {

            var board = LeaderBoard();
            board.id = id;
            board.first_name = firstName;
            board.last_name = lastName
            board.last_week_distance = distance;
            board.social_thumb = imageUrl;
            return board;
        }

        class TotalDistance : Serializable {
            @SerializedName("total_distance")
            public val totalDistance: Float? = 0f;
        }
    }

}