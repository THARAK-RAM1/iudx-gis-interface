package iudx.gis.server.apiserver.response;

import java.util.stream.Stream;

public enum ResponseUrn {
  SUCCESS("urn:dx:rs:success", "Successful operation"),
  INVALID_ATTR_PARAM("urn:dx:rs:invalidAttributeParam", "Invalid attribute param"),
  INVALID_ATTR_VALUE("urn:dx:rs:invalidAttributeValue", "Invalid attribute value"),
  INVALID_OPERATION("urn:dx:rs:invalidOperation", "Invalid operation"),
  BAD_REQUEST_URN("urn:dx:rs:badRequest","bad request parameter"),
  UNAUTHORIZED_ENDPOINT("urn:dx:rs:unauthorizedEndpoint", "Access to endpoint is not available"),
  UNAUTHORIZED_RESOURCE("urn:dx:rs:unauthorizedResource", "Access to resource is not available"),
  EXPIRED_TOKEN("urn:dx:rs:expiredAuthorizationToken", "Token has expired"),
  MISSING_TOKEN("urn:dx:rs:missingAuthorizationToken", "Token needed and not present"),
  INVALID_TOKEN("urn:dx:rs:invalidAuthorizationToken", "Token is invalid"),

  RESOURCE_ALREADY_EXISTS("urn:dx:rs:resourceAlreadyExists", "Document of given ID already exists"),

  INVALID_PAYLOAD_FORMAT("urn:dx:rs:invalidPayloadFormat", "Invalid json format in post request [schema mismatch]"),
  RESOURCE_NOT_FOUND("urn:dx:rs:resourceNotFound", "Document of given id does not exist"),
  METHOD_NOT_FOUND("urn:dx:rs:MethodNotAllowed", "Method not allowed for given endpoint"),
  UNSUPPORTED_MEDIA_TYPE("urn:dx:rs:UnsupportedMediaType", "Requested/Presented media type not supported"),

  RESPONSE_PAYLOAD_EXCEED("urn:dx:rs:responsePayloadLimitExceeded", "Search operations exceeds the default response payload limit"),
  REQUEST_PAYLOAD_EXCEED("urn:dx:rs:requestPayloadLimitExceeded", "Operation exceeds the default request payload limit"),
  REQUEST_OFFSET_EXCEED("urn:dx:rs:requestOffsetLimitExceeded", "Operation exceeds the default value of offset"),
  REQUEST_LIMIT_EXCEED("urn:dx:rs:requestLimitExceeded", "Operation exceeds the default value of limit"),

  BACKING_SERVICE_FORMAT("urn:dx:rs:backend", "format error from backing service [cat,auth etc.]"),

  YET_NOT_IMPLEMENTED("urn:dx:rs:general", "urn yet not implemented in backend verticle.");

  private final String urn;
  private final String message;

  ResponseUrn(String urn, String message) {
    this.urn = urn;
    this.message = message;
  }

  public String getUrn() {
    return urn;
  }

  public String getMessage() {
    return message;
  }

  public static ResponseUrn fromCode(final String urn) {
    return Stream.of(values())
        .filter(v -> v.urn.equalsIgnoreCase(urn))
        .findAny()
        .orElse(YET_NOT_IMPLEMENTED); /* If backend services don't respond with URN */
  }

  public String toString() {
    return "[" + urn + " : " + message + " ]";
  }
}
