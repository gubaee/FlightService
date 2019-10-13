package edu.uw.cs;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
//import javax.xml.bind.*;

/**
 * Runs queries against a back-end database
 */
public class Query {
  // DB Connection
  private Connection conn;

  // Password hashing parameter constants
  private static final int HASH_STRENGTH = 65536;
  private static final int KEY_LENGTH = 128;
  private String currentUsername = null;
  private int rid = 1;
  //fid , result_time , result , currentusername

  private int fid1_array[] = new int[100];
  private int fid2_array[] = new int[100];
  private int result_time_array[] = new int[100];
  private String result_array[] = new String[100];
  private String currentusername_array[] = new String[100];


  private int [] flightsOrderNum;
  private int itnum = 0;
  private StringBuffer sb = new StringBuffer();
  private StringBuffer sbReservation = new StringBuffer();
  private int numOfsearch=0;
  private int numOfInDirect;

  //TreeMap<Integer, String> flightOrder = new TreeMap<Integer,String>();

  // Canned queries
  private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement checkFlightCapacityStatement;

  ////////////  ////////////  ////////////    Create User Statement  ////////////  ////////////   //////////////
  private static final String CLEAR_USERS = "DELETE FROM users WHERE password_hash != 'aaa'";
  private PreparedStatement ClearUsersStatement;

  private PreparedStatement InsertUserStatement;
  private static final String INSERT_USER = "INSERT INTO USERS (username, password_hash, password_salt, balance) \n"
          + "VALUES( ? , ? , ? , ?)";


  ////////////  ////////////  ////////////    Login Statement  ////////////  ////////////   //////////////
  private PreparedStatement CheckUsernameStatement;
  private static final String CHECK_USERNAME = "SELECT username FROM USERS WHERE username = ?";

  private PreparedStatement GetSaltStatement;
  private static final String GET_SALT = "SELECT password_salt FROM USERS WHERE username = ?";

  private PreparedStatement GetHashPasswordStatement;
  private static final String GET_HASHPASSWORD = "SELECT password_hash FROM USERS WHERE username = ?";



  ////////////  ////////////  ////////////    Search Statement   ////////////  ////////////   //////////////
  private PreparedStatement directFlightStatement;
  private static final String DIRECT_FLIGHT = "SELECT TOP(?) fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price " +
          "FROM Flights WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? AND canceled = 0 ORDER BY actual_time ASC";

  private PreparedStatement IndirectFlightStatement;
  private static final String GET_INDIRECT_FLIGHT ="SELECT DISTINCT fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price\n" +
          "        FROM FLIGHTS WHERE fid = ?";

  private PreparedStatement IdSearchStatement;
  private static final String GET_ID = "SELECT TOP(?) (f1.fid) as f1_id, f2.fid as f2_id , (f1.origin_city) as f1_ori, f1.dest_city as f1_dest,  f2.origin_city as f2_ori, f2.dest_city as f2_dest," +
          " (MIN(f1.actual_time)) as f1_time, (MIN(f2.actual_time)) as f2_time, (MIN(f1.actual_time)+MIN(f2.actual_time)) AS total " +
          "FROM FLIGHTS AS  f1, FLIGHTS AS f2 " +
          "WHERE f1.origin_city = ? AND f2.dest_city = ? AND f1.dest_city = f2.origin_city AND f1.day_of_month = ? AND f1.day_of_month = f2.day_of_month AND f1.canceled = 0 AND f2.canceled = 0 " +
          "GROUP BY f1.fid, f2.fid, f1.origin_city,f1.dest_city,f2.origin_city,f2.dest_city ORDER BY total";

  private PreparedStatement DirectSortStatement;
  private static final String DIRECT_INSERT_SORTING = "INSERT INTO SORT (fid ,fid2, total_time, txt,username) VALUES (? , 0, ? , ?,?)";

  private PreparedStatement InDirectSortStatement;
  private static final String INDIRECT_INSERT_SORTING = "INSERT INTO SORT (fid ,fid2 , total_time, txt,username) VALUES (? ,?, ? , ?,?)";

  private PreparedStatement SortStatement2;
  private static final String GET_SORTING = "SELECT fid,fid2,total_time,txt,username FROM SORT WHERE username = ? ORDER BY total_time , fid";

  private PreparedStatement ClearSortTable;
  private static final String CLEAR_SORT_TABLE = "DELETE FROM sort WHERE fid >0";

  private PreparedStatement ClearUserTable;
  private static final String CLEAR_USER_TABLE = "DELETE FROM users WHERE balance != -193";

  private PreparedStatement ClearSortedTable;
  private static final String CLEAR_SORTED_TABLE = "DELETE FROM sortedlist WHERE fid >0";

  private PreparedStatement ClearBookingTable;
  private static final String CLEAR_BOOKING_TABLE = "DELETE FROM booking WHERE fid > 0";

  private PreparedStatement ClearReservationTable;
  private static final String CLEAR_RESERVATION_TABLE = "DELETE FROM reservations WHERE rid > 0";

  private PreparedStatement addToSortedListForDirect;
  private static final String ADD_TO_SROTED_LIST_TABLE = "INSERT INTO sortedList (itnum,fid,fid2, username,capacity) VALUES(?,?,?,?,?)";

  private PreparedStatement addToSortedListForInDirect;
  private static final String ADD_TO_SROTED_LIST_TABLE2 = "INSERT INTO sortedList (itnum,fid,fid2,username) VALUES(?,?,?,?)";



  ////////////  ////////////  ////////////    Booking Statement   ////////////  ////////////   //////////////
  private PreparedStatement getIdForBookingStatement;
  private static final String GET_ID_FROM_SORTEDLIST = "SELECT fid , fid2, username FROM sortedlist where itnum = ? AND username = ?";

  private PreparedStatement checkBookingTableStatement;
  private static final String CHECK_BOOKING_TABLE = "SELECT capacity FROM BOOKING WHERE fid = ?";

