package main;


import static com.mongodb.client.model.Filters.eq;
import static spark.Spark.*;
import com.mongodb.client.*;
import org.bson.Document;
import com.mongodb.MongoClient;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Main {
	public static void main(String[] args) {
		port(1234);
		MongoClient mongoClient = new MongoClient("localhost", 27017);
		MongoDatabase db = mongoClient.getDatabase("REST2");
		MongoCollection<Document> usersCollection = db.getCollection("users");
		MongoCollection<Document> authCollection = db.getCollection("auth");

		get("/hello", (req, res) -> "Hello World");

		get("/login", (request, response) -> {
			String un = request.queryParams("username");
			String pw = request.queryParams("password");
			Document user = usersCollection.find(eq("username", un)).first();
			String rightPassword = user.getString("password");
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			Document token = new Document();
			if (pw.equals(rightPassword)) {
				token.append("username", un);
				String currentTime = Long.toString(timestamp.getTime());
				token.append("token", currentTime);
				authCollection.insertOne(token);
				return currentTime;
			} else {
				return "login_failed";
			}
		});

		get("/newuser", (req, res) -> {
			String username = req.queryParams("username");
			System.out.print("New user: " + username + " created\n");
			String password = req.queryParams("password");
			Document dc = new Document("username", username);
			List<Document> friends = new ArrayList<Document>();
			dc.append("username", username).append("password", password).append("friends", friends);
			usersCollection.insertOne(dc);
			return "okay";
		});
		get("/addfriend", (req, res) -> {
			String token = req.queryParams("token");
			String reqFriendUserID = req.queryParams("friend");
			Document auth = authCollection.find(eq("token", token)).first();
			if (auth != null) {
				String reqUsername = auth.getString("username");
				Document user = usersCollection.find(eq("username", reqUsername)).first();
				String rightUsername = user.getString("username");
				String password = user.getString("password");
				Document friend = usersCollection.find(eq("username", reqFriendUserID)).first();
				List<Document> friends = user.get("friends", List.class);
				friend.remove("password");
				friend.remove("friends");
				friends.add(friend);
				usersCollection.deleteOne(usersCollection.find(eq("username", reqUsername)).first());
				Document user2 = new Document();
				user2.append("username", rightUsername);
				user2.append("password", password);
				user2.append("friends", friends);
				usersCollection.insertOne(user2);
				return "okay";
			} else {
				return "failed authentication";
			}
		});


		get("/friends", (req, res) -> {
			String token = req.queryParams("token");
			Document auth = authCollection.find(eq("token", token)).first();
			String reqUsername = auth.getString("username");
			Document user = usersCollection.find(eq("username", reqUsername)).first();
			List<Document> friends = user.get("friends", List.class);
			List<String> friendNames = new ArrayList<>();
			for (int i = 0; i < friends.size(); i++) {
				friendNames.add(friends.get(i).getString("username"));
			}
			return friendNames;
		});
	}
}
