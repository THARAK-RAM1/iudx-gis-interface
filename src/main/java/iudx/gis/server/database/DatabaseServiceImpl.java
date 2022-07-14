package iudx.gis.server.database;

import static iudx.gis.server.database.util.Constants.ACCESS_INFO;
import static iudx.gis.server.database.util.Constants.DELETE_ADMIN_DETAILS_QUERY;
import static iudx.gis.server.database.util.Constants.DETAIL;
import static iudx.gis.server.database.util.Constants.ID;
import static iudx.gis.server.database.util.Constants.INSERT_ADMIN_DETAILS_QUERY;
import static iudx.gis.server.database.util.Constants.PASSWORD;
import static iudx.gis.server.database.util.Constants.SECURE;
import static iudx.gis.server.database.util.Constants.SELECT_ADMIN_DETAILS_QUERY;
import static iudx.gis.server.database.util.Constants.SERVER_PORT;
import static iudx.gis.server.database.util.Constants.SERVER_URL;
import static iudx.gis.server.database.util.Constants.SUCCESS;
import static iudx.gis.server.database.util.Constants.TOKEN_URL;
import static iudx.gis.server.database.util.Constants.UPDATE_ADMIN_DETAILS_QUERY;
import static iudx.gis.server.database.util.Constants.USERNAME;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.gis.server.apiserver.response.ResponseUrn;
import iudx.gis.server.apiserver.util.HttpStatusCode;
import iudx.gis.server.database.util.Util;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseServiceImpl implements DatabaseService {

  public static final String SELECT_GIS_SERVER_URL =
      "SELECT * FROM gis WHERE iudx_resource_id='$1'";
  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  private final PostgresClient pgSQLClient;

  public DatabaseServiceImpl(PostgresClient pgClient) {
    // TODO Auto-generated constructor stub
    this.pgSQLClient = pgClient;
  }

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Future<JsonObject> getGISURL = getURLInDb(request.getString("id"));
    getGISURL.onComplete(getUserApiKeyHandler -> {
      if (getUserApiKeyHandler.succeeded()) {
        LOGGER.debug("DATABASE_READ_SUCCESS");
        handler.handle(Future.succeededFuture(getUserApiKeyHandler.result()));
      } else {
        LOGGER.debug("DATABASE_READ_FAILURE");
      }
    });
    return this;
  }

  @Override
  public DatabaseService insertIntoDb(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    handler.handle(Future.succeededFuture(new JsonObject().put("a", "b")));
    return this;
  }

  @Override
  public DatabaseService insertAdminDetails(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String resourceId = request.getString(ID);
    String serverUrl = request.getString(SERVER_URL);
    Long serverPort = request.getLong(SERVER_PORT);
    Boolean isSecure = request.getBoolean(SECURE);

    Optional<JsonObject> accessInfo = Optional.ofNullable(request.getJsonObject(ACCESS_INFO));

    if (isRequestInvalid(isSecure, accessInfo, handler)) {
      return this;
    }

    String selectQuery = SELECT_ADMIN_DETAILS_QUERY.replace("$1", resourceId);
    String insertQuery = INSERT_ADMIN_DETAILS_QUERY.replace("$1", resourceId)
        .replace("$2", serverUrl)
        .replace("$3", serverPort.toString())
        .replace("$4", isSecure.toString());

    if (accessInfo.isPresent() && !accessInfo.get().isEmpty()) {
      JsonObject accessObject = accessInfo.get();
      String username = accessObject.getString(USERNAME);
      String password = accessObject.getString(PASSWORD);
      String tokenUrl=accessObject.getString(TOKEN_URL);
      insertQuery = insertQuery.replace("$5", username).replace("$6", password).replace("$7",tokenUrl);
    } else {
      insertQuery = insertQuery.replace("$5", "").replace("$6", "").replace("$7","");
    }

    String finalInsertQuery = insertQuery;
    pgSQLClient.executeAsync(selectQuery)
        .compose(ar -> {
          if (ar.size() >= 1) {
            return Future.failedFuture(Util.getResponse(
                HttpStatusCode.CONFLICT,
                ResponseUrn.RESOURCE_ALREADY_EXISTS.getUrn(),
                "Insert operation failed because given resource ID already exists in the DB")
                .toString()
            );
          }
          return pgSQLClient.executeAsync(finalInsertQuery);
        })
        .onSuccess(ar -> {
          LOGGER.debug("Insert admin details operation successful");
          handler.handle(Future.succeededFuture(new JsonObject().put(DETAIL, SUCCESS)));
        })
        .onFailure(ar -> {
          LOGGER.error("Insert admin operation failed due to: {}", ar.getLocalizedMessage());
          handler.handle(Future.failedFuture(ar));
        });

    return this;
  }

  @Override
  public DatabaseService updateAdminDetails(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String resourceId = request.getString(ID);
    String serverUrl = request.getString(SERVER_URL);
    Long serverPort = request.getLong(SERVER_PORT);
    Boolean isSecure = request.getBoolean(SECURE);

    Optional<JsonObject> accessInfo = Optional.ofNullable(request.getJsonObject(ACCESS_INFO));

    if (isRequestInvalid(isSecure, accessInfo, handler)) {
      return this;
    }
    String searchQuery = SELECT_ADMIN_DETAILS_QUERY.replace("$1", resourceId);
    String updateQuery = UPDATE_ADMIN_DETAILS_QUERY.replace("$1", serverUrl)
        .replace("$2", serverPort.toString())
        .replace("$3", isSecure.toString())
        .replace("$6", resourceId);

    if (accessInfo.isPresent() && !accessInfo.get().isEmpty()) {
      JsonObject accessObject = accessInfo.get();
      String username = accessObject.getString(USERNAME);
      String password = accessObject.getString(PASSWORD);
      String tokenUrl=accessObject.getString(TOKEN_URL);
      updateQuery = updateQuery.replace("$4", username).replace("$5", password).replace("$7",tokenUrl);;
    } else {
      updateQuery = updateQuery.replace("$4", "").replace("$5", "").replace("$7", "");
    }

    String finalUpdateQuery = updateQuery;
    pgSQLClient.executeAsync(searchQuery)
        .compose(ar -> {
          if (ar.size() < 1) {
            return Future.failedFuture(Util.getResponse(
                HttpStatusCode.NOT_FOUND,
                ResponseUrn.RESOURCE_NOT_FOUND.getUrn(),
                "Update operation failed because passed resource ID does not exist in DB")
                .toString()
            );
          }
          return pgSQLClient.executeAsync(finalUpdateQuery);
        })
        .onSuccess(ar -> {
          LOGGER.debug("Update admin details operation successful!");
          handler.handle(Future.succeededFuture(new JsonObject().put(DETAIL, SUCCESS)));
        })
        .onFailure(ar -> {
          LOGGER.error("Update admin operation failed due to: {}", ar.getLocalizedMessage());
          handler.handle(Future.failedFuture(ar));
        });

    return this;
  }

  @Override
  public DatabaseService deleteAdminDetails(
      String resourceId, Handler<AsyncResult<JsonObject>> handler) {
    String searchQuery = SELECT_ADMIN_DETAILS_QUERY.replace("$1", resourceId);
    String deleteQuery = DELETE_ADMIN_DETAILS_QUERY.replace("$1", resourceId);

    pgSQLClient.executeAsync(searchQuery)
        .compose(ar -> {
          if (ar.size() < 1) {
            return Future.failedFuture(Util.getResponse(
                HttpStatusCode.NOT_FOUND,
                ResponseUrn.RESOURCE_NOT_FOUND.getUrn(),
                "Given resource ID does not exist in DB")
                .toString()
            );
          }
          return pgSQLClient.executeAsync(deleteQuery);
        })
        .onSuccess(ar -> {
          LOGGER.debug("Delete admin details operation successful!");
          handler.handle(Future.succeededFuture(new JsonObject().put(DETAIL, SUCCESS)));
        })
        .onFailure(ar -> {
          LOGGER.error("Delete admin operation failed due to: {}", ar.getLocalizedMessage());
          handler.handle(Future.failedFuture(ar));
        });

    return this;
  }

  Future<JsonObject> getURLInDb(String id) {

    LOGGER.trace("Info : PSQLClient#getUserInDb() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject response = new JsonObject();
    String query = SELECT_GIS_SERVER_URL.replace("$1", id);
    // Check in DB, get username and password
    pgSQLClient.executeAsync(query).onComplete(db -> {
      LOGGER.debug("Info : PSQLClient#getUserInDb()executeAsync completed");
      if (db.succeeded()) {
        LOGGER.debug("Info : PSQLClient#getUserInDb()executeAsync success");
        String url = null;
        // Get the apiKey
        RowSet<Row> result = db.result();
        if (db.result().size() > 0) {
          for (Row row : result) {
            LOGGER.debug("URL: "+row.getString(1));
            url = row.getString(1);
          }
        }
        response.put("URL", url);
        promise.complete(response);
      } else {
        LOGGER.fatal("Fail : PSQLClient#getUserInDb()executeAsync failed");
        promise.fail("Error : Get ID from database failed");
      }
    });
    return promise.future();
  }

  private boolean isRequestInvalid(
      Boolean isSecure, Optional<JsonObject> accessInfo, Handler<AsyncResult<JsonObject>> handler) {
    if (!isSecure) {
      return false;
    }
    String errorMessage = "";
    if (accessInfo.isPresent() && !accessInfo.get().isEmpty()) {
      JsonObject accessObject = accessInfo.get();
      String username = accessObject.getString(USERNAME);
      String password = accessObject.getString(PASSWORD);
      String tokenUrl=accessObject.getString(TOKEN_URL);
      if (username==null || username.isEmpty() || password==null || password.isEmpty() || tokenUrl==null || tokenUrl.isEmpty()) {
        errorMessage = "Access Info cannot contain empty fields.";
      } else {
        return false;
      }
    } else {
      errorMessage = "Access Info cannot be an empty object";
    }
    handler.handle(Future.failedFuture(Util.getResponse(
        HttpStatusCode.BAD_REQUEST,
        ResponseUrn.INVALID_PAYLOAD_FORMAT.getUrn(),
        errorMessage)
        .toString()
    ));
    return true;
  }
}