  private PreparedStatement insertDataIntoBookingStatement;
  private static final String INSERT_INTO_BOOKING_TABLE = "INSERT INTO BOOKING (fid, capacity,capacity_taken,bday,username) VALUES(? , ?,0,?,?)";

  private PreparedStatement GetCapacityTakenStatement;
  private static final String GET_CAPACITY_TAKEN = "SELECT count(*) AS total FROM booking WHERE fid = ?";



  private PreparedStatement UpdateCapacityTakenStatement;
  private static final String UPDATE_CAPACITY_TAKEN = "UPDATE booking SET capacity_taken = ? WHERE fid = ?;";

  private PreparedStatement CheckSortedListForItnum;
  private static final String CHECk_SORTED_LIST_FOR_ITNUM = "SELECT itnum FROM sortedlist WHERE itnum = ? AND username = ?";

  private PreparedStatement CheckForDayInReservationTable;
  private static final String CHECK_DAY_IN_BOOKING_RESERVATION = "SELECT rday FROM reservations WHERE rday = ? AND username ?";

  private PreparedStatement CheckForDayInBookingTable;
  private static final String CHECK_DAY_IN_BOOKING = "SELECT bday FROM booking WHERE bday = ? AND username ?";


  ////////////  ////////////  ////////////    Reservation Statement   ////////////  ////////////   //////////////
  private PreparedStatement checkFlightPriceStatement;
  private static final String CHECK_FLIGHT_PRICE =  "SELECT price FROM Flights WHERE fid = ?";

  private PreparedStatement checkFlightDayStatement;
  private static final String CHECK_FLIGHT_DAY =  "SELECT day_of_month FROM Flights WHERE fid = ?";

  private PreparedStatement InsertReservationStatement;
  private static final String INSERT_RESERVATION = "INSERT INTO Reservations (rid, username, price, paid , rday,fid1,fid2) VALUES( ? , ?, ?, ?, ?,?,?)";

  private PreparedStatement DeleteReservationStatement;
  private static final String DELETE_RESERVATION = "DELETE FROM reservations WHERE rid = ?";

  private PreparedStatement CheckInfoForReservation;
  private static final String CHECK_FLIGHT_INFO_FOR_RESERVATION = "SELECT rid, paid, fid1, fid2 FROM reservations WHERE username = ?";

  private PreparedStatement FlightInfoStatement;
  private static final String FLIGHT_INFO = "SELECT fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price FROM FLIGHTS WHERE fid = ?";

  ////////////  ////////////  ////////////    Pay Statement   ////////////  ////////////   //////////////
  private PreparedStatement PayStatusStatement;
  private static final String PAY_STATUS = "UPDATE reservations SET paid = ? WHERE username = ? AND rid = ?";

  private PreparedStatement ListOfReservationIdForUser;
  private static final String LIST_RESERVATION_ID = " SELECT rid, price, paid FROM reservations WHERE username = ? AND rid = ?";

  private PreparedStatement CheckUnpaidReservationStatement;
  private static final String CHECK_UNPAID_RESERVATIONS = "SELECT paid FROM reservations WHERE rid = ? AND paid = ?";

  private PreparedStatement UserBalanceStatement;
  private static final String USER_BALANCE = "SELECT balance FROM USERS WHERE username = ?";

  private PreparedStatement CurrentRidStatement;
  private static final String CURRENT_RID = "SELECT rid FROM reservations ORDER BY rid desc";

  private PreparedStatement SetNewBalanceStatement;
  private static final String SET_NEW_BALANCE = "UPDATE USERS SET balance = ? WHERE username = ?";

  private PreparedStatement CheckPaidStatusStatement;
  private static final String CHECK_PAID_STATUS = "SELECT paid FROM reservations WHERE username = ? AND rid = ?";


  ////////////  ////////////  ////////////    Cancel Statement   ////////////  ////////////   //////////////





  /**
   * Establishes a new application-to-database connection. Uses the
   * dbconn.properties configuration settings
   *
   * @throws IOException
   * @throws SQLException
   */
  public void openConnection() throws IOException, SQLException {
    // Connect to the database with the provided connection configuration
    Properties configProps = new Properties();
    configProps.load(new FileInputStream("C:\\Users\\filer\\Desktop\\cse344\\cse344-lab1\\cse344-lab1\\lab1\\dbconn.properties"));
    String serverURL = configProps.getProperty("hw1.server_url");
    String dbName = configProps.getProperty("hw1.database_name");
    String adminName = configProps.getProperty("hw1.username");
    String password = configProps.getProperty("hw1.password");
    String connectionUrl = String.format("jdbc:sqlserver://%s:1433;databaseName=%s;user=%s;password=%s", serverURL,
            dbName, adminName, password);
    conn = DriverManager.getConnection(connectionUrl);

    // By default, automatically commit after each statement
    conn.setAutoCommit(true);

    // By default, set the transaction isolation level to serializable
    conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
  }

  /**
   * Closes the application-to-database connection
   */
  public void closeConnection() throws SQLException {

    clearTables();
    conn.close();
  }

