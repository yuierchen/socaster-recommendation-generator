package ca.ubc.cs.commandrecommender.db;

import ca.ubc.cs.commandrecommender.Exception.DBConnectionException;
import ca.ubc.cs.commandrecommender.mocks.MockRecommendationDB;
import ca.ubc.cs.commandrecommender.model.IndexMap;
import ca.ubc.cs.commandrecommender.model.User;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Spencer on 6/23/2014.
 */
public class MongoRecommendationDBTest {
    private MongoClient client;
    private DBCollection userCollection;
    private DBCollection recommendationCollection;
    private DBCollection commandDetailsCollection;
    private AbstractCommandToolConverter toolConverter;
    private String DB_URL = "localhost";
    private int DB_PORT = 27000;
    private String DB_NAME = "commands-test";
    private AbstractRecommendationDB recommendationDB;
    private IndexMap userIndexMap, toolIndexMap;
    private String COMMAND_ID1 = "NEW_COMMAND1";
    private String USER_ID1 = "NEW_USER1";
    private String USER_ID = "NEW_USER";
    private String REASON1 = "REASON1";
    private String ALGORITHM_TYPE1 = "ALGOTYPE1";
    private double ALGORITHM_VALUE1 = 0.28f;
    private double REASON_VALUE1 = 0.3;
    private ObjectId command_detail_object_id_1;
    private List<Integer> recommendations;
    private HashSet<Integer> recs;
    private Date willUpdate;

    @Before
    public void setUp() throws UnknownHostException, DBConnectionException {
        client = new MongoClient(DB_URL, DB_PORT);
        this.userCollection = getCollection(MongoRecommendationDB.USER_COLLECTION);
        this.recommendationCollection = getCollection(MongoRecommendationDB.USER_RECOMMENDATION_COLLECTION);
        this.commandDetailsCollection = getCollection(MongoRecommendationDB.COMMAND_DETAILS_COLLECTION);
        userIndexMap = new IndexMap();
        toolIndexMap = new IndexMap();
        ConnectionParameters connectionParameters = new ConnectionParameters(DB_URL, DB_PORT, DB_NAME, "", "");
        toolConverter = new EclipseCommandToolConverter(toolIndexMap);
        initializeDataBase();
        recommendationDB = new MongoRecommendationDB(connectionParameters, toolConverter, userIndexMap);
    }

    private void initializeDataBase() {
        BasicDBObject commandDetail = new BasicDBObject(MongoRecommendationDB.COMMAND_ID_FIELD, COMMAND_ID1);
        commandDetailsCollection.insert(commandDetail);
        command_detail_object_id_1 = (ObjectId)commandDetail.get("_id");
    }

    @After
    public void tearDown(){
        userCollection.remove(new BasicDBObject());
        recommendationCollection.remove(new BasicDBObject());
        commandDetailsCollection.remove(new BasicDBObject());
    }

    private DBCollection getCollection(String collection) {
        return client.getDB(DB_NAME).getCollection(collection);
    }

    @Test
    public void testSaveRecommendationValid(){
        recommendationDB.saveRecommendation(COMMAND_ID1, USER_ID1, REASON1, REASON_VALUE1, ALGORITHM_TYPE1, ALGORITHM_VALUE1);
        DBObject recommendationQuery = new BasicDBObject(MongoRecommendationDB.USER_ID_FIELD, USER_ID1);
        DBCursor cursor = recommendationCollection.find(recommendationQuery);
        int numRecommendationsFound = 0;
        while(cursor.hasNext()){
            DBObject recommendation = cursor.next();
            assertEquals(command_detail_object_id_1, recommendation.get(MongoRecommendationDB.COMMAND_DETAIL_ID_FIELD));
            assertEquals(USER_ID1, recommendation.get(MongoRecommendationDB.USER_ID_FIELD));
            assertEquals(REASON1, recommendation.get(MongoRecommendationDB.REASON_FIELD));
            assertEquals(ALGORITHM_TYPE1, recommendation.get(MongoRecommendationDB.ALGORITHM_TYPE_FIELD));
            assertEquals(ALGORITHM_VALUE1, (Double) recommendation.get(MongoRecommendationDB.ALGORITHM_VALUE_FIELD), 0.0);
            numRecommendationsFound++;
        }
        assertEquals(numRecommendationsFound, 1);
    }