  /**
   * Clear the data in any custom tables created.
   *
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    try {

      ClearBookingTable.executeUpdate();
      ClearReservationTable.executeUpdate();
      ClearUserTable.executeUpdate();

      // TODO: YOUR CODE HERE
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  //  public void clearSortTables() {
//    try {
//      itnum = 0;
//      numOfsearch = 0;
//      ClearSortTable.executeUpdate();
//
//      // TODO: YOUR CODE HERE
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
  public void ClearBookingTable(){
    try{
      ClearBookingTable.executeUpdate();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  //  public void clearSortedTable(){
//    try{
//      ClearSortedTable.executeUpdate();
//    }
//    catch (Exception e) {
//      e.printStackTrace();
//    }
//  }
  public void clearReservationTable(){
    try{
      ClearReservationTable.executeUpdate();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  public void ClearArray(){

    for (int i = 0; i < 100; i++){
      fid1_array[i] = 0;
      fid2_array[i] = 0;
      result_time_array[i] = 0;
      result_array[i] = "";
      currentusername_array[i] = "";
    }
  }
  @SuppressWarnings("Duplicates")
  public void SortArray(){
    int temp1;
    int temp2;
    int temp3;
    String temp4;

    for (int i = 1; i < result_time_array.length; i++) {
      for (int j = i; j > 0; j--) {

        if(result_time_array[j] == result_time_array[j-1]){
          if(fid1_array[j] < fid1_array[j-1]){
            temp1 = result_time_array[j];
            result_time_array[j] = result_time_array[j - 1];
            result_time_array[j - 1] = temp1;

            temp2 = fid1_array[j];
            fid1_array[j] = fid1_array[j - 1];
            fid1_array[j - 1] = temp2;

            temp3 = fid2_array[j];
            fid2_array[j] = fid2_array[j - 1];
            fid2_array[j - 1] = temp3;

            temp4 = result_array[j];
            result_array[j] = result_array[j - 1];
            result_array[j - 1] = temp4;
          }
        }
        else if (result_time_array[j] < result_time_array[j - 1] && result_time_array[j] !=0 && result_time_array[j-1]!=0 ) {
          temp1 = result_time_array[j];
          result_time_array[j] = result_time_array[j - 1];
          result_time_array[j - 1] = temp1;

          temp2 = fid1_array[j];
          fid1_array[j] = fid1_array[j - 1];
          fid1_array[j - 1] = temp2;

          temp3 = fid2_array[j];
          fid2_array[j] = fid2_array[j - 1];
          fid2_array[j - 1] = temp3;

          temp4 = result_array[j];
          result_array[j] = result_array[j - 1];
          result_array[j - 1] = temp4;
        }
      }
    }
  }

  /*
   * prepare all the SQL statements in this method.
   */
  @SuppressWarnings("Duplicates")
  public void prepareStatements() throws SQLException {
    checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);
    InsertUserStatement = conn.prepareStatement(INSERT_USER);
    CheckUsernameStatement = conn.prepareStatement(CHECK_USERNAME);
    GetSaltStatement = conn.prepareStatement(GET_SALT);
    GetHashPasswordStatement = conn.prepareStatement(GET_HASHPASSWORD);
    IndirectFlightStatement = conn.prepareStatement(GET_INDIRECT_FLIGHT);
    ClearUsersStatement = conn.prepareStatement(CLEAR_USERS);
    directFlightStatement = conn.prepareStatement(DIRECT_FLIGHT);
    IdSearchStatement = conn.prepareStatement(GET_ID);
    DirectSortStatement = conn.prepareStatement(DIRECT_INSERT_SORTING);
    InDirectSortStatement = conn.prepareStatement(INDIRECT_INSERT_SORTING);
    SortStatement2 = conn.prepareStatement(GET_SORTING);
    ClearSortTable = conn.prepareStatement(CLEAR_SORT_TABLE);
    ClearSortedTable = conn.prepareStatement(CLEAR_SORTED_TABLE);
    addToSortedListForDirect = conn.prepareStatement(ADD_TO_SROTED_LIST_TABLE);
    addToSortedListForInDirect = conn.prepareStatement(ADD_TO_SROTED_LIST_TABLE2);
    getIdForBookingStatement = conn.prepareStatement(GET_ID_FROM_SORTEDLIST);
    insertDataIntoBookingStatement = conn.prepareStatement(INSERT_INTO_BOOKING_TABLE);
    checkBookingTableStatement = conn.prepareStatement(CHECK_BOOKING_TABLE);
    GetCapacityTakenStatement = conn.prepareStatement(GET_CAPACITY_TAKEN);
    UpdateCapacityTakenStatement = conn.prepareStatement(UPDATE_CAPACITY_TAKEN);
    ClearBookingTable = conn.prepareStatement(CLEAR_BOOKING_TABLE);
    CheckSortedListForItnum = conn.prepareStatement(CHECk_SORTED_LIST_FOR_ITNUM);
    checkFlightPriceStatement = conn.prepareStatement(CHECK_FLIGHT_PRICE);
    checkFlightDayStatement = conn.prepareStatement(CHECK_FLIGHT_DAY);
    InsertReservationStatement = conn.prepareStatement(INSERT_RESERVATION);
    ClearReservationTable = conn.prepareStatement(CLEAR_RESERVATION_TABLE);
    CheckForDayInReservationTable = conn.prepareStatement(CHECK_DAY_IN_BOOKING_RESERVATION);
    CheckInfoForReservation = conn.prepareStatement(CHECK_FLIGHT_INFO_FOR_RESERVATION);
    FlightInfoStatement = conn.prepareStatement(FLIGHT_INFO);
    PayStatusStatement = conn.prepareStatement(PAY_STATUS);
    ListOfReservationIdForUser = conn.prepareStatement(LIST_RESERVATION_ID);
    UserBalanceStatement = conn.prepareStatement(USER_BALANCE);
    CheckUnpaidReservationStatement = conn.prepareStatement(CHECK_UNPAID_RESERVATIONS);
    CurrentRidStatement = conn.prepareStatement(CURRENT_RID);
    SetNewBalanceStatement = conn.prepareStatement(SET_NEW_BALANCE);
    CheckPaidStatusStatement = conn.prepareStatement(CHECK_PAID_STATUS);
    ClearUserTable = conn.prepareStatement(CLEAR_USER_TABLE);
    CheckForDayInBookingTable = conn.prepareStatement(CHECK_DAY_IN_BOOKING);
    DeleteReservationStatement = conn.prepareStatement(DELETE_RESERVATION);

    // TODO: YOUR CODE HERE
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username user's username
   * @param password user's password
   *
   * @return If someone has already logged in, then return "User already logged
   *         in\n" For all other errors, return "Login failed\n". Otherwise,
   *         return "Logged in as [username]\n".
   */

  public byte[] getSalt(String username) throws SQLException{
    GetSaltStatement.clearParameters();
    GetSaltStatement.setString(1, username);

    ResultSet results = GetSaltStatement.executeQuery();

    results.next();
    byte[] salt = results.getBytes("password_salt");
    results.close();


    return salt;
  }
  public byte[] getHashPassword(String username) throws SQLException{
    GetHashPasswordStatement.clearParameters();
    GetHashPasswordStatement.setString(1, username);
    ResultSet results2 = GetHashPasswordStatement.executeQuery();
    results2.next();
    byte[] hashFromTable = results2.getBytes("password_hash");
    results2.close();

    return hashFromTable;
  }
  public String transaction_login(String username, String password)  {
    try {
      if (currentUsername != null) {
        return "User already logged in\n";
      }

      //get salt from username
      byte[] salt = getSalt(username);
      boolean CheckPass = false;

      KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);
      byte[] hash = generateHash(spec);
      byte[] hashFromTable = getHashPassword(username);

      for (int i = 0; i < hashFromTable.length; i++){
        if (hash[i] == hashFromTable[i]){
          CheckPass = true;
        }
      }
      if (CheckPass == true){
        currentUsername = username;
        return "Logged in as "+username+"\n";
      }
    }
    catch (SQLException e){
      return "Login failed\n";
    }

    return "Login failed\n";
  }

  /**
   * Implement the create user function.
   *
   * @param username   new user's username. User names are unique the system.
   * @param password   new user's password.
   * @param initAmount initial amount to deposit into the user's account, should
   *                   be >= 0 (failure otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n"
   *         if failed.
   */
  public boolean checkUsername(String username) throws SQLException{
    CheckUsernameStatement.clearParameters();
    CheckUsernameStatement.setString(1, username);
    ResultSet results = CheckUsernameStatement.executeQuery();

    while(results.next()) {
      String usernameResult = results.getString("username");
      if (usernameResult == username) {
        return false;
      }
    }
    return true;

  }
  public void InsertUser(String username, byte[] salt, byte[] hash, int initAmount) throws SQLException{
    InsertUserStatement.setString(1, username.toLowerCase());
    InsertUserStatement.setBytes(2,hash);
    InsertUserStatement.setBytes(3, salt);
    InsertUserStatement.setInt(4, initAmount);
    InsertUserStatement.executeUpdate();
  }
  public String transaction_createCustomer(String username, String password, int initAmount) {
    try {
      if(initAmount < 0) {
        return "Failed to create user\n";
      }
      // Generate a random cryptographic salt
      byte[] salt = generateSalt();
      // Specify the hash parameters
      KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);
      // Generate the hash
      byte[] hash = generateHash(spec);

      if(checkUsername(username) == true) {
        InsertUser(username , salt, hash, initAmount);
        return "Created user "+username+"\n";
      }

    }
    catch (SQLException e){
      return "Failed to create user\n";
    }
    //     INSERT_USER = ("INSERT INTO USERS (username, password_hash, password_salt, balance) \n"