    @Test
    public void testSaveRecommendationCommandIdNull(){
        recommendationDB.saveRecommendation(null, USER_ID1, REASON1, REASON_VALUE1, ALGORITHM_TYPE1, ALGORITHM_VALUE1);
        DBObject recommendationQuery = new BasicDBObject(MongoRecommendationDB.USER_ID_FIELD, USER_ID1);
        DBCursor cursor = recommendationCollection.find(recommendationQuery);
        int numRecommendationsFound = 0;
        while(cursor.hasNext()){
            cursor.next();
            numRecommendationsFound++;
        }
        assertEquals(numRecommendationsFound, 0);
    }

    @Test
    public void testSaveRecommendationUserIdEmpty(){
        recommendationDB.saveRecommendation(COMMAND_ID1, "", REASON1, REASON_VALUE1, ALGORITHM_TYPE1, ALGORITHM_VALUE1);
        DBObject recommendationQuery = new BasicDBObject(MongoRecommendationDB.USER_ID_FIELD, USER_ID1);
        DBCursor cursor = recommendationCollection.find(recommendationQuery);
        int numRecommendationsFound = 0;
        while(cursor.hasNext()){
            cursor.next();
            numRecommendationsFound++;
        }
        assertEquals(numRecommendationsFound, 0);
    }

    @Test
    public void testGetAllUsers(){
        Integer itemIndex = toolIndexMap.addItem(COMMAND_ID1);
        List<User> users = prepareUsers(itemIndex);
        List<User> usersFromDb = recommendationDB.getAllUsers();
        assertFalse(usersFromDb.isEmpty());
        for(User user : usersFromDb){
            compareUsers(user, users);
        }
    }

    private void compareUsers(User retrievedUser, List<User> users) {
        for(User savedUser : users){
            if(savedUser.getUserId().equals(retrievedUser.getUserId())){
                HashSet<Integer> savedUserRecs = savedUser.getPastRecommendations();
                HashSet<Integer> retrievedUserRecs = retrievedUser.getPastRecommendations();
                assertEquals(savedUserRecs.size(), retrievedUserRecs.size());
                assertTrue(savedUserRecs.containsAll(retrievedUserRecs));
            }
        }
    }

    private List<User> prepareUsers(Integer itemIndex) {
        List<User> users = new ArrayList<User>();
        for(int i=0; i < 5; i++){
            willUpdate = new Date(System.currentTimeMillis());
            recommendations = new ArrayList<Integer>();
            recs = createRecs(i, itemIndex);
            users.add(createNewUser(i, recs));
        }
        return users;
    }

    private User createNewUser(int i, HashSet<Integer> recs) {
        String userId =  USER_ID + i;
        BasicDBObject new_user = new BasicDBObject()
                .append(MongoRecommendationDB.USER_ID_FIELD, userId)
                .append(MongoRecommendationDB.LAST_UPLOADED_DATE_FIELD, willUpdate);
        userCollection.insert(new_user);
        return new User(userId, willUpdate, willUpdate, recs, new MockRecommendationDB(toolIndexMap));
    }

    private HashSet<Integer> createRecs(int i, Integer itemIndex) {
        String userId =  USER_ID + i;
        BasicDBObject new_recommendation = new BasicDBObject()
                .append(MongoRecommendationDB.USER_ID_FIELD, userId)
                .append(MongoRecommendationDB.COMMAND_DETAIL_ID_FIELD, command_detail_object_id_1)
                .append(MongoRecommendationDB.COMMAND_ID_FIELD, COMMAND_ID1)
                .append(MongoRecommendationDB.NEW_RECOMMENDATION_FIELD, true)
                .append(MongoRecommendationDB.REASON_FIELD, REASON1);
        recommendationCollection.insert(new_recommendation);
        HashSet<Integer> recs = new HashSet<Integer>();
        recs.add(itemIndex);
        return recs;
    }
}