//            + "VALUES('"+username+"', '"+hash+"', '"+salt+"', '"+initAmount+"'");

    return "Failed to create user\n";
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination
   * city, on the given day of the month. If {@code directFlight} is true, it only
   * searches for direct flights, otherwise is searches for direct flights and
   * flights with two "hops." Only searches for up to the number of itineraries
   * given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight        if true, then only search for direct flights,
   *                            otherwise include indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your
   *         selection\n". If an error occurs, then return "Failed to search\n".
   *
   *         Otherwise, the sorted itineraries printed in the following format:
   *
   *         Itinerary [itinerary number]: [number of flights] flight(s), [total
   *         flight time] minutes\n [first flight in itinerary]\n ... [last flight
   *         in itinerary]\n
   *
   *         Each flight should be printed using the same format as in the
   *         {@code Flight} class. Itinerary numbers in each search should always
   *         start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  @SuppressWarnings("Duplicates")
  public String transaction_search(String originCity, String destinationCity, boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {

    try {
//      clearSortedTable();
//      clearSortTables();
      ClearArray();

      if(directFlight == true){
//private static final String GET_SORTING = "SELECT fid,fid2,total_time,txt,username FROM SORT WHERE username = ? ORDER BY total_time , fid";

        sb = new StringBuffer();
        DirectFlight(originCity,destinationCity,dayOfMonth,numberOfItineraries);

        SortArray();

        //private static final String GET_SORTING = "SELECT fid,fid2,total_time,txt,username FROM SORT WHERE username = ? ORDER BY total_time , fid";

//        SortStatement2.setString(1,currentUsername);
//        ResultSet finalResults = SortStatement2.executeQuery();

        for(int i = 0; i < numOfsearch; i ++){
          sb.append("Itinerary "+itnum+": ");
          sb.append(result_array[i]);

          itnum++;
        }

        if(sb.toString().isEmpty()){
          //clearSortTables();
          return "No flights match your selection\n";
        }
        //clearSortTables();
        return sb.toString();
      }
      else{
        sb = new StringBuffer();

        DirectFlight(originCity,destinationCity,dayOfMonth,numberOfItineraries);
        InDirectFlight(originCity,destinationCity,dayOfMonth,numberOfItineraries);

//        SortStatement2.setString(1,currentUsername);
//        ResultSet finalResults = SortStatement2.executeQuery();

        SortArray();
        for(int i = 0; i < numOfsearch; i ++){
          sb.append("Itinerary "+itnum+": ");

          sb.append(result_array[i]);
          itnum++;
        }

        if(sb.toString().isEmpty()){
          //clearSortTables();
          return "No flights match your selection\n";
        }
        // clearSortTables();
        return sb.toString();
      }

    } catch (SQLException e) {
      e.printStackTrace();
    }

    //clearSortTables();

    return "Failed to search\n";
  }
  @SuppressWarnings("Duplicates")
  public void DirectFlight(String originCity, String destinationCity, int dayOfMonth,
                           int numberOfItineraries) throws SQLException{
    //"SELECT TOP(?) fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price " +
    //          "FROM Flights WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? ORDER BY actual_time ASC";

    directFlightStatement.setInt(1,numberOfItineraries);
    directFlightStatement.setString(2,originCity);
    directFlightStatement.setString(3,destinationCity);
    directFlightStatement.setInt(4,dayOfMonth);

    ResultSet oneHopResults = directFlightStatement.executeQuery();

    while (oneHopResults.next()) {
      int fid = oneHopResults.getInt("fid");
      int result_dayOfMonth = oneHopResults.getInt("day_of_month");
      String result_carrierId = oneHopResults.getString("carrier_id");
      String result_flightNum = oneHopResults.getString("flight_num");
      String result_originCity = oneHopResults.getString("origin_city");
      String result_destCity = oneHopResults.getString("dest_city");
      int result_time = oneHopResults.getInt("actual_time");
      int result_capacity = oneHopResults.getInt("capacity");
      int result_price = oneHopResults.getInt("price");


      String result = "1 flight(s), "+result_time+" minutes\nID: "+fid+" Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: " + result_flightNum
              + " Origin: " + result_originCity + " Dest: " + result_destCity + " Duration: " + result_time
              + " Capacity: " + result_capacity + " Price: " + result_price + "\n";

      //private static final String DIRECT_INSERT_SORTING = "INSERT INTO SORT (fid ,fid2, total_time, txt,username) VALUES (? , 0, ? , ?,?)";

      fid1_array[numOfsearch] = fid;
      fid2_array[numOfsearch] = 0;
      result_time_array[numOfsearch] = result_time;
      result_array[numOfsearch] = result;
      currentusername_array[numOfsearch] = currentUsername;

      numOfsearch++;
    }
  }

  @SuppressWarnings("Duplicates")
  public void InDirectFlight(String originCity, String destinationCity, int dayOfMonth,
                             int numberOfItineraries) throws SQLException{


    IdSearchStatement.setInt(1,numberOfItineraries - numOfsearch);
    IdSearchStatement.setString(2,originCity);
    IdSearchStatement.setString(3,destinationCity);
    IdSearchStatement.setInt(4,dayOfMonth);

    ResultSet twoHopResultsForID = IdSearchStatement.executeQuery();
    ArrayList<Integer> idarray = new ArrayList<Integer>();

    while(twoHopResultsForID.next()){

      int fid1 = twoHopResultsForID.getInt("f1_id");
      int fid2 = twoHopResultsForID.getInt("f2_id");
      idarray.add(fid1);
      idarray.add(fid2);
    }
    int totalTime = 0;
    String temp = "";
    String result ="";
    int sortedlistnumber = 0;
    int fidholder = 0;
    int fidholder2 = 0;
    for (int i = 0; i < idarray.size(); i++){

      //private static final String GET_INDIRECT_FLIGHT =
      // "SELECT DISTINCT fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price\n" +
      //          "        FROM FLIGHTS WHERE fid = ?";
      IndirectFlightStatement.setInt(1,idarray.get(i));
      ResultSet twoHopResults = IndirectFlightStatement.executeQuery();
      twoHopResults.next();
      if(i%2 == 0){
        totalTime = 0;
        temp = "";
      }

      int fid = twoHopResults.getInt("fid");
      int result_dayOfMonth = twoHopResults.getInt("day_of_month");
      String result_carrierId = twoHopResults.getString("carrier_id");
      String result_flightNum = twoHopResults.getString("flight_num");
      String result_originCity = twoHopResults.getString("origin_city");
      String result_destCity = twoHopResults.getString("dest_city");
      int result_time = twoHopResults.getInt("actual_time");
      int result_capacity = twoHopResults.getInt("capacity");
      int result_price = twoHopResults.getInt("price");
      totalTime = totalTime + result_time;

      if(i%2 == 0) {
        temp = temp + "ID: " + fid + " Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: " + result_flightNum
                + " Origin: " + result_originCity + " Dest: " + result_destCity + " Duration: " + result_time
                + " Capacity: " + result_capacity + " Price: " + result_price + "\n";
        fidholder = fid;
        fid1_array[numOfsearch] = fidholder;

        //InDirectSortStatement.setInt(1,fidholder);
      }
      if(i%2 !=0 ){
        result = "2 flight(s), " + totalTime + " minutes\n";
        temp = temp + "ID: " + fid + " Day: " + result_dayOfMonth + " Carrier: " + result_carrierId + " Number: " + result_flightNum
                + " Origin: " + result_originCity + " Dest: " + result_destCity + " Duration: " + result_time
                + " Capacity: " + result_capacity + " Price: " + result_price + "\n";
        fidholder2 = fid;

        sortedlistnumber++;
        result = result + temp;

        fid2_array[numOfsearch] = fidholder2;
        result_time_array[numOfsearch] = result_time;
        result_array[numOfsearch] = result;
        currentusername_array[numOfsearch] = currentUsername;

        numOfsearch++;
      }
      twoHopResults.close();
    }

  }




  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is
   *                    returned by search in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations,
   *         not logged in\n". If try to book an itinerary with invalid ID, then
   *         return "No such itinerary {@code itineraryId}\n". If the user already
   *         has a reservation on the same day as the one that they are trying to
   *         book now, then return "You cannot book two flights in the same
   *         day\n". For all other errors, return "Booking failed\n".
   *
   *         And if booking succeeded, return "Booked flight(s), reservation ID:
   *         [reservationId]\n" where reservationId is a unique number in the
   *         reservation system that starts from 1 and increments by 1 each time a
   *         successful reservation is made by any user in the system.
   */
  @SuppressWarnings("Duplicates")
  public String transaction_book(int itineraryId) {

    int capacityspace = 0;

    //Check itneray id and get fid1 and fid2
    // Go to booking table to check fid1 and fid2 both of them in order
    // and check capacity is not zero, if it has space, then check if capacity = capacity_taken
    // do it for also fid2 if fid2 != 0
    // if all good, then book and increment capacity_taken
    if(currentUsername == null){
      return "Cannot book reservations, not logged in\n";
    }

    try{

      if(itineraryId > numOfsearch){
        return "No such itinerary "+itineraryId+"\n";
      }

      int fid1 = fid1_array[itineraryId];
      int fid2 = fid2_array[itineraryId];
      int day1 = checkFlightDay(fid1);


      if(fid2 == 0){  // direct flight
        int capacity = checkFlightCapacity(fid1);
        int existDay = 0;
        //insert into booking table
        try {
          //private static final String CHECK_DAY_IN_BOOKING_RESERVATION = "SELECT rday FROM reservations WHERE rday = ? AND username ?";

          CheckForDayInReservationTable.setInt(1, day1);
          CheckForDayInReservationTable.setString(2, currentUsername);
          ResultSet CheckDay = CheckForDayInReservationTable.executeQuery();
          CheckDay.next();
          existDay = CheckDay.getInt("rday");
        }
        catch (SQLException e){

        }
        if (day1 == existDay){
          return "You cannot book two flights in the same day111\n";
        }


        if (capacity!=0){

          try{
            //check if booking is already added to fid


            //private static final String GET_CAPACITY_TAKEN = "SELECT count(*) FROM booking WHERE fid = ?";

            GetCapacityTakenStatement.setInt(1,fid1);
            ResultSet capacitySpaceResult = GetCapacityTakenStatement.executeQuery();
            capacitySpaceResult.next();
            int capacity_taken = capacitySpaceResult.getInt("total");
            capacitySpaceResult.close();

            if(capacity > capacity_taken){ // counted but has space


              insertDataIntoBookingStatement.setInt(1, fid1);
              insertDataIntoBookingStatement.setInt(2, capacity);
              insertDataIntoBookingStatement.setInt(3, day1);
              insertDataIntoBookingStatement.setString(4,currentUsername);
              insertDataIntoBookingStatement.executeUpdate();

              int price1 = checkFlightPrice(fid1);

              try {

                //check current rid number for currentuser
                //check current highest rid
                try {
                  //private static final String CURRENT_RID = "SELECT rid FROM reservations ORDER BY rid desc";
                  ResultSet ridResult = CurrentRidStatement.executeQuery();
                  ridResult.next();
                  rid = ridResult.getInt("rid");

                  rid++;
                }
                catch (SQLException e){

                }


                //private static final String INSERT_RESERVATION = "INSERT INTO Reservations (rid, username, price, paid , rday,fid1,fid2) VALUES( ? , ?, ?, ?, ?,?,?)";
                InsertReservationStatement.setInt(1, rid);
                InsertReservationStatement.setString(2, currentUsername);
                InsertReservationStatement.setInt(3, price1);
                InsertReservationStatement.setBoolean(4, false);
                InsertReservationStatement.setInt(5, day1);
                InsertReservationStatement.setInt(6, fid1);
                InsertReservationStatement.setInt(7, 0);
                InsertReservationStatement.executeUpdate();

                //INSERT_RESERVATION = "INSERT INTO Reservations (rid, username, price, paid , rday,fid1,fid2) VALUES( ? , ?, ?, ?, ?,?,?)";
                return "Booked flight(s), reservation ID: "+(rid++)+"\n";
              }
              catch(SQLException e){
                //ClearBookingTable();
                //clearSortedTable();
                e.printStackTrace();
                return "You cannot book two flights in the same day333\n";
              }
              //If the user already has a reservation on the same day as the one that they are trying to
              // book now, then return
              //"You cannot book two flights in the same day\n"

              //TODO reservation ID
            }
          }
          catch(SQLException e){

          }


        }
        else{
          // CAPACITY == 0;
          return "Booking failed\n";
        }

      }
      else{ // two hop flight
        int capacity1 = checkFlightCapacity(fid1);
        int capacity2 = checkFlightCapacity(fid2);

        if (capacity1!=0 && capacity2!=0){


          //insert into booking table
          try {

            //private static final String INSERT_INTO_BOOKING_TABLE = "INSERT INTO BOOKING (fid, capacity,capacity_taken,bday,username) VALUES(? , ?,0,?,?)";

            insertDataIntoBookingStatement.setInt(1, fid1);
            insertDataIntoBookingStatement.setInt(2, capacity1);
            insertDataIntoBookingStatement.setInt(3, day1);
            insertDataIntoBookingStatement.setString(4,currentUsername);
            insertDataIntoBookingStatement.executeUpdate();
            insertDataIntoBookingStatement.setInt(1, fid2);
            insertDataIntoBookingStatement.setInt(2, capacity2);
            insertDataIntoBookingStatement.setInt(3, day1);
            insertDataIntoBookingStatement.setString(4,currentUsername);
            insertDataIntoBookingStatement.executeUpdate();
          }
          catch (SQLException e){
            return "You cannot book two flights in the same day444\n";
          }


          //get capacity space for each fid
          GetCapacityTakenStatement.setInt(1,fid1);
          ResultSet capacitySpaceResult = GetCapacityTakenStatement.executeQuery();
          capacitySpaceResult.next();
          int capacity_taken1 = capacitySpaceResult.getInt("capacity_taken");
          capacitySpaceResult.close();

          //get capacity space for each fid
          GetCapacityTakenStatement.setInt(1,fid2);
          ResultSet capacitySpaceResult2 = GetCapacityTakenStatement.executeQuery();
          capacitySpaceResult2.next();
          int capacity_taken2 = capacitySpaceResult2.getInt("capacity_taken");
          capacitySpaceResult2.close();

          // update the capacity_taken if it has capacity
          if(capacity_taken1 < capacity1 && capacity_taken2 < capacity2){

            UpdateCapacityTakenStatement.setInt(1,capacity_taken1+1);
            UpdateCapacityTakenStatement.setInt(2,fid1);
            UpdateCapacityTakenStatement.executeUpdate();
            UpdateCapacityTakenStatement.setInt(1,capacity_taken2+1);
            UpdateCapacityTakenStatement.setInt(2,fid2);
            UpdateCapacityTakenStatement.executeUpdate();

            // get price information from fid 1 and fid 2
            // update the sum of the price information
            // update the reservation table with the total price, with the text
            int price1 = checkFlightPrice(fid1);
            int price2 = checkFlightPrice(fid2);


            int priceSum = price1 + price2;
            try{
              try {
                CurrentRidStatement.setString(1, currentUsername);
                ResultSet ridResult = CurrentRidStatement.executeQuery();
                ridResult.next();
                rid = ridResult.getInt("rid");

                rid++;
              }
              catch (SQLException e){

              }
              InsertReservationStatement.setInt(1,rid);
              InsertReservationStatement.setString(2,currentUsername);
              InsertReservationStatement.setInt(3,priceSum);
              InsertReservationStatement.setBoolean(4,false);
              InsertReservationStatement.setInt(5,day1);
              InsertReservationStatement.setInt(6, fid1);
              InsertReservationStatement.setInt(7, fid2);
              InsertReservationStatement.executeUpdate();

              return "Booked flight(s), reservation ID: "+(rid++)+"\n";
            }
            catch(SQLException e){
              return "You cannot book two flights in the same day555\n";
            }
          }
        }

      }

    }
    catch (SQLException e){
      e.printStackTrace();
    }

    return "Booking failed\n";
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n"
   *         If the reservation is not found / not under the logged in user's
   *         name, then return "Cannot find unpaid reservation [reservationId]
   *         under user: [username]\n" If the user does not have enough money in
   *         their account, then return "User has only [balance] in account but
   *         itinerary costs [cost]\n" For all other errors, return "Failed to pay
   *         for reservation [reservationId]\n"
   *
   *         If successful, return "Paid reservation: [reservationId] remaining
   *         balance: [balance]\n" where [balance] is the remaining balance in the
   *         user's account.
   */
  public String transaction_pay(int reservationId) {


    // pay allows a user to pay for an existing reservation.
    // It first checks whether the user has enough money to pay for all the flights in the given reservation.
    // If successful, it updates the reservation to be paid.

    if(currentUsername == null){
      return "Cannot pay, not logged in\n";
    }

    try{
      //check if reservation exist


      ListOfReservationIdForUser.setString(1, currentUsername);
      ListOfReservationIdForUser.setInt(2, reservationId);
      ResultSet result = ListOfReservationIdForUser.executeQuery();

      //private static final String CHECK_PAID_STATUS = "SELECT paid FROM reservations WHERE username = ? AND rid = ?";

//        CheckPaidStatusStatement.setString(1,currentUsername);
//        CheckPaidStatusStatement.setInt(2,reservationId);
      //CHECK_UNPAID_RESERVATIONS = "SELECT paid FROM reservations WHERE username = ? AND rid = ? AND paid = ?";

      CheckUnpaidReservationStatement.setInt(1,reservationId);
      CheckUnpaidReservationStatement.setBoolean(2,false);


      try {
        // CHECK_UNPAID_RESERVATIONS = "SELECT paid FROM reservations WHERE username = ? AND rid = ? AND paid = ?";

        ResultSet unpaidLeft = CheckUnpaidReservationStatement.executeQuery();
        //ResultSet unpaidLeft = CheckPaidStatusStatement.executeQuery();
        unpaidLeft.next();
        boolean unpaidResult = unpaidLeft.getBoolean("paid");


      }
      catch (SQLException e){
        return "Cannot find unpaid reservation "+reservationId+" under user: "+currentUsername+"\n";
      }

      while (result.next()) {
        int price = result.getInt("price");
        //check user balance if he has enough money to pay


        UserBalanceStatement.setString(1,currentUsername);
        ResultSet balanceResult = UserBalanceStatement.executeQuery();
        balanceResult.next();

        int balance = balanceResult.getInt("balance");



        if(balance < price){

          //User has only [balance] in account but itinerary costs [cost]\n
          return "User has only "+balance+" in account but itinerary costs "+price+"\n";
        }
        else{
          //PayStatusStatement = if enough then update the new balance


          PayStatusStatement.setBoolean(1,true);
          PayStatusStatement.setString(2,currentUsername);
          PayStatusStatement.setInt(3,reservationId);
          PayStatusStatement.executeUpdate();
          //private static final String PAY_STATUS = "UPDATE reservations SET paid = ? WHERE username = ? AND rid = ?";
          //"Paid reservation: [reservationId] remaining balance: [balance]\n"

          SetNewBalanceStatement.setInt(1,balance-price);
          SetNewBalanceStatement.setString(2,currentUsername);
          SetNewBalanceStatement.executeUpdate();
          //private static final String SET_NEW_BALANCE = "UPDATE USERS SET balance = ? WHERE username = ?";
          //update user balance

          return "Paid reservation: "+reservationId+" remaining balance: "+(balance-price)+"\n";
        }

      }

    }catch(SQLException e){

      return "Cannot find unpaid reservation "+reservationId+" under user: "+currentUsername+"\n";
    }

    return "Failed to pay for reservation " + reservationId + "\n";
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not
   *         logged in\n" If the user has no reservations, then return "No
   *         reservations found\n" For all other errors, return "Failed to
   *         retrieve reservations\n"
   *
   *         Otherwise return the reservations in the following format:
   *
   *         Reservation [reservation ID] paid: [true or false]:\n" [flight 1
   *         under the reservation] [flight 2 under the reservation] Reservation
   *         [reservation ID] paid: [true or false]:\n" [flight 1 under the
   *         reservation] [flight 2 under the reservation] ...
   *
   *         Each flight should be printed using the same format as in the
   *         {@code Flight} class.
   *
   * @see Flight#toString()
   */

  @SuppressWarnings("Duplicates")
  public String transaction_reservations() {
    if(currentUsername == null){
      return "Cannot view reservations, not logged in\n";
    }

    try {
      // check reservation table, and get flight id
      // with the flight id, check flights table to bring all other data
      // user flight class to return the result
      //String CHECK_FLIGHT_INFO_FOR_RESERVATION = "SELECT rid, paid, fid1, fid2 FROM reservations WHERE username = ?";



      sbReservation = new StringBuffer();
      CheckInfoForReservation.setString(1,currentUsername);
      ResultSet result = CheckInfoForReservation.executeQuery();

      while(result.next()){
        int rid = result.getInt("rid");
        boolean paid = result.getBoolean("paid");
        int fid1 = result.getInt("fid1");
        int fid2 = result.getInt("fid2");

        String FirstLine = "Reservation "+rid+" paid: "+paid+":\n";
        Flight FlightInfo = new Flight(fid1);
        String SecondLine = FlightInfo.toString()+"\n";

        if(fid2 == 0){
          sbReservation.append(FirstLine+SecondLine);
        }
        else{
          FlightInfo = new Flight(fid2);
          String ThirdLine = FlightInfo.toString()+"\n";
          sbReservation.append(FirstLine+SecondLine+ThirdLine);
        }
      }

      if (sbReservation.toString().isEmpty()){
        return "No reservations found\n";
      }
      return sbReservation.toString();
    }
    catch(SQLException e){
      e.printStackTrace();
    }

    return "Failed to retrieve reservations\n";
  }


  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations,
   *         not logged in\n" For all other errors, return "Failed to cancel
   *         reservation [reservationId]\n"
   *
   *         If successful, return "Canceled reservation [reservationId]\n"
   *
   *         Even though a reservation has been canceled, its ID should not be
   *         reused by the system.
   */
  public String transaction_cancel(int reservationId) {
    if(currentUsername == null){
      return "Cannot cancel reservations, not logged in\n";
    }
    try {
      //private static final String LIST_RESERVATION_ID = " SELECT rid, price, paid FROM reservations WHERE username = ? AND rid = ?";

      ListOfReservationIdForUser.setString(1, currentUsername);
      ListOfReservationIdForUser.setInt(2, reservationId);
      ResultSet result = ListOfReservationIdForUser.executeQuery();

      result.next();
      int price = result.getInt("price");
      Boolean paidStatus = result.getBoolean("paid");
      result.close();
      if(paidStatus ==true){
        int balance = checkUserBalance(currentUsername);

        SetNewBalanceStatement.setInt(1,balance+price);
        SetNewBalanceStatement.setString(2,currentUsername);
        SetNewBalanceStatement.executeUpdate();
//
        DeleteReservationStatement.setInt(1,reservationId);
        DeleteReservationStatement.executeUpdate();

        //update reservation table , change paid status to false;
        return "Canceled reservation "+reservationId+"\n";
      }
      else{
        DeleteReservationStatement.setInt(1,reservationId);
        DeleteReservationStatement.executeUpdate();

        return "Canceled reservation "+reservationId+"\n";
      }

    }
    catch(SQLException e){

    }

    // TODO: YOUR CODE HERE
    return "Failed to cancel reservation " + reservationId + "\n";
  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }
  private int checkFlightDay(int fid) throws SQLException {
    checkFlightDayStatement.clearParameters();
    checkFlightDayStatement.setInt(1, fid);
    ResultSet results = checkFlightDayStatement.executeQuery();
    results.next();
    int day = results.getInt("day_of_month");
    results.close();

    return day;
  }
  private int checkFlightPrice(int fid) throws SQLException {
    checkFlightPriceStatement.clearParameters();
    checkFlightPriceStatement.setInt(1, fid);
    ResultSet results = checkFlightPriceStatement.executeQuery();
    results.next();
    int price = results.getInt("price");
    results.close();

    return price;
  }
  private int checkUserBalance(String currentUsername) {
    try {
      UserBalanceStatement.setString(1, currentUsername);
      ResultSet balanceResult = UserBalanceStatement.executeQuery();
      balanceResult.next();

      int balance = balanceResult.getInt("balance");
      return balance;
    }
    catch(SQLException e){
      return 0;
    }

  }

  public byte[] generateSalt(){
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);
    return salt;
  }
  public byte[] generateHash(KeySpec spec){
    SecretKeyFactory factory = null;
    byte[] hash = null;
    try {
      factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      hash = factory.generateSecret(spec).getEncoded();
    }
    catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new IllegalStateException();
    }
    return hash;
  }

  /**
   * A class to store flight information.
   */
  class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;
    Flight(){
      this.fid = fid;
      this.dayOfMonth = dayOfMonth;
      this.carrierId = carrierId;
      this.flightNum = flightNum;
      this.originCity = originCity;
      this.destCity = destCity;
      this.time = time;
      this.capacity = capacity;
      this.price = price;
    }
    @SuppressWarnings("Duplicates")
    Flight(int fid){
      try {

        FlightInfoStatement.setInt(1,fid);
        ResultSet Flight_Info = FlightInfoStatement.executeQuery();
        Flight_Info.next();
        this.fid = Flight_Info.getInt("fid");
        this.dayOfMonth = Flight_Info.getInt("day_of_month");
        this.carrierId = Flight_Info.getString("carrier_id");
        this.flightNum = Flight_Info.getString("flight_num");
        this.originCity = Flight_Info.getString("origin_city");
        this.destCity = Flight_Info.getString("dest_city");
        this.time = Flight_Info.getInt("actual_time");
        this.capacity = Flight_Info.getInt("capacity");
        this.price = Flight_Info.getInt("price");
        Flight_Info.close();
      }
      catch (SQLException e){

      }
    }

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: " + flightNum + " Origin: "
              + originCity + " Dest: " + destCity + " Duration: " + time + " Capacity: " + capacity + " Price: " + price;
    }
  }

}